import light.HSBColor
import light.LightController
import networking.MCUType
import nio.Manager
import nio.NonBlockingServer
import networking.MCUClient
import util.Logger
import java.io.FileWriter
import java.io.PrintWriter
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.SocketChannel
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

open class Server(private val address: String, port: Int, private val manager: Manager, private val knownClients: ArrayList<String>) : NonBlockingServer(port) {

    private val interactiveMCUs = HashMap<String, MCUClient>()
    private val passiveMCUs = HashMap<String, MCUClient>()
    private val pendingRequests = HashMap<String, (String, String, MCUType) -> Unit>()

    private val requiredMCUConfigs = ArrayList<String>()
    private val isProcessingMCUs = AtomicBoolean(false)

    fun init() {
        knownClients.forEach { client ->
            try {
                val channel = SocketChannel.open()
                channel.connect(InetSocketAddress(client, 4442))
                val bytes = address.toByteArray()
                val buffer = ByteBuffer.allocate(bytes.size + 4)
                buffer.putInt(bytes.size)
                buffer.put(bytes)
                buffer.rewind()
                channel.write(buffer)
            } catch (e: Exception) {
                Logger.warn("FAILED CONNECTION WITH $client")
            }
        }
    }

    override fun onAccept(channel: SocketChannel) {
        val channelString = channel.toString()
        val startIndex = channelString.lastIndexOf('/') + 1
        val endIndex = channelString.lastIndexOf(':')

        val address = channelString.substring(startIndex, endIndex)

        val newMCU = MCUClient(channel, address, ::onReadCallback)
        manager.register(newMCU)

        if (!interactiveMCUs.containsKey(address) && !knownClients.contains(address)) {
            addToFile(address)
        }

        interactiveMCUs[address] = newMCU
    }

    private fun sendCommandToClients(command: String = "get_configuration", requiredMCU: MCUType = MCUType.UNKNOWN): String {
        val configs = ArrayList<String>()

        while (isProcessingMCUs.get()) {}

        isProcessingMCUs.set(true)

        interactiveMCUs.forEach { client ->
            val id = "${System.nanoTime().toInt()}_${client.value.address}"

            if (client.value.type == requiredMCU) {
                client.value.write("id=$id;$command")
            } else {
                client.value.write("id=$id;get_configuration")
            }
            requiredMCUConfigs.add(client.value.address)
            pendingRequests[id] = { message, address, type ->
                if (requiredMCUConfigs.contains(address)) {
                    configs.add("$type:[$message]")
                    requiredMCUConfigs.remove(address)
                }
            }
        }

        configs.clear()

        while (requiredMCUConfigs.isNotEmpty()) {
            Thread.sleep(1)
        }

        isProcessingMCUs.set(false)

        return configs.joinToString(",", "", "", -1, "", null)
    }

    fun processCommand(message: String): String {
        if (message == "get_configuration") {
            val configs = sendCommandToClients()
            return "{${Main.ROOM_NAME}: $configs}"
        }

        if (message == "get_all_configurations") {
            var configs = sendCommandToClients()
            passiveMCUs.forEach { (_, client) -> configs += " ${client.type} " }
            return "{${Main.ROOM_NAME}: $configs}"
        }

        val messageInfo = message.split('|')
        val mcuType = messageInfo[0].trim()
        val data = messageInfo[1].trim()

        val requiredMCU = if (mcuType.contains("led_strip")) {
            MCUType.LED_STRIP_CONTROLLER
        } else if (mcuType.contains("pc") || mcuType.contains("socket")) {
            MCUType.PC_CONTROLLER
        } else if (mcuType.contains("tv")) {
            MCUType.TV_CONTROLLER
        } else if (mcuType.contains("shutters")) {
            MCUType.SHUTTER_CONTROLLER
        } else if (mcuType.contains("phone")) {
            MCUType.PHONE
        } else {
            MCUType.UNKNOWN
        }

        val configs = sendCommandToClients(data, requiredMCU)
        return "{${Main.ROOM_NAME}: $configs}"
    }

    private fun onReadCallback(message: String, address: String, type: MCUType) {

        val client = interactiveMCUs.entries.find { (_, client) -> client.type == type } ?.component2()
                ?: passiveMCUs.entries.find { (_, client) -> client.type == type } ?.component2()
                ?: return

        if (message.contains("id")) {
            val startIndex = message.indexOf("id=") + 3
            val endIndex = message.indexOf(';')

            val id = message.substring(startIndex, endIndex)

            val content = message.substring(endIndex + 1)

            if (pendingRequests.containsKey(id)) {
                pendingRequests[id]?.invoke(content, client.address, client.type)
                pendingRequests.remove(id)
            }
        } else if (message.contains("Type: ") && client.type == MCUType.UNKNOWN) {
            client.type = MCUType.fromString(message)
            if (client.type == MCUType.SHUTTER_BUTTONS) {
                passiveMCUs[address] = interactiveMCUs[address] ?: return
                passiveMCUs[address] = interactiveMCUs.remove(address) ?: return
            }
            if (client.type == MCUType.LED_STRIP_CONTROLLER) {
                val id = "${System.nanoTime().toInt()}_${client.address}"
                client.write("id=$id;strip=1;r=235.0, g=20.0, b=46.0\\strip=2;r=235.0, g=20.0, b=46.0")
            }
        } else {
            if (client.type == MCUType.SHUTTER_BUTTONS) {
                interactiveMCUs.forEach { (_, client) ->
                    if (client.type == MCUType.SHUTTER_CONTROLLER) {
                        val id = "${System.nanoTime().toInt()}_${client.address}"
                        client.write("id=$id;$message")
                    }
                }
            }

            if (client.type == MCUType.SHUTTER_CONTROLLER) {
                passiveMCUs.forEach { (_, client) ->
                    if (client.type == MCUType.SHUTTER_BUTTONS) {
                        client.write(message)
                    }
                }
            }

            if (client.type == MCUType.PRESENCE_DETECTOR) {
                if (message.contains("occupied")) {
                    if (Calendar.getInstance().get(Calendar.HOUR_OF_DAY) < 7) {
                        LightController.setState(4, true, HSBColor(8381.0f, 141.0f, 77.0f))
                    } else {
                        LightController.setState(4, true, HSBColor(8404.0f, 140.0f, 254.0f))
                    }
                } else if (message.contains("empty")) {
                    LightController.setState(4, false, HSBColor(0.0f, 0.0f, 0.0f))
                }
            }
        }

        Logger.info("Message: $message")
    }

    private fun addToFile(connection: String) {
        val printWriter = PrintWriter(FileWriter("connections.txt", true))
        printWriter.println(connection)
        printWriter.close()
    }
}
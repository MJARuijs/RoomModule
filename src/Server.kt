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

    private var configs = ArrayList<String>()
    private val requiredMCUConfigs = ArrayList<String>()

    private val pendingRequests = HashMap<String, (String, String, MCUType) -> Unit>()

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
//                channel.close()
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
        Logger.debug("Address: $address")

        val newMCU = MCUClient(channel, address, ::onReadCallback)
        manager.register(newMCU)

        if (!interactiveMCUs.containsKey(address) && !knownClients.contains(address)) {
            addToFile(address)
        }

        interactiveMCUs[address] = newMCU
    }

    fun processCommand(message: String): String {
        if (message == "get_configuration") {

            while (isProcessingMCUs.get()) {}

            isProcessingMCUs.set(true)
            interactiveMCUs.forEach { client ->
//                println("AAADDING ${client.value.address} TO REQUIRED MCUS")
                val id = "${System.nanoTime().toInt()}_${client.value.address}"
                client.value.write("id=$id;get_configuration")
                requiredMCUConfigs.add(client.value.address)

                pendingRequests[id] = { message, address, type ->
                    if (requiredMCUConfigs.contains(address)) {
                        configs.add("$type:[$message]")
//                        println("REEEQUIRED: $address ::: $message")
                        requiredMCUConfigs.remove(address)
                    }
                }
            }

            configs.clear()

            while (requiredMCUConfigs.isNotEmpty()) {
                Thread.sleep(1)
            }

            isProcessingMCUs.set(false)

//            println("${Main.ROOM}|Config: ${configs.joinToString(",", "", "", -1, "", null)}")

            return "${Main.ROOM} Config: " + configs.joinToString(",", "", "", -1, "", null)
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

        while (isProcessingMCUs.get()) {}

        isProcessingMCUs.set(true)

        interactiveMCUs.forEach { client ->
//            println("ADDING ${client.value.address} TO REQUIRED MCUS")
            val id = "${System.nanoTime().toInt()}_${client.value.address}"

            if (client.value.type == requiredMCU) {
                client.value.write("id=$id;$data")
            } else {
                client.value.write("id=$id;get_configuration")
            }
            requiredMCUConfigs.add(client.value.address)
            pendingRequests[id] = { message, address, type ->
                if (requiredMCUConfigs.contains(address)) {
                    configs.add("$type:[$message]")
//                    println("REQUIRED: $address ::: $message")
                    requiredMCUConfigs.remove(address)
                }
            }
        }

        configs.clear()

        while (requiredMCUConfigs.isNotEmpty()) {
            Thread.sleep(1)
        }

        isProcessingMCUs.set(false)

//        println("Message was: $message")
//        println("${Main.ROOM}|Config: ${configs.joinToString(",", "", "", -1, "", null)}")

        return "${Main.ROOM} Config: " + configs.joinToString(",", "", "", -1, "", null)
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
//                println("MESSAGE ID: $id. CONTENT: $content")
                pendingRequests[id]?.invoke(content, client.address, client.type)
                pendingRequests.remove(id)
//                println("REMOVED REQUEST")
            }
        } else if (message.contains("Type: ") && client.type == MCUType.UNKNOWN) {
            client.type = MCUType.fromString(message)
            if (client.type == MCUType.SHUTTER_CONTROLLER || client.type == MCUType.SHUTTER_BUTTONS) {
                passiveMCUs[address] = interactiveMCUs[address] ?: return
                passiveMCUs[address] = interactiveMCUs.remove(address) ?: return
            }
            Logger.info("New client with type: ${client.type}")
        } else {
            if (client.type == MCUType.SHUTTER_BUTTONS) {
                passiveMCUs.forEach { (_, MCUClient) ->
                    if (MCUClient.type == MCUType.SHUTTER_CONTROLLER) {
                        MCUClient.write(message)
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

            if (client.type == MCUType.PHONE) {
                if (message.contains("fetch_occupants")) {

                }
            }
        }

        Logger.log(message)
        Logger.info("Message: $message")
    }

    private fun addToFile(connection: String) {
        val printWriter = PrintWriter(FileWriter("connections.txt", true))
        printWriter.println(connection)
        printWriter.close()
    }
}
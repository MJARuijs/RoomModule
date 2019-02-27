import light.HSBColor
import light.LightController
import networking.MCUType
import nio.Manager
import nio.NonBlockingServer
import networking.MCUClient
import java.io.FileWriter
import java.io.PrintWriter
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.SocketChannel
import java.util.*

open class Server(private val address: String, port: Int, private val manager: Manager, private val knownClients: ArrayList<String>) : NonBlockingServer(port) {

    private val clients = HashMap<String, MCUClient>()
    private var configs = ArrayList<String>()

    fun init() {
        knownClients.forEach { client ->
            println(client)
            try {
                val channel = SocketChannel.open()
                channel.connect(InetSocketAddress(client, 4442))
//                channel.write(ByteBuffer.wrap("192.168.178.18".toByteArray()))
                channel.write(ByteBuffer.wrap("192.168.178.38".toByteArray()))
                channel.close()
                Thread.sleep(1000)
            } catch (e: Exception) {
                println("FAILED CONNECTION WITH $client")
            }

        }
    }

    override fun onAccept(channel: SocketChannel) {
        val channelString = channel.toString()
        val startIndex = channelString.lastIndexOf('/') + 1
        val endIndex = channelString.lastIndexOf(':')

        val address = channelString.substring(startIndex, endIndex)
        println(address)

        val newMCU = MCUClient(channel, ::onReadCallback)
        manager.register(newMCU)

        if (!clients.containsKey(address) && !knownClients.contains(address)) {
            addToFile(address)
        }

        clients[address] = newMCU
    }

    fun processCommand(message: String): String {
        if (message == "get_configuration") {
            configs.clear()
            clients.forEach { client ->
                if (client.value.type == MCUType.PC_CONTROLLER) {
                    client.value.sendCommand("get_configuration")
                    while (!client.value.available()) {}
                    configs.add(client.value.lastMessageReceived)
                }
            }
            println("GOING IN")
//            while (configs.isEmpty()) {}
            println("Config: ${configs.joinToString("|", "", "", -1, "", null)}")
            return configs.joinToString("|", "", "", -1, "", null)
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
        } else {
            MCUType.UNKNOWN
        }

        clients.filter { client -> client.value.type == requiredMCU }.forEach {
            client -> client.value.write(data)
        }

        println("Message was: $message")
        return ""
    }

    private fun onReadCallback(message: String, type: MCUType) {

        val client = clients.entries.find {  (_, client) -> client.type == type } ?.component2() ?: return

        if (message.contains("Type: ") && client.type == MCUType.UNKNOWN) {
            client.type = MCUType.fromString(message)
            println("New client with type: ${client.type}")
        } else {

            if (client.type == MCUType.SHUTTER_BUTTONS) {
                clients.forEach { _, MCUClient ->
                    if (MCUClient.type == MCUType.SHUTTER_CONTROLLER) {
                        MCUClient.write(message)
                    }
                }

                if (message.contains("down")) {
                    clients.forEach { _, MCUClient ->
                        if (MCUClient.type == MCUType.LED_STRIP_CONTROLLER) {
                            MCUClient.write("turn_off")
                        }
                    }
                }

                if (message.contains("up")) {
                    clients.forEach { _, MCUClient ->
                        if (MCUClient.type == MCUType.LED_STRIP_CONTROLLER) {
                            MCUClient.write("turn_on")
                        }
                    }
                }

            }

            if (client.type == MCUType.PRESENCE_DETECTOR) {
                if (message.contains("occupied")) {

                    println(Calendar.getInstance().get(Calendar.HOUR_OF_DAY))
                    if (Calendar.getInstance().get(Calendar.HOUR_OF_DAY) < 7) {
                        LightController.setState(4, true, HSBColor(8381.0f, 141.0f, 77.0f))
                    } else {
                        LightController.setState(4, true, HSBColor(8404.0f, 140.0f, 254.0f))
                    }
                } else if (message.contains("empty")) {
                    LightController.setState(4, false, HSBColor(0.0f, 0.0f, 0.0f))
                }
            }

            if (client.type == MCUType.PC_CONTROLLER) {
                    println("lol")
                    configs.add(message)
            }
        }
        println("Message: $message")

    }

    private fun addToFile(connection: String) {
        val printWriter = PrintWriter(FileWriter("res/connections.txt", true))
        printWriter.println(connection)
        printWriter.close()
    }
}
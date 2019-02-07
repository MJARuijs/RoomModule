import light.Color
import light.LightController
import light.rgb.RGBLamp
import networking.MCUClient
import networking.MCUType
import nio.Manager
import nio.NonBlockingServer
import java.nio.channels.SocketChannel
import java.util.*

object Main {

    class DemoServer(port: Int, private val manager: Manager) : NonBlockingServer(port) {

        private val clients = HashMap<String, User>()

        override fun onAccept(channel: SocketChannel) {
            val channelString = channel.toString()
            val startIndex = channelString.lastIndexOf('/') + 1
            val endIndex = channelString.lastIndexOf(':')

            val address = channelString.substring(startIndex, endIndex)
            println(address)

            val newMCU = User(address, channel, ::onReadCallback)
            manager.register(newMCU)
            clients[address] = newMCU
        }

        private fun onReadCallback(message: String, type: MCUType) {

            val client = clients.entries.find {  (_, client) -> client.type == type } ?.component2() ?: return

            if (message.startsWith("Type: ") && client.type == MCUType.UNKNOWN) {
                client.type = MCUType.fromString(message)
                println("Message: $message")
                println("New client with type: ${client.type}")
            } else {

                if (client.type == MCUType.SHUTTER_BUTTONS) {
                    clients.forEach { _, user ->
                        if (user.type == MCUType.SHUTTER_CONTROLLER) {
                            println(user.address)
                            user.write(message)
                        }
                    }

                    if (message.contains("down")) {
                        clients.forEach { _, user ->
                            if (user.type == MCUType.LED_STRIP_CONTROLLER) {
                                println(user.address)
                                user.write("turn_off")
                            }
                        }
                    }

                    if (message.contains("up")) {
                        clients.forEach { _, user ->
                            if (user.type == MCUType.LED_STRIP_CONTROLLER) {
                                println(user.address)
                                user.write("turn_on")
                            }
                        }
                    }

                }

                if (client.type == MCUType.PRESENCE_DETECTOR) {
                    if (message.contains("occupied")) {

                        println(Calendar.getInstance().get(11))
                        if (Calendar.getInstance().get(Calendar.HOUR_OF_DAY) < 7) {
                            LightController.setState(4, true, Color(8381.0f, 141.0f, 77.0f))
                        } else {
                            LightController.setState(4, true, Color(8404.0f, 140.0f, 254.0f))
                        }
                    } else if (message.contains("empty")) {
                        LightController.setState(4, false, Color(0.0f, 0.0f, 0.0f))
                    }
                }
                println("Message: $message")
            }
        }
    }

    class User(val address: String, channel: SocketChannel, private val callback: (String, MCUType) -> Unit) : MCUClient(channel) {
        override fun onRead() {
            callback(String(read()), type)
        }
    }

    @JvmStatic
    fun main(arguments: Array<String>) {
        println("Start of program")
        val manager = Manager()
        val thread = Thread(manager, "Manager")
        thread.start()
        Thread.sleep(1000)
        LightController.addLamp(RGBLamp(4))

        println("Manager started")
        val server = DemoServer(4444, manager)
        manager.register(server)
        println("Server started")
    }

}
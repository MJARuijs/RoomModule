import light.LightController
import light.XYState
import light.rgb.RGBLamp
import networking.MCUClient
import networking.MCUType
import nio.Manager
import nio.NonBlockingServer
import java.nio.channels.SocketChannel
import kotlin.reflect.jvm.internal.impl.load.java.structure.LightClassOriginKind

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
                            user.write(message.toByteArray())
                        }
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

        println("Manager started")
        val server = DemoServer(4444, manager)
        manager.register(server)
        println("Server started")
        LightController.addLamp(RGBLamp(4))
        LightController.setXYState(4, XYState(true, 254.0f, 0.5564f, 0.4098f))

    }

}
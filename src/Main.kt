import light.LightController
import light.rgb.RGBLamp
import networking.HomeClient
import nio.Manager
import java.net.InetSocketAddress
import java.nio.channels.SocketChannel

object Main {

    private lateinit var server: Server

    @JvmStatic
    fun main(arguments: Array<String>) {
        println("Start of program")
        val manager = Manager()
        val thread = Thread(manager, "Manager")
        thread.start()
        Thread.sleep(1000)
        LightController.addLamp(RGBLamp(4))

        println("Manager started")
        server = Server(4444, manager)
        manager.register(server)
        println("Server started")

//        val channel = SocketChannel.open()
//        channel.connect(InetSocketAddress("192.168.178.18", 4443))
//        val client = HomeClient(channel, ::onReadCallback)
//        manager.register(client)
//        client.write("PI: StudyRoom")
    }

    private fun onReadCallback(message: String) {
        server.processCommand(message)
    }
}
import light.LightController
import light.rgb.RGBLamp
import networking.HomeClient
import nio.Manager
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.nio.channels.SocketChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.Exception

object Main {

    private lateinit var server: Server
    private var ledStrip = true
    private var presenceDetector = true

    @JvmStatic
    fun main(arguments: Array<String>) {
        println("Start of program")

        if (!Files.exists(Path.of("connections.txt"))) {
            Files.createFile(Path.of("connections.txt"))
            println("File created!")
        } else {
            println("File already exists!")
        }

        val connections = readConnections()

        val address = try {
            val socket = DatagramSocket()
            socket.connect(InetAddress.getByName("8.8.8.8"), 10002)
            socket.localAddress.hostAddress
        } catch (e: Exception) {
            ""
        }

        println(address)


        val manager = Manager()
        val thread = Thread(manager, "Manager")
        thread.start()
        Thread.sleep(1000)
        LightController.addLamp(RGBLamp(4))
        println("Manager started")
        server = Server(address, 4444, manager, connections)
        manager.register(server)
        println("Server started")

        server.init()

        val channel = SocketChannel.open()
        channel.connect(InetSocketAddress("192.168.178.29", 4443))
        val client = HomeClient(channel, ::onReadCallback)
        manager.register(client)
        client.write("PI: StudyRoom")
    }

    private fun onReadCallback(message: String): String {
        return server.processCommand(message)
    }

    private fun readConnections(): ArrayList<String> {
        val connections = ArrayList<String>()

        try {
            val stream = Files.lines(Paths.get("connections.txt"))
            stream.forEach { line -> connections += line }
        } catch (e: Exception) {
            println("FILE COULD NOT BE READ")
            return connections
        }

        return connections
    }
}
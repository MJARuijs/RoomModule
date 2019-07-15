import light.LightController
import light.rgb.RGBLamp
import networking.HomeClient
import networking.HomeServer
import nio.Manager
import util.Logger
import util.PrintStreamType
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
    private lateinit var manager: Manager

    const val ROOM_NAME = "Study Room"

    @JvmStatic
    fun main(arguments: Array<String>) {
        Logger.setPrintColored(true)
        Logger.setPrintTag(true)
        Logger.setPrintTimeStamp(true)
        Logger.setOut("output.txt", true, PrintStreamType.FILE)
        Logger.info("Start of program")

        if (!Files.exists(Path.of("connections.txt"))) {
            Files.createFile(Path.of("connections.txt"))
            Logger.info("File created!")
        } else {
            Logger.info("File already exists!")
        }

        val connections = readConnections()

        val address = try {
            val socket = DatagramSocket()
            socket.connect(InetAddress.getByName("8.8.8.8"), 10002)
            val socketAddress = socket.localAddress.hostAddress
            socket.close()
            socketAddress
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }

        Logger.debug("My Address: $address")

        manager = Manager()
        val thread = Thread(manager, "Manager")
        thread.start()
        Thread.sleep(1000)
        LightController.addLamp(RGBLamp(4))
        Logger.info("Manager started")
        server = Server(address, 4442, manager, connections)
        manager.register(server)

        Logger.info("Server started")

        server.init()

        Thread(HomeServer(address, 4443, ::onServerReconnected)).start()
        onServerReconnected("192.168.178.18")
    }

    private fun onServerReconnected(serverAddress: String) {
        try {
            val homeServerChannel = SocketChannel.open()
            homeServerChannel.connect(InetSocketAddress(serverAddress, 4440))
            val homeServer = HomeClient(homeServerChannel, ::onReadCallback)
            manager.register(homeServer)
            homeServer.write("PI: $ROOM_NAME")
        } catch (e: Exception) {
            Logger.err("Failed to reconnect with server!")
        }
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
            Logger.err("FILE COULD NOT BE READ")
            return connections
        }
        return connections
    }
}
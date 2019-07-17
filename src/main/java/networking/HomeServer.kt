package networking

import util.Logger
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.ServerSocketChannel

class HomeServer(address: String, port: Int, private val onServerConnectedCallback: (String) -> Unit) : Runnable {

    private val serverChannel: ServerSocketChannel = ServerSocketChannel.open()

    init {
        serverChannel.bind(InetSocketAddress(address, port))
    }

    override fun run() {
        while (true) {
            val clientChannel = serverChannel.accept()

            Logger.info("CONNECTED")

            val sizeBuffer = ByteBuffer.allocate(4)
            val sizeBytes = clientChannel.read(sizeBuffer)
            if (sizeBytes < 0) {
                clientChannel.close()
            }

            sizeBuffer.rewind()

            val size = sizeBuffer.int

            val dataBuffer = ByteBuffer.allocate(size)
            val bytesRead = clientChannel.read(dataBuffer)

            if (bytesRead < 0) {
                clientChannel.close()
            }

            dataBuffer.rewind()

            val message = String(dataBuffer.array())
            if (message.startsWith("SERVER_ADDRESS")) {
                val startIndex = message.indexOf(':') + 1
                val serverAddress = message.substring(startIndex, message.length)
                onServerConnectedCallback(serverAddress)
            }
            clientChannel.close()
        }
    }
}
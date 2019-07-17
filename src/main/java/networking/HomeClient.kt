package networking

import nio.NonBlockingClient
import util.Logger
import java.nio.ByteBuffer
import java.nio.channels.SocketChannel

class HomeClient(channel: SocketChannel, private val callback: (String) -> String) : NonBlockingClient(channel) {

    private val readSizeBuffer = ByteBuffer.allocateDirect(Integer.BYTES)

    override fun write(bytes: ByteArray) {

        val buffer = ByteBuffer.allocate(bytes.size + 4)
        buffer.putInt(bytes.size)
        buffer.put(bytes)
        buffer.rewind()

        Logger.info("Writing to server: ${String(bytes)}")

        channel.write(buffer)
    }

    @Throws (ClientException::class)
    override fun read(): ByteArray {
        // Read size
        readSizeBuffer.clear()
        val sizeBytesRead = channel.read(readSizeBuffer)

        if (sizeBytesRead == -1) {
            throw ClientException("Size was too large")
        }

        readSizeBuffer.rewind()

        // Read data
        val size = readSizeBuffer.int
        if (size > 1000) {
            Logger.err("ERROR: too large: $size")
            throw ClientException("Size was too large")
        }

        val data = ByteBuffer.allocate(size)
        val bytesRead = channel.read(data)

        if (bytesRead == -1) {
            Logger.err("BYTESREAD ")
            close()
            throw ClientException("Client was closed")
        }

        data.rewind()
        return data.array()
    }

    override fun close() {
        channel.close()
    }

    override fun onRead() {
        val message = readMessage()
        Thread {
            val response = callback(message)
            if (response != "") {
                Logger.info("Response: $response")
                write(response)
            }
        }.start()
    }
}
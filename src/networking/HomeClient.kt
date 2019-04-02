package networking

import nio.NonBlockingClient
import java.nio.ByteBuffer
import java.nio.channels.SocketChannel

class HomeClient(channel: SocketChannel, private val callback: (String) -> String) : NonBlockingClient(channel) {

    private val readSizeBuffer = ByteBuffer.allocateDirect(Integer.BYTES)

    override fun write(bytes: ByteArray) {

        val buffer = ByteBuffer.allocate(bytes.size + 4)
        buffer.putInt(bytes.size)
        buffer.put(bytes)
        buffer.rewind()

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
            println("ERROR: too large: $size")
            throw ClientException("Size was too large")
        }

        val data = ByteBuffer.allocate(size)
        val bytesRead = channel.read(data)

        if (bytesRead == -1) {
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
        write(callback(readMessage()))
    }
}
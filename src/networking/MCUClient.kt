package networking

import nio.NonBlockingClient
import java.nio.ByteBuffer
import java.nio.channels.SocketChannel

abstract class MCUClient(channel: SocketChannel): NonBlockingClient(channel) {

    private val readSizeBuffer = ByteBuffer.allocateDirect(Integer.BYTES)

    var type = MCUType.UNKNOWN

    fun write(message: String) = write(message.toByteArray())

    override fun write(bytes: ByteArray) {
        val buffer = ByteBuffer.allocate(bytes.size + 4)
        buffer.putInt(bytes.size)
        buffer.put(bytes)
        buffer.rewind()

        channel.write(buffer)
    }

    override fun read(): ByteArray {
        readSizeBuffer.clear()

        val sizeBytes = channel.read(readSizeBuffer)
        readSizeBuffer.rewind()

        if (sizeBytes == -1) {
            throw ClientException("Client was closed")
        }

        var sizeString = ""

        for (i in 0 until 4) {
            sizeString += readSizeBuffer.get().toChar()
        }

        val size = sizeString.toInt()

        val dataBuffer = ByteBuffer.allocate(size)
        val dataBytes = channel.read(dataBuffer)
        dataBuffer.rewind()

        if (dataBytes == -1) {
            throw ClientException("Client was closed!")
        }

        return dataBuffer.array()
    }

    override fun close() {
        println("closing")
        channel.close()
    }

}
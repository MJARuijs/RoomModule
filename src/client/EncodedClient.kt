package client

import java.nio.ByteBuffer
import java.nio.channels.SocketChannel
import java.nio.charset.StandardCharsets.UTF_8
import java.util.*

open class EncodedClient(private val channel: SocketChannel): Client {

    init {
        channel.configureBlocking(false)
    }

    private var message = ByteArray(0)

    override fun write(bytes: ByteArray) {
        if (!channel.isConnected) {
            return
        }

        val buffer = ByteBuffer.wrap(Base64.getEncoder().encode(bytes))

        val bufferSize = buffer.array().size.toString()
        val size = Base64.getEncoder().encode(bufferSize.toByteArray(UTF_8))
        val writeSizeBuffer = ByteBuffer.allocateDirect(Integer.BYTES)

        // Prepare size buffer
        writeSizeBuffer.clear()
        writeSizeBuffer.put(size)
        writeSizeBuffer.rewind()

        // Write size
        channel.write(writeSizeBuffer)

        buffer.rewind()

        // Write data
        channel.write(buffer)
    }

    override fun messageAvailable(): Boolean {

        if (!channel.isConnected) {
            return false
        }

        // Read size
        val readSizeBuffer = ByteBuffer.allocateDirect(Integer.BYTES)
        readSizeBuffer.clear()

        val sizeBytesRead = channel.read(readSizeBuffer)

        if (sizeBytesRead == -1) {
            throw ClientException("Size was too large")
        }

        readSizeBuffer.rewind()

        // Read data
        val size = try {
            String(Base64.getDecoder().decode(readSizeBuffer).array(), UTF_8).toInt()
        } catch (e: Exception) {
            return false
        }

        if (readSizeBuffer.remaining() != 0) {
            return false
        }

        if (size > 1000) {
            throw ClientException("Size was too large")
        }

        val data = ByteBuffer.allocate(size)
        val bytesRead = channel.read(data)

        if (bytesRead == -1) {
            throw ClientException("client.Client was closed")
        }

        data.rewind()

        try {
            message = Base64.getDecoder().decode(data).array()
        } catch (e: Exception) {
            message = "ERROR".toByteArray(UTF_8)
            return true
        }

        return true
    }

    override fun getMessage() = message

//    override fun messageAvailable() = available

    override fun close() {
        channel.close()
    }
}

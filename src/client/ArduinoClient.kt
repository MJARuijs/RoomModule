package client

import java.io.IOException
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.SocketChannel
import java.nio.charset.StandardCharsets

class ArduinoClient(address: InetSocketAddress) {

    private var channel: SocketChannel = SocketChannel.open(address)

    private val writeSizeBuffer = ByteBuffer.allocateDirect(Integer.BYTES)
    private val readSizeBuffer = ByteBuffer.allocateDirect(Integer.BYTES)

    private fun write(bytes: ByteArray) {

        // Prepare size buffer
        writeSizeBuffer.clear()
        writeSizeBuffer.putInt(bytes.size)
        writeSizeBuffer.rewind()

        // Write size
        channel.write(writeSizeBuffer)

        val buffer = ByteBuffer.wrap(bytes)

        buffer.rewind()

        // Write data
        channel.write(buffer)
    }

    @Throws(IOException::class)
    fun read(): ByteBuffer {
        return try {
            // Read size
            readSizeBuffer.clear()
            val sizeBytesRead = channel.read(readSizeBuffer)

            if (sizeBytesRead == -1) {
                close()
                throw ClientException("Client was closed")
            }

            readSizeBuffer.rewind()

            // Read data
            val size = readSizeBuffer.int

            if (size > 1000) {
                throw ClientException("Size was too large")
            }

            val data = ByteBuffer.allocate(size)
            val bytesRead = channel.read(data)

            if (bytesRead == -1) {
                close()
                throw ClientException("Client was closed")
            }

            data.rewind()
            data
        } catch (e: IOException) {
            ByteBuffer.allocate(0)
        }
    }

    private fun writeMessage(message: String) {
        val bytes = message.toByteArray(StandardCharsets.UTF_8)
        write(bytes)
    }

    private fun readMessage(): String {
        return try {
            String(read().array(), StandardCharsets.UTF_8)
        } catch (e: ClientException) {
            "ERROR"
        }
    }

    fun sendCommand(message:String): String {
        writeMessage(message)
        val read = readMessage()
        return if (read == "ERROR") {
            sendCommand(message)
        } else {
            read
        }
    }

    private fun close() {
        channel.close()
    }

}

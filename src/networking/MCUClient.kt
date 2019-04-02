package networking

import nio.NonBlockingClient
import java.lang.Exception
import java.nio.ByteBuffer
import java.nio.channels.SocketChannel

class MCUClient(channel: SocketChannel, val address: String, private val callback: (String, MCUType) -> Unit): NonBlockingClient(channel) {

    private val readSizeBuffer = ByteBuffer.allocateDirect(Integer.BYTES)
    var lastMessageReceived = ""

    var type = MCUType.UNKNOWN

    fun available(): Boolean {
        return lastMessageReceived != ""
    }

    fun sendCommand(command: String): String {
        write(command)
        return readMessage()
    }

    override fun write(bytes: ByteArray) {
        try {
            val buffer = ByteBuffer.allocate(bytes.size + 4)
            buffer.putInt(bytes.size)
            buffer.put(bytes)
            buffer.rewind()
            println("Writing: ${String(bytes)}. To : $type")
            channel.write(buffer)
        } catch(e: Exception) {
            throw ClientException(e.message!!)
        }
    }

    override fun onRead() {
        lastMessageReceived = readMessage()
        println("LAST MESSAGE: $lastMessageReceived")
        callback(lastMessageReceived, type)
    }

    override fun read(): ByteArray {
        try {
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
        } catch(e: Exception) {
            throw ClientException(e.message!!)
        }
    }

    override fun close() {
        println("closing")
        channel.close()
    }

}
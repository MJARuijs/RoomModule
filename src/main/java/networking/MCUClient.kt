package networking

import nio.NonBlockingClient
import util.Logger
import java.lang.Exception
import java.nio.ByteBuffer
import java.nio.channels.SocketChannel

class MCUClient(channel: SocketChannel, val address: String, private val callback: (String, String, MCUType) -> Unit): NonBlockingClient(channel) {

    private val readSizeBuffer = ByteBuffer.allocateDirect(Integer.BYTES)

    var type = MCUType.UNKNOWN

    override fun write(bytes: ByteArray) {
        try {
            val buffer = ByteBuffer.allocate(bytes.size + 4)
            buffer.putInt(bytes.size)
            buffer.put(bytes)
            buffer.rewind()
            Logger.debug("Writing: ${String(bytes)}. To : $type")
            channel.write(buffer)
        } catch(e: Exception) {
            throw ClientException("")
        }
    }

    override fun onRead() {
        callback(readMessage(), address, type)
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
                val char = readSizeBuffer.get().toChar()
                sizeString += char
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
            Logger.err(e.message!!)
            throw ClientException("")
        }
    }

    override fun close() {
        Logger.info("closing")
        channel.close()
    }

}
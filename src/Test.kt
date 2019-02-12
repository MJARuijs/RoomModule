import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.SocketChannel

object Test {

    @JvmStatic
    fun main(arguments: Array<String>) {
        val address = "192.168.178.18"
        val channel = SocketChannel.open()
        channel.connect(InetSocketAddress("192.168.178.45", 4442))
        channel.write(ByteBuffer.wrap(address.toByteArray()))
        channel.close()
    }
}
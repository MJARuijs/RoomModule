package nio

import java.net.InetSocketAddress
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel

open class Server(port: Int) {

    protected val channel = ServerSocketChannel.open()!!

    init {
        val address = InetSocketAddress(port)
        channel.bind(address)
    }

    open fun close() {
        channel.close()
    }

    fun accept(): SocketChannel {
        return channel.accept()
    }

}
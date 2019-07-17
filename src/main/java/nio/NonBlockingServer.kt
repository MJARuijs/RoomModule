package nio

import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.SocketChannel

abstract class NonBlockingServer(port: Int) : Server(port), Registrable {

    init {
        channel.configureBlocking(false)
    }

    final override fun register(selector: Selector) {
        channel.register(selector, SelectionKey.OP_ACCEPT, this)
    }

    abstract fun onAccept(channel: SocketChannel)

}
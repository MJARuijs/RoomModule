package nio

import networking.ClientException
import util.Logger
import java.nio.channels.Selector
import java.util.concurrent.atomic.AtomicBoolean

class Manager : Runnable {

    private val selector = Selector.open()
    private val running = AtomicBoolean(true)
    private val registering = AtomicBoolean(false)

    fun register(obj: Registrable) {
        registering.set(true)
        selector.wakeup()
        obj.register(selector)
        registering.set(false)
    }

    override fun run() {

        while (running.get()) {

            try {
                while (registering.get()) {}

                selector.select()
                val keys = selector.selectedKeys()
                val iterator = keys.iterator()
                while (iterator.hasNext()) {

                    val key = iterator.next()
                    iterator.remove()
                    if (key.isValid) {

                        if (key.isAcceptable) {
                            val server = key.attachment() as NonBlockingServer
                            server.onAccept(server.accept())
                        }

                        if (key.isReadable) {
                            val client = key.attachment() as NonBlockingClient
                            try {
                                client.onRead()
                            } catch (exception: ClientException) {
                                exception.printStackTrace()
                                client.close()
                                key.cancel()
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Logger.err("STOPPED: ${e.message}")
//                stop()
//                break
            }
        }
    }

    private fun stop() {
        running.set(false)
        registering.set(false)
        selector.close()
    }

}
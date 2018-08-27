package client

interface Client {

    fun write(bytes: ByteArray)

    fun messageAvailable(): Boolean

    fun getMessage(): ByteArray

    fun close()

}


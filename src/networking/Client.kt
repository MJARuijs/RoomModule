package networking

interface Client {

    fun write(bytes: ByteArray)

    fun read(): ByteArray

    fun close()

}


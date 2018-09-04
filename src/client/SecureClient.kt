package client

import java.nio.channels.SocketChannel
import java.nio.charset.StandardCharsets.UTF_8
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

class SecureClient(channel: SocketChannel): EncodedClient(channel) {

    private companion object {
        val asymmetricGenerator: KeyPairGenerator = KeyPairGenerator.getInstance("RSA")
        val symmetricGenerator: KeyGenerator = KeyGenerator.getInstance("AES")

        init {
            asymmetricGenerator.initialize(2048)
            symmetricGenerator.init(128)
        }
    }

    private val encryptor = Cipher.getInstance("RSA/ECB/PKCS1Padding")
    private val decryptor = Cipher.getInstance("RSA/ECB/PKCS1Padding")
    private val symmetricKey: SecretKey

    private var message: String = ""

    init {
        channel.configureBlocking(false)
        symmetricKey = symmetricGenerator.generateKey()

        val keyPair = asymmetricGenerator.generateKeyPair()
        val clientKey = keyPair.private

        write(keyPair.public.encoded)

        val keyFactory = KeyFactory.getInstance("RSA")

        while (!super.messageAvailable()) {}
        val serverKey = keyFactory.generatePublic(X509EncodedKeySpec(getMessage()))

        encryptor.init(Cipher.PUBLIC_KEY, serverKey)
        decryptor.init(Cipher.PRIVATE_KEY, clientKey)
        println("client initialized")
    }

    fun message() = message

    override fun messageAvailable(): Boolean {
        while (!super.messageAvailable()) {}
        val clientMessage = getMessage()

        while (!super.messageAvailable()) {}
        val key = getMessage()

        if (String(clientMessage) == "ERROR" || String(key) == "ERROR") {
            writeMessage("ERROR")
            return true
        }


        return try {
            val decryptedKey = decryptor.doFinal(key)

            val secretKey = SecretKeySpec(decryptedKey, 0, decryptedKey.size, "AES")
            val cipher = Cipher.getInstance("AES")
            cipher.init(Cipher.DECRYPT_MODE, secretKey)

            val decryptedMessage = cipher.doFinal(clientMessage)
            message = String(decryptedMessage, UTF_8)

            writeMessage("OK")

            true
        } catch (e: Exception) {
            false
        }
    }

    fun writeMessage(message: String) {
        val cipher = Cipher.getInstance("AES")
        cipher.init(Cipher.ENCRYPT_MODE, symmetricKey)
        val messageBytes = cipher.doFinal(message.toByteArray(UTF_8))

        val keyBytes = encryptor.doFinal(symmetricKey.encoded)

        write(messageBytes)
        write(keyBytes)
    }

}
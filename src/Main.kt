import client.SecureClient
import com.pi4j.io.gpio.GpioFactory
import com.pi4j.io.gpio.PinState
import com.pi4j.io.gpio.RaspiPin
import com.pi4j.system.SystemInfo.getOsName
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object Main {

    private const val hueIP = "192.168.0.101"
    private const val huesername = "dMTAhV9kA9GNdMoTiBdndnhIjRchkAULjIjtLPXE"

    @JvmStatic
    fun main() {
        val server = Server(4444)
        println("Server started")
        if (getOsName().startsWith("Linux")) {
            val gpioController = GpioFactory.getInstance()
            println("pin should be on now")
            val pin = gpioController.provisionDigitalOutputPin(RaspiPin.GPIO_01, "LED", PinState.HIGH)
        }
        println("Done")
        val client = SecureClient(server.accept())

        while (true) {
            val decodedMessage = client.readMessage()

            when (decodedMessage) {
                "light_on" -> setState(6, true)
                "light_off" -> setState(6, false)
                "get_configuration" -> getConfiguration()
            }
            client.writeMessage("SUCCESS")
        }

    }

    private fun getConfiguration() {

    }

    private fun setState(lampID: Int, newState: Boolean) {
        val url = URL("http://$hueIP/api/$huesername/lights/$lampID/state")
        val connection = url.openConnection() as HttpURLConnection
        connection.doOutput = true
        connection.requestMethod = "PUT"

        val outputStream = OutputStreamWriter(connection.outputStream)
        outputStream.write("{\"on\":$newState}")
        outputStream.close()

        val ins = connection.inputStream
        val rd = BufferedReader(InputStreamReader(ins))

        val builder = StringBuilder()
        var line = rd.readLine()

        while (line != null) {
            builder.append(line).append("\n")
            line = rd.readLine()
        }

        rd.close()
    }

    private fun getState(lampID: Int): Boolean {
        val url = URL("http://$hueIP/api/$huesername/lights/$lampID")
        val connection = url.openConnection() as HttpURLConnection
        connection.doOutput = true
        connection.requestMethod = "GET"

        val ins = connection.inputStream
        val rd = BufferedReader(InputStreamReader(ins))

        val builder = StringBuilder()
        var line = rd.readLine()

        while (line != null) {
            builder.append(line).append("\n")
            line = rd.readLine()
        }

        rd.close()

        val startIndex = builder.indexOf("\"on\"", 0, false) + 5
        val endIndex = builder.indexOf(",", startIndex, false)

        val state = builder.substring(startIndex, endIndex)

        return state == "true"
    }
}

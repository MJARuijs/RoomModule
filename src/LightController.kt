import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class LightController {

    fun setState(lampID: Int, newState: Boolean) {
        val url = URL("http://$HUE_IP/api/$HUESERNAME/lights/$lampID/state")
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

    fun getState(lampID: Int): Boolean {
        val url = URL("http://$HUE_IP/api/$HUESERNAME/lights/$lampID")
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

    companion object {
        private const val HUE_IP = "192.168.0.101"
        private const val HUESERNAME = "dMTAhV9kA9GNdMoTiBdndnhIjRchkAULjIjtLPXE"
    }

}
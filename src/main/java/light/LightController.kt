package light

import light.rgb.RGBLamp
import light.rgb.RGBState
import light.white.WhiteLamp
import light.white.WhiteState
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.locks.ReentrantLock
import java.util.regex.Pattern
import kotlin.math.roundToInt

object LightController {

    private const val HUE_IP = "192.168.178.13"
    private const val HUESERNAME = "lmLRYysh-k-NDdh7AwdoeayPUizr3whOEesAkMHK"

    private val lamps = ArrayList<Lamp>()

    private val lock = ReentrantLock()

    fun addLamp(vararg lamps: Lamp) = this.lamps.addAll(lamps)

    fun setState(id: Int, newState: Boolean, color: Color) {
        while (lock.isLocked) {}
        lock.lock()
        try {
            val lamp = lamps.find { lamp -> lamp.id == id } ?: return

            lamp.state.on = newState

            if (lamp is RGBLamp) {
                (lamp.state as RGBState).color
            }

            val url = URL("http://$HUE_IP/api/$HUESERNAME/lights/${lamp.id}/state/")
            val connection = url.openConnection() as HttpURLConnection
            connection.doOutput = true
            connection.requestMethod = "PUT"

            val outputStream = OutputStreamWriter(connection.outputStream)
            if (lamp is WhiteLamp) {
                outputStream.write("{\"on\":$newState}")
            } else {
                if (color is HSBColor) {
                    outputStream.write("{\"on\":$newState, \"sat\":${color.saturation.roundToInt()}, \"bri_inc\":${color.brightness.roundToInt()}, \"hue\":${color.hue.roundToInt()}}")
                }
            }
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
        } finally {
            lock.unlock()
        }

    }

    fun sendCommand(id: Int, color: HSBColor, command: String) {
        while (lock.isLocked) {}
        lock.lock()
        try {
            val lamp = lamps.find { lamp -> lamp.id == id } ?: return

            if (lamp is RGBLamp) {
                (lamp.state as RGBState).color
            }

            val url = URL("http://$HUE_IP/api/$HUESERNAME/lights/${lamp.id}/state/")
            val connection = url.openConnection() as HttpURLConnection
            connection.doOutput = true
            connection.requestMethod = "PUT"

            val outputStream = OutputStreamWriter(connection.outputStream)
            outputStream.write(command)

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
        } finally {
            lock.unlock()
        }
    }

    fun getState(lampID: Int): State {
        val lamp = lamps.find { lamp -> lamp.id == lampID } ?: throw IllegalArgumentException("No lamp with ID: $lampID was found")
        return getState(lamp)
    }

    private fun getState(lamp: Lamp): State {
        while (lock.isLocked) {}
        lock.lock()
        try {
            val url = URL("http://$HUE_IP/api/$HUESERNAME/lights/${lamp.id}")
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

            if (lamp is WhiteLamp) {
                val startIndex = builder.indexOf("\"on\"", 0, false) + 5
                val endIndex = builder.indexOf(",", startIndex, false)

                val state = builder.substring(startIndex, endIndex)
                return WhiteState(state == "true")
            } else {
                val pattern = Pattern.compile("(.)+\\{\"on\":(?<on>true|false),\"bri\":(?<bri>[0-9]+),\"hue\":(?<hue>[0-9]+),\"sat\":(?<sat>[0-9]+)(.)+")
                val matcher = pattern.matcher(builder.toString().trim())

                if (matcher.matches()) {
                    val on = matcher.group("on")!!.toBoolean()
                    val sat = matcher.group("sat").toFloat()
                    val bri = matcher.group("bri").toFloat()
                    val hue = matcher.group("hue").toFloat()
                    return RGBState(on, HSBColor(sat, bri, hue))
                } else {
                    throw IllegalArgumentException("Invalid RGB state")
                }
            }
        } finally {
            lock.unlock()
        }
    }

}
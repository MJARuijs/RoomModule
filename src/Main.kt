import client.ArduinoClient
import client.SecureClient
import com.pi4j.io.gpio.*
import com.pi4j.system.SystemInfo.getOsName
import light.Color
import light.LightController
import light.NotificationLight
import light.XYState
import light.rgb.RGBLamp
import java.net.InetSocketAddress
import java.util.regex.Pattern

object Main : MotionSensor.MotionSensorCallback {

    private var hardwareManager = HardwareManager()
    private lateinit var motionSensor: MotionSensor

    private const val lampID = 4
    private var enableLighting = false

    private val pattern = Pattern.compile("(.)+r=(-)?(?<r>\\d+.\\d+), g=(-)?(?<g>\\d+.\\d+), b=(?<b>(-)?\\d+.\\d+)")
    private lateinit var previousState: XYState

    @JvmStatic
    fun main(args: Array<String>) {



        hardwareManager.addDeviceManager(ArduinoClient(InetSocketAddress("192.168.178.14", 80)))

        LightController.addLamp(RGBLamp(4))

        val server = Server(4444)
        println("Server started")

        if (getOsName().startsWith("Linux")) {

            val runTime = Runtime.getRuntime()
            runTime.exec("gpio mode 1 pwm")
            runTime.exec("gpio pwm-ms")
            runTime.exec("gpio pwmc 192")
            runTime.exec("gpio pwmr 2000")
            runTime.exec("gpio pwm 1 3000")
            Thread.sleep(5000)

            val gpioController = GpioFactory.getInstance()
            val motionSensorPin = gpioController.provisionDigitalInputPin(RaspiPin.GPIO_07)
            motionSensor = MotionSensor(motionSensorPin, this)

            Thread {
                while (true) {
                    motionSensor.update()
                }
            }.start()
        }

//        LightController.setState(4, true, Color(1200000f, 100f, 254f))
        LightController.setXYState(4, XYState(true, 254.0f, 0.5564f, 0.4098f))

        while (true) {

            val socketChannel = server.accept()

            if (socketChannel != null) {
                val client = SecureClient(socketChannel)

                while (true) {

                    if (client.messageAvailable()) {
                        val decodedMessage = client.message()
                        val matcher = pattern.matcher(decodedMessage)

                        if (matcher.matches()) {
                            val r = matcher.group("r").toFloat()
                            val g = matcher.group("g").toFloat()
                            val b = matcher.group("b").toFloat()

                            if (b == -1.0f) {
                                NotificationLight.stopLighting()
                                LightController.setXYState(4, previousState)
                            } else {
                                previousState = LightController.getXYState(4)
                                NotificationLight.startLighting(Color(r, g, b))
                                LightController.setState(4, true, Color(r, g, b))
                            }
                        }

                        val response: String = when (decodedMessage) {

                            "light_on" -> {
                                LightController.setXYState(lampID, XYState(true, 254f, 0.4575f, 0.4099f))
                                getConfiguration()
                            }
                            "light_off" -> {
                                LightController.setState(lampID, false)
                                getConfiguration()
                            }

                            "socket_power_on" -> hardwareManager.togglePowerSocket(true)
                            "socket_power_off" -> hardwareManager.togglePowerSocket(false)
                            "confirm_socket_off" -> hardwareManager.forceSocketOff()

                            "pc_power_on" -> hardwareManager.togglePC(true)
                            "pc_power_off" -> hardwareManager.togglePC(false)
                            "confirm_pc_shutdown" -> hardwareManager.forceShutdownPC()

                            "use_motion_sensor_on" -> {
                                if (getOsName().startsWith("Linux")) {
                                    motionSensor.enabled = true
                                    getConfiguration()
                                } else "ERR"
                            }
                            "use_motion_sensor_off" -> {
                                if (getOsName().startsWith("Linux")) {
                                    motionSensor.enabled = false
                                    getConfiguration()
                                } else "ERR"
                            }

                            "get_configuration" -> getConfiguration()

                            else -> "COMMAND_NOT_SUPPORTED"
                        }

                        if (response == "PC_STILL_ON") {
                            client.writeMessage(response)
                            continue
                        }
                        println(response)
                        println()
                        Thread.sleep(50)
                        client.writeMessage(response)
                    }

                    NotificationLight.update()
                }
            }
        }
    }

    private fun getConfiguration(): String {
        val builder = StringBuilder()
        builder.append(getHardwareConfig())
        builder.append(getModuleConfig())
        return builder.toString()
    }

    private fun getHardwareConfig(): String {
        return hardwareManager.getConfiguration()
    }

    private fun getModuleConfig(): String {
        val builder = StringBuilder()
        builder.append("light=${LightController.getState(lampID).on}, ")

        if (getOsName().startsWith("Linux")) {
            builder.append("use_motion_sensor=${motionSensor.enabled}, ")
        } else {
            builder.append("use_motion_sensor=false, ")
        }
        return builder.toString()
    }

    override fun onStateChanged(state: Boolean) {
        LightController.setState(lampID, state)
    }

}

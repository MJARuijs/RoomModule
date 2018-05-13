import client.ArduinoClient
import client.SecureClient
import com.pi4j.io.gpio.*
import com.pi4j.system.SystemInfo.getOsName
import java.net.InetSocketAddress

object Main : MotionSensor.MotionSensorCallback {

    private val lightController = LightController()
    private var hardwareManager = HardwareManager()
    private lateinit var motionSensor: MotionSensor

    @JvmStatic
    fun main(args: Array<String>) {

        hardwareManager.addDeviceManager(ArduinoClient(InetSocketAddress("192.168.0.14", 80)))

        val server = Server(4444)
        println("Server started")

        if (getOsName().startsWith("Linux")) {
            val gpioController = GpioFactory.getInstance()

            val motionSensorPin = gpioController.provisionDigitalInputPin(RaspiPin.GPIO_07)
            motionSensor = MotionSensor(motionSensorPin, this)

            Thread {
                while (true) {
                    motionSensor.update()
                }
            }.start()
        }

        val client = SecureClient(server.accept())

        while (true) {
            val decodedMessage = client.readMessage()

            var response: String = when (decodedMessage) {

                "light_on" -> {
                    lightController.setState(6, true)
                    getConfiguration()
                }
                "light_off" -> {
                    lightController.setState(6, false)
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

            if (response.startsWith("configuration")) {
                response += getModuleConfig()
            }
            println(response)
            client.writeMessage(response)
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
        builder.append("light=${lightController.getState(6)}, ")

        if (getOsName().startsWith("Linux")) {
            builder.append("use_motion_sensor=${motionSensor.enabled}, ")
        } else {
            builder.append("use_motion_sensor=false, ")
        }
        return builder.toString()
    }

    override fun onStateChanged(state: Boolean) {
        lightController.setState(6, state)
    }

}

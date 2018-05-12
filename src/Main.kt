import client.ArduinoClient
import client.SecureClient
import com.pi4j.io.gpio.*
import com.pi4j.system.SystemInfo.getOsName
import java.net.InetSocketAddress
import java.nio.channels.SocketChannel

object Main : MotionSensor.MotionSensorCallback {

    private val lightController = LightController()
    private var hardwareManager = HardwareManager()
    private lateinit var motionSensor: MotionSensor

    @JvmStatic
    fun main(args: Array<String>) {

        hardwareManager.addDeviceManager(ArduinoClient(SocketChannel.open(InetSocketAddress("192.168.0.14", 80))))

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

            val response: String = when (decodedMessage) {

                "light_on" -> lightController.setState(6, true)
                "light_off" -> lightController.setState(6, false)

                "socket_power_on" -> hardwareManager.togglePowerSocket(true)
                "socket_power_off" -> hardwareManager.togglePowerSocket(false)

                "pc_power_on" -> hardwareManager.togglePC(true)
                "pc_power_off" -> hardwareManager.togglePC(false)

                "use_motion_sensor_on" -> {
                    if (getOsName().startsWith("Linux")) motionSensor.enable() else "ERR"
                }
                "use_motion_sensor_off" -> {
                    if (getOsName().startsWith("Linux")) motionSensor.disable() else "ERR"
                }

                "get_configuration" -> getConfiguration()
                else -> "COMMAND_NOT_SUPPORTED"

            }
            client.writeMessage(response)
        }

    }

    private fun getConfiguration(): String {
        val builder = StringBuilder()
        builder.append(hardwareManager.getConfiguration())
        builder.append("light=${lightController.getState(6)}, ")
        builder.append("use_motion_sensor=${motionSensor.isEnabled()}, ")
        return builder.toString()
    }

    override fun onStateChanged(state: Boolean) {
        lightController.setState(6, state)
    }

}

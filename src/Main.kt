import client.SecureClient
import com.pi4j.io.gpio.*
import com.pi4j.system.SystemInfo.getOsName

object Main : MotionSensor.MotionSensorCallback {

    private val lightController = LightController()
    private var hardwareManager: HardwareManager? = null

    @JvmStatic
    fun main(args: Array<String>) {

        val server = Server(4444)
        println("Server started")

        if (getOsName().startsWith("Linux")) {
            val gpioController = GpioFactory.getInstance()

            val socketPin = gpioController.provisionDigitalOutputPin(RaspiPin.GPIO_02)
            val pcPin = gpioController.provisionDigitalOutputPin(RaspiPin.GPIO_03)
            socketPin.state = PinState.LOW
            pcPin.state = PinState.LOW
            hardwareManager = HardwareManager(socketPin, pcPin)

            val motionSensorPin = gpioController.provisionDigitalInputPin(RaspiPin.GPIO_07)
            val motionSensor = MotionSensor(motionSensorPin, this)

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
                "light_on" -> {
                    lightController.setState(6, true)
                    "SUCCESS"
                }
                "light_off" -> {
                    lightController.setState(6, false)
                    "SUCCESS"
                }
                "socket_power_on" -> {
                    hardwareManager?.setSocketState(true)
                    "SUCCESS"
                }
                "socket_power_off" -> {
                    hardwareManager?.setSocketState(false)
                    "SUCCESS"
                }
                "pc_power_on" -> {
                    hardwareManager?.setPcState(true)
                    "SUCCESS"
                }
                "pc_power_off" -> {
                    hardwareManager?.setPcState(false)
                    "SUCCESS"
                }
                "get_configuration" -> getConfiguration()
                else -> {
                    "COMMAND_NOT_SUPPORTED"
                }
            }
            client.writeMessage(response)
        }

    }

    private fun getConfiguration(): String {
        val builder = StringBuilder()
        builder.append("socket_power=true, ")
        builder.append("pc_power=true, ")
        builder.append("light=${lightController.getState(6)}, ")
        return builder.toString()
    }

    override fun onStateChanged(state: Boolean) {
        lightController.setState(6, state)
    }

}

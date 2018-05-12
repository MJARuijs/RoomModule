import client.ArduinoClient

class HardwareManager {

    private val deviceManagers = ArrayList<ArduinoClient>()

    fun addDeviceManager(device: ArduinoClient) {
        deviceManagers += device
    }

    fun togglePowerSocket(state:Boolean): String {
        return if (state) deviceManagers[0].sendCommand("socket_power_on") else deviceManagers[0].sendCommand("socket_power_off")
    }

    fun togglePC(state: Boolean): String {
        return if (state) deviceManagers[0].sendCommand("pc_power_on") else deviceManagers[0].sendCommand("pc_power_off")
    }

    fun getConfiguration(): String {
        val response = deviceManagers[0].sendCommand("get_configuration")
        if (response == "COMMAND_NOT_SUPPORTED") {
            println("RETRYING")
            return getConfiguration()
        }

        return response
    }
}
import com.pi4j.io.gpio.GpioPinDigitalOutput
import com.pi4j.io.gpio.PinState

class HardwareManager(private val socketPin: GpioPinDigitalOutput, val pcPin: GpioPinDigitalOutput) {

    fun setSocketState(state: Boolean) {
        if (state) {
            socketPin.high()
        } else {
            socketPin.low()
        }
    }

    fun setPcState(state: Boolean) {
        pcPin.pulse(50)
    }

}
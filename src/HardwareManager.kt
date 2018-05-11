import com.pi4j.io.gpio.GpioPinDigitalOutput
import com.pi4j.io.gpio.PinState

class HardwareManager(private val socketPin: GpioPinDigitalOutput, val pcPin: GpioPinDigitalOutput) {

    fun setSocketState(state: Boolean) {
        socketPin.state = if (state) PinState.HIGH else PinState.LOW
    }

    fun setPcState(state: Boolean) {
        socketPin.pulse(50)
    }

}
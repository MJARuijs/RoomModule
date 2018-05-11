import com.pi4j.io.gpio.GpioPinDigitalInput

class MotionSensor(val sensorPin: GpioPinDigitalInput, val callback: MotionSensorCallback) {

    private var lastMovementDetected = 0L
    var movementDetected = false

    var stateChanged = false

    fun update() {
        val currentTime = System.currentTimeMillis()
        stateChanged = false

        if (!movementDetected && sensorPin.isHigh) {
            movementDetected = true
            callback.onStateChanged(true)
        }

    }

    companion object {
        private const val LIGHT_OFF_DELAY = 10
    }

    interface MotionSensorCallback {
        fun onStateChanged(state: Boolean)
    }
}
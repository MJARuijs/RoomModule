import com.pi4j.io.gpio.GpioPinDigitalInput

class MotionSensor(private val sensorPin: GpioPinDigitalInput, private val callback: MotionSensorCallback) {

    private var enabled = false

    private var lastMovementDetected = 0L
    private var movementDetected = false

    fun update() {
        if (!enabled) {
            return
        }

        val currentTime = System.currentTimeMillis()

        if (sensorPin.isHigh) {
            lastMovementDetected = currentTime

            if (!movementDetected) {
                movementDetected = true
                callback.onStateChanged(true)
            }
        } else if (movementDetected && sensorPin.isLow) {
            if (currentTime > lastMovementDetected + LIGHT_OFF_DELAY) {
                movementDetected = false
                callback.onStateChanged(false)
            }
        }

    }

    fun isEnabled(): Boolean {
        return enabled
    }

    fun enable(): String {
        enabled = true
        return "SUCCESS"
    }

    fun disable(): String {
        enabled = false
        return "SUCCESS"
    }

    companion object {
        private const val LIGHT_OFF_DELAY = 5000
    }

    interface MotionSensorCallback {
        fun onStateChanged(state: Boolean)
    }
}
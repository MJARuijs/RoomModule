import com.pi4j.io.gpio.GpioPinDigitalInput

class MotionSensor(private val sensorPin: GpioPinDigitalInput, private val callback: MotionSensorCallback) {

    private var lastMovementDetected = 0L
    var movementDetected = false

    fun update() {
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

    companion object {
        private const val LIGHT_OFF_DELAY = 5000
    }

    interface MotionSensorCallback {
        fun onStateChanged(state: Boolean)
    }
}
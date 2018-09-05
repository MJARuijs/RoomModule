import com.pi4j.io.gpio.GpioPinDigitalInput
import kotlin.math.roundToInt
import kotlin.math.roundToLong

class MotionSensor(private val sensorPin: GpioPinDigitalInput, private val callback: MotionSensorCallback) {

    var enabled = true

    private var lastMovementDetected = 0L
    private var movementDetected = false
    private var checkPresence = false

    fun update() {
        if (!enabled) {
            return
        }

        val currentTime = System.currentTimeMillis()

        if (sensorPin.isHigh) {
            lastMovementDetected = currentTime
            checkPresence = false

            if (!movementDetected) {
                movementDetected = true
                callback.onStateChanged(true)
            }
        } else if (movementDetected && sensorPin.isLow) {

            if (currentTime > (lastMovementDetected.toFloat() + 0.9f * LIGHT_OFF_DELAY.toFloat()).roundToLong()) {
                checkPresence = true
            }

            if (checkPresence) {
                checkPresence = false
                println("CHECKING")
                Thread {
                    val runTime = Runtime.getRuntime()
//                    runTime.exec("gpio mode 1 pwm")
//                    runTime.exec("gpio pwm-ms")
//                    runTime.exec("gpio pwmc 192")
//                    runTime.exec("gpio pwmr 2000")

                    //            runTime.exec("gpio pwm 1 152")
                    //            Thread.sleep(5000)

//                    runTime.exec("gpio pwm 1 180")
                    Thread.sleep(250)

//                    runTime.exec("gpio pwm 1 220")
                    Thread.sleep(250)

//                    runTime.exec("gpio pwm 1 200")
                    Thread.sleep(250)
                    println("DONE")
                }.start()
            }

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

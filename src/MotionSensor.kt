import com.pi4j.io.gpio.GpioPinDigitalInput
import kotlin.math.roundToLong

class MotionSensor(private val sensorPin: GpioPinDigitalInput, private val callback: MotionSensorCallback) {

    var enabled = true

    private var lastMovementDetected = 0L
    private var movementDetected = false
    private var presenceChecked = false

    fun update() {
        if (!enabled) {
            return
        }

        val currentTime = System.currentTimeMillis()

        if (sensorPin.isHigh) {
            lastMovementDetected = currentTime
            presenceChecked = false

            if (!movementDetected) {
                movementDetected = true
                callback.onStateChanged(true)
            }
        } else if (movementDetected && sensorPin.isLow) {

            if (!presenceChecked && currentTime > (lastMovementDetected.toFloat() + 0.9f * LIGHT_OFF_DELAY.toFloat()).roundToLong()) {
//                Thread {
                    println("CHECKING")
                    val runTime = Runtime.getRuntime()
                    runTime.exec("gpio mode 1 pwm")
                    runTime.exec("gpio pwm-ms")
                    runTime.exec("gpio pwmc 192")
                    runTime.exec("gpio pwmr 2000")

                    //            runTime.exec("gpio pwm 1 152")
                    //            Thread.sleep(5000)

                    runTime.exec("gpio pwm 1 180")
                    Thread.sleep(250)

                    runTime.exec("gpio pwm 1 220")
                    Thread.sleep(250)

                    runTime.exec("gpio pwm 1 200")
                    Thread.sleep(250)
                    println("DONE")
//                }.start()
                presenceChecked = true
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

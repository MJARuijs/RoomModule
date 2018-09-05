import com.pi4j.io.gpio.GpioPinDigitalInput
import com.pi4j.io.gpio.GpioPinDigitalOutput
import com.pi4j.io.gpio.RaspiPin
import kotlin.math.roundToLong

class MotionSensor(private val sensorPin: GpioPinDigitalInput, private val powerPin: GpioPinDigitalOutput, private val callback: MotionSensorCallback) {

    var enabled = true

    private var lastMovementDetected = 0L
    private var movementDetected = false
    private var presenceChecked = false

    init {
        powerPin.setState(true)
    }

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

            if (!presenceChecked && currentTime > lastMovementDetected + 8000) {
                Thread {
                    println("CHECKING")
                    powerPin.setState(false)
                    Thread.sleep(10)
                    powerPin.setState(true)
                    println(sensorPin.isHigh)
//                    val runTime = Runtime.getRuntime()
//                    runTime.exec("gpio mode 1 pwm")
//                    runTime.exec("gpio pwm-ms")
//                    runTime.exec("gpio pwmc 192")
//                    runTime.exec("gpio pwmr 2000")
//
//                    //            runTime.exec("gpio pwm 1 152")
//                    //            Thread.sleep(5000)
//
//                    runTime.exec("gpio pwm 1 150")
//                    Thread.sleep(500)
//
//                    runTime.exec("gpio pwm 1 250")
//                    Thread.sleep(500)
//
//                    runTime.exec("gpio pwm 1 200")
//                    Thread.sleep(500)
                    println("DONE")
                }.start()
                presenceChecked = true
            } else if (currentTime > lastMovementDetected + LIGHT_OFF_DELAY) {
                movementDetected = false
                callback.onStateChanged(false)
            }
        }
    }

    companion object {
        private const val LIGHT_OFF_DELAY = 10000
    }

    interface MotionSensorCallback {
        fun onStateChanged(state: Boolean)
    }
}

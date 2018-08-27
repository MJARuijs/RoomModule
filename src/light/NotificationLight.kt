package light

object NotificationLight {

    private var startTime = 0L
    private var enabled = false
    private lateinit var color: Color

    fun startLighting(color: Color) {
        startTime = System.currentTimeMillis()
        enabled = true
        this.color = color
    }

    fun update() {
        val currentTime = System.currentTimeMillis()
        val deltaTime = currentTime - startTime
        println(deltaTime)
    }

    fun stopLighting() {
        enabled = false
        startTime = 0L
    }

}
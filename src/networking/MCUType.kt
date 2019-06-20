package networking

enum class MCUType {

    UNKNOWN,

    SHUTTER_CONTROLLER,

    SHUTTER_BUTTONS,

    PRESENCE_DETECTOR,

    LED_STRIP_CONTROLLER,

    PC_CONTROLLER,

    TV_CONTROLLER,

    PHONE;

    companion object {
        fun fromString(string: String): MCUType {

            val type = string.removePrefix("Type: ")
            return when (type) {
                "SHUTTER_CONTROLLER" -> SHUTTER_CONTROLLER
                "SHUTTER_BUTTONS" -> SHUTTER_BUTTONS
                "PRESENCE_DETECTOR" -> PRESENCE_DETECTOR
                "LED_STRIP_CONTROLLER" -> LED_STRIP_CONTROLLER
                "PC_CONTROLLER" -> PC_CONTROLLER
                "TV_CONTROLLER" -> TV_CONTROLLER
                "PHONE" -> PHONE
                else -> UNKNOWN
            }

        }
    }


}
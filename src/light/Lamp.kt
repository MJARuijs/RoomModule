package light

import light.rgb.RGBState
import light.white.WhiteState

abstract class Lamp(val id: Int, var state: State = RGBState(false, Color(0.0f, 0.0f, 0.0f))) {

    init {
//        state = LightController.getState(this)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }

        if (other == null) {
            return false
        }

        if (other is Lamp) {
            return id == other.id
        }

        return false
    }
}
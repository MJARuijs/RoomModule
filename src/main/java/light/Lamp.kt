package light

import light.rgb.RGBState

abstract class Lamp(val id: Int, var state: State = RGBState(false, HSBColor(0.0f, 0.0f, 0.0f))) {

//    override fun equals(other: Any?): Boolean {
//        if (this === other) {
//            return true
//        }
//
//        if (other == null) {
//            return false
//        }
//
//        if (other is Lamp) {
//            return id == other.id
//        }
//
//        return false
//    }
}
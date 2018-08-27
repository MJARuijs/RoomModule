package light.rgb

import light.Color
import light.State

class RGBState(on: Boolean, var color: Color) : State(on)
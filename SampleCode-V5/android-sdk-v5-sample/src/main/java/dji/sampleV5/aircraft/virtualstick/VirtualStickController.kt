package dji.sampleV5.aircraft.virtualstick

import dji.sampleV5.aircraft.models.VirtualStickVM
import dji.sdk.keyvalue.value.flightcontroller.VirtualStickFlightControlParam

class VirtualStickController(private val virtualStickVM: VirtualStickVM) {

    fun sendControl(yaw: Double, roll: Double, pitch: Double, vertical: Double) {
        val param = VirtualStickFlightControlParam().apply {
            this.yaw = yaw
            this.roll = roll
            this.pitch = pitch
            this.verticalThrottle = vertical
        }
        virtualStickVM.sendVirtualStickAdvancedParam(param)
    }
}
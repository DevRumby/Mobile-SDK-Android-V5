package dji.sampleV5.aircraft.models

import dji.sdk.keyvalue.key.FlightAssistantKey
import dji.sdk.keyvalue.key.FlightControllerKey
import dji.sdk.keyvalue.value.common.EmptyMsg
import dji.sdk.keyvalue.value.flightassistant.LandingProtectionState
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError
import dji.v5.et.action
import dji.v5.et.create
import dji.v5.et.get
import dji.sampleV5.aircraft.util.ToastUtils


class BasicAircraftControlVM : DJIViewModel() {

    fun startTakeOff(callback: CommonCallbacks.CompletionCallbackWithParam<EmptyMsg>) {
        FlightControllerKey.KeyStartTakeoff.create().action({
            callback.onSuccess(it)
        }, { e: IDJIError ->
            callback.onFailure(e)
        })
    }

    fun startLanding(callback: CommonCallbacks.CompletionCallbackWithParam<EmptyMsg>) {
        FlightControllerKey.KeyStartAutoLanding.create().action({
            callback.onSuccess(it)
        }, { e: IDJIError ->
            callback.onFailure(e)
        })
    }

    fun confirmLanding(callback: CommonCallbacks.CompletionCallbackWithParam<EmptyMsg>) {


        val confirmLanding = FlightControllerKey.KeyIsLandingConfirmationNeeded.create().get();
            if (confirmLanding == true && FlightAssistantKey.KeyLandingProtectionState.create().get() != LandingProtectionState.NOT_SAFE_TO_LAND){
                FlightControllerKey.KeyConfirmLanding.create().action({
                    callback.onSuccess(it)
                }, { e: IDJIError ->
                    callback.onFailure(e)
                })
        }
        ToastUtils.showToast("confirmLanding: $confirmLanding")
        ToastUtils.showToast("LandingProtectionState: ${FlightAssistantKey.KeyLandingProtectionState.create().get()}")

    }
}
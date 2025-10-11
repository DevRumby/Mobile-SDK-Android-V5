package dji.sampleV5.aircraft.virtualstick;

import android.content.Context;
import android.content.ContextWrapper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;


import dji.sampleV5.aircraft.models.BasicAircraftControlVM;
import dji.sdk.keyvalue.key.FlightControllerKey;
import dji.sdk.keyvalue.value.common.EmptyMsg;
import dji.sdk.keyvalue.value.flightcontroller.FlightControlAuthorityChangeReason;
import dji.sdk.keyvalue.value.flightcontroller.VirtualStickFlightControlParam;
import dji.v5.common.callback.CommonCallbacks;
import dji.v5.common.error.IDJIError;
import dji.v5.manager.KeyManager;
import dji.v5.manager.SDKManager;
import dji.v5.manager.aircraft.virtualstick.VirtualStickManager;
import dji.v5.manager.aircraft.virtualstick.VirtualStickState;
import dji.v5.manager.aircraft.virtualstick.VirtualStickStateListener;
import android.widget.Toast;

public class JoystickBCIController {

    private BasicAircraftControlVM aircraftControlVM;
    private static final String TAG = "JoystickBCI";
    private static final float MAX_VELOCITY = 15.0f; // m/s
    private static final float MAX_YAW_ANGULAR_VELOCITY = 30.0f; // degrees/s
    private static final float MAX_VERTICAL_VELOCITY = 4.0f; // m/s

    private boolean isVirtualStickEnabled = false;
    private boolean isBCIControlActive = false;
    private Context context;

    public void initialize(FragmentActivity activity) {
        this.context = activity.getApplicationContext();
        aircraftControlVM = new ViewModelProvider(activity).get(BasicAircraftControlVM.class);
    }
    private void setupVirtualStickMode() {
        VirtualStickManager.getInstance().setVirtualStickStateListener(new VirtualStickStateListener() {
            @Override
            public void onVirtualStickStateUpdate(@NonNull VirtualStickState virtualStickState) {
                isVirtualStickEnabled = virtualStickState.isVirtualStickEnable();
                Log.d(TAG, "Virtual stick state: " + virtualStickState.isVirtualStickEnable());
            }

            @Override
            public void onChangeReasonUpdate(@NonNull FlightControlAuthorityChangeReason reason) {
                Log.d(TAG, "Virtual stick change reason: " + reason);

            }

        });
    }

    public interface BCIStatusListener {
        void onBCIStatusChanged(boolean isReady, String statusMessage);
    }

    private BCIStatusListener statusListener;

    public void setBCIStatusListener(BCIStatusListener listener) {
        this.statusListener = listener;
    }

    public void enableBCIControl(CommonCallbacks.CompletionCallback callback) {
        Log.d(TAG, "Attempting to enable BCI control...");

        // Check aircraft connection using AircraftManager
        boolean isConnected = false;
        try {
            isConnected = SDKManager.getInstance().isRegistered();
        } catch (Exception e) {
            Log.e(TAG, "Error checking aircraft connection: " + e.getMessage());
        }

        if (!isConnected) {
            Log.e(TAG, "Cannot enable BCI control - aircraft not connected");
            if (statusListener != null) {
                statusListener.onBCIStatusChanged(false, "BCI Failed: Aircraft Not Connected");
            }
            return;
        }

        VirtualStickManager.getInstance().enableVirtualStick(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onSuccess() {
                isVirtualStickEnabled = true;
                isBCIControlActive = true;
                Log.d(TAG, "BCI control enabled successfully");

                if (statusListener != null) {
                    statusListener.onBCIStatusChanged(true, "BCI Is Ready to use");
                }

                if (callback != null) {
                    callback.onSuccess();
                }
            }

            @Override
            public void onFailure(IDJIError error) {
                Log.e(TAG, "Failed to enable BCI control: " + error.description());

                if (statusListener != null) {
                    statusListener.onBCIStatusChanged(false, "BCI Failed: " + error.description());
                }

                if (callback != null) {
                    callback.onFailure(error);
                }
            }
        });
    }


    public void disableBCIControl(CommonCallbacks.CompletionCallback callback) {
        VirtualStickManager.getInstance().disableVirtualStick(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onSuccess() {
                isVirtualStickEnabled = false;
                isBCIControlActive = false;
                Log.d(TAG, "BCI control disabled successfully");
                if (callback != null) {
                    callback.onSuccess();
                }
            }

            @Override
            public void onFailure(IDJIError error) {
                Log.e(TAG, "Failed to disable BCI control: " + error.description());
                if (callback != null) {
                    callback.onFailure(error);
                }
            }
        });
    }

    public void initiateTakeoff(CommonCallbacks.CompletionCallback callback) {
        if (!isVirtualStickEnabled) {
            Log.e(TAG, "Cannot takeoff - virtual stick not enabled");
            return;
        }
            aircraftControlVM.startTakeOff(new CommonCallbacks.CompletionCallbackWithParam<EmptyMsg>() {
            @Override
            public void onSuccess(EmptyMsg emptyMsg) {
                Toast.makeText(context, "start takeOff onSuccess.", Toast.LENGTH_SHORT).show();

            }

                @Override
            public void onFailure(@NonNull IDJIError idjiError) {
                Toast.makeText(context, "start landing onSuccess.", Toast.LENGTH_SHORT).show();
            }

        });
    }

    public void initiateLanding(CommonCallbacks.CompletionCallback callback) {
        if (!isVirtualStickEnabled) {
            Log.e(TAG, "Cannot takeoff - virtual stick not enabled");
            return;
        }
        aircraftControlVM.startLanding(new CommonCallbacks.CompletionCallbackWithParam<EmptyMsg>() {
            @Override
            public void onSuccess(EmptyMsg emptyMsg) {
                Toast.makeText(context, "start landing onSuccess.", Toast.LENGTH_SHORT).show();            }

            @Override
            public void onFailure(@NonNull IDJIError idjiError) {
                Toast.makeText(context, "start landing onSuccess.", Toast.LENGTH_SHORT).show();
            }

        });
    }

    public void completeLanding(CommonCallbacks.CompletionCallback callback) {
        if (!isVirtualStickEnabled) {
            Log.e(TAG, "Cannot takeoff - virtual stick not enabled");
            return;
        }
        aircraftControlVM.confirmLanding(new CommonCallbacks.CompletionCallbackWithParam<EmptyMsg>() {
            @Override
            public void onSuccess(EmptyMsg emptyMsg) {
                Toast.makeText(context, "finsh landing onSuccess.", Toast.LENGTH_SHORT).show();            }

            @Override
            public void onFailure(@NonNull IDJIError idjiError) {
                Toast.makeText(context, "finish landing onSuccess.", Toast.LENGTH_SHORT).show();
            }

        });
    }


    public void sendBCIControlData(float rollVelocity, float pitchVelocity,
                                   float yawAngularVelocity, float verticalVelocity) {
        if (!isVirtualStickEnabled || !isBCIControlActive) {
            Log.w(TAG, "BCI control not active");
            return;
        }

        double scaledRoll = clamp(rollVelocity * MAX_VELOCITY, -MAX_VELOCITY, MAX_VELOCITY);
        double scaledPitch = clamp(pitchVelocity * MAX_VELOCITY, -MAX_VELOCITY, MAX_VELOCITY);
        double scaledYaw = clamp(yawAngularVelocity * MAX_YAW_ANGULAR_VELOCITY, -MAX_YAW_ANGULAR_VELOCITY, MAX_YAW_ANGULAR_VELOCITY);
        double scaledVertical = clamp(verticalVelocity * MAX_VERTICAL_VELOCITY, -MAX_VERTICAL_VELOCITY, MAX_VERTICAL_VELOCITY);

        VirtualStickFlightControlParam controlParam = new VirtualStickFlightControlParam();
        controlParam.setRoll(scaledRoll);
        controlParam.setPitch(scaledPitch);
        controlParam.setYaw(scaledYaw);
        controlParam.setVerticalThrottle(scaledVertical);

        VirtualStickManager.getInstance().sendVirtualStickAdvancedParam(controlParam);
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    public void overrideJoystickInput(float leftStickX, float leftStickY,
                                      float rightStickX, float rightStickY) {
        sendBCIControlData(
                leftStickX,           // rollVelocity
                -leftStickY,          // pitchVelocity (invert Y axis)
                rightStickX,          // yawAngularVelocity
                rightStickY           // verticalVelocity
        );
    }

    public boolean isBCIControlActive() {
        return isBCIControlActive;
    }

    public String getControllerStatus() {
        if (!isVirtualStickEnabled) {
            return "Virtual Stick Disabled";
        } else if (!isBCIControlActive) {
            return "BCI Control Inactive";
        } else {
            return "BCI Control Active";
        }
    }
}

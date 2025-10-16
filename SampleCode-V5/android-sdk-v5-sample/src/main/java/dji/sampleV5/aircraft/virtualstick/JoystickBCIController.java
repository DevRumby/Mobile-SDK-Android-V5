package dji.sampleV5.aircraft.virtualstick;

import android.content.Context;
import android.content.ContextWrapper;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;


import dji.sampleV5.aircraft.models.BasicAircraftControlVM;
import dji.sampleV5.aircraft.models.VirtualStickVM;
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
import dji.v5.manager.aircraft.virtualstick.Stick;
import android.widget.Toast;

public class JoystickBCIController {

    private BasicAircraftControlVM aircraftControlVM;
    private VirtualStickVM virtualStickVM;
    private static final String TAG = "JoystickBCI";
    private static final float MAX_VELOCITY = 15.0f; // m/s
    private static final float MAX_YAW_ANGULAR_VELOCITY = 30.0f; // degrees/s
    private static final float MAX_VERTICAL_VELOCITY = 4.0f; // m/s

    private boolean isVirtualStickEnabled = false;
    private boolean isBCIControlActive = false;
    private Context context;
    private Handler mainHandler; // Add Handler for UI thread operations

    public void initialize(FragmentActivity activity) {
        this.context = activity.getApplicationContext();
        this.mainHandler = new Handler(Looper.getMainLooper()); // Initialize Handler
        Log.d(TAG, "Initializing JoystickBCIController for RC2/Mini4Pro");
        try {
            aircraftControlVM = new ViewModelProvider(activity).get(BasicAircraftControlVM.class);
            virtualStickVM = new ViewModelProvider(activity).get(VirtualStickVM.class);
            setupVirtualStickMode();
            Log.d(TAG, "JoystickBCIController initialization completed successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error during JoystickBCIController initialization: " + e.getMessage(), e);
            throw e;
        }
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
        Log.d(TAG, "sendBCIControlData called with values: roll=" + rollVelocity + " pitch=" + pitchVelocity + " yaw=" + yawAngularVelocity + " vertical=" + verticalVelocity);
        
        // Check control state
        Log.d(TAG, "Control state - virtualStick:" + isVirtualStickEnabled + " bciActive:" + isBCIControlActive);
        if (!isVirtualStickEnabled || !isBCIControlActive) {
            Log.w(TAG, "BCI control not active");
            showToast("BCI control not active");
            return;
        }

        // Check virtualStickVM
        Log.d(TAG, "VirtualStickVM null check: " + (virtualStickVM == null));
        if (virtualStickVM == null) {
            Log.e(TAG, "VirtualStickVM is null!");
            showToast("VirtualStickVM is null!");
            throw new RuntimeException("VirtualStickVM not initialized");
        }

        Log.d(TAG, "Converting to stick positions...");
        // Convert normalized values (-1.0 to 1.0) to stick position values
        // Left stick: horizontal = yaw, vertical = throttle (vertical velocity)
        int leftHorizontal = (int) (clamp(yawAngularVelocity, -1.0f, 1.0f) * Stick.MAX_STICK_POSITION_ABS);
        int leftVertical = (int) (clamp(verticalVelocity, -1.0f, 1.0f) * Stick.MAX_STICK_POSITION_ABS);
        
        // Right stick: horizontal = roll, vertical = pitch
        int rightHorizontal = (int) (clamp(rollVelocity, -1.0f, 1.0f) * Stick.MAX_STICK_POSITION_ABS);
        int rightVertical = (int) (clamp(-pitchVelocity, -1.0f, 1.0f) * Stick.MAX_STICK_POSITION_ABS); // Invert pitch

        Log.d(TAG, "Stick positions: leftH=" + leftHorizontal + " leftV=" + leftVertical + 
              " rightH=" + rightHorizontal + " rightV=" + rightVertical);
        
        showToast("Sending stick positions...");

        try {
            Log.d(TAG, "Calling virtualStickVM.setLeftPosition and setRightPosition...");
            virtualStickVM.setLeftPosition(leftHorizontal, leftVertical);
            virtualStickVM.setRightPosition(rightHorizontal, rightVertical);
            Log.d(TAG, "Stick positions set successfully");
            showToast("Control sent successfully!");
        } catch (Exception e) {
            Log.e(TAG, "Error setting stick positions: " + e.getMessage(), e);
            showToast("Control send error: " + e.getMessage());
            throw e;
        }
/*
            VirtualStickFlightControlParam controlParam = new VirtualStickFlightControlParam();
        controlParam.setRoll(scaledRoll);
        controlParam.setPitch(scaledPitch);
        controlParam.setYaw(scaledYaw);
        controlParam.setVerticalThrottle(scaledVertical);
        VirtualStickManager.getInstance().setVirtualStickAdvancedModeEnabled(true);
        VirtualStickManager.getInstance().sendVirtualStickAdvancedParam(controlParam);
    */
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

    public boolean isVirtualStickEnabled() {
        return isVirtualStickEnabled;
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

    private void showToast(String message) {
        // Use Handler to post Toast messages to the UI thread
        mainHandler.post(() -> Toast.makeText(context, message, Toast.LENGTH_SHORT).show());
    }
}

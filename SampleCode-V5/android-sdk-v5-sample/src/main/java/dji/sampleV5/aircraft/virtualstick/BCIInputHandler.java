package dji.sampleV5.aircraft.virtualstick;

import android.util.Log;

import androidx.annotation.NonNull;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import dji.v5.common.callback.CommonCallbacks;
import dji.v5.common.error.IDJIError;
public class BCIInputHandler {

    private static final String TAG = "BCIInputHandler";
    private static final long UPDATE_INTERVAL_MS = 50L; // 20Hz update rate

    private JoystickBCIController joystickController;
    private ScheduledExecutorService executorService;
    private ScheduledFuture<?> inputProcessingTask;
    private boolean isProcessing = false;

    // BCI input values (normalized -1.0 to 1.0)
    private volatile float bciRoll = 0.0f;
    private volatile float bciPitch = 0.0f;
    private volatile float bciYaw = 0.0f;
    private volatile float bciThrottle = 0.0f;

    public BCIInputHandler() {
        joystickController = new JoystickBCIController();
        executorService = Executors.newSingleThreadScheduledExecutor();
    }

    public void startBCIControl() {
        joystickController.enableBCIControl(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onSuccess() {
                startInputProcessing();
                Log.d(TAG, "BCI control started successfully");
            }

            @Override
            public void onFailure(@NonNull IDJIError idjiError) {
                Log.e(TAG, "Failed to start BCI control: " + idjiError.description());
            }
        });
    }

    public void stopBCIControl() {
        stopInputProcessing();
        joystickController.disableBCIControl(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onSuccess() {
                startInputProcessing();
                Log.d(TAG, "BCI control stopped successfully");
            }

            @Override
            public void onFailure(@NonNull IDJIError idjiError) {
                Log.e(TAG, "Failed to stop BCI control: " + idjiError.description());
            }
        });
    }

    private void startInputProcessing() {
        if (isProcessing) return;

        isProcessing = true;
        inputProcessingTask = executorService.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                try {
                    if (isProcessing) {
                        // Send current BCI inputs to joystick controller
                        joystickController.overrideJoystickInput(
                                bciRoll,
                                bciPitch,
                                bciYaw,
                                bciThrottle
                        );
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error processing BCI input: " + e.getMessage());
                }
            }
        }, 0, UPDATE_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    private void stopInputProcessing() {
        isProcessing = false;
        if (inputProcessingTask != null) {
            inputProcessingTask.cancel(true);
            inputProcessingTask = null;
        }
    }

    // Methods to update BCI inputs from your BCI system
    public void updateBCIRoll(float value) {
        bciRoll = Math.max(-1.0f, Math.min(1.0f, value));
    }

    public void updateBCIPitch(float value) {
        bciPitch = Math.max(-1.0f, Math.min(1.0f, value));
    }

    public void updateBCIYaw(float value) {
        bciYaw = Math.max(-1.0f, Math.min(1.0f, value));
    }

    public void updateBCIThrottle(float value) {
        bciThrottle = Math.max(-1.0f, Math.min(1.0f, value));
    }

    // Batch update method for simultaneous input updates
    public void updateAllBCIInputs(float roll, float pitch, float yaw, float throttle) {
        bciRoll = Math.max(-1.0f, Math.min(1.0f, roll));
        bciPitch = Math.max(-1.0f, Math.min(1.0f, pitch));
        bciYaw = Math.max(-1.0f, Math.min(1.0f, yaw));
        bciThrottle = Math.max(-1.0f, Math.min(1.0f, throttle));
    }

    public float[] getCurrentInputs() {
        return new float[]{bciRoll, bciPitch, bciYaw, bciThrottle};
    }

    public boolean isControlActive() {
        return joystickController.isBCIControlActive();
    }

    public void cleanup() {
        stopBCIControl();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(1, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
}

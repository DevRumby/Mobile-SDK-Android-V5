/*
 * Copyright (c) 2018-2020 DJI
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package dji.v5.ux.sample.showcase.defaultlayout;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import dji.sdk.keyvalue.value.common.CameraLensType;
import dji.sdk.keyvalue.value.common.ComponentIndexType;
import dji.v5.manager.datacenter.MediaDataCenter;
import dji.v5.manager.interfaces.ICameraStreamManager;
import dji.v5.network.DJINetworkManager;
import dji.v5.network.IDJINetworkStatusListener;
import dji.v5.utils.common.JsonUtil;
import dji.v5.utils.common.LogPath;
import dji.v5.utils.common.LogUtils;
import dji.v5.ux.R;
import dji.v5.ux.accessory.RTKStartServiceHelper;
import dji.v5.ux.cameracore.widget.autoexposurelock.AutoExposureLockWidget;
import dji.v5.ux.cameracore.widget.cameracontrols.CameraControlsWidget;
import dji.v5.ux.cameracore.widget.cameracontrols.lenscontrol.LensControlWidget;
import dji.v5.ux.cameracore.widget.focusexposureswitch.FocusExposureSwitchWidget;
import dji.v5.ux.cameracore.widget.focusmode.FocusModeWidget;
import dji.v5.ux.cameracore.widget.fpvinteraction.FPVInteractionWidget;
import dji.v5.ux.core.base.SchedulerProvider;
import dji.v5.ux.core.communication.BroadcastValues;
import dji.v5.ux.core.communication.GlobalPreferenceKeys;
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore;
import dji.v5.ux.core.communication.UXKeys;
import dji.v5.ux.core.extension.ViewExtensions;
import dji.v5.ux.core.panel.systemstatus.SystemStatusListPanelWidget;
import dji.v5.ux.core.panel.topbar.TopBarPanelWidget;
import dji.v5.ux.core.util.CameraUtil;
import dji.v5.ux.core.util.DataProcessor;
import dji.v5.ux.core.util.ViewUtil;
import dji.v5.ux.core.widget.fpv.FPVWidget;
import dji.v5.ux.core.widget.hsi.HorizontalSituationIndicatorWidget;
import dji.v5.ux.core.widget.hsi.PrimaryFlightDisplayWidget;
import dji.v5.ux.core.widget.setting.SettingWidget;
import dji.v5.ux.core.widget.simulator.SimulatorIndicatorWidget;
import dji.v5.ux.core.widget.systemstatus.SystemStatusWidget;
import dji.v5.ux.gimbal.GimbalFineTuneWidget;
import dji.v5.ux.map.MapWidget;
import dji.v5.ux.mapkit.core.maps.DJIUiSettings;
import dji.v5.ux.training.simulatorcontrol.SimulatorControlWidget;
import dji.v5.ux.visualcamera.CameraNDVIPanelWidget;
import dji.v5.ux.visualcamera.CameraVisiblePanelWidget;
import dji.v5.ux.visualcamera.zoom.FocalZoomWidget;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

/**
 * Displays a sample layout of widgets similar to that of the various DJI apps.
 */
public class DefaultLayoutActivity extends AppCompatActivity {

    //region Fields
    private final String TAG = LogUtils.getTag(this);

    protected FPVWidget primaryFpvWidget;
    protected FPVInteractionWidget fpvInteractionWidget;
    protected FPVWidget secondaryFPVWidget;
    protected SystemStatusListPanelWidget systemStatusListPanelWidget;
    protected SimulatorControlWidget simulatorControlWidget;
    protected LensControlWidget lensControlWidget;
    protected AutoExposureLockWidget autoExposureLockWidget;
    protected FocusModeWidget focusModeWidget;
    protected FocusExposureSwitchWidget focusExposureSwitchWidget;
    protected CameraControlsWidget cameraControlsWidget;
    protected HorizontalSituationIndicatorWidget horizontalSituationIndicatorWidget;
    protected PrimaryFlightDisplayWidget pfvFlightDisplayWidget;
    protected CameraNDVIPanelWidget ndviCameraPanel;
    protected CameraVisiblePanelWidget visualCameraPanel;
    protected FocalZoomWidget focalZoomWidget;
    protected SettingWidget settingWidget;
    protected MapWidget mapWidget;
    protected TopBarPanelWidget topBarPanel;
    protected ConstraintLayout fpvParentView;
    private DrawerLayout mDrawerLayout;
    private TextView gimbalAdjustDone;
    private GimbalFineTuneWidget gimbalFineTuneWidget;
    private ComponentIndexType lastDevicePosition = ComponentIndexType.UNKNOWN;
    private CameraLensType lastLensType = CameraLensType.UNKNOWN;

    // Debug logging system
    private static final StringBuilder debugLog = new StringBuilder();
    private static final int MAX_DEBUG_LOG_SIZE = 50000; // Limit log size

    private CompositeDisposable compositeDisposable;
    private final DataProcessor<CameraSource> cameraSourceProcessor = DataProcessor.create(new CameraSource(ComponentIndexType.UNKNOWN,
            CameraLensType.UNKNOWN));
    private final IDJINetworkStatusListener networkStatusListener = isNetworkAvailable -> {
        if (isNetworkAvailable) {
            LogUtils.d(TAG, "isNetworkAvailable=" + true);
            RTKStartServiceHelper.INSTANCE.startRtkService(false);
        }
    };
    private final ICameraStreamManager.AvailableCameraUpdatedListener availableCameraUpdatedListener = new ICameraStreamManager.AvailableCameraUpdatedListener() {
        @Override
        public void onAvailableCameraUpdated(@NonNull List<ComponentIndexType> availableCameraList) {
            runOnUiThread(() -> updateFPVWidgetSource(availableCameraList));
        }

        @Override
        public void onCameraStreamEnableUpdate(@NonNull Map<ComponentIndexType, Boolean> cameraStreamEnableMap) {
            //
        }
    };

    //endregion

    //region Lifecycle
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.uxsdk_activity_default_layout);
        fpvParentView = findViewById(R.id.fpv_holder);
        mDrawerLayout = findViewById(R.id.root_view);
        topBarPanel = findViewById(R.id.panel_top_bar);
        settingWidget = topBarPanel.getSettingWidget();
        primaryFpvWidget = findViewById(R.id.widget_primary_fpv);
        fpvInteractionWidget = findViewById(R.id.widget_fpv_interaction);
        secondaryFPVWidget = findViewById(R.id.widget_secondary_fpv);
        systemStatusListPanelWidget = findViewById(R.id.widget_panel_system_status_list);
        simulatorControlWidget = findViewById(R.id.widget_simulator_control);
        lensControlWidget = findViewById(R.id.widget_lens_control);
        ndviCameraPanel = findViewById(R.id.panel_ndvi_camera);
        visualCameraPanel = findViewById(R.id.panel_visual_camera);
        autoExposureLockWidget = findViewById(R.id.widget_auto_exposure_lock);
        focusModeWidget = findViewById(R.id.widget_focus_mode);
        focusExposureSwitchWidget = findViewById(R.id.widget_focus_exposure_switch);
        pfvFlightDisplayWidget = findViewById(R.id.widget_fpv_flight_display_widget);
        focalZoomWidget = findViewById(R.id.widget_focal_zoom);
        cameraControlsWidget = findViewById(R.id.widget_camera_controls);
        horizontalSituationIndicatorWidget = findViewById(R.id.widget_horizontal_situation_indicator);
        gimbalAdjustDone = findViewById(R.id.fpv_gimbal_ok_btn);
        gimbalFineTuneWidget = findViewById(R.id.setting_menu_gimbal_fine_tune);
        mapWidget = findViewById(R.id.widget_map);

        initClickListener();
        
        // Debug: Log initial camera stream manager state
        addDebugLog("=== CAMERA DEBUG: Initializing camera stream ===");
        try {
            ICameraStreamManager streamManager = MediaDataCenter.getInstance().getCameraStreamManager();
            addDebugLog("Camera stream manager: " + (streamManager != null ? "Available" : "NULL"));
            
            streamManager.addAvailableCameraUpdatedListener(availableCameraUpdatedListener);
            
            // Try to get current available cameras immediately
            addDebugLog("Attempting to get current available cameras...");
            
        } catch (Exception e) {
            addDebugLog("ERROR: Error initializing camera stream manager: " + e.getMessage());
            LogUtils.e(TAG, "Error initializing camera stream manager: " + e.getMessage(), e);
        }
        
        primaryFpvWidget.setOnFPVStreamSourceListener((devicePosition, lensType) -> {
            addDebugLog("FPV Stream Source Changed: position=" + devicePosition + ", lens=" + lensType);
            cameraSourceProcessor.onNext(new CameraSource(devicePosition, lensType));
        });

        //小surfaceView放置在顶部，避免被大的遮挡
        secondaryFPVWidget.setSurfaceViewZOrderOnTop(true);
        secondaryFPVWidget.setSurfaceViewZOrderMediaOverlay(true);
        
        // Debug: Set fallback camera sources
        addDebugLog("Setting fallback camera sources...");
        initializeFallbackCameras();


        mapWidget.initMapLibreMap(getApplicationContext(), map -> {
            DJIUiSettings uiSetting = map.getUiSettings();
            if (uiSetting != null) {
                uiSetting.setZoomControlsEnabled(false);//hide zoom widget
            }
        });
        mapWidget.onCreate(savedInstanceState);
        getWindow().setBackgroundDrawable(new ColorDrawable(Color.BLACK));

        //实现RTK监测网络，并自动重连机制
        DJINetworkManager.getInstance().addNetworkStatusListener(networkStatusListener);

    }

    private void isGimableAdjustClicked(BroadcastValues broadcastValues) {
        if (mDrawerLayout.isDrawerOpen(GravityCompat.END)) {
            mDrawerLayout.closeDrawers();
        }
        horizontalSituationIndicatorWidget.setVisibility(View.GONE);
        if (gimbalFineTuneWidget != null) {
            gimbalFineTuneWidget.setVisibility(View.VISIBLE);
        }
    }

    private void initClickListener() {
        secondaryFPVWidget.setOnClickListener(v -> swapVideoSource());

        if (settingWidget != null) {
            settingWidget.setOnClickListener(v -> toggleRightDrawer());
        }

        // Setup top bar state callbacks
        SystemStatusWidget systemStatusWidget = topBarPanel.getSystemStatusWidget();
        if (systemStatusWidget != null) {
            systemStatusWidget.setOnClickListener(v -> ViewExtensions.toggleVisibility(systemStatusListPanelWidget));
        }

        SimulatorIndicatorWidget simulatorIndicatorWidget = topBarPanel.getSimulatorIndicatorWidget();
        if (simulatorIndicatorWidget != null) {
            simulatorIndicatorWidget.setOnClickListener(v -> ViewExtensions.toggleVisibility(simulatorControlWidget));
        }
        gimbalAdjustDone.setOnClickListener(view -> {
            horizontalSituationIndicatorWidget.setVisibility(View.VISIBLE);
            if (gimbalFineTuneWidget != null) {
                gimbalFineTuneWidget.setVisibility(View.GONE);
            }

        });
    }

    private void toggleRightDrawer() {
        mDrawerLayout.openDrawer(GravityCompat.END);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapWidget.onDestroy();
        MediaDataCenter.getInstance().getCameraStreamManager().removeAvailableCameraUpdatedListener(availableCameraUpdatedListener);
        DJINetworkManager.getInstance().removeNetworkStatusListener(networkStatusListener);

    }

    @Override
    protected void onResume() {
        super.onResume();
        
        // Debug: Log connection and camera status on resume
        addDebugLog("=== CAMERA DEBUG: onResume called ===");
        logConnectionStatus();
        
        mapWidget.onResume();
        compositeDisposable = new CompositeDisposable();
        compositeDisposable.add(systemStatusListPanelWidget.closeButtonPressed()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(pressed -> {
                    if (pressed) {
                        ViewExtensions.hide(systemStatusListPanelWidget);
                    }
                }));
        compositeDisposable.add(simulatorControlWidget.getUIStateUpdates()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(simulatorControlWidgetState -> {
                    if (simulatorControlWidgetState instanceof SimulatorControlWidget.UIState.VisibilityUpdated) {
                        if (((SimulatorControlWidget.UIState.VisibilityUpdated) simulatorControlWidgetState).isVisible()) {
                            hideOtherPanels(simulatorControlWidget);
                        }
                    }
                }));
        compositeDisposable.add(cameraSourceProcessor.toFlowable()
                .observeOn(SchedulerProvider.io())
                .throttleLast(500, TimeUnit.MILLISECONDS)
                .subscribeOn(SchedulerProvider.io())
                .subscribe(result -> runOnUiThread(() -> onCameraSourceUpdated(result.devicePosition, result.lensType)))
        );
        compositeDisposable.add(ObservableInMemoryKeyedStore.getInstance()
                .addObserver(UXKeys.create(GlobalPreferenceKeys.GIMBAL_ADJUST_CLICKED))
                .observeOn(SchedulerProvider.ui())
                .subscribe(this::isGimableAdjustClicked));
        ViewUtil.setKeepScreen(this, true);
    }

    @Override
    protected void onPause() {
        if (compositeDisposable != null) {
            compositeDisposable.dispose();
            compositeDisposable = null;
        }
        mapWidget.onPause();
        super.onPause();
        ViewUtil.setKeepScreen(this, false);
    }
    //endregion

    private void hideOtherPanels(@Nullable View widget) {
        View[] panels = {
                simulatorControlWidget
        };

        for (View panel : panels) {
            if (widget != panel) {
                panel.setVisibility(View.GONE);
            }
        }
    }

    private void updateFPVWidgetSource(List<ComponentIndexType> availableCameraList) {
        addDebugLog("=== CAMERA DEBUG: updateFPVWidgetSource called ===");
        addDebugLog("Available cameras JSON: " + JsonUtil.toJson(availableCameraList));
        addDebugLog("Available cameras count: " + (availableCameraList != null ? availableCameraList.size() : "NULL"));
        
        if (availableCameraList == null) {
            addDebugLog("WARNING: Available camera list is NULL - trying fallback initialization");
            initializeFallbackCameras();
            return;
        }

        ArrayList<ComponentIndexType> cameraList = new ArrayList<>(availableCameraList);
        addDebugLog("Camera list after copy: " + cameraList.toString());

        //没有数据
        if (cameraList.isEmpty()) {
            addDebugLog("WARNING: Camera list is EMPTY - hiding secondary and trying fallback for primary");
            secondaryFPVWidget.setVisibility(View.GONE);
            
            // Don't return immediately - try fallback for primary widget
            addDebugLog("Attempting fallback camera initialization for primary FPV widget");
            initializeFallbackCameras();
            return;
        }

        //仅一路数据
        if (cameraList.size() == 1) {
            ComponentIndexType singleSource = availableCameraList.get(0);
            addDebugLog("Single camera detected: " + singleSource + " - setting as primary");
            primaryFpvWidget.updateVideoSource(singleSource);
            secondaryFPVWidget.setVisibility(View.GONE);
            addDebugLog("SUCCESS: Primary FPV widget updated with single source: " + singleSource);
            return;
        }

        //大于两路数据
        addDebugLog("Multiple cameras detected (" + cameraList.size() + ") - configuring primary and secondary");
        ComponentIndexType primarySource = getSuitableSource(cameraList, ComponentIndexType.LEFT_OR_MAIN);
        addDebugLog("Selected primary source: " + primarySource);
        primaryFpvWidget.updateVideoSource(primarySource);
        cameraList.remove(primarySource);

        ComponentIndexType secondarySource = getSuitableSource(cameraList, ComponentIndexType.FPV);
        addDebugLog("Selected secondary source: " + secondarySource);
        secondaryFPVWidget.updateVideoSource(secondarySource);

        secondaryFPVWidget.setVisibility(View.VISIBLE);
        addDebugLog("SUCCESS: Both FPV widgets configured - Primary: " + primarySource + ", Secondary: " + secondarySource);
    }

    private ComponentIndexType getSuitableSource(List<ComponentIndexType> cameraList, ComponentIndexType defaultSource) {
        addDebugLog("getSuitableSource called with list: " + cameraList + ", default: " + defaultSource);
        
        if (cameraList.contains(ComponentIndexType.LEFT_OR_MAIN)) {
            addDebugLog("Selected LEFT_OR_MAIN camera");
            return ComponentIndexType.LEFT_OR_MAIN;
        } else if (cameraList.contains(ComponentIndexType.RIGHT)) {
            addDebugLog("Selected RIGHT camera");
            return ComponentIndexType.RIGHT;
        } else if (cameraList.contains(ComponentIndexType.UP)) {
            addDebugLog("Selected UP camera");
            return ComponentIndexType.UP;
        } else if (cameraList.contains(ComponentIndexType.PORT_1)) {
            addDebugLog("Selected PORT_1 camera");
            return ComponentIndexType.PORT_1;
        } else if (cameraList.contains(ComponentIndexType.PORT_2)) {
            addDebugLog("Selected PORT_2 camera");
            return ComponentIndexType.PORT_2;
        } else if (cameraList.contains(ComponentIndexType.PORT_3)) {
            addDebugLog("Selected PORT_3 camera (mapped to PORT_4)");
            return ComponentIndexType.PORT_4;
        } else if (cameraList.contains(ComponentIndexType.PORT_4)) {
            addDebugLog("Selected PORT_4 camera");
            return ComponentIndexType.PORT_4;
        } else if (cameraList.contains(ComponentIndexType.VISION_ASSIST)) {
            addDebugLog("Selected VISION_ASSIST camera");
            return ComponentIndexType.VISION_ASSIST;
        }
        addDebugLog("WARNING: No suitable camera found, using default: " + defaultSource);
        return defaultSource;
    }

    private void onCameraSourceUpdated(ComponentIndexType devicePosition, CameraLensType lensType) {
        LogUtils.i(LogPath.SAMPLE, "onCameraSourceUpdated", devicePosition, lensType);
        if (devicePosition == lastDevicePosition && lensType == lastLensType) {
            return;
        }
        lastDevicePosition = devicePosition;
        lastLensType = lensType;
        updateViewVisibility(devicePosition, lensType);
        updateInteractionEnabled();
        //如果无需使能或者显示的，也就没有必要切换了。
        if (fpvInteractionWidget.isInteractionEnabled()) {
            fpvInteractionWidget.updateCameraSource(devicePosition, lensType);
        }
        if (lensControlWidget.getVisibility() == View.VISIBLE) {
            lensControlWidget.updateCameraSource(devicePosition, lensType);
        }
        if (ndviCameraPanel.getVisibility() == View.VISIBLE) {
            ndviCameraPanel.updateCameraSource(devicePosition, lensType);
        }
        if (visualCameraPanel.getVisibility() == View.VISIBLE) {
            visualCameraPanel.updateCameraSource(devicePosition, lensType);
        }
        if (autoExposureLockWidget.getVisibility() == View.VISIBLE) {
            autoExposureLockWidget.updateCameraSource(devicePosition, lensType);
        }
        if (focusModeWidget.getVisibility() == View.VISIBLE) {
            focusModeWidget.updateCameraSource(devicePosition, lensType);
        }
        if (focusExposureSwitchWidget.getVisibility() == View.VISIBLE) {
            focusExposureSwitchWidget.updateCameraSource(devicePosition, lensType);
        }
        if (cameraControlsWidget.getVisibility() == View.VISIBLE) {
            cameraControlsWidget.updateCameraSource(devicePosition, lensType);
        }
        if (focalZoomWidget.getVisibility() == View.VISIBLE) {
            focalZoomWidget.updateCameraSource(devicePosition, lensType);
        }
        if (horizontalSituationIndicatorWidget.getVisibility() == View.VISIBLE) {
            horizontalSituationIndicatorWidget.updateCameraSource(devicePosition, lensType);
        }
    }

    private void updateViewVisibility(ComponentIndexType devicePosition, CameraLensType lensType) {
        //只在fpv下显示
        pfvFlightDisplayWidget.setVisibility(CameraUtil.isFPVTypeView(devicePosition) ? View.VISIBLE : View.INVISIBLE);

        //fpv下不显示
        lensControlWidget.setVisibility(CameraUtil.isFPVTypeView(devicePosition) ? View.INVISIBLE : View.VISIBLE);
        ndviCameraPanel.setVisibility(CameraUtil.isFPVTypeView(devicePosition) ? View.INVISIBLE : View.VISIBLE);
        visualCameraPanel.setVisibility(CameraUtil.isFPVTypeView(devicePosition) ? View.INVISIBLE : View.VISIBLE);
        autoExposureLockWidget.setVisibility(CameraUtil.isFPVTypeView(devicePosition) ? View.INVISIBLE : View.VISIBLE);
        focusModeWidget.setVisibility(CameraUtil.isFPVTypeView(devicePosition) ? View.INVISIBLE : View.VISIBLE);
        focusExposureSwitchWidget.setVisibility(CameraUtil.isFPVTypeView(devicePosition) ? View.INVISIBLE : View.VISIBLE);
        cameraControlsWidget.setVisibility(CameraUtil.isFPVTypeView(devicePosition) ? View.INVISIBLE : View.VISIBLE);
        focalZoomWidget.setVisibility(CameraUtil.isFPVTypeView(devicePosition) ? View.INVISIBLE : View.VISIBLE);
        horizontalSituationIndicatorWidget.setSimpleModeEnable(CameraUtil.isFPVTypeView(devicePosition));

        //只在部分len下显示
        ndviCameraPanel.setVisibility(CameraUtil.isSupportForNDVI(lensType) ? View.VISIBLE : View.INVISIBLE);
    }

    /**
     * Swap the video sources of the FPV and secondary FPV widgets.
     */
    private void swapVideoSource() {
        ComponentIndexType primarySource = primaryFpvWidget.getWidgetModel().getCameraIndex();
        ComponentIndexType secondarySource = secondaryFPVWidget.getWidgetModel().getCameraIndex();
        //两个source都存在的情况下才进行切换
        if (primarySource != ComponentIndexType.UNKNOWN && secondarySource != ComponentIndexType.UNKNOWN) {
            primaryFpvWidget.updateVideoSource(secondarySource);
            secondaryFPVWidget.updateVideoSource(primarySource);
        }
    }

    private void updateInteractionEnabled() {
        fpvInteractionWidget.setInteractionEnabled(!CameraUtil.isFPVTypeView(primaryFpvWidget.getWidgetModel().getCameraIndex()));
    }
    
    /**
     * Initialize fallback camera sources when automatic detection fails
     */
    private void initializeFallbackCameras() {
        addDebugLog("=== CAMERA DEBUG: initializeFallbackCameras called ===");
        
        // Try common camera sources for Mini 4 Pro
        ComponentIndexType[] fallbackSources = {
            ComponentIndexType.LEFT_OR_MAIN,
            ComponentIndexType.FPV,
            ComponentIndexType.RIGHT,
            ComponentIndexType.UP,
            ComponentIndexType.PORT_1
        };
        
        for (ComponentIndexType source : fallbackSources) {
            addDebugLog("Trying fallback camera source: " + source);
            try {
                primaryFpvWidget.updateVideoSource(source);
                addDebugLog("SUCCESS: Successfully set fallback camera source: " + source);
                
                // Hide secondary widget when using fallback
                secondaryFPVWidget.setVisibility(View.GONE);
                break;
            } catch (Exception e) {
                addDebugLog("FAILED: Failed to set fallback source " + source + ": " + e.getMessage());
            }
        }
    }

    private static class CameraSource {
        ComponentIndexType devicePosition;
        CameraLensType lensType;

        public CameraSource(ComponentIndexType devicePosition, CameraLensType lensType) {
            this.devicePosition = devicePosition;
            this.lensType = lensType;
        }
    }

    /**
     * Log current connection and camera status for debugging
     */
    private void logConnectionStatus() {
        try {
            addDebugLog("=== CONNECTION STATUS DEBUG ===");
            
            // Check SDK registration
            boolean isRegistered = dji.v5.manager.SDKManager.getInstance().isRegistered();
            addDebugLog("SDK Registered: " + isRegistered);
            
            // Check product connection
            try {
                // SDK V5 doesn't have getProduct() method - check via registration status
                boolean hasProduct = dji.v5.manager.SDKManager.getInstance().isRegistered();
                addDebugLog("Product Connected: " + hasProduct);
                
                // Try to get product type from DataCenter instead
                try {
                    addDebugLog("Product Type: Available via DataCenter");
                } catch (Exception productEx) {
                    addDebugLog("Product Type: Unable to determine - " + productEx.getMessage());
                }
            } catch (Exception e) {
                addDebugLog("ERROR: Error checking product: " + e.getMessage());
            }
            
            // Check camera stream manager
            try {
                ICameraStreamManager streamManager = MediaDataCenter.getInstance().getCameraStreamManager();
                addDebugLog("Camera Stream Manager Available: " + (streamManager != null));
            } catch (Exception e) {
                addDebugLog("ERROR: Error accessing camera stream manager: " + e.getMessage());
            }
            
            // Check current FPV widget sources
            ComponentIndexType primarySource = primaryFpvWidget.getWidgetModel().getCameraIndex();
            ComponentIndexType secondarySource = secondaryFPVWidget.getWidgetModel().getCameraIndex();
            addDebugLog("Current Primary FPV Source: " + primarySource);
            addDebugLog("Current Secondary FPV Source: " + secondarySource);
            addDebugLog("Secondary FPV Visibility: " + (secondaryFPVWidget.getVisibility() == View.VISIBLE ? "VISIBLE" : "HIDDEN"));
            
        } catch (Exception e) {
            addDebugLog("ERROR: Error in logConnectionStatus: " + e.getMessage());
            LogUtils.e(TAG, "Error in logConnectionStatus: " + e.getMessage(), e);
        }
    }
    
    /**
     * Add a debug log entry with timestamp
     */
    private static void addDebugLog(String message) {
        synchronized (debugLog) {
            String timestamp = new java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.getDefault())
                    .format(new java.util.Date());
            debugLog.append("[").append(timestamp).append("] ").append(message).append("\n");
            
            // Trim log if it gets too large
            if (debugLog.length() > MAX_DEBUG_LOG_SIZE) {
                String currentLog = debugLog.toString();
                debugLog.setLength(0);
                debugLog.append("[LOG TRIMMED]\n");
                debugLog.append(currentLog.substring(currentLog.length() - (MAX_DEBUG_LOG_SIZE - 1000)));
            }
        }
        
        // Also log to standard Android log for development
        LogUtils.d("CameraDebug", message);
    }
    
    /**
     * Get all debug logs as a formatted string
     */
    public static String getDebugLogs() {
        synchronized (debugLog) {
            if (debugLog.length() == 0) {
                return "No debug logs available yet. Try opening the Default Layout and interacting with the camera.";
            }
            return debugLog.toString();
        }
    }
    
    /**
     * Clear debug logs
     */
    public static void clearDebugLogs() {
        synchronized (debugLog) {
            debugLog.setLength(0);
            addDebugLog("Debug logs cleared");
        }
    }
    
    @Override
    public void onBackPressed() {
        if (mDrawerLayout.isDrawerOpen(GravityCompat.END)) {
            mDrawerLayout.closeDrawers();
        } else {
            super.onBackPressed();
        }
    }
}

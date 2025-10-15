package dji.sampleV5.aircraft

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.activityViewModels
import dji.sampleV5.aircraft.databinding.ActivityMainBinding
import dji.sampleV5.aircraft.models.BaseMainActivityVm
import dji.sampleV5.aircraft.models.BasicAircraftControlVM
import dji.sampleV5.aircraft.models.MSDKInfoVm
import dji.sampleV5.aircraft.models.MSDKManagerVM
import dji.sampleV5.aircraft.models.VirtualStickVM
import dji.sampleV5.aircraft.models.globalViewModels
import dji.sampleV5.aircraft.util.Helper
import dji.sampleV5.aircraft.util.ToastUtils
import dji.sampleV5.aircraft.virtualstick.JoystickBCIController
import dji.v5.manager.SDKManager
import dji.v5.utils.common.LogUtils
import dji.v5.utils.common.PermissionUtil
import dji.v5.utils.common.StringUtils
import io.reactivex.rxjava3.disposables.CompositeDisposable
import kotlin.getValue

/**
 * Class Description
 *
 * @author Hoker
 * @date 2022/2/10
 *
 * Copyright (c) 2022, DJI All Rights Reserved.
 */
abstract class DJIMainActivity : AppCompatActivity(), JoystickBCIController.BCIStatusListener {

    val tag: String = LogUtils.getTag(this)

    private var bciServer: BCIHttpServer? = null

    private var uriTextView: TextView? = null
    private var versionTextView: TextView? = null

    private val BCI_SERVER_PORT = 8080
    private val BCI_APP_VERSION = "v0.0.1"

    private lateinit var bciStatusTextView: TextView
    private lateinit var joystickController: JoystickBCIController


    private val permissionArray = arrayListOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.KILL_BACKGROUND_PROCESSES,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION,
    )

    init {
        permissionArray.apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//                add(Manifest.permission.READ_MEDIA_IMAGES)
//                add(Manifest.permission.READ_MEDIA_VIDEO)
//                add(Manifest.permission.READ_MEDIA_AUDIO)
            } else {
                add(Manifest.permission.READ_EXTERNAL_STORAGE)
                add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
    }

    private val baseMainActivityVm: BaseMainActivityVm by viewModels()
    private val msdkInfoVm: MSDKInfoVm by viewModels()
    private val msdkManagerVM: MSDKManagerVM by globalViewModels()
    private lateinit var binding: ActivityMainBinding
    private val handler: Handler = Handler(Looper.getMainLooper())
    private val disposable = CompositeDisposable()


    abstract fun prepareUxActivity()

    abstract fun prepareTestingToolsActivity()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 有一些手机从系统桌面进入的时候可能会重启main类型的activity
        // 需要校验这种情况，业界标准做法，基本所有app都需要这个
        if (!isTaskRoot && intent.hasCategory(Intent.CATEGORY_LAUNCHER) && Intent.ACTION_MAIN == intent.action) {

                finish()
                return

        }

        window.decorView.apply {
            systemUiVisibility =
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        }

        initMSDKInfoView()
        observeSDKManager()
        checkPermissionAndRequest()
        bciStatusTextView = findViewById(R.id.bciStatusTextView)

        // Initialize BCI server after permissions
        initBCIServer()
    }

    private fun initBCIServer() {
        val isConnected = try {
            SDKManager.getInstance().isRegistered &&
                    msdkManagerVM.lvProductConnectionState.value?.first == true
        } catch (e: Exception) {
            LogUtils.w(tag, "Could not check connection: ${e.message}")
            false
        }

        if (!isConnected) {
            LogUtils.w(tag, "Aircraft not connected, BCI server will start but virtual stick won't work until connected")
        }

        try {
            joystickController = JoystickBCIController()
            joystickController.initialize(this)
            joystickController.setBCIStatusListener(this)
            // Initialize BCI server with the controller
            bciServer = BCIHttpServer(BCI_SERVER_PORT, joystickController, this)
            bciServer?.start()
            LogUtils.d(tag, "BCI HTTP Server started on port $BCI_SERVER_PORT")

            setupBCIButtons()

            val deviceIp: String? = bciServer?.getDeviceIpAddress()
            val serverUri = "http://$deviceIp:$BCI_SERVER_PORT"
            
            // Initialize version text view (gracefully handle if not in layout)
            try {
                if (versionTextView == null) {
                    versionTextView = findViewById(R.id.versionTextView)
                }
                versionTextView?.text = "BCI Control App $BCI_APP_VERSION"
            } catch (e: Exception) {
                LogUtils.d(tag, "Version TextView not found in layout: ${e.message}")
            }
            
            // Initialize URI text view
            if (uriTextView == null) {
                uriTextView = findViewById(R.id.uriTextView) // Add this ID to your layout
            }
            uriTextView?.text = "BCI Server: $serverUri"

            LogUtils.d(tag, "BCI Server URI: $serverUri")
            LogUtils.d(tag, "Available endpoints: /enable_bci, /disable_bci, /takeoff, /land, /fland, /send_control")

        } catch (e: java.io.IOException) {
            val errorMsg = "Failed to start BCI server (IOException): ${e.message}"
            uriTextView?.text = errorMsg
            LogUtils.e(tag, errorMsg, e)
            e.printStackTrace()
        } catch (e: Exception) {
            val errorMsg = "Failed to start BCI HTTP server: ${e.message}"
            LogUtils.e(tag, errorMsg, e)
            uriTextView?.text = errorMsg
            e.printStackTrace()
        }
    }

    private fun setupBCIButtons() {
        // Add enable BCI button listener
        binding.enableBciButton.setOnClickListener {
            joystickController?.enableBCIControl(null)
            LogUtils.d(tag, "BCI Control Enabled")
            showToast("BCI Control Enabled")
        }

        // Add disable BCI button listener
        binding.disableBciButton.setOnClickListener {
            joystickController?.disableBCIControl(null)
            LogUtils.d(tag, "BCI Control Disabled")
            showToast("BCI Control Disabled")
        }
    }

    override fun onBCIStatusChanged(isReady: Boolean, statusMessage: String) {
        runOnUiThread {
            bciStatusTextView.text = statusMessage
            bciStatusTextView.setBackgroundColor(
                if (isReady) Color.GREEN else Color.RED
            )
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (checkPermission()) {
            handleAfterPermissionPermitted()
        }
    }

    override fun onResume() {
        super.onResume()
        if (checkPermission()) {
            handleAfterPermissionPermitted()
        }
    }

    private fun handleAfterPermissionPermitted() {
        prepareTestingToolsActivity()
    }

    @SuppressLint("SetTextI18n")
    private fun initMSDKInfoView() {
        msdkInfoVm.msdkInfo.observe(this) {
            binding.textViewVersion.text = StringUtils.getResStr(R.string.sdk_version, it.SDKVersion + " " + it.buildVer)
            binding.textViewProductName.text = StringUtils.getResStr(R.string.product_name, it.productType.name)
            binding.textViewPackageProductCategory.text = StringUtils.getResStr(R.string.package_product_category, it.packageProductCategory)
            binding.textViewIsDebug.text = StringUtils.getResStr(R.string.is_sdk_debug, it.isDebug)
            binding.textCoreInfo.text = it.coreInfo.toString()
        }

        binding.iconSdkForum.setOnClickListener {
            Helper.startBrowser(this, StringUtils.getResStr(R.string.sdk_forum_url))
        }

        binding.iconReleaseNode.setOnClickListener {
            Helper.startBrowser(this, StringUtils.getResStr(R.string.release_node_url))
        }
        binding.iconTechSupport.setOnClickListener {
            Helper.startBrowser(this, StringUtils.getResStr(R.string.tech_support_url))
        }
        binding.viewBaseInfo.setOnClickListener {
            baseMainActivityVm.doPairing {
                showToast(it)
            }
        }
    }

    private fun observeSDKManager() {
        msdkManagerVM.lvRegisterState.observe(this) { resultPair ->
            val statusText: String?
            if (resultPair.first) {
                ToastUtils.showToast("Register Success")
                statusText = StringUtils.getResStr(this, R.string.registered)
                msdkInfoVm.initListener()
                handler.postDelayed({
                    prepareUxActivity()
                }, 5000)
            } else {
                showToast("Register Failure: ${resultPair.second}")
                statusText = StringUtils.getResStr(this, R.string.unregistered)
            }
            binding.textViewRegistered.text = StringUtils.getResStr(R.string.registration_status, statusText)
        }

        msdkManagerVM.lvProductConnectionState.observe(this) { resultPair ->
            showToast("Product: ${resultPair.second} ,ConnectionState:  ${resultPair.first}")
        }

        msdkManagerVM.lvProductChanges.observe(this) { productId ->
            showToast("Product: $productId Changed")
        }

        msdkManagerVM.lvInitProcess.observe(this) { processPair ->
            showToast("Init Process event: ${processPair.first.name}")
        }

        msdkManagerVM.lvDBDownloadProgress.observe(this) { resultPair ->
            showToast("Database Download Progress current: ${resultPair.first}, total: ${resultPair.second}")
        }
    }

    private fun showToast(content: String) {
        ToastUtils.showToast(content)

    }


    fun <T> enableDefaultLayout(cl: Class<T>) {
        enableShowCaseButton(binding.defaultLayoutButton, cl)
    }

    fun <T> enableWidgetList(cl: Class<T>) {
        enableShowCaseButton(binding.widgetListButton, cl)
    }

    fun <T> enableTestingTools(cl: Class<T>) {
        enableShowCaseButton(binding.testingToolButton, cl)
    }

    private fun <T> enableShowCaseButton(view: View, cl: Class<T>) {
        view.isEnabled = true
        view.setOnClickListener {
            Intent(this, cl).also {
                startActivity(it)
            }
        }
    }

    private fun checkPermissionAndRequest() {
        if (!checkPermission()) {
            requestPermission()
        }
    }

    private fun checkPermission(): Boolean {
        for (i in permissionArray.indices) {
            if (!PermissionUtil.isPermissionGranted(this, permissionArray[i])) {
                return false
            }
        }
        return true
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        result?.entries?.forEach {
            if (!it.value) {
                requestPermission()
                return@forEach
            }
        }
    }

    private fun requestPermission() {
        requestPermissionLauncher.launch(permissionArray.toArray(arrayOf()))
    }

    override fun onDestroy() {
        super.onDestroy()
        bciServer?.stop()
        joystickController?.disableBCIControl(null)
        LogUtils.d(tag, "BCI HTTP Server stopped")
        handler.removeCallbacksAndMessages(null)
        disposable.dispose()
    }
}
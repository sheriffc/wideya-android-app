package uk.org.cgatechnologies.wideya

import SecuGen.FDxSDKPro.JSGFPLib
import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.*
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.core.view.isVisible
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.work.WorkInfo
import androidx.work.WorkManager
import cn.liyuyu.akpermission.PermissionRationale
import cn.liyuyu.akpermission.callWithPermissions
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nullwire.trace.ExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import uk.org.cgatechnologies.wideya.common.data.Constants
import uk.org.cgatechnologies.wideya.common.data.database.AppDatabase
import uk.org.cgatechnologies.wideya.common.locationmanager.LocationService
import uk.org.cgatechnologies.wideya.common.utils.Utils
import uk.org.cgatechnologies.wideya.common.viewmodels.SecugenViewModel
import uk.org.cgatechnologies.wideya.databinding.ActivityMainBinding
import uk.org.cgatechnologies.wideya.school_management.models.SchoolModel
import java.util.*

private const val TAG = "MainActivity"
private const val ACTION_USB_PERMISSION = "uk.org.cgatechnologies.fptest.USB_PERMISSION"

@SuppressLint("UnspecifiedImmutableFlag")
class MainActivity : AppCompatActivity() {

    private var _binding: ActivityMainBinding? = null
    private val binding get() = _binding!!

    private lateinit var navController: NavController

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var filter: IntentFilter
    private lateinit var initDialog: AlertDialog
    private lateinit var checkDeviceJob: Job

    private val secugenHandler by viewModels<SecugenViewModel>()
    private var deviceCheck: Boolean = false

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var toolbar: MaterialToolbar

    private lateinit var headerView: View

    val mPermissionIntent: PendingIntent by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.getBroadcast(
                this,
                0,
                Intent(ACTION_USB_PERMISSION),
                PendingIntent.FLAG_IMMUTABLE
            )
        } else {
            PendingIntent.getBroadcast(this, 0, Intent(ACTION_USB_PERMISSION), 0)
        }
    }

    private val mUsbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                    synchronized(this) {
                        Log.d(TAG, "br: device is detached")
                        secugenHandler.sgfplib.CloseDevice()
                    }
                }
                UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                    synchronized(this) {
                        Log.d(TAG, "br: device is attached")
                        deviceCheck()
                    }
                }
                ACTION_USB_PERMISSION -> {
                    synchronized(this) {
                        val device: UsbDevice = secugenHandler.sgfplib.GetUsbDevice()
                        if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                            device.apply {
                                Log.d(TAG, "br: device has permission")
                            }
                        } else {
                            Log.d(TAG, "br: permission denied for device $device")
                            deviceCheck()
                        }
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "MainActivity Going onCreate")

        if (Build.VERSION.SDK_INT >= 31.0) {
            installSplashScreen()
        } else {
            setTheme(R.style.Theme_Wideya)
        }
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setTokenExpiryObserver()

        checkDeviceJob = Job()

        Log.d(TAG, "assign sgfplib")

        secugenHandler.sgfplib =
            JSGFPLib(this, getSystemService(USB_SERVICE) as UsbManager)

        //register receiver
        filter = IntentFilter()
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
        filter.addAction(ACTION_USB_PERMISSION)
        registerReceiver(mUsbReceiver, filter)

        toolbar = binding.toolbar
        setSupportActionBar(toolbar)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main) as NavHostFragment
        navController = navHostFragment.navController

        drawerLayout = binding.drawerLayout

        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.HomeFragment,
                R.id.SchoolProfileFragment,
                R.id.SchoolListFragment,
                R.id.SettingsFragment,
                R.id.SyncFragment,
                R.id.SchoolAnalysisFragment,
                R.id.SchoolTeacherAttendanceListFragment,
                R.id.SchoolLearnerAttendanceListFragment
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)

        binding.contentMain.bottomNavVw.setupWithNavController(navController)
        binding.navDrawerVw.setupWithNavController(navController)

        headerView = binding.navDrawerVw.getHeaderView(0)
        val navMenu = binding.navDrawerVw.menu

        if (BuildConfig.BUILD_TYPE == "prepilot") {
            navMenu.findItem(R.id.SchoolListFragment).isVisible = false
        }

        val enterSchoolProfileNavMenu = navMenu.findItem(R.id.SchoolProfileFragment)
        displaySchoolName()

        val tvWebsiteLink = headerView.findViewById<TextView>(R.id.tvWebsiteLink)
        tvWebsiteLink.setOnClickListener {
            try {
                val myIntent =
                    Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.wideya_url)))
                startActivity(Intent.createChooser(myIntent, "Choose Browser"));
            } catch (e: ActivityNotFoundException) {
                e.printStackTrace()
            }
        }


        binding.apply {
            logoutBtn.setOnClickListener {
                drawerLayout.close()
                logOut()
            }
            tvAppVersionCode.text = getString(R.string.app_version_v, BuildConfig.VERSION_CODE)
            tvAppVersionName.text = BuildConfig.VERSION_NAME
        }

        navController.addOnDestinationChangedListener { _, nd: NavDestination, _ ->
            binding.contentMain.bottomNavVw.apply {
                when (nd.id) {
                    R.id.SchoolProfileFragment,
                    R.id.SchoolAnalysisFragment,
                    R.id.SchoolTeacherAttendanceListFragment,
                    R.id.SchoolLearnerAttendanceListFragment,
                    R.id.LearnerPerformanceFragment -> {
                        visibility = View.VISIBLE
                        enterSchoolProfileNavMenu.isVisible = true
                        displaySchoolName()
                    }
                    R.id.LoginFragment -> {
                        setDrawerLocked()
                        visibility = View.GONE
                        enterSchoolProfileNavMenu.isVisible = false
                        displaySchoolName()
                    }
                    R.id.HomeFragment -> {
                        val username = Utils.getEncSharedPrefs(this@MainActivity)
                            .getString(Constants.PREF_USERNAME, "")
                        if (!username.isNullOrEmpty()) {
                            headerView.findViewById<TextView>(R.id.navHeaderUsername).text =
                                getString(R.string.logged_in_as, username)

                            if (BuildConfig.BUILD_TYPE == "demo") {
                                headerView.findViewById<TextView>(R.id.tvAppVariantTitle).text =
                                    "DEMO"
                                headerView.findViewById<TextView>(R.id.tvAppVariantTitle).visibility =
                                    View.VISIBLE
                            }
                        }
                        val schoolUuid =
                            Utils.getEncSharedPrefs(this@MainActivity)
                                .getString(Constants.PREF_SCHOOL_ID, null)
                        if (!schoolUuid.isNullOrEmpty()) enterSchoolProfileNavMenu.isVisible = true
                    }
                    else -> {
                        visibility = View.GONE
                    }
                }
            }
        }

        startLocationService()
    }


    private fun setDrawerLocked() {
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
        toolbar.navigationIcon = null
    }

    // have hidden navHeaderSchoolName for now since was pretty redundant
    private fun displaySchoolName() {
        return
//        val schoolUuid = Utils.getEncSharedPrefs(this)
//            .getString(Constants.PREF_SCHOOL_ID, null)
//        val navHeaderSchoolName = headerView.findViewById<TextView>(R.id.navHeaderSchoolName)
//        if (!schoolUuid.isNullOrEmpty()) {
//            val schoolModel: SchoolModel =
//                AppDatabase.getDatabase(this, lifecycleScope).schoolManagementDao()
//                    .getSchoolModelById(schoolUuid.toString())
//
//            navHeaderSchoolName.isVisible = true
//            navHeaderSchoolName.text = schoolModel.name
//        } else {
//            navHeaderSchoolName.isVisible = false
//        }
    }

    @SuppressLint("NewApi")
    private fun startLocationService() =
        callWithPermissions(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) {
            onGranted {
                startService(Intent(this@MainActivity, LocationService::class.java))
            }
            onShowRationale {
                showRationaleDialog(R.string.request_location_permission, it)
            }
            onRequestCompleted {
                requestNotificationsPermission()
            }
        }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun requestNotificationsPermission() =
        callWithPermissions(Manifest.permission.POST_NOTIFICATIONS) {
            onGranted {
                createNotificationChannel()
            }
            onShowRationale {
                showRationaleDialog(R.string.request_notification_permission, it)
            }
        }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Wideya Background Notifications"
            val descriptionText = "Alerts user to any background work such as sync"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(Constants.CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            // Register the channel with the system
            val notificationManager: NotificationManager =
                getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }

    fun deviceCheck() {
        if (deviceCheck.not()) {
            deviceCheck = true
            Log.d(TAG, "start device check")
            runOnUiThread {
                waitingDialog()
            }
            if (checkDeviceJob.isActive) checkDeviceJob.cancel()

            checkDeviceJob = lifecycleScope.launch(Dispatchers.Default) {
                try {

                    if (secugenHandler.checkScannerConnected()) {
                        Log.d(TAG, "device connected")
                        if (secugenHandler.checkPermission(
                                secugenHandler.sgfplib.GetUsbDevice(),
                                this@MainActivity
                            )
                        ) {
                            if (secugenHandler.initialiseDevice()) {
                                Log.d(TAG, "device ready")
                                runOnUiThread { closeDialog() }
                            } else {
                                Log.d(TAG, "problem initialising device")
                                runOnUiThread { closeDialog() }
                            }
                        } else {
                            Log.d(TAG, "does not have permission")
                            runOnUiThread { closeDialog() }
                        }
                    } else {
                        Log.d(TAG, "device not connected")
                        runOnUiThread { closeDialog() }
                    }
                } catch (e: Exception) {
                    // Handle exception
                    Log.e(TAG, e.toString())
                    runOnUiThread {
                        closeDialog()
                    }
                }
            }
        }
    }

    private fun isRecreating(): Boolean {
        //consider pre honeycomb not recreating
        secugenHandler.configChanged = true
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB &&
                this.isChangingConfigurations
    }

    private fun setTokenExpiryObserver() {
        WorkManager.getInstance(this)
            .getWorkInfosByTagLiveData(Constants.TOKEN_RELATED_WORKER_TAG)
            .observe(this) { workInfos ->
                if (workInfos != null) {
                    for (workInfo in workInfos) {
                        if (workInfo != null) {
                            if (workInfo.state == WorkInfo.State.FAILED) {
                                if (workInfo.outputData.getString("token") != null) {
                                    Log.d(TAG, "Logging Out")
                                    Log.d(TAG, "Source: ${workInfo.outputData.getString("source")}")
                                    Utils.logout(this)
                                    navController
                                        .navigate(
                                            R.id.LoginFragment, null, NavOptions.Builder()
                                                .setPopUpTo(
                                                    navController.graph.startDestinationId, true
                                                )
                                                .build()
                                        )

                                } else if (workInfo.outputData.getString("school") != null) {
                                    Log.d(TAG, "Getting Out Of School")
                                    Log.d(TAG, "Source: ${workInfo.outputData.getString("source")}")

                                    Utils.clearSavedSchoolPreferences(this)
                                    navController
                                        .navigate(
                                            R.id.SchoolListFragment, null, NavOptions.Builder()
                                                .setPopUpTo(
                                                    navController.graph.startDestinationId, true
                                                )
                                                .build()
                                        )
                                }
                            }
                        }
                    }
                }
            }
    }

    private fun logOut() {
        val builder = MaterialAlertDialogBuilder(this)

        builder.setTitle(R.string.confirm)
            .setMessage(R.string.confirm_logout_message)

        builder.setPositiveButton(R.string.ok) { _, _ ->
            Utils.logout(this)
            navController.navigate(R.id.LoginFragment)
        }
        builder.setNegativeButton(R.string.cancel, null)
        val dialog = builder.create()
        dialog.show()
    }

    override fun onPause() {
        Log.d(TAG, "enter onPause()")
        if (isFinishing) {
            Log.d("onPause", "Finishing")
        } else {
            Log.d(TAG, "not finishing")
            if (isRecreating()) {
                Log.d("onPause", "Rotating")
            } else {
                Log.d("onPause", "Not rotating (task switch / home etc)")
                if (secugenHandler.bSecuGenDeviceOpened) {
                    secugenHandler.sgfplib.CloseDevice()
                    secugenHandler.bSecuGenDeviceOpened = false
                }
                unregisterReceiver(mUsbReceiver)
            }
        }

        super.onPause()
        Log.d(TAG, "Exit onPause()")
    }

    override fun onResume() {
        Log.d(TAG, "enter onResume()")
        deviceCheck()
        super.onResume()
        Log.d(TAG, "not rotated, registering receiver")
        registerReceiver(mUsbReceiver, filter)
        Log.d(TAG, "Exit onResume()")
    }

    override fun onDestroy() {
        Log.d(TAG, "Enter onDestroy()")
        if (isFinishing) {
            Log.d(TAG, "act finishing")
            if (secugenHandler.bSecuGenDeviceOpened) {
                Log.d(TAG, "closing secugen")
                secugenHandler.sgfplib.CloseDevice()
                secugenHandler.sgfplib.Close()
            }
        }
        super.onDestroy()
        Log.d(TAG, "Exit onDestroy()")
    }

    private fun showRationaleDialog(@StringRes messageResId: Int, rationale: PermissionRationale) {
        AlertDialog.Builder(this)
            .setPositiveButton(R.string.permission_allow) { _, _ -> rationale.retry() }
            .setNegativeButton(R.string.permission_deny, null)
            .setCancelable(false)
            .setMessage(messageResId)
            .show()
    }

    private fun waitingDialog() {
        Log.d(TAG, "waiting dialog")
        try {
            val inflater = this.getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
            val builder = AlertDialog.Builder(this)
            val dialogView: View = inflater.inflate(R.layout.dialog_fragment_fp_wait, null)
            builder.setView(dialogView)
            initDialog = builder.create()
            initDialog.setCanceledOnTouchOutside(false)
            initDialog.show()
        } catch (e: Exception) {
            Log.d(TAG, "waiting dialog: $e")
        }
    }

    private fun closeDialog() {
        Log.d(TAG, "close dialog")
        try {
            if (initDialog.isShowing) initDialog.dismiss()
        } catch (e: Exception) {
            Log.d(TAG, "close dialog: $e")
        }
        deviceCheck = false
    }
}
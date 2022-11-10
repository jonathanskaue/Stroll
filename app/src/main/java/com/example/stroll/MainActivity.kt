package com.example.stroll

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.stroll.other.Constants.ACTION_SHOW_MAP_FRAGMENT
import com.example.stroll.presentation.fragment.*
import com.example.stroll.presentation.viewmodel.MainViewModel
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task
import com.google.android.material.bottomnavigation.BottomNavigationView
import dagger.hilt.android.AndroidEntryPoint
import kotlin.collections.ArrayList


//It works!!!!!!!!
@Suppress("DEPRECATION")
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private var activityTrackingEnabled = false
    private lateinit var activityTransitionList: List<ActivityTransition>

    private val TRANSITIONS_RECEIVER_ACTION: String =
        BuildConfig.APPLICATION_ID + "TRANSITIONS_RECEIVER_ACTION"
    private var mActivityTransitionsPendingIntent: PendingIntent? = null
    private var mTransitionsReceiver: TransitionsReceiver? = null

    private lateinit var navController: NavController
    lateinit var bottomNavBar: BottomNavigationView
    private val viewModel: MainViewModel by viewModels()
    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) &&
                    permissions.getOrDefault(Manifest.permission.ACTIVITY_RECOGNITION, false) -> {
                Toast.makeText(
                    applicationContext,
                    "You have given us permission to use your precise location and activity",
                    Toast.LENGTH_SHORT
                ).show()
                navController.navigate(R.id.action_global_mapFragment)
            }
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                Toast.makeText(
                    applicationContext,
                    "you have given us permission to use your precise location, but we need your permissionto use your activity too",
                    Toast.LENGTH_SHORT
                ).show()
            }
            permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                Toast.makeText(
                    applicationContext,
                    "You have only given us access to your approximate location, we need your precise location",
                    Toast.LENGTH_SHORT
                ).show()
            }
            permissions.getOrDefault(Manifest.permission.CAMERA, false) -> {
                Toast.makeText(
                    applicationContext,
                    "you have given us access to your camera",
                    Toast.LENGTH_SHORT
                ).show()
                navController.navigate(R.id.action_global_cameraFragment)
            }
            else -> {
                Toast.makeText(
                    applicationContext,
                    "You have chosen to not share your location, we need your precise location",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen().apply {
            setKeepOnScreenCondition() {
                viewModel.isLoading.value
            }
        }
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        activityTrackingEnabled = false

        activityTransitionList = ArrayList()

        (activityTransitionList as ArrayList<ActivityTransition>).add(
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.WALKING)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                .build()
        )
        (activityTransitionList as ArrayList<ActivityTransition>).add(
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.WALKING)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                .build()
        )
        (activityTransitionList as ArrayList<ActivityTransition>).add(
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.STILL)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                .build()
        )
        (activityTransitionList as ArrayList<ActivityTransition>).add(
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.STILL)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                .build()
        )

        val intent = Intent(TRANSITIONS_RECEIVER_ACTION)
        mActivityTransitionsPendingIntent =
            PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val viewModel = viewModel

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController
        val navGraph = navController.navInflater.inflate(R.navigation.main_navigation)
        bottomNavBar = findViewById(R.id.bottomNav)
        val bottomBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.mainFragment,
                R.id.mapFragment,
                R.id.cameraFragment
            )
        )
        setupActionBarWithNavController(navController, bottomBarConfiguration)


        bottomNavBar.setupWithNavController(navController)
        bottomNavBar.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.mapFragment -> {
                    checkLocationPermissions()
                    return@setOnItemSelectedListener true
                }
                R.id.mainFragment -> {
                    navController.navigate(R.id.action_global_mainFragment)
                    return@setOnItemSelectedListener true
                }
                R.id.cameraFragment -> {
                    checkCameraPermissions()
                    return@setOnItemSelectedListener true
                }
                else -> {
                    return@setOnItemSelectedListener true
                }
            }
        }
        mTransitionsReceiver = TransitionsReceiver()
        navigateToMapFragmentIfNeeded(intent)
        if (viewModel.allData.value?.size == null) {
            navGraph.setStartDestination(R.id.introductionFragment)
        } else {
            navGraph.setStartDestination(R.id.mainFragment)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.toolbar, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.toolbar_settings ->
                navController.navigate(MainFragmentDirections.actionGlobalSettingsFragment())
            R.id.toolbar_sensors ->
                navController.navigate(MainFragmentDirections.actionGlobalSensorFragment())
            R.id.toolbar_introduction ->
                navController.navigate(MainFragmentDirections.actionGlobalIntroductionFragment())
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onStart() {
        super.onStart()
        registerReceiver(mTransitionsReceiver, IntentFilter(TRANSITIONS_RECEIVER_ACTION))
    }

    override fun onPause() {
        if (activityTrackingEnabled) {
            disableActivityTransitions()
        }
        super.onPause()
    }

    override fun onStop() {
        unregisterReceiver(mTransitionsReceiver)
        super.onStop()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (activityRecognitionPermissionApproved() && !activityTrackingEnabled) {
            enableActivityTransitions()
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    @SuppressLint("MissingPermission")
    private fun enableActivityTransitions() {
        val request = ActivityTransitionRequest(activityTransitionList)

        val task: Task<Void> = ActivityRecognition.getClient(this).requestActivityTransitionUpdates(
            request,
            mActivityTransitionsPendingIntent!!
        )

        task.addOnSuccessListener {
            activityTrackingEnabled = true
        }
        task.addOnFailureListener { e ->
            Log.e(TAG, "Transitions Api could NOT be registered: $e")
        }
    }

    @SuppressLint("MissingPermission")
    private fun disableActivityTransitions() {
        ActivityRecognition.getClient(this).removeActivityTransitionUpdates(
            mActivityTransitionsPendingIntent!!
        )
            .addOnSuccessListener {
                activityTrackingEnabled = false
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Transitions could not be unregistered: $e")
            }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        navigateToMapFragmentIfNeeded(intent)
    }

    private fun navigateToMapFragmentIfNeeded(intent: Intent?) {
        if (intent?.action == ACTION_SHOW_MAP_FRAGMENT) {
            val navHostFragment =
                supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
            navController = navHostFragment.navController
            navController.navigate(R.id.action_global_mapFragment)
        }
    }

    private fun checkLocationPermissions() {

        if (ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.ACTIVITY_RECOGNITION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            locationPermissionRequest.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACTIVITY_RECOGNITION,
                )
            )
        } else {
            navController.navigate(R.id.action_global_mapFragment)
        }
    }

    fun checkCameraPermissions() {
        if (ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            locationPermissionRequest.launch(
                arrayOf(
                    Manifest.permission.CAMERA
                )
            )
        } else {
            navController.navigate(R.id.action_global_cameraFragment)
        }
    }

    private fun activityRecognitionPermissionApproved(): Boolean {
        return PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
            this, Manifest.permission.ACTIVITY_RECOGNITION
        )
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp()
    }

    fun onClickEnableOrDisableActivityRecognition(view: View?) {
        if (activityRecognitionPermissionApproved()) {
            if (activityTrackingEnabled) {
                disableActivityTransitions()
            } else {
                enableActivityTransitions()
            }
        } else {
            checkLocationPermissions()
        }
    }

    inner class TransitionsReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (ActivityTransitionResult.hasResult(intent)) {
                val result = ActivityTransitionResult.extractResult(intent)

                /*for (event in result!!.transitionEvents) {
                    viewModel.setDetectedActivity(toActivityString(event.activityType))
                }*/
            }
        }
    }

    companion object {
        private const val TAG = "MainActivity"
        private fun toActivityString(activity: Int): String {
            return when (activity) {
                DetectedActivity.STILL -> "STILL"
                DetectedActivity.WALKING -> "WALKING"
                else -> "UNKNOWN"
            }
        }

        private fun toTransitionType(transitionType: Int): String {
            return when (transitionType) {
                ActivityTransition.ACTIVITY_TRANSITION_ENTER -> "ENTER"
                ActivityTransition.ACTIVITY_TRANSITION_EXIT -> "EXIT"
                else -> "UNKNOWN"
            }
        }
    }
}
















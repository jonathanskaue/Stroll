package com.example.stroll

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import android.widget.Toolbar
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import com.example.stroll.other.Constants.ACTION_SHOW_MAP_FRAGMENT
import com.example.stroll.presentation.fragment.*
import com.example.stroll.presentation.viewmodel.MainViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.HiltAndroidApp

//It works!!!!!!!!
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var navController: NavController
    lateinit var bottomNavBar: BottomNavigationView
    private val viewModel: MainViewModel by viewModels()
    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) &&
                    permissions.getOrDefault(Manifest.permission.ACTIVITY_RECOGNITION, false)-> {
                Toast.makeText(applicationContext, "You have given us permission to use your precise location and activity", Toast.LENGTH_SHORT).show()
                navController.navigate(R.id.action_global_mapFragment)
            }
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                Toast.makeText(applicationContext, "you have given us permission to use your precise location, but we need your permissionto use your activity too", Toast.LENGTH_SHORT).show()
            }
            permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                Toast.makeText(applicationContext, "You have only given us access to your approximate location, we need your precise location", Toast.LENGTH_SHORT).show()
            }
            else -> {
                Toast.makeText(applicationContext, "You have chosen to not share your location, we need your precise location", Toast.LENGTH_SHORT).show()
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

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController
        val navGraph = navController.navInflater.inflate(R.navigation.main_navigation)
        bottomNavBar = findViewById(R.id.bottomNav)
        val bottomBarConfiguration = AppBarConfiguration(setOf(R.id.mainFragment, R.id.mapFragment, R.id.graphFragment, R.id.introductionFragment))
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
                R.id.graphFragment -> {
                    navController.navigate(R.id.action_global_graphFragment)
                    return@setOnItemSelectedListener true

                }
                R.id.introductionFragment -> {
                    navController.navigate(R.id.action_global_introductionFragment)
                    return@setOnItemSelectedListener true
                }
                else -> {return@setOnItemSelectedListener true}
        } }
        navigateToMapFragmentIfNeeded(intent)
        if (viewModel.allData.value?.size == null) {
            navGraph.setStartDestination(R.id.introductionFragment)
        }
        else {
            navGraph.setStartDestination(R.id.mainFragment)
        }

    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        navigateToMapFragmentIfNeeded(intent)
    }

    private fun navigateToMapFragmentIfNeeded(intent: Intent?) {
        if(intent?.action == ACTION_SHOW_MAP_FRAGMENT) {
            val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
            navController = navHostFragment.navController
            navController.navigate(R.id.action_global_mapFragment)
        }
    }

    private fun checkLocationPermissions() {

        if (ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.ACCESS_FINE_LOCATION
            )  != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.ACTIVITY_RECOGNITION
            )  != PackageManager.PERMISSION_GRANTED
        ) {
            locationPermissionRequest.launch(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACTIVITY_RECOGNITION
            ))
        }
        else {
            navController.navigate(R.id.action_global_mapFragment)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp()
    }
}
















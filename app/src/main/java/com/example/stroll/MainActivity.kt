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
import androidx.navigation.ui.NavigationUI
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
    private lateinit var bottomNavBar: BottomNavigationView
    private val viewModel: MainViewModel by viewModels()
    lateinit var startingDestination : String
    val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) &&
                    permissions.getOrDefault(Manifest.permission.ACTIVITY_RECOGNITION, false)-> {
                Toast.makeText(applicationContext, "You have given us permission to use your precise location and activity", Toast.LENGTH_SHORT).show()
                when (startingDestination) {
                    "fragment_main" -> loadFragment(MapFragment())
                    "fragment_introduction" -> loadFragment(MapFragment())
                    "fragment_graph" -> loadFragment(MapFragment())
                }
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

        navigateToMapFragmentIfNeeded(intent)
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController
        val navGraph = navController.navInflater.inflate(R.navigation.main_navigation)
        navGraph.setStartDestination(R.id.introductionFragment)
        NavigationUI.setupActionBarWithNavController(this, navController)
        bottomNavBar = findViewById(R.id.bottomNav)
        bottomNavBar.visibility = View.GONE
        bottomNavBar.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.map -> {
                    checkLocationPermissions()
                    return@setOnItemSelectedListener true
                }
                R.id.home -> {
                    loadFragment(MainFragment())
                    return@setOnItemSelectedListener true
                }
                R.id.sensors -> {
                    loadFragment(GraphFragment())
                    return@setOnItemSelectedListener true

                }
                R.id.intro -> {
                    loadFragment(IntroductionFragment())
                    return@setOnItemSelectedListener true
                }
                else -> {return@setOnItemSelectedListener true}
            }
        }
    }

    fun loadFragment(fragment: Fragment) {
        bottomNavBar.visibility = View.VISIBLE
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.nav_host_fragment, fragment)
        transaction.addToBackStack(null)
        transaction.commit()
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = this.findNavController(R.id.nav_host_fragment)
        return navController.navigateUp()
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

    fun checkLocationPermissions() {
        startingDestination = navController.currentDestination?.label.toString()

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
            when (startingDestination) {
                "fragment_main" -> loadFragment(MapFragment())
                "fragment_introduction" -> loadFragment(MapFragment())
                "fragment_graph" -> loadFragment(MapFragment())
            }
        }
    }
}
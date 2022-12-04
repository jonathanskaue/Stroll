package com.example.stroll

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
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
import com.google.android.material.bottomnavigation.BottomNavigationView
import dagger.hilt.android.AndroidEntryPoint


/*
MainActivity, Checks permissions and sets up bottom bar and toggle menu in action bar.
 */
@Suppress("DEPRECATION")
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var navController: NavController
    lateinit var bottomNavBar: BottomNavigationView
    private val viewModel: MainViewModel by viewModels()
    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                Toast.makeText(
                    applicationContext,
                    "you have given us permission to use your precise location",
                    Toast.LENGTH_SHORT
                ).show()
                navController.navigate(R.id.action_global_mapFragment)
            }
            permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                Toast.makeText(
                    applicationContext,
                    "You have only given us access to your approximate location, we need your precise location",
                    Toast.LENGTH_SHORT
                ).show()
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
            setKeepOnScreenCondition {
                viewModel.isLoading.value
            }
        }
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController
        bottomNavBar = findViewById(R.id.bottomNav)
        val bottomBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.hikesFragment,
                R.id.mapFragment,
            )
        )
        setupActionBarWithNavController(navController, bottomBarConfiguration)


        bottomNavBar.setupWithNavController(navController)
        bottomNavBar.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.homeFragment -> {
                    navController.navigate(R.id.action_global_homeFragment)
                    return@setOnItemSelectedListener true
                }
                R.id.mapFragment -> {
                    checkLocationPermissions()
                    return@setOnItemSelectedListener true
                }
                R.id.hikesFragment -> {
                    navController.navigate(R.id.action_global_hikesFragment)
                    return@setOnItemSelectedListener true
                }
                else -> {
                    return@setOnItemSelectedListener true
                }
            }
        }

        navigateToMapFragmentIfNeeded(intent)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.toolbar, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.toolbar_settings ->
                navController.navigate(HikesFragmentDirections.actionGlobalSettingsFragment())
            R.id.toolbar_sensors ->
                navController.navigate(HikesFragmentDirections.actionGlobalSensorFragment())
            R.id.toolbar_introduction ->
                navController.navigate(HikesFragmentDirections.actionGlobalIntroductionFragment())
            R.id.toolbar_about ->
                navController.navigate(HikesFragmentDirections.actionGlobalAboutFragment())

        }
        return super.onOptionsItemSelected(item)
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
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            locationPermissionRequest.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                )
            )
        } else {
            navController.navigate(R.id.action_global_mapFragment)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp()
    }
}
















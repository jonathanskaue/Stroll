package com.example.stroll.presentation.fragment

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import android.util.Log
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.navigation.findNavController
import androidx.preference.PreferenceManager
import com.example.stroll.R
import com.example.stroll.backgroundlocationtracking.DefaultLocationClient
import com.example.stroll.backgroundlocationtracking.LocationClient
import com.example.stroll.backgroundlocationtracking.LocationService
import com.example.stroll.databinding.FragmentMapBinding
import com.example.stroll.presentation.viewmodel.MainViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import timber.log.Timber

@AndroidEntryPoint
class MapFragment() : Fragment() {

    private lateinit var mapView: MapView
    private lateinit var controller: MapController
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var defaultLocationClient: DefaultLocationClient
    private lateinit var latLng: GeoPoint
    private val viewModel: MainViewModel by viewModels()

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!

    val locationPermissionRequest = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                Toast.makeText(requireContext(), "You have given us permission to use your precise location", Toast.LENGTH_SHORT).show()
                onMapReady()
            }
            permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                Toast.makeText(requireContext(), "You have only given us access to your approximate location", Toast.LENGTH_SHORT).show()
                onMapReady()
            } else -> {
                Toast.makeText(requireContext(), "You have chosen to not share your location", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        setHasOptionsMenu(true)
        latLng = GeoPoint(19.43621, 28.4916)
        fusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(requireContext())
        defaultLocationClient = DefaultLocationClient(requireContext(), fusedLocationProviderClient)
        defaultLocationClient.getLocationUpdates(1000)

        Configuration.getInstance().load(context,
            context?.let { PreferenceManager.getDefaultSharedPreferences(it.applicationContext) })

        mapView = binding.map

        val controller = mapView.controller

        val myLocationOverlay = MyLocationNewOverlay(mapView)
        mapView.overlays.add(myLocationOverlay)

        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            locationPermissionRequest.launch(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ))
        } else {
            onMapReady()
            Intent(requireContext(), LocationService::class.java).apply {
                action = LocationService.ACTION_START
                activity?.startService(this)
            }
        }

        fusedLocationProviderClient.lastLocation.addOnSuccessListener { location: Location? ->
            latLng.latitude = location!!.latitude
            latLng.longitude = location.longitude
            newLocation()
        }

        controller.setZoom(18.0)
        controller.setCenter(latLng)

        mapView.setTileSource(TileSourceFactory.MAPNIK)

        return binding.root
    }

    @SuppressLint("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadSettings()
    }

    @SuppressLint("MissingPermission")
    fun onMapReady() {
        Configuration.getInstance().load(context,
            context?.let { PreferenceManager.getDefaultSharedPreferences(it.applicationContext) })
        mapView = binding.map
        fusedLocationProviderClient.lastLocation.addOnSuccessListener { location: Location? ->
            latLng.latitude = location!!.latitude
            latLng.longitude = location.longitude
        }

        mapView.setMultiTouchControls(true)
        val controller = mapView.controller

        val myLocationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(requireContext()), mapView)
        myLocationOverlay.enableMyLocation()
        myLocationOverlay.enableFollowLocation()
        myLocationOverlay.isDrawAccuracyEnabled = true
        mapView.overlays.add(myLocationOverlay)
        controller.setCenter(latLng)
    }

    @SuppressLint("MissingPermission")
    fun newLocation() {
        val controller = mapView.controller
        fusedLocationProviderClient.lastLocation.addOnSuccessListener { location: Location? ->
            latLng.latitude = location!!.latitude
            latLng.longitude = location.longitude
            controller.setCenter(latLng)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.settings -> {
                val action = MapFragmentDirections.actionMapFragmentToSettingsFragment()
                view?.findNavController()?.navigate(action)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    fun loadSettings() {
        val sp = context?.let { androidx.preference.PreferenceManager.getDefaultSharedPreferences(it) }
        val dark_mode = sp?.getBoolean("dark_mode", false)

        if (dark_mode == true) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        }
        else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }

    override fun onDestroy() {
        Intent(requireContext(), LocationService::class.java).apply {
            action = LocationService.ACTION_STOP
            activity?.startService(this)
        }
        super.onDestroy()
    }
}
package com.example.stroll.presentation.fragment

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.view.*
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.navigation.findNavController
import androidx.preference.PreferenceManager
import com.example.stroll.R
import com.example.stroll.backgroundlocationtracking.LocationService
import com.example.stroll.backgroundlocationtracking.Polyline
import com.example.stroll.databinding.FragmentMapBinding
import com.example.stroll.other.Constants.ACTION_PAUSE
import com.example.stroll.other.Constants.ACTION_START
import com.example.stroll.presentation.viewmodel.MainViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import dagger.hilt.android.AndroidEntryPoint
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

@AndroidEntryPoint
class MapFragment() : BaseFragment() {

    private lateinit var mapView: MapView
    private lateinit var controller: MapController
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var latLng: GeoPoint
    private lateinit var myLocationOverlay: MyLocationNewOverlay
    private val viewModel: MainViewModel by viewModels()

    private var isTracking = false
    private var pathPoints = mutableListOf<Polyline>()

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!

    @SuppressLint("MissingPermission")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        latLng = GeoPoint(10.0, 10.0)

        // Inflate the layout for this fragment
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        fusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(requireContext())

        Configuration.getInstance().load(context,
            context?.let { PreferenceManager.getDefaultSharedPreferences(it.applicationContext) })

        mapView = binding.map
        mapView.setMultiTouchControls(true)

        controller = mapView.controller as MapController

        myLocationOverlay =
            MyLocationNewOverlay(GpsMyLocationProvider(requireContext()), mapView)
        myLocationOverlay.enableMyLocation()
        myLocationOverlay.enableFollowLocation()
        myLocationOverlay.isDrawAccuracyEnabled = true
        mapView.overlays.add(myLocationOverlay)


        fusedLocationProviderClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                latLng = GeoPoint(latLng.latitude, latLng.longitude)
                controller.setCenter(latLng)
            }
        }


        controller.setZoom(18.0)

        mapView.setTileSource(TileSourceFactory.MAPNIK)
        subscribeToObservers()
        addAllPolyLines()

        return binding.root
    }

    @SuppressLint("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.toolbar, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.settings -> {
                        val action = MapFragmentDirections.actionMapFragmentToSettingsFragment()
                        view.findNavController().navigate(action)
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    override fun onResume() {
        super.onResume()
        mapView?.onResume()
    }

    override fun onStart() {
        super.onStart()
    }

    override fun onStop() {
        super.onStop()
    }

    override fun onPause() {
        super.onPause()
        mapView?.onPause()
    }

    override fun onLowMemory() {
        super.onLowMemory()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
    }

    private fun subscribeToObservers() {
        LocationService.isTracking.observe(viewLifecycleOwner, Observer {
            updateTracking(it)
        })

        LocationService.pathPoints.observe(viewLifecycleOwner, Observer {
            pathPoints = it
            addLatestPolyline()
        })
    }
    private fun updateTracking(isTracking: Boolean) {
        this.isTracking = isTracking
        if(!isTracking) {
            binding.startHikeBtn.text = "Start"
            binding.finishHikeBtn.visibility = View.VISIBLE
        } else {
            binding.startHikeBtn.text = "Stop"
            binding.finishHikeBtn.visibility = View.GONE
        }
    }

    private fun toggleRun() {
        if(isTracking) {
            sendCommandToService(ACTION_PAUSE)
        } else {
            sendCommandToService(ACTION_START)
        }
    }

    private fun addAllPolyLines() {
        for (polyline in pathPoints) {
            val polygon = Polygon()
            polygon.fillColor = Color.RED
            polygon.strokeWidth = 8f
            mapView?.overlayManager?.add(polygon)

        }
    }

    private fun addLatestPolyline() {
        if(pathPoints.isNotEmpty() && pathPoints.last().size > 1) {
            val preLastLatLng = pathPoints.last()[pathPoints.last().size - 2]
            val lastLatLng = pathPoints.last().last()
            val polygon = Polygon()
            polygon.fillColor = Color.RED
            polygon.strokeWidth = 8f
            polygon.addPoint(GeoPoint(preLastLatLng.latitude, preLastLatLng.longitude))
            polygon.addPoint(GeoPoint(lastLatLng.latitude, lastLatLng.longitude))
            mapView?.overlayManager?.add(polygon)
        }
    }

    private fun sendCommandToService(action: String) =
        Intent(requireContext(), LocationService::class.java).also {
            it.action = action
            requireContext().startService(it)
        }
}
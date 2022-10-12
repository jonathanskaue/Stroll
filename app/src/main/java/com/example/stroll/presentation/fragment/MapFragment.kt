package com.example.stroll.presentation.fragment

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.drawable.Drawable
import android.graphics.drawable.Icon
import android.location.Location
import android.os.Bundle
import android.view.*
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.findNavController
import androidx.preference.PreferenceManager
import com.example.stroll.R
import com.example.stroll.backgroundlocationtracking.LocationService
import com.example.stroll.databinding.FragmentMapBinding
import com.example.stroll.presentation.viewmodel.MainViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import dagger.hilt.android.AndroidEntryPoint
import org.osmdroid.api.IGeoPoint
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.events.MapListener
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

@AndroidEntryPoint
class MapFragment() : BaseFragment(), MapEventsReceiver {

    private lateinit var mapView: MapView
    private lateinit var controller: MapController
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var latLng: GeoPoint
    private lateinit var myLocationOverlay: MyLocationNewOverlay
    private val viewModel: MainViewModel by viewModels()

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

        val points = ArrayList<IGeoPoint>()
        val marker: Marker = Marker(mapView)
        marker.icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_baseline_place_24)
        marker.position = marker.position

        Intent(requireContext(), LocationService::class.java).apply {
            action = LocationService.ACTION_START
            activity?.startService(this)
        }

        fusedLocationProviderClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                latLng = GeoPoint(latLng.latitude, latLng.longitude)
                controller.setCenter(latLng)
            }
        }


        controller.setZoom(18.0)

        mapView.setTileSource(TileSourceFactory.MAPNIK)

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

    fun addMarker(center: GeoPoint?) {
        val marker = Marker(mapView)
        marker.position = center
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        marker.icon = ResourcesCompat.getDrawable(resources, R.drawable.ic_baseline_place_24, null)
        mapView.getOverlays().clear()
        mapView.getOverlays().add(marker)
        mapView.invalidate()
        marker.title = "Destination"
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onDestroy() {
        Intent(requireContext(), LocationService::class.java).apply {
            action = LocationService.ACTION_STOP
            activity?.startService(this)
        }
        super.onDestroy()
    }

    override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
        addMarker(p)
        return true
    }

    override fun longPressHelper(p: GeoPoint?): Boolean {
        TODO("Not yet implemented")
    }
}
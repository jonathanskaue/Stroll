package com.example.stroll.presentation.fragment

import android.annotation.SuppressLint
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.content.res.Configuration.UI_MODE_NIGHT_MASK
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Point
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.preference.PreferenceManager
import kotlinx.coroutines.withContext
import com.example.stroll.MainActivity
import com.example.stroll.R
import com.example.stroll.backgroundlocationtracking.LocationService
import com.example.stroll.backgroundlocationtracking.Polyline
import com.example.stroll.data.local.InternalStoragePhoto
import com.example.stroll.data.local.StrollDataEntity
import com.example.stroll.databinding.FragmentMapBinding
import com.example.stroll.other.Constants.ACTION_PAUSE
import com.example.stroll.other.Constants.ACTION_START
import com.example.stroll.other.Constants.ACTION_STOP
import com.example.stroll.other.MapEventsReceiverImpl
import com.example.stroll.other.Utility
import com.example.stroll.presentation.viewmodel.MainViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.osmdroid.api.IMapView
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapController
import org.osmdroid.views.MapView
import org.osmdroid.views.drawing.MapSnapshot
import org.osmdroid.views.drawing.MapSnapshot.INCLUDE_FLAG_SCALED
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Overlay.Snappable
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.TilesOverlay
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.io.File
import java.io.IOException
import java.util.*
import javax.inject.Inject
import kotlin.math.cos
import kotlin.math.round
import kotlin.math.sin

@AndroidEntryPoint
class MapFragment() : BaseFragment(), MapEventsReceiver, Snappable {

    private lateinit var mapView: MapView
    private lateinit var controller: MapController
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var latLng: GeoPoint
    private lateinit var myLocationOverlay: MyLocationNewOverlay
    private lateinit var mapEventsReceiver: MapEventsReceiverImpl
    private val viewModel: MainViewModel by viewModels()

    private var isTracking = false
    private var pathPoints = mutableListOf<Polyline>()
    private var currentTimeInMillis = 0L
    private var highestHikeId = MutableLiveData<Int>()

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!

    private var initializeViewModel = true
    @set:Inject
    var weight = 80f

    @set:Inject
    var name = "Default"



    @SuppressLint("MissingPermission")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        viewModel.initialize(initializeViewModel)
        Log.d("Sharedpreferences works", "Weight: $weight, Name: $name")


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
        mapEventsReceiver = MapEventsReceiverImpl()
        val mapEventsOverLay = MapEventsOverlay(mapEventsReceiver)
        mapView.overlays.add(mapEventsOverLay)

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
        if (resources.getString(R.string.mode) == "Night") {
            mapView.overlayManager.tilesOverlay.setColorFilter(TilesOverlay.INVERT_COLORS)
        }
        subscribeToObservers()

        return binding.root
    }

    @SuppressLint("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (viewModel.isHeatMap.value) {
            viewModel.allData.observe(viewLifecycleOwner) {
                it.forEach { pos ->
                    myHeatMap(pos.startLatitude, pos.startLongitude, 0.001)
                }
                it.forEach { pos ->
                    myMarker(pos.startLatitude, pos.startLongitude, pos.id.toString(), pos.timeInMillis.toString(), pos.averageSpeedInKMH.toString())
                }
            }
        }

        binding.toggleHikeBtn.setOnClickListener {
            toggleHike()
        }
        binding.cancelHikeBtn.setOnClickListener {
            showCancelHikeDialog()
        }
        binding.finishHikeBtn.setOnClickListener {

            zoomToSeeWholeTrack()
            endHikeAndSaveToDb()
            myLocationOverlay.disableFollowLocation()
            myLocationOverlay.disableMyLocation()
        }
        viewModel.highestHikeId.observe(viewLifecycleOwner, Observer {
            if (it != null) {
                highestHikeId.value = it
            }
            else highestHikeId.value = 0
        })

    }

    override fun onResume() {
        super.onResume()
        mapView?.onResume()
        addAllPolyLines()

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
            //addAllPolyLines()

        })

        LocationService.timeHikedInMillis.observe(viewLifecycleOwner, Observer {
            currentTimeInMillis = it
            val formattedTime = Utility.getFormattedStopWatchTime(currentTimeInMillis, true)
            binding.timerTV.text = formattedTime
            if (currentTimeInMillis > 0L) {
                binding.cancelHikeBtn.isVisible = true
            }
        })
    }

    private fun showCancelHikeDialog() {
        val dialog = MaterialAlertDialogBuilder(requireContext(), androidx.appcompat.R.style.AlertDialog_AppCompat)
            .setTitle("Cancel Hike?")
            .setMessage("Are you sure you want to cancel the hike and delete data for current hike?")
            .setIcon(R.drawable.group_1)
            .setPositiveButton("YES") {_,_ ->
                deleteFolderWhenCancellingHike()
                sendCommandToService(ACTION_STOP) // another idea: calling stopHike() with parameter
            }
            .setNegativeButton("NO") { dialogInterface, _ ->
                dialogInterface.cancel()
            }
            .create()
        dialog.show()
    }

    private fun stopHike() {
        sendCommandToService(ACTION_STOP)
        //view?.findNavController()?.navigate(R.id.action_global_hikesFragment)
    }
    @SuppressLint("UseCompatLoadingForDrawables")
    private fun updateTracking(isTracking: Boolean) {
        this.isTracking = isTracking
        if (!isTracking) {
            if (currentTimeInMillis == 0L) {
                binding.toggleHikeBtn.setImageDrawable(resources.getDrawable(R.drawable.ic_baseline_play_arrow_24))
            }
            else {
                binding.toggleHikeBtn.setImageDrawable(resources.getDrawable(R.drawable.ic_baseline_play_arrow_24))
                binding.finishHikeBtn.show()
            }

        } else {
            binding.toggleHikeBtn.setImageDrawable(resources.getDrawable(R.drawable.ic_baseline_pause_24))
            binding.finishHikeBtn.hide()
        }
    }

    private fun toggleHike() {
        if(isTracking) {
            sendCommandToService(ACTION_PAUSE)
        } else {
            sendCommandToService(ACTION_START)
            makeFolderInInternalStorage()
        }
    }

    override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
        Log.d("Single tap", "singleTapConfirmedHelper: $p")
        return false
    }

    override fun longPressHelper(p: GeoPoint?): Boolean {
        Log.d("Single tap", "singleTapConfirmedHelper: $p")
        return false
    }



    private fun zoomToSeeWholeTrack() {
        val firstAndLastLocation = mutableListOf<GeoPoint>()
        for (polyline in pathPoints) {
            for (position in polyline) {
                firstAndLastLocation.add(GeoPoint(position.latitude, position.longitude))
            }
        }
        mapView.zoomToBoundingBox(getBounds(), false, 150)
    }

    private fun getBounds(): BoundingBox {
        var north = -90.0
        var south = 90.0
        var west = 180.0
        var east = -180.0
        for (polyline in pathPoints) {
            for (position in polyline) {
                if (position.latitude > north) {
                    north = position.latitude
                }
                if (position.latitude < south) {
                    south = position.latitude
                }
                if (position.longitude < west) {
                    west = position.longitude
                }
                if (position.longitude > east) {
                    east = position.longitude
                }
            }
        }
        return BoundingBox(north, east, south, west)
    }

    private fun endHikeAndSaveToDb() {
        val latLng = pathPoints.first().first()
        var distanceInMeters = 0
        for (polyline in pathPoints) {
            distanceInMeters += Utility.calculatePolylineLength(polyline).toInt()
        }
        val averageSpeed = round((distanceInMeters / 1000f) / (currentTimeInMillis / 1000f / 60 / 60) * 10) / 10f
        val dateTimeStamp = Calendar.getInstance().timeInMillis
        val folderPath = context?.filesDir?.absolutePath + "/${highestHikeId.value?.plus(1)}/"
        Log.d("testDatabase", "endHikeAndSaveToDb: $latLng")
        val mapSnapshot = MapSnapshot(MapSnapshot.MapSnapshotable { pMapSnapshot ->
            if (pMapSnapshot.status != MapSnapshot.Status.CANVAS_OK) {
                return@MapSnapshotable
            }

            Log.d("myPathPoints", "endHikeAndSaveToDb: $pathPoints")
            saveBitmapToInternalStorage(pMapSnapshot.bitmap.toString(), pMapSnapshot.bitmap)

            val hike = StrollDataEntity(
                pMapSnapshot.bitmap.toString().plus(".png"),
                dateTimeStamp,
                averageSpeed,
                distanceInMeters,
                currentTimeInMillis,
                folderPath,
                latLng.latitude,
                latLng.longitude
            )
            viewModel.addDataToRoom(hike)
            Snackbar.make(
                requireActivity().findViewById(R.id.hikesFragment),
                "Hike saved successfully",
                Snackbar.LENGTH_LONG
            ).show()
            //sendCommandToService(ACTION_STOP)
        }, MapSnapshot.INCLUDE_FLAG_UPTODATE + INCLUDE_FLAG_SCALED, mapView)
        Thread(mapSnapshot).start()
        stopHike()
    }

    private fun addAllPolyLines() {
        if(pathPoints.isNotEmpty() && pathPoints.last().size > 1) {
            for (polyline in pathPoints) {

                for (i in 0..polyline.size - 2) {
                    var polygon = Polygon()
                    val position = polyline[i] // LATLNG
                    val position2 = polyline[i + 1]
                    polygon.outlinePaint.color = Color.parseColor("#99" + "1E90FF")
                    polygon.addPoint(GeoPoint(position.latitude, position.longitude))
                    polygon.addPoint(GeoPoint(position2.latitude, position2.longitude))
                    mapView.overlayManager.add((polygon))
                }
            }
        }
    }

    private fun addLatestPolyline() {
        if(pathPoints.isNotEmpty() && pathPoints.last().size > 1) {
            val preLastLatLng = pathPoints.last()[pathPoints.last().size - 2]
            val lastLatLng = pathPoints.last().last()
            val polygon = Polygon(mapView)
            polygon.strokeWidth = 12f
            polygon.fillPaint.color = Color.parseColor("#55"+ "FFFF00")
            polygon.outlinePaint.color = Color.parseColor("#99" + "1E90FF")
            polygon.addPoint(GeoPoint(preLastLatLng.latitude, preLastLatLng.longitude))
            polygon.addPoint(GeoPoint(lastLatLng.latitude, lastLatLng.longitude))
            mapView.overlayManager?.add(polygon)
        }
    }

    private fun sendCommandToService(action: String) =
        Intent(requireContext(), LocationService::class.java).also {
            it.action = action
            requireContext().startService(it)
        }

    override fun onSnapToItem(x: Int, y: Int, snapPoint: Point?, mapView: IMapView?): Boolean {

        return true
    }

    private fun saveBitmapToInternalStorage(filename: String, bitmap: Bitmap): Boolean {
        return try {
            context?.openFileOutput("$filename.png", MODE_PRIVATE).use { stream ->
                if(!bitmap.compress(Bitmap.CompressFormat.PNG, 95, stream)) {
                    throw IOException("Couldn't save bitmap.")
                }
                true
            }
        } catch (e: IOException) {
            e.printStackTrace()
            false
        }
    }

    private fun makeFolderInInternalStorage(){
        val folder = context?.filesDir
        val children = folder?.listFiles()
            ?.filter { it.isDirectory && !it.name.equals("osmdroid") }
        if (children?.size!! > 0){
            val newHikeId = highestHikeId.value?.plus(1)
            if(newHikeId != null){
                val folderToBeCreated = File(folder, "$newHikeId")
                if (!folderToBeCreated.exists()){
                    folderToBeCreated.mkdir()
                }
            }
        }
        else {
            val f = File(folder, "1")
            f.mkdir()
        }
    }

    private fun deleteFolderWhenCancellingHike(){
        val folder = context?.filesDir
        val children = folder?.listFiles()
            ?.filter { it.isDirectory && !it.name.equals("osmdroid") }

        val folders = folder?.listFiles()
        folders?.sortByDescending { dir ->
            dir.lastModified()
        }

        val newestFolder = folders?.first()
        newestFolder?.delete()
    }

    private fun myMarker(lat: Double, lon: Double, id: String, title: String, subDescription: String) {
        lifecycleScope.launch {
            var startMarker: Marker = Marker(mapView)
            startMarker.position = GeoPoint(lat, lon)
            startMarker.icon = resources.getDrawable(R.drawable.ic_baseline_hiking_24)
            startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            startMarker.title = title
            startMarker.subDescription = subDescription
            startMarker.setOnMarkerClickListener { marker, mapView ->
                Log.d("onclick", "myMarker: Hello world")
                false
            }
            var myPhoto: InternalStoragePhoto
            if (loadMyPhoto(id).isNotEmpty()) {
                myPhoto = loadMyPhoto(id).first()
                val myDrawable = BitmapDrawable(resources, myPhoto.bmp)
                startMarker.image = myDrawable
            }
            mapView.overlayManager?.add(startMarker)
        }
    }

    private fun myHeatMap(
        lat: Double,
        lon: Double,
        radius: Double,
    ) {
        val steps = 50
        val polygon = Polygon(mapView)
        var centerX = mutableListOf<Double>()
        var centerY = mutableListOf<Double>()
        polygon.fillPaint.color = Color.parseColor("#10"+ "FF0000")
        polygon.strokeWidth = 0f
        polygon.outlinePaint.color = Color.parseColor("#50"+ "FF0000")
        for (i in 0..steps) {
            centerX.add((lat + radius * cos(2*Math.PI * i / steps)))
            centerY.add((lon + radius * sin(2*Math.PI * i/ steps)*2.5))
            polygon.addPoint(GeoPoint(centerX[i], centerY[i]))
        }
        mapView.overlayManager?.add(polygon)
    }


    private suspend fun loadMyPhoto(id: String): List<InternalStoragePhoto>{
        return withContext(Dispatchers.IO) {
            val path = context?.filesDir?.absolutePath + "/$id/"
            val dir = File(path).listFiles()
            dir.filter { it.canRead() && it.isFile && it.name.endsWith(".png") }!!.map {
                val bytes = it.readBytes()
                val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                InternalStoragePhoto(it.name, bmp)
            }
        }
    }
}


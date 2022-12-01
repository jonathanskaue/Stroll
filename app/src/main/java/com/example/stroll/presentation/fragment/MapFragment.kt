package com.example.stroll.presentation.fragment

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentValues
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.Point
import android.graphics.drawable.BitmapDrawable
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.MainThread
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
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
import com.example.stroll.other.Utility
import com.example.stroll.presentation.viewmodel.MainViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import org.osmdroid.api.IMapView
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapController
import org.osmdroid.views.MapView
import org.osmdroid.views.drawing.MapSnapshot
import org.osmdroid.views.drawing.MapSnapshot.INCLUDE_FLAG_SCALED
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Overlay.Snappable
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.TilesOverlay
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
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
    private val viewModel: MainViewModel by viewModels()

    private var isTracking = false
    private var pathPoints = mutableListOf<Polyline>()
    private var currentTimeInMillis = 0L
    private var highestHikeId = MutableLiveData<Int>()
    private var polygonColor = "#00B7FF"

    private val IMAGE_CAPTURE_CODE = 1001
    private var imageUri: Uri? = null
    private var folder: String = ""

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!

    private var initializeViewModel = true
    @set:Inject
    var weight = 80f

    @set:Inject
    var name = "Default"

    private val cameraPermissionResult = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.CAMERA, false) -> {
                Toast.makeText(
                    requireContext(),
                    "You have given us permission to use your camera",
                    Toast.LENGTH_SHORT
                ).show()
                openCameraInterface()
            }
        }
    }

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
        Configuration.getInstance().userAgentValue = context?.packageName;
        Configuration.getInstance().load(context,
            context?.let { PreferenceManager.getDefaultSharedPreferences(it.applicationContext) })

        mapView = binding.map
        if (resources.getString(R.string.mode) == "Night") {
            polygonColor = "#D74177"
            mapView.overlayManager.tilesOverlay.setColorFilter(TilesOverlay.INVERT_COLORS)
        }
        mapView.setMultiTouchControls(true)
        mapView.zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)

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

        return binding.root
    }

    @SuppressLint("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
            viewModel.allData.observe(viewLifecycleOwner) {
                if (viewModel.isHeatMap.value) {
                    it.forEach { pos ->
                        myHeatMap(pos.startLatitude, pos.startLongitude, 0.001)
                    }
                }
                if (viewModel.isMarker.value) {
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
            lifecycleScope.launch {
                zoomToSeeWholeTrack()
                endHikeAndSaveToDb()
            }
        }
        viewModel.highestHikeId.observe(viewLifecycleOwner, Observer {
            if (it != null) {
                highestHikeId.value = it
                folder = context?.filesDir?.absolutePath + "/${it.plus(1)}/"
            }
            else {
                highestHikeId.value = 0
                folder = context?.filesDir?.absolutePath + "/1/"
            }
        })
        binding.btnTakePhoto.setOnClickListener {
            checkCameraPermissions()
        }

    }
    fun checkCameraPermissions() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ){
            cameraPermissionResult.launch(arrayOf(
                Manifest.permission.CAMERA
            ))
        }
        else {
            openCameraInterface()
        }
    }

    override fun onResume() {
        super.onResume()
        mapView?.onResume()
        addAllPolyLines(polygonColor)

    }


    override fun onPause() {
        super.onPause()
        mapView?.onPause()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Callback from camera intent
        if (resultCode == Activity.RESULT_OK){
            // Set image captured to image view
            val source = imageUri?.let { ImageDecoder.createSource(requireActivity().contentResolver, it) }
            val bitmap = source?.let { ImageDecoder.decodeBitmap(it) }
            if (bitmap != null) {
                saveBitmapToSpecificFolder(bitmap.toString().plus(".png"), bitmap)
            }
        }
        else {
            // Failed to take picture
            // showAlert("Failed to take camera picture")
        }
    }

    private fun subscribeToObservers() {
        LocationService.isTracking.observe(viewLifecycleOwner, Observer {
            updateTracking(it)
        })

        LocationService.pathPoints.observe(viewLifecycleOwner, Observer {
            pathPoints = it
            addLatestPolyline(polygonColor)

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
        view?.findNavController()?.navigate(R.id.action_mapFragment_to_hikesFragment)
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
        TODO("not implemented")
    }

    override fun longPressHelper(p: GeoPoint?): Boolean {
        TODO("Not yet implemented")
    }

    private fun zoomToSeeWholeTrack() {
        myLocationOverlay.disableFollowLocation()
        myLocationOverlay.disableMyLocation()
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

    private suspend fun endHikeAndSaveToDb() {
        Log.d("timeInMillis", "endHikeAndSaveToDb: $currentTimeInMillis")
        var distanceInMeters = 0
        var latLng = pathPoints.first().first()
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
            sendCommandToService(ACTION_STOP)
            view?.findNavController()?.navigate(R.id.action_mapFragment_to_hikesFragment)
        }, MapSnapshot.INCLUDE_FLAG_UPTODATE + INCLUDE_FLAG_SCALED, mapView)
        Thread(mapSnapshot).start()
    }

    private fun addAllPolyLines(color: String) {
        if(pathPoints.isNotEmpty() && pathPoints.last().size > 1) {
            for (polyline in pathPoints) {

                for (i in 0..polyline.size - 2) {
                    var polygon = Polygon()
                    val position = polyline[i] // LATLNG
                    val position2 = polyline[i + 1]
                    polygon.outlinePaint.color = Color.parseColor(color)
                    polygon.addPoint(GeoPoint(position.latitude, position.longitude))
                    polygon.addPoint(GeoPoint(position2.latitude, position2.longitude))
                    mapView.overlayManager.add((polygon))
                }
            }
        }
    }

    private fun addLatestPolyline(color: String) {
        if(pathPoints.isNotEmpty() && pathPoints.last().size > 1) {
            val preLastLatLng = pathPoints.last()[pathPoints.last().size - 2]
            val lastLatLng = pathPoints.last().last()
            val polygon = Polygon()
            polygon.strokeWidth = 8f
            polygon.fillPaint.color = 2
            polygon.outlinePaint.color = Color.parseColor(color)
            polygon.addPoint(GeoPoint(preLastLatLng.latitude, preLastLatLng.longitude))
            polygon.addPoint(GeoPoint(lastLatLng.latitude, lastLatLng.longitude))
            mapView.overlayManager?.add(polygon)
        }
    }

    private fun myMarker(lat: Double, lon: Double, id: String, title: String, subDescription: String) {
        lifecycleScope.launch {
            var startMarker: Marker = Marker(mapView)
            startMarker.position = GeoPoint(lat, lon)
            startMarker.icon = resources.getDrawable(R.drawable.ic_baseline_hiking_24)
            startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            startMarker.setOnMarkerClickListener { marker, mapView ->
                startMarker.title = title
                startMarker.subDescription = subDescription
                startMarker.showInfoWindow()
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
        polygon.setOnClickListener { polygon, mapView, eventPos ->
            false
        }
        for (i in 0..steps) {
            centerX.add((lat + radius * cos(2*Math.PI * i / steps)))
            centerY.add((lon + radius * sin(2*Math.PI * i/ steps)*2.5))
            polygon.addPoint(GeoPoint(centerX[i], centerY[i]))
        }
        mapView.overlayManager?.add(polygon)
    }

    private suspend fun loadMyPhoto(id: String): List<InternalStoragePhoto> {
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

    private fun openCameraInterface(){
        val values = ContentValues()
        values.put(MediaStore.Images.Media.TITLE, R.string.take_picture)
        values.put(MediaStore.Images.Media.DESCRIPTION, R.string.take_picture_description)
        imageUri = activity?.contentResolver?.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

        // Create camera intent
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)

        // Launch intent
        startActivityForResult(intent, IMAGE_CAPTURE_CODE)
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

    private fun saveBitmapToSpecificFolder(filename: String, bitmap: Bitmap): Boolean {
        return try {
            val file = File(folder + File.separator + filename)
            file.createNewFile()
            val bos = ByteArrayOutputStream()
            if(!bitmap.compress(Bitmap.CompressFormat.PNG, 95, bos)) {
                throw IOException("Couldn't save bitmap.")
            }
            val bitmapData = bos.toByteArray()
            val fos = FileOutputStream(file)
            fos.write(bitmapData)
            fos.flush()
            fos.close()
            true
        } catch (e: IOException) {
            e.printStackTrace()
            false
        }
    }
}

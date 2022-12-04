package com.example.stroll.presentation.fragment

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentValues
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.util.Log
import android.view.*
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.MainThread
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.example.stroll.R
import com.example.stroll.backgroundlocationtracking.LocationService
import com.example.stroll.backgroundlocationtracking.Polyline
import com.example.stroll.data.local.LatLong
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
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapController
import org.osmdroid.views.MapView
import org.osmdroid.views.drawing.MapSnapshot
import org.osmdroid.views.drawing.MapSnapshot.INCLUDE_FLAG_SCALED
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.TilesOverlay
import org.osmdroid.views.overlay.infowindow.InfoWindow
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*
import kotlin.math.cos
import kotlin.math.round
import kotlin.math.sin


@AndroidEntryPoint
class MapFragment : BaseFragment() {

    private lateinit var mapView: MapView
    private lateinit var controller: MapController
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var latLng: GeoPoint
    private lateinit var myLocationOverlay: MyLocationNewOverlay
    private lateinit var geocoder: Geocoder
    private val viewModel: MainViewModel by activityViewModels()

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

    private val cameraPermissionResult = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.CAMERA, false) -> {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.yes_to_camera_request),
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
    ): View {

        viewModel.initialize(initializeViewModel)
        latLng = GeoPoint(10.0,10.0)
        // Inflate the layout for this fragment
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireContext())
        Configuration.getInstance().userAgentValue = context?.packageName
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
        geocoder = Geocoder(this.requireContext(), Locale.getDefault())
        return binding.root
    }

    @SuppressLint("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //Loop through every marker in database and display on map.
        viewModel.getAllMarkers.observe(viewLifecycleOwner) {
            if (viewModel.isMarker.value) {
                it.forEach { poi ->
                    if (viewModel.isMountain.value && poi.category.toString() == "Mountain") {
                        myPOIs(poi.name, poi.category!!, poi.lat, poi.lon, poi.id.toString(), poi.photo)
                    }
                    if (viewModel.isFishing.value && poi.category.toString() == "Fishing") {
                        myPOIs(poi.name, poi.category!!, poi.lat, poi.lon, poi.id.toString(), poi.photo)
                    }
                    if (viewModel.isAttraction.value && poi.category.toString() == "Attraction") {
                        myPOIs(poi.name, poi.category!!, poi.lat, poi.lon, poi.id.toString(), poi.photo)
                    }
                    if (viewModel.isMisc.value && poi.category.toString() == "Misc") {
                        myPOIs(poi.name, poi.category!!, poi.lat, poi.lon, poi.id.toString(), poi.photo)
                    }
                    if (viewModel.isCamping.value && poi.category.toString() == "Camping") {
                        myPOIs(poi.name, poi.category!!, poi.lat, poi.lon, poi.id.toString(), poi.photo)
                    }
                    if (viewModel.isCanoe.value && poi.category.toString() == "Canoe") {
                        myPOIs(poi.name, poi.category!!, poi.lat, poi.lon, poi.id.toString(), poi.photo)
                    }
                }
            }
        }
        // Heatmap from starting position of hike & Display markers from starting position of hike.
        viewModel.allData.observe(viewLifecycleOwner) {
            if (viewModel.isHeatMap.value) {
                it.forEach { pos ->
                    myHeatMap(pos.startLatitude, pos.startLongitude, 0.001)
                }
            }
            if (viewModel.isMarker.value && viewModel.isStartingPos.value) {
                it.forEach { pos ->
                    myMarker(pos.startLatitude, pos.startLongitude)
                }
            }
        }

        // Centers map on current location.
        binding.btnCenterLocation.setOnClickListener {
            controller.animateTo(myLocationOverlay.myLocation, 18.0, 1000L)
            myLocationOverlay.enableFollowLocation()
        }

        // Expandable menu while on a hike.
        binding.arrowButton.setOnClickListener {
            if (binding.hiddenView.visibility == View.VISIBLE) {
                TransitionManager.beginDelayedTransition(binding.baseCardview, AutoTransition())
                binding.hiddenView.visibility = View.GONE
                binding.arrowButton.setImageResource(R.drawable.expand_less)
            } else {
                TransitionManager.beginDelayedTransition(binding.baseCardview, AutoTransition())
                binding.hiddenView.visibility = View.VISIBLE
                binding.arrowButton.setImageResource(R.drawable.expand_more)
            }
        }

        // Navigates to Add Marker Fragment while on a hike.
        binding.btnAddMarker.setOnClickListener {
            viewModel.getCurrentLatLng(LatLng(myLocationOverlay.myLocation.latitude, myLocationOverlay.myLocation.longitude))
            findNavController().navigate(R.id.action_mapFragment_to_addMarkerFragment)


        }

        // Navigates to Add Marker Fragment while not on a hike.
        binding.fabAddMarker.setOnClickListener {
            viewModel.getCurrentLatLng(LatLng(myLocationOverlay.myLocation.latitude, myLocationOverlay.myLocation.longitude))
            findNavController().navigate(R.id.action_mapFragment_to_addMarkerFragment)
        }

        //Buttons for start/pause, cancel and finish a hike.
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
        viewModel.highestHikeId.observe(viewLifecycleOwner) {
            if (it != null) {
                highestHikeId.value = it
                folder = context?.filesDir?.absolutePath + "/${it.plus(1)}/"
            } else {
                highestHikeId.value = 0
                folder = context?.filesDir?.absolutePath + "/1/"
            }
        }
        binding.btnTakePhoto.setOnClickListener {
            checkCameraPermissions()
        }
    }

    private fun checkCameraPermissions() {
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
        mapView.onResume()
        addAllPolyLines(polygonColor)

    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
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
    }

    /*
    * Function to add values from service to fragment.
    * Toggles the Floating action button to become a menu if on a hike.
    * Draws a trail behind you when on a hike.
    */
    private fun subscribeToObservers() {
        LocationService.timeHikedInMillis.observe(viewLifecycleOwner) {
            currentTimeInMillis = it
            val formattedTime = Utility.getFormattedStopWatchTime(currentTimeInMillis, true)
            binding.timerTV.text = formattedTime
            if (currentTimeInMillis > 0L) {
                binding.fabAddMarker.visibility = View.GONE
                binding.arrowButton.visibility = View.VISIBLE
                binding.cancelHikeBtn.isVisible = true
                binding.btnTakePhoto.isVisible = true
                binding.distanceHiked.isVisible = true
            }
        }
        LocationService.isTracking.observe(viewLifecycleOwner) {
            updateTracking(it)
        }

        LocationService.pathPoints.observe(viewLifecycleOwner) {
            pathPoints = it
            addLatestPolyline(polygonColor)

        }
    }

    // Cancel hike.
    private fun showCancelHikeDialog() {
        val dialog = MaterialAlertDialogBuilder(requireContext(), androidx.appcompat.R.style.AlertDialog_AppCompat)
            .setTitle(getString(R.string.cancel_hike_question))
            .setMessage(getString(R.string.are_you_sure_you_wanna_cancel_the_hike))
            .setIcon(R.drawable.group_1)
            .setPositiveButton(getString(R.string.yes_caps)) {_,_ ->
                deleteFolderWhenCancellingHike()
                sendCommandToService(ACTION_STOP) // another idea: calling stopHike() with parameter
                findNavController().navigate(R.id.action_global_homeFragment)
            }
            .setNegativeButton(getString(R.string.no_caps)) { dialogInterface, _ ->
                dialogInterface.cancel()
            }
            .create()
        dialog.show()
    }

    // Logic for when to show play/pause icon and finish button.
    @SuppressLint("UseCompatLoadingForDrawables")
    private fun updateTracking(isTracking: Boolean) {
        this.isTracking = isTracking
        if (!isTracking) {
            if (currentTimeInMillis == 0L) {
                binding.toggleHikeBtn.setImageDrawable(ContextCompat.getDrawable(requireActivity(), R.drawable.ic_baseline_play_arrow_24))
            }
            else {
                binding.toggleHikeBtn.setImageDrawable(ContextCompat.getDrawable(requireActivity(), R.drawable.ic_baseline_play_arrow_24))
                binding.finishHikeBtn.show()
            }

        } else {
            binding.toggleHikeBtn.setImageDrawable(ContextCompat.getDrawable(requireActivity(), R.drawable.ic_baseline_pause_24))
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

    // Used before taking a snapshot of the map.
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

    /*
    Function to take a snapshot of map,
    save all relevant hike data to database and stop the service.
     */
    @MainThread
    private fun endHikeAndSaveToDb() {
        var distanceInMeters = 0
        val latLng = pathPoints.first().first()
        for (polyline in pathPoints) {
            distanceInMeters += Utility.calculatePolylineLength(polyline).toInt()
        }
        val averageSpeed = round((distanceInMeters / 1000f) / (currentTimeInMillis / 1000f / 60 / 60) * 10) / 10f
        val dateTimeStamp = Calendar.getInstance().timeInMillis
        val folderPath = context?.filesDir?.absolutePath + "/${highestHikeId.value?.plus(1)}/"
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
            viewModel.addDataToRoom(hike, findNavController())
            Snackbar.make(
                requireActivity().findViewById(R.id.hikesFragment),
                getString(R.string.hike_saved_successfully),
                Snackbar.LENGTH_LONG
            ).show()
            sendCommandToService(ACTION_STOP)
        }, MapSnapshot.INCLUDE_FLAG_UPTODATE + INCLUDE_FLAG_SCALED, mapView)
        Thread(mapSnapshot).start()

    }

    /*
    Loops through every polyline in your hike and displays it to map.
    Gets called when a user is currently on a hike and enters the map fragment.
     */
    private fun addAllPolyLines(color: String) {
        if(pathPoints.isNotEmpty() && pathPoints.last().size > 1) {
            for (polyline in pathPoints) {

                for (i in 0..polyline.size - 2) {
                    val polygon = Polygon()
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

    /*
    Makes a line from your last position and previous last position.
    Adds it to the mapview.
     */
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
            (if (myLocationOverlay.myLocation != null) {
                binding.tvDistanceHiked.text = myLocationOverlay.myLocation.altitude.toString()
                }
            )
            mapView.overlayManager?.add(polygon)
        }
    }

    // Creates a marker without infowindow from starting position
    private fun myMarker(lat: Double, lon: Double) {
        lifecycleScope.launch {
            val startMarker = Marker(mapView)
            startMarker.position = GeoPoint(lat, lon)
            startMarker.icon = ContextCompat.getDrawable(requireActivity(), R.drawable.location)
            startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            startMarker.setInfoWindow(null)
            mapView.overlayManager?.add(startMarker)
        }
    }


    /*
    Homemade heatmap.
    Creates a semi transparent circle around starting position.
     */
    private fun myHeatMap(
        lat: Double,
        lon: Double,
        radius: Double,
    ) {
        val steps = 50
        val polygon = Polygon(mapView)
        val centerX = mutableListOf<Double>()
        val centerY = mutableListOf<Double>()
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

    /*
    Point of Interest with custom infowindow.
    Marker will be customized depending on the parameters.
    Button for showing POI in AR.
    Button for deleting a marker
     */
    private fun myPOIs(name: String?, category: String, lat: Double, lon: Double, id: String, myPhoto: String?) {
        val infoWindow = MarkerWindow(mapView)
        val infoImage = infoWindow.view.findViewById<ImageView>(R.id.ivInfoWindow)
        val titleText = infoWindow.view.findViewById<TextView>(R.id.tvInfoWindowTitle)
        val locationText = infoWindow.view.findViewById<TextView>(R.id.tvInfoWindowDestination)
        val categoryText = infoWindow.view.findViewById<TextView>(R.id.tvInfoWindowCategory)
        val arButton = infoWindow.view.findViewById<ImageButton>(R.id.ibShowARInfoWindow)
        val deleteButton = infoWindow.view.findViewById<ImageButton>(R.id.ibDeleteMarker)

        lifecycleScope.launch {
            val poiMarker = Marker(mapView)
            poiMarker.position = GeoPoint(lat, lon)
            when(category) {
                "Mountain" -> poiMarker.icon = ContextCompat.getDrawable(requireActivity(), R.drawable.mountain)
                "Fishing" -> poiMarker.icon = ContextCompat.getDrawable(requireActivity(), R.drawable.fishing)
                "Attraction" -> poiMarker.icon = ContextCompat.getDrawable(requireActivity(), R.drawable.attraction)
                "Camping" -> poiMarker.icon = ContextCompat.getDrawable(requireActivity(), R.drawable.tent)
                "Canoe" -> poiMarker.icon = ContextCompat.getDrawable(requireActivity(), R.drawable.canoe)
                "Misc" -> poiMarker.icon = ContextCompat.getDrawable(requireActivity(), R.drawable.map)
            }
            try {
                Glide.with(requireContext())
                    .load(BitmapFactory.decodeFile(context?.filesDir?.path + "/${myPhoto}"))
                    .transform(CircleCrop())
                    .into(infoImage)
            } catch (e: java.lang.NullPointerException) {
                Log.d("TAG", "myPOIs: Myphoto is null")
            }
            titleText.text = name
            categoryText.text = category
            poiMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            poiMarker.setOnMarkerClickListener { marker, mapView ->
                poiMarker.title = name
                poiMarker.infoWindow = infoWindow
                val addresses: String =
                    geocoder.getFromLocation(lat, lon, 1)?.first()?.getAddressLine(0).toString()
                if (addresses.isEmpty()) {
                    locationText.text = "${round(lat * 10000) / 10000}, ${round(lon * 10000) / 10000}"
                }
                else {
                    locationText.text = addresses
                }
                arButton.setOnClickListener{
                    val action = MapFragmentDirections.actionMapFragmentToARFragment(poiMarker.title, LatLong(lat, lon), category)
                    findNavController().navigate(action)
                }
                deleteButton.setOnClickListener{
                    val dialog = MaterialAlertDialogBuilder(requireContext(), androidx.appcompat.R.style.AlertDialog_AppCompat)
                        .setTitle(resources.getString(R.string.delete_marker_question))
                        .setMessage(resources.getString(R.string.delete_marker_forever_question))
                        .setIcon(R.drawable.group_1)
                        .setPositiveButton(resources.getString(R.string.yes_caps)) {_,_ ->
                            lifecycleScope.launch {
                                viewModel.deleteMarkerById(id.toInt())
                                poiMarker.infoWindow.close()
                                poiMarker.remove(mapView)
                                Toast.makeText(requireContext(), "POI deleted forever", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .setNegativeButton(resources.getString(R.string.no_caps)) { dialogInterface, _ ->
                            dialogInterface.cancel()
                        }
                        .create()
                    dialog.show()
                }
                poiMarker.showInfoWindow()
                false
            }
            mapView.overlayManager?.add(poiMarker)
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

    //Saving a bitmap in the apps internal storage
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

    //Making a folder in the internal where the name is the highest hike id + 1
    //Here we put photos for a hike in progress
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

    //Deleting the newest folder when canceling hike. So photos on a hike that gets cancelled gets deleted
    private fun deleteFolderWhenCancellingHike(){
        val folder = context?.filesDir

        val folders = folder?.listFiles()
        folders?.sortByDescending { dir ->
            dir.lastModified()
        }

        val newestFolder = folders?.first()
        newestFolder?.delete()
    }

    //Saving bitmap to the folder in internal storage with the highest Id-number. For when a hike gets finished
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

// Custom Infowindow used in POI markers.
class MarkerWindow(mapView: MapView) :
        InfoWindow(R.layout.info_window, mapView) {

    override fun onOpen(item: Any?) {
        closeAllInfoWindowsOn(mapView)

        mView.setOnClickListener{
            close()
        }
    }

    override fun onClose() {
    }
}

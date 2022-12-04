package com.example.stroll.presentation.fragment

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.stroll.R
import com.example.stroll.data.local.MarkerEntity
import com.example.stroll.databinding.FragmentAddMarkerBinding
import com.example.stroll.presentation.viewmodel.MainViewModel
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import java.io.IOException

@AndroidEntryPoint
class AddMarkerFragment : BaseFragment() {
    private val viewModel: MainViewModel by activityViewModels()
    private var _binding: FragmentAddMarkerBinding? = null
    private val binding get() = _binding!!

    private val IMAGE_CAPTURE_CODE = 1001
    private var imageUri: Uri? = null
    private var bitmapForMarker: Bitmap? = null

    private val cameraPermissionResult = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.CAMERA, false) -> {
                Toast.makeText(
                    requireContext(),
                    resources.getString(R.string.yes_to_camera_request),
                    Toast.LENGTH_SHORT
                ).show()
                openCameraInterface()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentAddMarkerBinding.inflate(inflater, container, false)

        //Gives value to category from the spinner in view.
        var markerCategory = ""
        binding.markerCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(p0: AdapterView<*>?) {}

            override fun onItemSelected(adapterView: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                when(pos) {
                    0 -> {
                        markerCategory = "Mountain"
                    }
                    1 -> {
                        markerCategory = "Fishing"
                    }
                    2 -> {
                        markerCategory = "Attraction"
                    }
                    3 -> {
                        markerCategory = "Camping"
                    }
                    4 -> {
                        markerCategory = "Canoe"
                    }
                    5 -> {
                        markerCategory = "Misc"
                    }
                }
            }
        }
        binding.btnAddPhotoToMarker.setOnClickListener{
            checkCameraPermissions()
        }

        //Adding the marker to database.
        binding.createMarkerBtn.setOnClickListener {
            val markerName = binding.markerName.text.toString()
            if (markerName.isNotEmpty()) {
                try {
                    if (bitmapForMarker != null){
                        saveBitmapToInternalStorage(bitmapForMarker.toString(), bitmapForMarker!!)
                        viewModel.addMarkerDataToRoom(
                            MarkerEntity(
                                name = markerName,
                                category = markerCategory,
                                lat = viewModel.currentLatLng.value!!.latitude,
                                lon = viewModel.currentLatLng.value!!.longitude,
                                photo = bitmapForMarker.toString().plus(".png")
                            )
                        )
                    }
                    else{
                        viewModel.addMarkerDataToRoom(
                            MarkerEntity(
                                name = markerName,
                                category = markerCategory,
                                lat = viewModel.currentLatLng.value!!.latitude,
                                lon = viewModel.currentLatLng.value!!.longitude,
                            )
                        )
                    }
                    Snackbar.make(
                        requireActivity().findViewById(R.id.mapFragment),
                        resources.getString(R.string.poi_made_successfully),
                        Snackbar.LENGTH_LONG
                    ).show()
                    findNavController().navigate(R.id.action_global_mapFragment)
                } catch (e: java.lang.NullPointerException) {
                    Log.d("nullpointer", "onCreateView: $e")
                }
            }
            else {
                Snackbar.make(requireView(), resources.getString(R.string.please_enter_a_name_for_poi), Snackbar.LENGTH_SHORT).show()
            }

        }

        return binding.root
    }

    //If the user has camera permission, then the camera interface gets opened.
    //If not, the user gets a prompt to decide if he wants to give the app camera permission
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Callback from camera intent
        if (resultCode == Activity.RESULT_OK){
            // Set image captured to image view
            val source = imageUri?.let { ImageDecoder.createSource(requireActivity().contentResolver, it) }
            val bitmap = source?.let { ImageDecoder.decodeBitmap(it) }
            if (bitmap != null) {
                bitmapForMarker = bitmap
            }
        }
    }

    private fun saveBitmapToInternalStorage(filename: String, bitmap: Bitmap): Boolean {
        return try {
            context?.openFileOutput("$filename.png", Context.MODE_PRIVATE).use { stream ->
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

}
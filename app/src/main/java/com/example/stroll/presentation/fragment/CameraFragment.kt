package com.example.stroll.presentation.fragment

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.example.stroll.R
import com.example.stroll.backgroundlocationtracking.hasCameraPermission
import com.example.stroll.databinding.FragmentCameraBinding
import com.example.stroll.databinding.FragmentIntroductionBinding
import com.example.stroll.databinding.FragmentMainBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CameraFragment() : Fragment() {

    private var _binding: FragmentCameraBinding? = null
    private val binding get() = _binding!!
    private val IMAGE_CAPTURE_CODE = 1001
    private var imageUri: Uri? = null

    @SuppressLint("MissingPermission")
    override fun onCreateView(

        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentCameraBinding.inflate(inflater, container, false)

        val permissionGranted = requestCameraPermission()
        if (permissionGranted){
            openCameraInterface()
        }





        return binding.root
    }

    @SuppressLint("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        /*binding.openCamera.setOnClickListener {
            openCamera()
        }*/
    }

    private fun requestCameraPermission(): Boolean {
        val permissionGranted = activity?.let {
            ContextCompat.checkSelfPermission(
                it.applicationContext,
                android.Manifest.permission.CAMERA
            )
        } == PackageManager.PERMISSION_GRANTED
        return permissionGranted
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

}


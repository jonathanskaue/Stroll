package com.example.stroll.presentation.fragment

import android.R.attr.bitmap
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.example.stroll.R
import com.example.stroll.databinding.FragmentCameraBinding
import com.example.stroll.presentation.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException


@AndroidEntryPoint
class CameraFragment() : Fragment() {

    private var _binding: FragmentCameraBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by viewModels()

    private val IMAGE_CAPTURE_CODE = 1001
    private var imageUri: Uri? = null
    private var folder: String = ""

    @SuppressLint("MissingPermission")
    override fun onCreateView(

        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentCameraBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnTakePhoto.setOnClickListener {
            openCameraInterface()
        }
        binding.btnAR.setOnClickListener {
            openAR()
        }

        viewModel.highestHikeId.observe(viewLifecycleOwner, Observer {
            Log.d("hikeId", "$it")
            folder = if (it != null){
                context?.filesDir?.absolutePath + "/${it.plus(1)}/"
            } else context?.filesDir?.absolutePath + "/1/"
        })
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
            binding.imageviewPicture.setImageURI(imageUri)
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

    private fun openAR(){

    }
}


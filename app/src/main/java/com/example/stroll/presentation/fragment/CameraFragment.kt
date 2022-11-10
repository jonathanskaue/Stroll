package com.example.stroll.presentation.fragment

import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.stroll.databinding.FragmentCameraBinding
import dagger.hilt.android.AndroidEntryPoint
import java.io.IOException

typealias CornersListener = () -> Unit

@AndroidEntryPoint
class CameraFragment() : Fragment() {

    private var _binding: FragmentCameraBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentCameraBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    /*private fun takePhoto() {
        val folder = File()
    }*/

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



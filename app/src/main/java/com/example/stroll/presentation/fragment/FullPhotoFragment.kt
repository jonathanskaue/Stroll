package com.example.stroll.presentation.fragment

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.*
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.example.stroll.databinding.FragmentFullPhotoBinding

class FullPhotoFragment : BaseFragment() {

    private val args: FullPhotoFragmentArgs by navArgs()
    private var _binding: FragmentFullPhotoBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentFullPhotoBinding.inflate(inflater, container, false)
        loadMyPhoto(args.photoName, args.hikeId)
        return binding.root
    }

    private fun loadMyPhoto(photoName: String, hikeId: Int) {
        Glide.with(this)
            .load(BitmapFactory.decodeFile(context?.filesDir?.path + "/$hikeId/$photoName"))
            .into(binding.fullPhoto)
    }
}
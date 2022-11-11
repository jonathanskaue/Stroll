package com.example.stroll.presentation.fragment

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.stroll.databinding.FragmentHomeBinding


import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.example.stroll.data.local.InternalStoragePhoto
import com.example.stroll.databinding.FragmentHikeDetailsBinding
import com.example.stroll.other.Utility
import com.example.stroll.presentation.adapters.PhotoAdapter
import com.example.stroll.presentation.viewmodel.StatisticsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.lang.Math.round
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class HikeDetailsFragment : BaseFragment() {

    @set:Inject
    var name = ""

    val args: HikeDetailsFragmentArgs by navArgs()
    private var photoAdapter = PhotoAdapter()

    private val viewModel: StatisticsViewModel by viewModels()

    private var _binding: FragmentHikeDetailsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        // Inflate the layout for this fragment
        _binding = FragmentHikeDetailsBinding.inflate(inflater, container, false)
        setUpPhotoRecyclerView()
        loadHikePhotosIntoRecyclerView()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("args", "onViewCreated: $args")

    }

    private fun setUpPhotoRecyclerView() = binding.rvPrivatePhotos.apply {
        adapter = photoAdapter
        layoutManager = StaggeredGridLayoutManager(3, RecyclerView.VERTICAL)
    }

    private fun loadHikePhotosIntoRecyclerView(){
        lifecycleScope.launch{
            val photos = loadHikePhotos()
            photoAdapter.submitList(photos)
        }
    }

    private suspend fun loadHikePhotos(): List<InternalStoragePhoto>{
        return withContext(Dispatchers.IO) {
            val path = context?.filesDir?.absolutePath + "/${args.id}/"
            val dir = File(path).listFiles()
            Log.d("dir", "${dir?.toList()}: ")
            dir.filter { it.canRead() && it.isFile && it.name.endsWith(".png") }!!.map {
                val bytes = it.readBytes()
                val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                InternalStoragePhoto(it.name, bmp)
            }
        }
    }
}
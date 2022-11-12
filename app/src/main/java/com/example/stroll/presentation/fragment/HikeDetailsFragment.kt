package com.example.stroll.presentation.fragment

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Geocoder
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
import com.bumptech.glide.Glide
import com.example.stroll.R
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
import java.text.SimpleDateFormat
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

    private lateinit var geocoder: Geocoder
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        geocoder = Geocoder(this.requireContext(), Locale.getDefault())

        // Inflate the layout for this fragment
        _binding = FragmentHikeDetailsBinding.inflate(inflater, container, false)
        setUpPhotoRecyclerView()
        loadHikePhotosIntoRecyclerView()
        viewModel.getHikeById(args.id)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.hikeById.observe(viewLifecycleOwner) {
            if (it == null) {
                Log.d("gethikebyid", "onViewCreated: ${args.id}")
                Log.d("gethikebyid", "onViewCreated: hikeEntity is null")
            }
            else {
                viewModel.getDetailsByLatLng(geocoder, it.startLatitude, it.startLongitude)
                loadImage()
                binding.tvLocation.text = viewModel.address.value
                binding.tvDistance.append("${it.distanceInMeters/1000f}km")
                binding.tvAverageSpeed.append("${it.averageSpeedInKMH}km/h")
                binding.tvTime.text = Utility.getFormattedStopWatchTime(it.timeInMillis)
                val calendar = Calendar.getInstance().apply {
                    timeInMillis = it.timeStamp
                }
                val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                binding.tvHikeDate.text = dateFormat.format(calendar.time)

                Log.d("gethikebyid", "onViewCreated: ${it}")
                Log.d("gethikebyid", "onViewCreated: ${viewModel.address.value}")
                Log.d("gethikebyid", "onViewCreated: ${viewModel.city.value}")

            }
        }
    }

    private fun loadImage() {
        Glide.with(this)
            .load(BitmapFactory.decodeFile(context?.filesDir?.path + "/${viewModel.hikeById.value?.mapSnapShot}"))
            .into(binding.hikeMapSnapShot)
    }

    private fun setUpPhotoRecyclerView() = binding.rvPrivatePhotos.apply {
        adapter = photoAdapter
        layoutManager = StaggeredGridLayoutManager(1, RecyclerView.HORIZONTAL)
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
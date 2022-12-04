package com.example.stroll.presentation.fragment

import android.graphics.BitmapFactory
import android.location.Geocoder
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.bumptech.glide.Glide
import com.example.stroll.R
import com.example.stroll.data.local.InternalStoragePhoto
import com.example.stroll.databinding.FragmentHikeDetailsBinding
import com.example.stroll.domain.repository.RVClickListener
import com.example.stroll.other.Utility
import com.example.stroll.presentation.adapters.PhotoAdapter
import com.example.stroll.presentation.viewmodel.StatisticsViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class HikeDetailsFragment : BaseFragment(), RVClickListener {

    private val args: HikeDetailsFragmentArgs by navArgs()

    private val photoAdapter by lazy { PhotoAdapter(this) }
    private val viewModel: StatisticsViewModel by viewModels()

    private var _binding: FragmentHikeDetailsBinding? = null
    private val binding get() = _binding!!

    private lateinit var geocoder: Geocoder
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        geocoder = Geocoder(this.requireContext(), Locale.getDefault())

        // Inflate the layout for this fragment
        _binding = FragmentHikeDetailsBinding.inflate(inflater, container, false)
        setUpPhotoRecyclerView()
        loadHikePhotosIntoRecyclerView()
        val hikeId = args.id
        viewModel.getHikeById(hikeId)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.allHikeId.observe(viewLifecycleOwner) {
            if ((it.maxOrNull() ?: 0) != args.id) {
                binding.fabDeleteHike.setOnClickListener {
                    showDeleteHikeDialog()
                }
            }
            else {
                binding.fabDeleteHike.visibility = View.GONE
            }
        }


        viewModel.hikeById.observe(viewLifecycleOwner) {
            if (it == null) {
                Log.d("gethikebyid", "onViewCreated: ${args.id}")
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

            }
        }
    }

    private fun setUpPhotoRecyclerView() = binding.rvPrivatePhotos.apply {
        adapter = photoAdapter
        layoutManager = StaggeredGridLayoutManager(1, RecyclerView.HORIZONTAL)
    }

    //Loading the map associated with the hike
    private fun loadImage() {
        Glide.with(this)
            .load(BitmapFactory.decodeFile(context?.filesDir?.path + "/${viewModel.hikeById.value?.mapSnapShot}"))
            .into(binding.hikeMapSnapShot)
    }

    private fun loadHikePhotosIntoRecyclerView(){
        lifecycleScope.launch{
            val photos = loadHikePhotos()
            if (photos.isNotEmpty()) {
                binding.cvPrivatePhotos.isVisible = true
            }
            photoAdapter.submitList(photos)
        }
    }

    private suspend fun loadHikePhotos(): List<InternalStoragePhoto>{
        return withContext(Dispatchers.IO) {
            val path = context?.filesDir?.absolutePath + "/${args.id}/"
            val dir = File(path).listFiles()
            dir?.filter { it.canRead() && it.isFile && it.name.endsWith(".png") }!!.map {
                val bytes = it.readBytes()
                val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                InternalStoragePhoto(it.name, bmp)
            }
        }
    }

    private fun showDeleteHikeDialog() {
        val dialog = MaterialAlertDialogBuilder(requireContext(), androidx.appcompat.R.style.AlertDialog_AppCompat)
            .setTitle(resources.getString(R.string.delete_hike_question))
            .setMessage(resources.getString(R.string.delete_hike_forever_question))
            .setIcon(R.drawable.group_1)
            .setPositiveButton(resources.getString(R.string.yes_caps)) {_,_ ->
                lifecycleScope.launch {
                    viewModel.deleteHikeById(args.id)
                }
                val action = HikeDetailsFragmentDirections.actionGlobalHikesFragment()
                findNavController().navigate(action)
            }
            .setNegativeButton(resources.getString(R.string.no_caps)) { dialogInterface, _ ->
                dialogInterface.cancel()
            }
            .create()
        dialog.show()
    }

    //Navigating to FullPhoto fragment to show the full photo clicked on
    override fun onClick(position: Int) {
        val photoString = photoAdapter.differ.currentList[position].name
        val hikeId = args.id
        val action = HikeDetailsFragmentDirections.actionHikeDetailsFragmentToFullPhotoFragment(hikeId, photoString)
        findNavController().navigate(action)
    }
}
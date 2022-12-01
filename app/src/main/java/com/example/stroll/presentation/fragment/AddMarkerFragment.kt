package com.example.stroll.presentation.fragment

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.stroll.R
import com.example.stroll.data.local.MarkerEntity
import com.example.stroll.databinding.FragmentAddMarkerBinding
import com.example.stroll.databinding.FragmentRegisterBinding
import com.example.stroll.other.SortType
import com.example.stroll.presentation.viewmodel.MainViewModel
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AddMarkerFragment : BaseFragment() {
    private val viewModel: MainViewModel by activityViewModels()
    private var _binding: FragmentAddMarkerBinding? = null
    private val binding get() = _binding!!


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d("MYFragment", "THIS IS MY MARKER POSITION: ${viewModel.currentLatLng.value}")
        // Inflate the layout for this fragment
        _binding = FragmentAddMarkerBinding.inflate(inflater, container, false)

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
        binding.createMarkerBtn.setOnClickListener {
            val markerName = binding.markerName.text.toString()
            try {
                viewModel.addMarkerDataToRoom(
                    MarkerEntity(
                        name = markerName,
                        category = markerCategory,
                        lat = viewModel.currentLatLng.value!!.latitude,
                        lon = viewModel.currentLatLng.value!!.longitude,
                    )
                )
                Snackbar.make(
                    requireActivity().findViewById(R.id.mapFragment),
                    "POI made successfully",
                    Snackbar.LENGTH_LONG
                ).show()
                findNavController().navigate(R.id.action_addMarkerFragment_to_mapFragment)
            } catch (e: java.lang.NullPointerException) {
                Log.d("nullpointer", "onCreateView: $e")
            }
        }

        return binding.root
    }

}
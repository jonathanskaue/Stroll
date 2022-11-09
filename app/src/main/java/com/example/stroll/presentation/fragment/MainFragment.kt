package com.example.stroll.presentation.fragment

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AppCompatDelegate
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.navigation.findNavController
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.stroll.MainActivity
import com.example.stroll.R
import com.example.stroll.backgroundlocationtracking.LocationService
import com.example.stroll.backgroundlocationtracking.Polyline
import com.example.stroll.databinding.FragmentMainBinding
import com.example.stroll.other.Constants.ACTION_START
import com.example.stroll.other.SortType
import com.example.stroll.presentation.adapters.HikeAdapter
import com.example.stroll.presentation.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlin.concurrent.fixedRateTimer
import kotlin.math.sqrt

@AndroidEntryPoint
class MainFragment() : BaseFragment() {

    private val viewModel: MainViewModel by viewModels()

    private var isTracking = false
    private var pathPoints = mutableListOf<Polyline>()

    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!

    private lateinit var hikeAdapter: HikeAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        // Inflate the layout for this fragment
        _binding = FragmentMainBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val menuHost: MenuHost = requireActivity()
        setupRecyclerView()

        when(viewModel.sortType) {
            SortType.DATE -> binding.spFilter.setSelection(0)
            SortType.HIKE_TIME -> binding.spFilter.setSelection(1)
            SortType.DISTANCE -> binding.spFilter.setSelection(2)
            SortType.AVG_SPEED -> binding.spFilter.setSelection(3)
        }

        binding.spFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(p0: AdapterView<*>?) {}

            override fun onItemSelected(adapterView: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                when(pos) {
                    0 -> viewModel.sortHikes(SortType.DATE)
                    1 -> viewModel.sortHikes(SortType.HIKE_TIME)
                    2 -> viewModel.sortHikes(SortType.DISTANCE)
                    3 -> viewModel.sortHikes(SortType.AVG_SPEED)
                }
            }
        }

        viewModel.hikes.observe(viewLifecycleOwner, Observer {
            hikeAdapter.submitList(it)
        })
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.toolbar, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.settings -> {
                        view.findNavController().navigate(MainFragmentDirections.actionMainFragmentToSettingsFragment())
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun setupRecyclerView() = binding.rvHikes.apply {
        hikeAdapter = HikeAdapter()
        adapter = hikeAdapter
        layoutManager = LinearLayoutManager(requireContext())
    }

    private fun sendCommandToService(action: String) =
        Intent(requireContext(), LocationService::class.java).also {
            it.action = action
            requireContext().startService(it)
        }

}


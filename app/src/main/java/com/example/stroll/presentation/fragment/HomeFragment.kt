package com.example.stroll.presentation.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.stroll.databinding.FragmentHomeBinding
import androidx.fragment.app.viewModels
import com.example.stroll.R
import com.example.stroll.other.Utility
import com.example.stroll.presentation.viewmodel.StatisticsViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.lang.Math.round
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class HomeFragment : BaseFragment() {

    @set:Inject
    var name = ""

    private val viewModel: StatisticsViewModel by viewModels()

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        // Inflate the layout for this fragment
        _binding = FragmentHomeBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        subscribeToObservers()
        binding.tvGreetings.text = myGreetingsMessage()
        binding.tvUserName.text = name.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
    }
    private fun subscribeToObservers() {
        viewModel.totalTimeInMillisHiked.observe(viewLifecycleOwner) {
            it?.let {
                val totalTimeHiked = Utility.getFormattedStopWatchTime(it)
                binding.tvTotalTime.text = totalTimeHiked
            }
        }
        viewModel.totalDistanceHiked.observe(viewLifecycleOwner) {
            it?.let {
                val km = it / 1000f
                val totalDistance = round(km * 10f) / 10f
                val totalDistanceString = "${totalDistance}km"
                binding.tvTotalDistance.text = totalDistanceString
            }
        }
        viewModel.totalAverageSpeed.observe(viewLifecycleOwner) {
            it?.let {
                val avgSpeed = round(it * 10f) / 10f
                val avgSpeedString = "${avgSpeed}km/h"
                binding.tvAverageSpeed.text = avgSpeedString
            }
        }
    }

    private fun myGreetingsMessage() : String {
        val cal = Calendar.getInstance()

        return when(cal.get(Calendar.HOUR_OF_DAY)) {
            in 0..11 -> resources.getString(R.string.good_morning)
            in 12..16 -> resources.getString(R.string.good_afternoon)
            in 17..23 -> getString(R.string.good_evening)
            else -> resources.getString(R.string.greetings)
        }
    }
}
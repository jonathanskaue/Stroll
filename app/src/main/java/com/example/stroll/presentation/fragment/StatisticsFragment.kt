package com.example.stroll.presentation.fragment

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.view.*
import android.widget.AdapterView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.example.stroll.R
import com.example.stroll.data.local.StrollDataEntity
import com.example.stroll.databinding.FragmentStatisticsBinding
import com.example.stroll.other.CustomMarkerView
import com.example.stroll.other.SortType
import com.example.stroll.presentation.viewmodel.StatisticsViewModel
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class StatisticsFragment : BaseFragment() {

    private val viewModel: StatisticsViewModel by viewModels()

    private var _binding: FragmentStatisticsBinding? = null
    private val binding get() = _binding!!
    private var show_top5: Boolean = false
    private var selected_filter: Int = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        // Inflate the layout for this fragment
        _binding = FragmentStatisticsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        subscribeToObservers(selected_filter)
        setupBarChart()
        binding.toggleViewButton.setOnClickListener {
            val action = StatisticsFragmentDirections.actionStatisticsFragmentToHikesFragment()
            findNavController().navigate(action)
        }
        binding.checkTop5.setOnCheckedChangeListener { _, isChecked ->
            show_top5 = isChecked
            subscribeToObservers(selected_filter)

        }

        when (viewModel.sortType) {
            SortType.DATE -> {
                binding.statisticsSpFilter.setSelection(0)
            }
            SortType.HIKE_TIME -> {
                binding.statisticsSpFilter.setSelection(1)
            }
            SortType.DISTANCE -> {
                binding.statisticsSpFilter.setSelection(2)
            }
            SortType.AVG_SPEED -> {
                binding.statisticsSpFilter.setSelection(3)
            }
        }

        binding.statisticsSpFilter.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(p0: AdapterView<*>?) {}

                override fun onItemSelected(
                    adapterView: AdapterView<*>?,
                    view: View?,
                    pos: Int,
                    id: Long
                ) {
                    selected_filter = pos
                    subscribeToObservers(pos)
                }
            }

    }


    private fun setupBarChart() {
        binding.barChart.xAxis.apply {
            position = XAxis.XAxisPosition.BOTTOM
            setDrawLabels(false)
            axisLineColor = Color.WHITE
            textColor = Color.WHITE
            setDrawGridLines(false)
        }
        binding.barChart.axisLeft.apply {
            axisLineColor = Color.WHITE
            textColor = Color.WHITE
            setDrawGridLines(false)
        }
        binding.barChart.axisRight.apply {
            axisLineColor = Color.WHITE
            textColor = Color.WHITE
            setDrawGridLines(false)
        }
        binding.barChart.apply {
            description.setPosition(700f, 100f)
            description.text = getString(R.string.distance_in_km)
            description.textColor = Color.WHITE
            description.textSize = 16f
            legend.isEnabled = false
        }
    }

    private fun subscribeToObservers(action_pos: Int) {
        when (action_pos) {
            0 -> {  //pos = 1: Date
                viewModel.hikeSortedByDate.observe(viewLifecycleOwner, Observer {
                    it?.let {val allDates = it.indices.map { i -> BarEntry(i.toFloat(), it[i].distanceInMeters.toFloat() / 1000) }
                        create_barDataSet(allDates, getString(R.string.distance_in_km), it)
                    }
                })
                binding.barChart.apply {
                    description.text = getString(R.string.distance_in_km)
                }
            }

            1 -> {  //pos = 1: hikingTime
                viewModel.hikeSortedByTime.observe(viewLifecycleOwner, Observer {
                    it?.let { val allDates = it.indices.map { i -> BarEntry(i.toFloat(), it[i].timeInMillis.toFloat() / 60000) }
                        create_barDataSet(allDates, getString(R.string.time_in_min), it)
                    }
                })
                binding.barChart.apply {
                    description.text = getString(R.string.time_in_min)
                }
            }
            2 -> {  //pos = 2: distanceInMeters
                viewModel.hikeSortedByDistance.observe(viewLifecycleOwner, Observer {
                    it?.let { val allDates = it.indices.map { i -> BarEntry(i.toFloat(), it[i].distanceInMeters.toFloat() / 1000) }
                        create_barDataSet(allDates, getString(R.string.distance_in_km), it)
                    }
                })
                binding.barChart.apply {
                    description.text = getString(R.string.average_speed_in_kmh)
                }
            }
            3 -> {  //pos = 3: averageSpeed
                viewModel.hikeSortedBySpeed.observe(viewLifecycleOwner, Observer {
                    it?.let { val allDates = it.indices.map { i -> BarEntry(i.toFloat(), it[i].averageSpeedInKMH)}
                        create_barDataSet(allDates, getString(R.string.average_speed_over_time), it)
                    }
                })
                binding.barChart.apply {
                    description.text = getString(R.string.average_speed_in_kmh)
                }
            }
        }
    }

    @SuppressLint("ResourceType")
    private fun create_barDataSet(allDates: List<BarEntry>, label: String, dataList: List<StrollDataEntity>){
        if(allDates.isEmpty()){
            binding.tvNoHikesRecorded.visibility = View.VISIBLE
            binding.checkTop5.visibility = View.INVISIBLE
        }
        else{
            binding.tvNoHikesRecorded.visibility = View.INVISIBLE
        }
        val bardataSet: BarDataSet
        if (show_top5 && allDates.size > 5){
            bardataSet = BarDataSet(allDates.subList(0,5), label).apply {
                valueTextColor = Color.WHITE
                valueTextSize = 16f
                color = ContextCompat.getColor(requireContext(), pub.devrel.easypermissions.R.color.colorAccent)
            }
        }
        else{
            bardataSet = BarDataSet(allDates, label).apply {
                valueTextColor = Color.WHITE
                valueTextSize = 16f
                color = ContextCompat.getColor(requireContext(), pub.devrel.easypermissions.R.color.colorAccent)
            }
        }
        binding.barChart.data = BarData(bardataSet)
        binding.barChart.marker =
            CustomMarkerView(dataList, requireContext(), R.layout.marker_view)
        binding.barChart.invalidate()
    }
}
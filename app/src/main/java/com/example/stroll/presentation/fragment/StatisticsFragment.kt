package com.example.stroll.presentation.fragment

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.view.*
import android.widget.AdapterView
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
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
    /*
    This Fragment shows the user's hike in a bar chart.
    He can filter the chart by date, distance, average speed and time and enable to only show the
        top 5 hikes for that category. The barchart will be rebuilt according to the filter.
     */

    // adding view model to access data from the database
    private val viewModel: StatisticsViewModel by viewModels()

    private var _binding: FragmentStatisticsBinding? = null
    private val binding get() = _binding!!
    private var show_top5: Boolean = false  // when true only top 5 results will be shown
    private var selected_filter: Int = 0    // param for the selected filter (date, distance etc.)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentStatisticsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupBarChart()     // setting up the empty barchart
        subscribeToObservers(selected_filter)   // the observer function fills the barchart with data

        // button to head back to the hikes fragment
        binding.toggleViewButton.setOnClickListener {
            val action = StatisticsFragmentDirections.actionStatisticsFragmentToHikesFragment()
            findNavController().navigate(action)
        }
        // checkbox to toggle showing top 5
        binding.checkTop5.setOnCheckedChangeListener { _, isChecked ->
            show_top5 = isChecked
            subscribeToObservers(selected_filter)

        }
        // the data is sorted by the view model depending on the selected filter
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

        // Listener for the filter to update the chart
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
        // setting the basic appearance of the barchart
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
        /*
        function to fill the chart with data, depending on the selected filter the data will be sorted
            accordingly and the bar chart changes, the function create_barDataSet() is called to set
            up the data
         */
        when (action_pos) {
            0 -> {  //pos = 1: Date
                viewModel.hikeSortedByDate.observe(viewLifecycleOwner) {
                    it?.let {
                        val allDates = it.indices.map { i -> 
                            BarEntry(i.toFloat(), it[i].distanceInMeters.toFloat() / 1000
                            )
                        }
                        create_barDataSet(allDates, getString(R.string.distance_in_km), it)
                    }
                }
                binding.barChart.apply {
                    description.text = getString(R.string.distance_in_km)
                }
            }

            1 -> {  //pos = 1: hikingTime
                viewModel.hikeSortedByTime.observe(viewLifecycleOwner) {
                    it?.let {
                        val allDates = it.indices.map { i ->
                            BarEntry(i.toFloat(), it[i].timeInMillis.toFloat() / 60000
                            )
                        }
                        create_barDataSet(allDates, getString(R.string.time_in_min), it)
                    }
                }
                binding.barChart.apply {
                    description.text = getString(R.string.time_in_min)
                }
            }
            2 -> {  //pos = 2: distanceInMeters
                viewModel.hikeSortedByDistance.observe(viewLifecycleOwner) {
                    it?.let {
                        val allDates = it.indices.map { i ->
                            BarEntry(i.toFloat(), it[i].distanceInMeters.toFloat() / 1000
                            )
                        }
                        create_barDataSet(allDates, getString(R.string.distance_in_km), it)
                    }
                }
                binding.barChart.apply {
                    description.text = getString(R.string.distance_in_km)
                }
            }
            3 -> {  //pos = 3: averageSpeed
                viewModel.hikeSortedBySpeed.observe(viewLifecycleOwner) {
                    it?.let {
                        val allDates =
                            it.indices.map { i -> BarEntry(i.toFloat(), it[i].averageSpeedInKMH) }
                        create_barDataSet(allDates, getString(R.string.average_speed_over_time), it)
                    }
                }
                binding.barChart.apply {
                    description.text = getString(R.string.average_speed_in_kmh)
                }
            }
        }
    }

    @SuppressLint("ResourceType")
    private fun create_barDataSet(allDates: List<BarEntry>, label: String, dataList: List<StrollDataEntity>){
        /*
        function to set up the dataset displayed by the barchart
         */

        // only displaying the barchart if there are recorded hikes
        if(allDates.isEmpty()){
            binding.tvNoHikesRecorded.visibility = View.VISIBLE
            binding.barChart.visibility = View.INVISIBLE
        }
        else{
            binding.tvNoHikesRecorded.visibility = View.INVISIBLE
        }

        val bardataSet: BarDataSet
        // if top 5 is selected and there are more than 5 hikes, then only display the first 5 hikes
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
        // binding the dataset to the actual barchart
        binding.barChart.data = BarData(bardataSet)
        // the marker is showing the hikes stats when a bar is selected by the user
        binding.barChart.marker =
            CustomMarkerView(dataList, requireContext(), R.layout.marker_view)
        binding.barChart.invalidate()
    }
}
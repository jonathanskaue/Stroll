package com.example.stroll.presentation.fragment

import android.annotation.SuppressLint
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.viewModels
import androidx.navigation.findNavController
import androidx.preference.PreferenceManager
import com.example.stroll.R
import com.example.stroll.backgroundlocationtracking.LocationService
import com.example.stroll.backgroundlocationtracking.LocationService.Companion.ACTION_START
import com.example.stroll.backgroundlocationtracking.SensorService
import com.example.stroll.databinding.FragmentGraphBinding
import com.example.stroll.presentation.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlin.math.sqrt

@AndroidEntryPoint
class GraphFragment() : Fragment(){

    private val viewModel: MainViewModel by viewModels()

    private var _binding: FragmentGraphBinding? = null
    private val binding get() = _binding!!
    lateinit var sensorData: MutableMap<String, FloatArray>


/*
    private lateinit var sensorManager: SensorManager
    private var sensorAccChange = floatArrayOf(0f, 0f, 0f)
    private var accSensorData = floatArrayOf(0f, 0f, 0f)
    private var sensorGyroChange = listOf(0, 0, 0)
    private var sensorMagData = floatArrayOf(0f, 0f, 0f)
    private var rotationMatrix = floatArrayOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f)
    private var deviceOrientation = floatArrayOf(0f, 0f, 0f)
    private var accDataList = mutableListOf<MutableList<Float>>()*/


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        // Inflate the layout for this fragment
        _binding = FragmentGraphBinding.inflate(inflater, container, false)
        setHasOptionsMenu(true)

        //Displays latest data in database
        viewModel.allData.observe(viewLifecycleOwner) { data ->
            data.forEach {
                binding.accdata.text = (it.accData.last().toString())
            }
        }

       /* binding.startButton.setOnClickListener {
            setUpSensors()
        }*/

        binding.stopButton.setOnClickListener {
            viewModel.addDataToRoom()
        }
        Intent(requireContext(), SensorService::class.java).apply {
            action = SensorService.ACTION_START
            activity?.startService(this)
        }

        Intent(requireContext(), SensorService::class.java).apply {
            action = SensorService.ACTION_START
            activity?.startService(this)
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadSettings()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.settings -> {
                val action = GraphFragmentDirections.actionGraphFragmentToSettingsFragment()
                view?.findNavController()?.navigate(action)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    fun loadSettings() {
        val sp = context?.let { PreferenceManager.getDefaultSharedPreferences(it) }
        val dark_mode = sp?.getBoolean("dark_mode", false)

        if (dark_mode == true) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }

    /*private fun setUpSensors() {
        val sensorManager =
            activity?.getSystemService(AppCompatActivity.SENSOR_SERVICE) as SensorManager
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.also {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)?.also {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)?.also {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)

        }
    }*/

    /*override fun onSensorChanged(event: SensorEvent?) {

        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {

            accSensorData = event.values
            val accTotal =
                sqrt(accSensorData[0] * accSensorData[0] + accSensorData[1] * accSensorData[1] + accSensorData[2] * accSensorData[2]) - 9.81
            if (accTotal > 0.5) {
                binding.tvSensorDataAccFiltered.text = displayDataTriple("acc", accSensorData)
            }
            binding.tvSensorDataAcc.text = displayDataTriple("acc", accSensorData)

            var accData: MutableList<Float> = mutableListOf(accSensorData[0], accSensorData[1], accSensorData[2])
            accDataList.add(accData)
            viewModel.getAccData(accDataList)
        }
        if (event?.sensor?.type == Sensor.TYPE_GYROSCOPE) {
            val gyroSensorData = event.values
            val gyroTotal =
                sqrt(gyroSensorData[0] * gyroSensorData[0] + gyroSensorData[1] * gyroSensorData[1] + gyroSensorData[2] * gyroSensorData[2])
            if (gyroTotal > 0.1) {
                binding.tvSensorDataGyroFiltered.text = displayDataTriple("gyro", gyroSensorData)
            }

                binding.tvSensorDataGyro.text = displayDataTriple("gyro", gyroSensorData)

            }
            if (event?.sensor?.type == Sensor.TYPE_MAGNETIC_FIELD) {
                sensorMagData = event.values
                binding.tvSensorDataMag.text = displayDataTriple("mag", sensorMagData)
            }

            SensorManager.getRotationMatrix(rotationMatrix, null, accSensorData, sensorMagData)
            SensorManager.getOrientation(rotationMatrix, deviceOrientation)
            binding.tvOrientation.text =
                "Device Orientation: ${(deviceOrientation[0] * 180 / 3.14159).toInt()}," +
                        " ${(deviceOrientation[1] * 180 / 3.14159).toInt()}, ${(deviceOrientation[2] * 180 / 3.14159).toInt()}"

            // binding.tvBox.apply { translationX = - (deviceOrientation[0]*180/3.14159f) * 20f }

    }*/

    /*override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        return
    }*/

    @SuppressLint("SetTextI18n")
    private fun displayDataTriple(name: String, data: FloatArray): String {
        return "$name x ${String.format("%.3f", data[0])}, $name y " +
                "${String.format("%.3f", data[1])}, $name z ${
                    String.format(
                        "%.3f",
                        data[2]
                    )
                }"
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("sensorService", "Stop")

        Intent(requireContext(), SensorService::class.java).apply {
            action = SensorService.ACTION_STOP
            activity?.startService(this)
        }
    }

}
package com.example.stroll.presentation.fragment

import android.annotation.SuppressLint
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.viewModels
import com.example.stroll.R
import com.example.stroll.databinding.FragmentMainBinding
import com.example.stroll.presentation.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlin.concurrent.fixedRateTimer
import kotlin.math.sqrt

@AndroidEntryPoint
class MainFragment() : Fragment(), SensorEventListener {

    private val viewModel: MainViewModel by viewModels()

    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!

    private lateinit var sensorManager: SensorManager
    private var sensorAccChange = floatArrayOf(0f, 0f, 0f)
    private var accSensorData = floatArrayOf(0f, 0f, 0f)
    private var sensorGyroChange = listOf(0, 0, 0)
    private var sensorMagData = floatArrayOf(0f, 0f, 0f)
    private var rotationMatrix = floatArrayOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f)
    private var deviceOrientation = floatArrayOf(0f, 0f, 0f)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentMainBinding.inflate(inflater, container, false)
        viewModel.addDataToRoom()
        viewModel.allData.observe(viewLifecycleOwner) { data ->
            binding.textView.text = data[0].id.toString()
        }
        setUpSensors()

        fixedRateTimer("timer", false, 0, 500){
            Log.d("changesAcc", listOf(sensorAccChange[0], sensorAccChange[1], sensorAccChange[2]).toString())

            // resetting sensorAccChange
            sensorAccChange = floatArrayOf(0f, 0f, 0f)


        }
        return binding.root
    }


    private fun setUpSensors() {
        val sensorManager =
            activity?.getSystemService(AppCompatActivity.SENSOR_SERVICE) as SensorManager
        val sensorAcc = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.also {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)?.also {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        val sensorMag = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)?.also {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)

        }
    }

    @SuppressLint("SetTextI18n")
    override fun onSensorChanged(event: SensorEvent?) {

        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {

            accSensorData = event.values
            val accTotal =
                sqrt(accSensorData[0] * accSensorData[0] + accSensorData[1] * accSensorData[1] + accSensorData[2] * accSensorData[2]) - 9.81
            if (accTotal > 0.5) {
                Log.d("changesAcc", listOf(accSensorData[0], accSensorData[1],accSensorData[2]).toString())
                for (i in sensorAccChange.indices){
                    sensorAccChange[i] += accSensorData[i]
                }

            }
            binding.tvSensorDataAcc.text =
                "acc x ${String.format("%.3f", accSensorData[0])}, acc y " +
                        "${String.format("%.3f", accSensorData[1])}, acc z ${
                            String.format(
                                "%.3f",
                                accSensorData[2]
                            )
                        }"
        }
        if (event?.sensor?.type == Sensor.TYPE_GYROSCOPE) {
            val gyroSensorData = event.values
            binding.tvSensorDataGyro.text =
                "gyro x ${String.format("%.3f", gyroSensorData[0])}, gyro y " +
                        "${String.format("%.3f", gyroSensorData[1])}, gyro z ${
                            String.format(
                                "%.3f",
                                gyroSensorData[2]
                            )
                        }"
        }
        if (event?.sensor?.type == Sensor.TYPE_MAGNETIC_FIELD) {
            sensorMagData = event.values
            binding.tvSensorDataMag.text =
                "mag x ${String.format("%.3f", sensorMagData[0])}, mag y " +
                        "${String.format("%.3f",sensorMagData[1])}, mag z ${
                            String.format(
                                "%.3f",
                                sensorMagData[2]
                            )
                        }"
        }

        SensorManager.getRotationMatrix(rotationMatrix, null, accSensorData, sensorMagData)
        SensorManager.getOrientation(rotationMatrix, deviceOrientation)
        binding.tvOrientation.text = "Device Orientation: ${(deviceOrientation[0]*180/3.14159).toInt()}," +
                " ${(deviceOrientation[1]*180/3.14159).toInt()}, ${(deviceOrientation[2]*180/3.14159).toInt()}"

        // binding.tvBox.apply { translationX = - (deviceOrientation[0]*180/3.14159f) * 20f }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        return
    }


}

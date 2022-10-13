package com.example.stroll.backgroundlocationtracking

import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LifecycleService
import java.util.*
import kotlin.math.sqrt

class SensorService : LifecycleService(), SensorEventListener {

    private lateinit var mainHandler: Handler
    private lateinit var sensorManager: SensorManager

    private val allData = mutableMapOf(
        "accData_raw" to floatArrayOf(0f, 0f, 0f),
        "gyroData_raw" to floatArrayOf(0f, 0f, 0f),
        "magData_raw" to floatArrayOf(0f, 0f, 0f),
        "accData_filtered" to floatArrayOf(0f, 0f, 0f),
        "gyroData_filtered" to floatArrayOf(0f, 0f, 0f),
        "deviceOrientation" to floatArrayOf(0f, 0f, 0f)
    )

    private var accData_change = floatArrayOf(0f, 0f, 0f)
    private var accSensorData = floatArrayOf(0f, 0f, 0f)
    private var sensorMagData = floatArrayOf(0f, 0f, 0f)
    private var rotationMatrix = floatArrayOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f)
    private var accDataList = mutableListOf<MutableList<Float>>()

    override fun onCreate() {
        super.onCreate()
        sensorManager = this.getSystemService(AppCompatActivity.SENSOR_SERVICE) as SensorManager

        mainHandler = Handler(Looper.getMainLooper())
        mainHandler.post(object: Runnable {
            override fun run(){
                Log.d("handler", "1 sec passed")
                Log.d("acc_change", Arrays.deepToString(accData_change.toTypedArray()))
                accData_change = floatArrayOf(0f, 0f, 0f)
                //fun get sensor change and calc Kalman
                mainHandler.postDelayed(this, 1000)
            }
        })
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            SensorService.ACTION_START -> start()
            SensorService.ACTION_STOP -> stop()
            SensorService.ACTION_GET_DATA -> getSensorData()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun start() {

        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.also {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)?.also {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)?.also {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)

        }
        Log.d("sensorService", "start: ")
    }

    private fun stop() {
        stopSelf()
    }


    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {

            accSensorData = event.values


            val accTotal =
                sqrt(accSensorData[0] * accSensorData[0] + accSensorData[1] * accSensorData[1] + accSensorData[2] * accSensorData[2]) - 9.81
            if (accTotal > 0.5) {
                allData["accData_filtered"] = accSensorData
                for (i in accSensorData.indices){
                    accData_change[i] += accSensorData[i]
                }

                Log.d("sensorService", "filtered AccData ${Arrays.deepToString(allData["accData_filtered"]?.toTypedArray())}")
                val accData: MutableList<Float> =
                    mutableListOf(accSensorData[0], accSensorData[1], accSensorData[2])
                accDataList.add(accData)
            }
            allData["accData_raw"] = accSensorData
            //Log.d("sensorService", "raw AccData ${allData["accData_raw"]?.get(0)}")


        }
        if (event?.sensor?.type == Sensor.TYPE_GYROSCOPE) {
            val gyroSensorData = event.values
            val gyroTotal =
                sqrt(gyroSensorData[0] * gyroSensorData[0] + gyroSensorData[1] * gyroSensorData[1] + gyroSensorData[2] * gyroSensorData[2])
            if (gyroTotal > 0.1) {
                allData["gyroData_filtered"] = gyroSensorData
            }
            allData["gyroData_raw"] = gyroSensorData
        }
        if (event?.sensor?.type == Sensor.TYPE_MAGNETIC_FIELD) {
            allData["magData_raw"] = event.values

        }

        SensorManager.getRotationMatrix(rotationMatrix, null, accSensorData, sensorMagData)
        SensorManager.getOrientation(rotationMatrix, allData["deviceOrientation"])

        // binding.tvBox.apply { translationX = - (deviceOrientation[0]*180/3.14159f) * 20f }


    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        return
    }

    fun getSensorData(): MutableMap<String, FloatArray> {
        return allData
    }


    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_GET_DATA = "ACTION_GET_DATA"
    }
}
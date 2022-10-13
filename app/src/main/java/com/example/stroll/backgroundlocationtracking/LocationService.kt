package com.example.stroll.backgroundlocationtracking
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.NotificationManager.IMPORTANCE_LOW
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.example.stroll.MainActivity
import com.example.stroll.R
import com.example.stroll.other.Constants.ACTION_PAUSE
import com.example.stroll.other.Constants.ACTION_SHOW_MAP_FRAGMENT
import com.example.stroll.other.Constants.ACTION_START
import com.example.stroll.other.Constants.ACTION_STOP
import com.example.stroll.other.Constants.FASTEST_LOCATION_INTERVAL
import com.example.stroll.other.Constants.LOCATION_UPDATE_INTERVAL
import com.example.stroll.other.Constants.TIMER_UPDATE_INTERVAL
import com.example.stroll.other.Utility
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY
import com.google.android.gms.location.LocationResult
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.*
import javax.inject.Inject
import kotlin.math.sqrt

typealias Polyline = MutableList<LatLng>
typealias Polylines = MutableList<Polyline>

@AndroidEntryPoint
class LocationService : LifecycleService(), SensorEventListener {

    var isFirstRun = true
    private val timeHikedInSeconds = MutableLiveData<Long>()

    @Inject
    lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    @Inject
    lateinit var baseNotificationBuilder: NotificationCompat.Builder
    lateinit var currentNotificationBuilder: NotificationCompat.Builder


    private lateinit var mainHandler: Handler
    private lateinit var sensorManager: SensorManager
    //Sensor variables
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


    companion object {
        val timeHikedInMillis = MutableLiveData<Long>()
        val isTracking = MutableLiveData<Boolean>()
        val pathPoints = MutableLiveData<Polylines>()
    }

    private fun postInitialValues() {
        isTracking.postValue(false)
        pathPoints.postValue(mutableListOf())
        timeHikedInSeconds.postValue(0L)
        timeHikedInMillis.postValue(0L)
    }

    override fun onCreate() {
        super.onCreate()
        currentNotificationBuilder = baseNotificationBuilder
        postInitialValues()
        fusedLocationProviderClient = FusedLocationProviderClient(this)

        isTracking.observe(this, Observer {
            updateLocationTracking(it)
        })


        sensorManager = this.getSystemService(AppCompatActivity.SENSOR_SERVICE) as SensorManager

        //loop every second to get the last change and apply kalman filter
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
        intent?.let {
            when (it.action) {
                ACTION_START -> {
                    startSensorTracking()
                    if(isFirstRun) {
                        startForegroundService()
                        isFirstRun = false
                    } else {
                        startTimer()
                        Log.d("LOCATIONSERVICE", "startForegroundService: RESUMING SERVICE")
                    }
                }
                ACTION_PAUSE -> {
                    pauseService()
                    Log.d("LOCATIONSERVICE", "startForegroundService: PAUSED SERVICE")
                }
                ACTION_STOP -> {
                    Log.d("LOCATIONSERVICE", "startForegroundService: STOPPED SRVICE")
                }
                else -> {}
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private var isTimerEnabled = false
    private var hikeTime = 0L
    private var timeHiked = 0L
    private var timeStarted = 0L
    private var lastSecondTimeStamp = 0L

    private fun startTimer() {
        addEmptyPolyline()
        isTracking.postValue(true)
        timeStarted = System.currentTimeMillis()
        isTimerEnabled = true
        CoroutineScope(Dispatchers.Main).launch {
            while (isTracking.value!!) {
                hikeTime = System.currentTimeMillis() - timeStarted
                timeHikedInMillis.postValue(timeHiked + hikeTime)
                if (timeHikedInMillis.value!! >= lastSecondTimeStamp + 1000L) {
                    timeHikedInSeconds.postValue(timeHikedInSeconds.value!! + 1)
                    lastSecondTimeStamp += 1000L
                }
                delay(TIMER_UPDATE_INTERVAL)
            }
            timeHiked += hikeTime
        }
    }

    private fun pauseService() {
        isTimerEnabled = false
        isTracking.postValue(false)
    }

    @SuppressLint("MissingPermission")
    private fun updateLocationTracking(isTracking: Boolean) {
        if(isTracking) {
            val request = LocationRequest().apply {
                interval = LOCATION_UPDATE_INTERVAL
                fastestInterval = FASTEST_LOCATION_INTERVAL
                priority = PRIORITY_HIGH_ACCURACY
            }
            fusedLocationProviderClient.requestLocationUpdates(
                request,
                locationCallback,
                Looper.getMainLooper()
            )
        } else {
            fusedLocationProviderClient.removeLocationUpdates(locationCallback)
        }
    }

    val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            super.onLocationResult(result)
            if(isTracking.value!!) {
                result?.locations?.let { locations ->
                    for(location in locations) {
                        addPathPoint(location)
                        Log.d("NEW LOCATION:", "${location.latitude}, ${location.longitude}")
                    }
                }
            }
        }
    }

    private fun addPathPoint(location: Location?) {
        location?.let {
            val pos = LatLng(location.latitude, location.longitude)
            pathPoints.value?.apply {
                last().add(pos)
                pathPoints.postValue(this)
            }
        }
    }

    private fun addEmptyPolyline() = pathPoints.value?.apply {
        add(mutableListOf())
        pathPoints.postValue(this)
    } ?: pathPoints.postValue(mutableListOf(mutableListOf()))

    private fun startForegroundService() {
        startTimer()
        isTracking.postValue(true)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager

        createNotificationChannel(notificationManager)

        startForeground(1, baseNotificationBuilder.build())
        Log.d("LOCATIONSERVICE", "startForegroundService: INSIDE FOREGROUND")
    }


    private fun createNotificationChannel(notificationManager: NotificationManager) {
        val channel = NotificationChannel(
            "TRACKING CHANNEL",
            "TRACKING",
            IMPORTANCE_LOW
        )
        notificationManager.createNotificationChannel(channel)
    }


    private fun startSensorTracking() {
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

    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        return
    }
}
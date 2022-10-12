package com.example.stroll.backgroundlocationtracking
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.NotificationManager.IMPORTANCE_LOW
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresApi
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
import javax.inject.Inject

typealias Polyline = MutableList<LatLng>
typealias Polylines = MutableList<Polyline>

@AndroidEntryPoint
class LocationService : LifecycleService() {

    var isFirstRun = true
    private val timeHikedInSeconds = MutableLiveData<Long>()

    @Inject
    lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    lateinit var baseNotificationBuilder: NotificationCompat.Builder

    lateinit var currentNotificationBuilder: NotificationCompat.Builder

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
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            when (it.action) {
                ACTION_START -> {
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
}
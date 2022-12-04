package com.example.stroll.backgroundlocationtracking
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.NotificationManager.IMPORTANCE_LOW
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.example.stroll.R
import com.example.stroll.other.Constants.ACTION_PAUSE
import com.example.stroll.other.Constants.ACTION_START
import com.example.stroll.other.Constants.ACTION_STOP
import com.example.stroll.other.Constants.FASTEST_LOCATION_INTERVAL
import com.example.stroll.other.Constants.LOCATION_UPDATE_INTERVAL
import com.example.stroll.other.Constants.TIMER_UPDATE_INTERVAL
import com.example.stroll.other.Utility
import com.google.android.gms.location.*
import com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

typealias Polyline = MutableList<LatLng>
typealias Polylines = MutableList<Polyline>

@AndroidEntryPoint
class LocationService : LifecycleService() {

    private var isFirstRun = true
    private var serviceStopped = false
    private val timeHikedInSeconds = MutableLiveData<Long>()

    @Inject
    lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    @Inject
    lateinit var baseNotificationBuilder: NotificationCompat.Builder

    private lateinit var currentNotificationBuilder: NotificationCompat.Builder

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

        isTracking.observe(this) {
            updateLocationTracking(it)
            updateNotificationTrackingState(it)
        }
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
                    stopService()
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

    // Notice isTracking value will be set to true
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

    private fun stopService() {
        serviceStopped = true
        isFirstRun = true
        pauseService()
        postInitialValues()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
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


    // When location changes add new location to pathpoints
    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            super.onLocationResult(result)
            if(isTracking.value!!) {
                result.locations.let { locations ->
                    for(location in locations) {
                        addPathPoint(location)
                    }
                }
            }
        }
    }


    // Function to add value to pathpoints. Used in locationCallBack
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

    // Starting the service
    private fun startForegroundService() {
        startTimer()
        isTracking.postValue(true)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager

        createNotificationChannel(notificationManager)

        startForeground(1, baseNotificationBuilder.build())

        timeHikedInSeconds.observe(this) {
            if (!serviceStopped) {
                val notification = currentNotificationBuilder
                    .setContentText(Utility.getFormattedStopWatchTime(it * 1000L))
                notificationManager.notify(1, notification.build())
            }
        }
    }


    /*
    Section for creating notification which you can access while not having the app open.
     */
    private fun createNotificationChannel(notificationManager: NotificationManager) {
        val channel = NotificationChannel(
            "TRACKING CHANNEL",
            "TRACKING",
            IMPORTANCE_LOW
        )
        notificationManager.createNotificationChannel(channel)
    }

    private fun updateNotificationTrackingState(isTracking: Boolean) {
        val notificationActionText = if(isTracking) "Pause" else "Resume"
        val pendingIntent = if (isTracking) {
            val pauseIntent = Intent(this, LocationService::class.java).apply {
                action = ACTION_PAUSE
            }
            PendingIntent.getService(this, 1, pauseIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        }
        else {
            val resumeIntent = Intent(this, LocationService::class.java).apply {
                action = ACTION_START
            }
            PendingIntent.getService(this, 2, resumeIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        }
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        currentNotificationBuilder.javaClass.getDeclaredField("mActions").apply {
            isAccessible = true
            set(currentNotificationBuilder, ArrayList<NotificationCompat.Action>())
        }
        if (!serviceStopped) {
            currentNotificationBuilder = baseNotificationBuilder
                .addAction(R.drawable.ic_baseline_pause_24, notificationActionText, pendingIntent)
            notificationManager.notify(1, currentNotificationBuilder.build())
        }
    }
}
package com.example.stroll.other

import android.Manifest

object Constants {
    const val ACTION_START = "ACTION_START"
    const val ACTION_PAUSE = "ACTION_PAUSE"
    const val ACTION_STOP = "ACTION_STOP"
    const val TIMER_UPDATE_INTERVAL = 50L
    const val ACTION_SHOW_MAP_FRAGMENT = "ACTION_SHOW_MAP_FRAGMENT"
    const val LOCATION_UPDATE_INTERVAL = 5000L
    const val FASTEST_LOCATION_INTERVAL = 2000L

    //CameraFragmentConstants
    const val TAG = "cameraX"
    const val FILE_NAME_FORMAT = "yy-MM-dd-HH-mm-ss-SSS"
    const val CAMERA_REQUEST_CODE_PERMISSIONS = 123
    val CAMERA_REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)

    //SHARED PREFERENCES
    const val SHARED_PREFERENCES_NAME = "sharedPref"
    const val KEY_FIRST_TIME_TOGGLE = "KEY_FIRST_TIME_TOGGLE"
    const val KEY_NAME = "KEY_NAME"
    const val KEY_WEIGHT = "KEY_WEIGHT"
}
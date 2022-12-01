package com.example.stroll.other

import android.content.Context
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.example.stroll.presentation.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.withContext
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.MapEventsOverlay

class MapEventsReceiverImpl: MapEventsReceiver {
    override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
        Log.d("singleTapConfirmedHelper", "${p?.latitude} - ${p?.longitude}")
        return true
    }

    override fun longPressHelper(p: GeoPoint?): Boolean {
        Log.d("longPressHelper", "${p?.latitude} - ${p?.longitude}")
        return false
    }
}
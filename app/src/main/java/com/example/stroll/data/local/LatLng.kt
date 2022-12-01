package com.example.stroll.data.local

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class LatLong(
    var lat: Double = 0.0,
    var long: Double = 0.0
): Parcelable

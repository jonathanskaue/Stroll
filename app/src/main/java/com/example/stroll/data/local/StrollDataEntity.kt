package com.example.stroll.data.local
import android.graphics.Bitmap
import androidx.room.Entity
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import com.google.android.gms.maps.model.LatLng

@Entity(tableName = "hike_table")
data class StrollDataEntity(
    //var accData: List<List<Float>>,
    var mapSnapShot: String? = null,
    var timeStamp: Long = 0L,
    var averageSpeedInKMH: Float = 0f,
    var distanceInMeters: Int = 0,
    var timeInMillis: Long = 0L,
    var folderPath: String = "",
    var startLatitude: Double = 0.0,
    var startLongitude: Double = 0.0,
) {

    @PrimaryKey(autoGenerate = true)
    var id: Int? = null
}
package com.example.stroll.data.local
import android.graphics.Bitmap
import androidx.room.Entity
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey

@Entity(tableName = "hike_table")
data class StrollDataEntity(
    //var accData: List<List<Float>>,
    var mapSnapShot: Bitmap? = null,
    var timeStamp: Long = 0L,
    var averageSpeedInKMH: Float = 0f,
    var distanceInMeters: Int = 0,
    var timeInMillis: Long = 0L
) {
    @PrimaryKey(autoGenerate = true)
    var id: Int? = null
}
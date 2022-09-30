package com.example.stroll.data.local
import androidx.room.Entity
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey

@Entity
data class StrollDataEntity(
    var accData: List<List<Float>>,
    // var orientation: Int = 0
) {
    @PrimaryKey(autoGenerate = true)
    var id: Int? = null
}
package com.example.stroll.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.sql.Timestamp

@Entity(tableName = "hike_table")
data class Hike(
    @PrimaryKey
    val timestamp: Timestamp,
    val strollDataEntity: StrollDataEntity
)

package com.example.stroll.data.local
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class StrollDataEntity(
    @PrimaryKey val id: Int
)
package com.example.stroll.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [StrollDataEntity::class, MarkerEntity::class],
    version = 1
)

@TypeConverters(Converters::class)
abstract class StrollDataBase: RoomDatabase() {
    abstract val dao: StrollDao
}
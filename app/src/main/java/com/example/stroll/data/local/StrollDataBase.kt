package com.example.stroll.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [StrollDataEntity::class],
    version = 1
)

abstract class StrollDataBase: RoomDatabase() {
    abstract val dao: StrollDao
}
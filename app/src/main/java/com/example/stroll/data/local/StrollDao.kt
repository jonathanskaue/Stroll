package com.example.stroll.data.local

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface StrollDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertData(data: StrollDataEntity)

    @Delete
    suspend fun deleteData(data: StrollDataEntity)

    @Query("SELECT * FROM hike_table ORDER BY distanceInMeters DESC")
    fun selectAllHikesSortedByDistance(): LiveData<List<StrollDataEntity>>

    @Query("SELECT * FROM hike_table ORDER BY timeInMillis DESC")
    fun selectAllHikesSortedByTimeInMillis(): LiveData<List<StrollDataEntity>>

    @Query("SELECT * FROM hike_table ORDER BY timeStamp DESC")
    fun selectAllHikesSortedByDate(): LiveData<List<StrollDataEntity>>

    @Query("SELECT * FROM hike_table ORDER BY averageSpeedInKMH DESC")
    fun selectAllHikesSortedByAvgSpeed(): LiveData<List<StrollDataEntity>>

    @Query("SELECT * FROM hike_table")
    fun getAllData(): LiveData<List<StrollDataEntity>>

    @Query("SELECT SUM(distanceInMeters) FROM hike_table")
    fun getTotalDistanceHiked(): LiveData<Int>

    @Query("SELECT SUM(timeInMillis) FROM hike_table")
    fun getTotalTimeInMillis(): LiveData<Long>

    @Query("SELECT AVG(averageSpeedInKMH) FROM hike_table")
    fun getTotalAverageSpeed(): LiveData<Float>

    @Query("SELECT * FROM hike_table WHERE id= :id")
    fun getHikeById(id: Int): LiveData<StrollDataEntity>
}
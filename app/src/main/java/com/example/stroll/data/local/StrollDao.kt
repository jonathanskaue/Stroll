package com.example.stroll.data.local

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/*
All Queries for database
 */
@Dao
interface StrollDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertData(data: StrollDataEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMarkerData(markerData: MarkerEntity)

    @Query("DELETE FROM marker_table WHERE id= :id")
    suspend fun deleteMarkerById(id: Int)

    @Query("SELECT * FROM marker_table")
    fun getAllMarkers(): LiveData<List<MarkerEntity>>

    @Query("SELECT * FROM marker_table WHERE category= :category")
    fun getMarkersByCategory(category: String): LiveData<List<MarkerEntity>>

    @Query("SELECT id FROM hike_table")
    fun getAllHikeId(): LiveData<List<Int>>

    @Delete
    suspend fun deleteData(data: StrollDataEntity)

    @Query("DELETE FROM hike_table WHERE id= :id")
    fun deleteHikeById(id: Int)

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

    /*@Query("SELECT id FROM hike_table ORDER BY id DESC LIMIT 1")*/
    @Query("SELECT MAX(id) FROM hike_table")
    fun getHighestHikeId(): LiveData<Int>

    @Query("SELECT SUM(distanceInMeters) FROM hike_table")
    fun getTotalDistanceHiked(): LiveData<Int>

    @Query("SELECT SUM(timeInMillis) FROM hike_table")
    fun getTotalTimeInMillis(): LiveData<Long>

    @Query("SELECT AVG(averageSpeedInKMH) FROM hike_table")
    fun getTotalAverageSpeed(): LiveData<Float>

    @Query("SELECT * FROM hike_table WHERE id= :id")
    fun getHikeById(id: Int): LiveData<StrollDataEntity>
}
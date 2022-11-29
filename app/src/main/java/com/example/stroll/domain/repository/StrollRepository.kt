package com.example.stroll.domain.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.stroll.data.local.StrollDataEntity

interface StrollRepository {

    suspend fun insertData(data: StrollDataEntity)

    suspend fun deleteData(data: StrollDataEntity)

    suspend fun deleteHikeById(id: Int)

    fun getHikeById(id: Int): LiveData<StrollDataEntity>

    val selectAllHikesSortedByDistance: LiveData<List<StrollDataEntity>>
    val selectAllHikesSortedByDate: LiveData<List<StrollDataEntity>>
    val selectAllHikesSortedByTimeInMillis: LiveData<List<StrollDataEntity>>
    val selectAllHikesSortedByAvgSpeed: LiveData<List<StrollDataEntity>>
    val highestHikeId: LiveData<Int>

    val getTotalDistanceHiked: LiveData<Int>
    val getTotalTimeInMillis: LiveData<Long>
    val getTotalAverageSpeed: LiveData<Float>

    val readAllData: LiveData<List<StrollDataEntity>>

}
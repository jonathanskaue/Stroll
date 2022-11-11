package com.example.stroll.domain.repository

import androidx.lifecycle.LiveData
import com.example.stroll.data.local.StrollDataEntity

interface StrollRepository {

    suspend fun insertData(data: StrollDataEntity)

    suspend fun deleteData(data: StrollDataEntity)

    val selectAllHikesSortedByDistance: LiveData<List<StrollDataEntity>>
    val selectAllHikesSortedByDate: LiveData<List<StrollDataEntity>>
    val selectAllHikesSortedByTimeInMillis: LiveData<List<StrollDataEntity>>
    val selectAllHikesSortedByAvgSpeed: LiveData<List<StrollDataEntity>>
    val highestHikeId: LiveData<Int>

    val readAllData: LiveData<List<StrollDataEntity>>

}
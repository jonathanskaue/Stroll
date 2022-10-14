package com.example.stroll.domain.repository

import androidx.lifecycle.LiveData
import com.example.stroll.data.local.StrollDataEntity

interface StrollRepository {

    suspend fun insertData(data: StrollDataEntity)

    suspend fun deleteData(data: StrollDataEntity)

    suspend fun selectAllHikesSortedByDistance()

    val readAllData: LiveData<List<StrollDataEntity>>

}
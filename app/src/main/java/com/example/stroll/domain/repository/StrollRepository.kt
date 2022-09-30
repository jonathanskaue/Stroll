package com.example.stroll.domain.repository

import androidx.lifecycle.LiveData
import com.example.stroll.data.local.StrollDataEntity

interface StrollRepository {

    suspend fun insertData(data: StrollDataEntity)

    val readAllData: LiveData<List<StrollDataEntity>>

}
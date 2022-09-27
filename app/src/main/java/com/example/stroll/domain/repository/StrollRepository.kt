package com.example.stroll.domain.repository

import androidx.lifecycle.LiveData
import com.example.stroll.data.local.StrollDataEntity
import com.example.stroll.domain.model.StrollData
import kotlinx.coroutines.flow.Flow

interface StrollRepository {

    suspend fun insertData(data: StrollDataEntity)

    val readAllData: LiveData<List<StrollDataEntity>>

}
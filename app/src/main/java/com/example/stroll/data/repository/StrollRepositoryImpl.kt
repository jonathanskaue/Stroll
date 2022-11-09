package com.example.stroll.data.repository

import androidx.lifecycle.LiveData
import com.example.stroll.data.local.StrollDao
import com.example.stroll.data.local.StrollDataEntity
import com.example.stroll.domain.repository.StrollRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StrollRepositoryImpl @Inject constructor(
    private val dao: StrollDao,
    override val readAllData: LiveData<List<StrollDataEntity>> = dao.getAllData(),
    override val selectAllHikesSortedByDistance: LiveData<List<StrollDataEntity>> = dao.selectAllHikesSortedByDistance(),
    override val selectAllHikesSortedByDate: LiveData<List<StrollDataEntity>> = dao.selectAllHikesSortedByDate(),
    override val selectAllHikesSortedByTimeInMillis: LiveData<List<StrollDataEntity>> = dao.selectAllHikesSortedByTimeInMillis(),
    override val selectAllHikesSortedByAvgSpeed: LiveData<List<StrollDataEntity>> = dao.selectAllHikesSortedByAvgSpeed()
): StrollRepository {

    override suspend fun insertData(data: StrollDataEntity) {
        dao.insertData(data)
    }

    override suspend fun deleteData(data: StrollDataEntity) {
        dao.deleteData(data)
    }
}
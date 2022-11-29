package com.example.stroll.data.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
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
    override val selectAllHikesSortedByAvgSpeed: LiveData<List<StrollDataEntity>> = dao.selectAllHikesSortedByAvgSpeed(),
    override val highestHikeId: LiveData<Int> = dao.getHighestHikeId(),
    override val getTotalDistanceHiked: LiveData<Int> = dao.getTotalDistanceHiked(),
    override val getTotalTimeInMillis: LiveData<Long> = dao.getTotalTimeInMillis(),
    override val getTotalAverageSpeed: LiveData<Float> = dao.getTotalAverageSpeed(),
): StrollRepository {

    override suspend fun insertData(data: StrollDataEntity) {
        dao.insertData(data)
    }

    override suspend fun deleteData(data: StrollDataEntity) {
        dao.deleteData(data)
    }

    override suspend fun deleteHikeById(id: Int) {
        dao.deleteHikeById(id)
    }

    override fun getHikeById(id: Int): LiveData<StrollDataEntity> = dao.getHikeById(id)
}
package com.example.stroll.presentation.viewmodel

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.*
import com.example.stroll.backgroundlocationtracking.Polyline
import com.example.stroll.data.local.StrollDataEntity
import com.example.stroll.domain.repository.StrollRepository
import com.example.stroll.other.SortType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val strollRepo: StrollRepository
): ViewModel() {

    private val hikesSortedByDate = strollRepo.selectAllHikesSortedByDate
    private val hikesSortedByDistance = strollRepo.selectAllHikesSortedByDistance
    private val hikesSortedByTotalTimeInMillis = strollRepo.selectAllHikesSortedByTimeInMillis
    private val hikesSortedByAvgSpeed = strollRepo.selectAllHikesSortedByAvgSpeed


    val hikes = MediatorLiveData<List<StrollDataEntity>>()
    val highestHikeId: LiveData<Int>

    var sortType = SortType.DATE

    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()


    private val _isStarted = MutableStateFlow(false)
    val isStarted = _isStarted

    init {
        viewModelScope.launch {
            _isLoading.value = false
        }
        hikes.addSource(hikesSortedByDate) { result ->
            if(sortType == SortType.DATE) {
                result?.let { hikes.value = it }
            }
        }
        hikes.addSource(hikesSortedByAvgSpeed) { result ->
            if(sortType == SortType.AVG_SPEED) {
                result?.let { hikes.value = it }
            }
        }
        hikes.addSource(hikesSortedByDistance) { result ->
            if(sortType == SortType.DISTANCE) {
                result?.let { hikes.value = it }
            }
        }
        hikes.addSource(hikesSortedByTotalTimeInMillis) { result ->
            if(sortType == SortType.HIKE_TIME) {
                result?.let { hikes.value = it }
            }
        }

        highestHikeId = strollRepo.highestHikeId
    }

    fun sortHikes(sortType: SortType) = when(sortType) {
        SortType.DATE -> hikesSortedByDate.value?.let { hikes.value = it }
        SortType.HIKE_TIME -> hikesSortedByTotalTimeInMillis.value?.let { hikes.value = it }
        SortType.AVG_SPEED -> hikesSortedByAvgSpeed.value?.let { hikes.value = it }
        SortType.DISTANCE -> hikesSortedByDistance.value?.let { hikes.value = it }
    }.also {
        this.sortType = sortType
    }


    fun checkStarted() {
        _isStarted.value = true
    }


    var allData: LiveData<List<StrollDataEntity>> = strollRepo.readAllData

    var _initialize = MutableLiveData<Boolean>()
    val initialize: LiveData<Boolean> = _initialize

    fun initialize(initialize: Boolean) {
        _initialize.value = initialize
    }

    var _accData = MutableLiveData<List<List<Float>>>(listOf(listOf(0f, 0f, 0f)))
    val accData: LiveData<List<List<Float>>> = _accData

    var _hikePhotos = MutableLiveData<List<Bitmap>>()
    val hikePhotos: LiveData<List<Bitmap>> = _hikePhotos

    fun getAccData(data: List<List<Float>>) {
        _accData.value = data
    }

    fun addPhotoToHike(data: Bitmap){
        _hikePhotos.value?.plus(data)
    }

    fun addDataToRoom(hike: StrollDataEntity) {
        viewModelScope.launch {
            strollRepo.insertData(hike)
        }
    }

}


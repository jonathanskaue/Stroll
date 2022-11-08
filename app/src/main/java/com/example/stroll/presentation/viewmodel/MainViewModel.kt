package com.example.stroll.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stroll.backgroundlocationtracking.Polyline
import com.example.stroll.data.local.StrollDataEntity
import com.example.stroll.domain.repository.StrollRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val strollRepo: StrollRepository
): ViewModel() {

    private val _pathPoints = MutableLiveData<MutableList<Polyline?>>()
    val pathPoints: LiveData<MutableList<Polyline?>> = _pathPoints

    fun insertPathPoints(pathPoints: MutableList<Polyline?>) {
        _pathPoints.value = pathPoints
    }

    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    init {
        viewModelScope.launch {
            _isLoading.value = false
        }
    }

    private val _isStarted = MutableStateFlow(false)
    val isStarted = _isStarted

    fun checkStarted() {
        _isStarted.value = true
    }


    var allData: LiveData<List<StrollDataEntity>> = strollRepo.readAllData
    val hikesSortedByDistance = viewModelScope.launch {
        strollRepo.selectAllHikesSortedByDistance()
    }

    var _accData = MutableLiveData<List<List<Float>>>(listOf(listOf(0f, 0f, 0f)))
    val accData: LiveData<List<List<Float>>> = _accData

    fun getAccData(data: List<List<Float>>) {
        _accData.value = data
    }

    fun addDataToRoom(hike: StrollDataEntity) {
        viewModelScope.launch {
            strollRepo.insertData(hike)
        }
    }

}
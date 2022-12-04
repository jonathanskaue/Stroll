package com.example.stroll.presentation.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.*
import androidx.navigation.NavController
import com.example.stroll.R
import com.example.stroll.data.local.MarkerEntity
import com.example.stroll.data.local.StrollDataEntity
import com.example.stroll.domain.repository.StrollRepository
import com.example.stroll.other.SortType
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
/*
Main viewmodel used as a shared viewmodel for keeping values alive when navigating.
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val strollRepo: StrollRepository
): ViewModel() {

    private val hikesSortedByDate = strollRepo.selectAllHikesSortedByDate
    private val hikesSortedByDistance = strollRepo.selectAllHikesSortedByDistance
    private val hikesSortedByTotalTimeInMillis = strollRepo.selectAllHikesSortedByTimeInMillis
    private val hikesSortedByAvgSpeed = strollRepo.selectAllHikesSortedByAvgSpeed
    val getAllMarkers = strollRepo.getAllMarkers


    val hikes = MediatorLiveData<List<StrollDataEntity>>()
    val highestHikeId: LiveData<Int>

    var sortType = SortType.DATE

    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    private val _isStarted = MutableStateFlow(false)
    val isStarted = _isStarted

    fun checkStarted() {
        _isStarted.value = true
    }

    private val _isHeatMap = MutableStateFlow(false)
    val isHeatMap = _isHeatMap

    fun isHeatMap() {
        _isHeatMap.value = true
    }
    fun isNotHeatMap() {
        _isHeatMap.value = false
    }

    private val _isMarker = MutableStateFlow(false)
    val isMarker = _isMarker

    fun isMarker() {
        _isMarker.value = true
    }
    fun isNotMarker() {
        _isMarker.value = false
    }

    private val _isMountain = MutableStateFlow(false)
    val isMountain = _isMountain

    fun isMountain() {
        _isMountain.value = true
    }
    fun isNotMountain() {
        _isMountain.value = false
    }

    private val _isFishing = MutableStateFlow(false)
    val isFishing = _isFishing

    fun isFishing() {
        _isFishing.value = true
    }
    fun isNotFishing() {
        _isFishing.value = false
    }

    private val _isAttraction = MutableStateFlow(false)
    val isAttraction = _isAttraction

    fun isAttraction() {
        _isAttraction.value = true
    }
    fun isNotAttraction() {
        _isAttraction.value = false
    }
    private val _isCamping = MutableStateFlow(false)
    val isCamping = _isCamping

    fun isCamping() {
        _isCamping.value = true
    }
    fun isNotCamping() {
        _isCamping.value = false
    }
    private val _isCanoe = MutableStateFlow(false)
    val isCanoe = _isCanoe

    fun isCanoe() {
        _isCanoe.value = true
    }
    fun isNotCanoe() {
        _isCanoe.value = false
    }

    private val _isMisc = MutableStateFlow(false)
    val isMisc = _isMisc

    fun isMisc() {
        _isMisc.value = true
    }
    fun isNotMisc() {
        _isMisc.value = false
    }

    private val _isStartingPos = MutableStateFlow(false)
    val isStartingPos = _isStartingPos

    fun isStartingPos() {
        _isStartingPos.value = true
    }
    fun isNotStartingPos() {
        _isStartingPos.value = false
    }

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

    var allData: LiveData<List<StrollDataEntity>> = strollRepo.readAllData

    var _initialize = MutableLiveData<Boolean>()
    val initialize: LiveData<Boolean> = _initialize

    fun initialize(initialize: Boolean) {
        _initialize.value = initialize
    }

    var _accData = MutableLiveData(listOf(listOf(0f, 0f, 0f)))
    val accData: LiveData<List<List<Float>>> = _accData

    fun getAccData(data: List<List<Float>>) {
        _accData.value = data
    }

    var _hikePhotos = MutableLiveData<List<Bitmap>>()
    val hikePhotos: LiveData<List<Bitmap>> = _hikePhotos

    fun addPhotoToHike(data: Bitmap){
        _hikePhotos.value?.plus(data)
    }

    private var _currentLatLng = MutableLiveData<LatLng>()
    val currentLatLng: LiveData<LatLng> = _currentLatLng

    fun getCurrentLatLng(latLng: LatLng) {
        _currentLatLng.value = latLng
    }

    fun addDataToRoom(hike: StrollDataEntity, controller: NavController) {
        viewModelScope.launch(Dispatchers.Main) {
            strollRepo.insertData(hike)
            controller.navigate(R.id.action_mapFragment_to_hikesFragment)
        }
    }

    fun addMarkerDataToRoom(markerData: MarkerEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            strollRepo.insertMarkerData(markerData)
        }
    }

    fun deleteMarkerById(id: Int) {
        viewModelScope.launch(Dispatchers.Main) {
            strollRepo.deleteMarkerById(id)
        }
    }

    fun getMarkersByCategory(category: String) {
        viewModelScope.launch {
            strollRepo.getMarkersByCategory(category)
        }
    }

}


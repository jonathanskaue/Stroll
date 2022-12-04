package com.example.stroll.presentation.viewmodel

import android.location.Geocoder
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stroll.data.local.StrollDataEntity
import com.example.stroll.domain.repository.StrollRepository
import com.example.stroll.other.SortType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("DEPRECATION")
@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val strollRepo: StrollRepository
): ViewModel() {
    /*
    ViewModel to access the database from the statisticFragment
     */
    private var _city: MutableLiveData<String> = MutableLiveData()

    private var _address: MutableLiveData<String> = MutableLiveData()
    val address: LiveData<String> = _address

    lateinit var hikeById: LiveData<StrollDataEntity>
    val totalDistanceHiked = strollRepo.getTotalDistanceHiked
    val totalTimeInMillisHiked = strollRepo.getTotalTimeInMillis
    val totalAverageSpeed = strollRepo.getTotalAverageSpeed
    // returns all the hikes sorted by a value
    val hikeSortedByDate = strollRepo.selectAllHikesSortedByDate
    val hikeSortedByTime = strollRepo.selectAllHikesSortedByTimeInMillis
    val hikeSortedByDistance = strollRepo.selectAllHikesSortedByDistance
    val hikeSortedBySpeed = strollRepo.selectAllHikesSortedByAvgSpeed
    var sortType = SortType.DATE
    var allHikeId: LiveData<List<Int>> = strollRepo.allHikeId

    fun getHikeById(id: Int) {
        viewModelScope.launch {
            hikeById = strollRepo.getHikeById(id)
        }
    }

    fun deleteHikeById(id: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            strollRepo.deleteHikeById(id)
        }
    }

    fun getDetailsByLatLng(geocoder: Geocoder, lat: Double, lng: Double) {
        viewModelScope.launch {
            try {
                val addresses = geocoder.getFromLocation(lat, lng, 1)
                _address.value = addresses?.get(0)?.getAddressLine(0)
                _city.value = addresses?.get(0)?.subAdminArea

            } catch (e: java.lang.NullPointerException) {
                Log.d("error", "getDetailsByLatLng: $e")
            }
        }
    }
}
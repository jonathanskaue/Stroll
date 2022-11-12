package com.example.stroll.presentation.viewmodel

import android.location.Geocoder
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stroll.data.local.StrollDataEntity
import com.example.stroll.domain.repository.StrollRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val strollRepo: StrollRepository
): ViewModel() {

    private var _city: MutableLiveData<String> = MutableLiveData()
    val city: LiveData<String> = _city

    private var _address: MutableLiveData<String> = MutableLiveData()
    val address: LiveData<String> = _address

    lateinit var hikeById: LiveData<StrollDataEntity>
    val totalDistanceHiked = strollRepo.getTotalDistanceHiked
    val totalTimeInMillisHiked = strollRepo.getTotalTimeInMillis
    val totalAverageSpeed = strollRepo.getTotalAverageSpeed
    val hikeSortedByDate = strollRepo.selectAllHikesSortedByDate

    fun getHikeById(id: Int) {
        viewModelScope.launch {
            hikeById = strollRepo.getHikeById(id)
        }
    }

    fun getDetailsByLatLng(geocoder: Geocoder, lat: Double, lng: Double) {
        try {
            var adresses = geocoder.getFromLocation(lat, lng, 1)
            _address.value = adresses?.get(0)?.getAddressLine(0)
            _city.value = adresses?.get(0)?.subAdminArea

        } catch (e: java.lang.NullPointerException) {
            Log.d("error", "getDetailsByLatLng: $e")
        }
    }
}
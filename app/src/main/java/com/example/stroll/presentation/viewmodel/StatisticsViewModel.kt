package com.example.stroll.presentation.viewmodel

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

    lateinit var hikeById: LiveData<StrollDataEntity>

    fun getHikeById(id: Int) {
        viewModelScope.launch {
            hikeById = strollRepo.getHikeById(id)
        }
    }


    val totalDistanceHiked = strollRepo.getTotalDistanceHiked
    val totalTimeInMillisHiked = strollRepo.getTotalTimeInMillis
    val totalAverageSpeed = strollRepo.getTotalAverageSpeed

    val hikeSortedByDate = strollRepo.selectAllHikesSortedByDate


}
package com.example.stroll.presentation.viewmodel

import androidx.lifecycle.ViewModel
import com.example.stroll.domain.repository.StrollRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val strollRepo: StrollRepository
): ViewModel() {
    val totalDistanceHiked = strollRepo.getTotalDistanceHiked
    val totalTimeInMillisHiked = strollRepo.getTotalTimeInMillis
    val totalAverageSpeed = strollRepo.getTotalAverageSpeed

    val hikeSortedByDatte = strollRepo.selectAllHikesSortedByDate
}
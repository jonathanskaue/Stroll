package com.example.stroll.presentation.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stroll.data.local.StrollDataEntity
import com.example.stroll.domain.model.StrollData
import com.example.stroll.domain.repository.StrollRepository
import com.example.stroll.presentation.DataState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val strollRepo: StrollRepository
): ViewModel() {

    var allData: LiveData<List<StrollDataEntity>> = strollRepo.readAllData

    fun addDataToRoom() {
        viewModelScope.launch {
            strollRepo.insertData(
                StrollData(
                    id = 2
                )
            )
        }
    }

}
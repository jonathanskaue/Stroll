package com.example.stroll.presentation

import com.example.stroll.domain.model.StrollData

data class DataState(
    val StrollData: List<StrollData> = emptyList()
)
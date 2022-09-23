package com.example.stroll.data.local

import com.example.stroll.domain.model.StrollData

fun StrollDataEntity.toStrollData(): StrollData {
    return StrollData(
        id = id
    )
}

fun StrollData.toStrollDataEntity(): StrollDataEntity {
    return StrollDataEntity(
        id = id!!
    )
}
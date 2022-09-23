package com.example.stroll.data.local

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.stroll.domain.model.StrollData
import kotlinx.coroutines.flow.Flow

@Dao
interface StrollDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertData(data: StrollDataEntity)

    @Query("SELECT * FROM strolldataentity")
    fun getAllData(): LiveData<List<StrollDataEntity>>

}
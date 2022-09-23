package com.example.stroll.di

import android.app.Application
import androidx.room.Room
import com.example.stroll.data.local.StrollDataBase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Singleton
    @Provides
    fun provideDataBase(app: Application): StrollDataBase {
        return Room.databaseBuilder(
            app,
            StrollDataBase::class.java,
            "stroll_data.db"
        )
            .build()
    }


}

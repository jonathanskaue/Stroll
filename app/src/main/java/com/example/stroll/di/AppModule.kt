package com.example.stroll.di

import android.app.Application
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import androidx.room.Room
import com.example.stroll.data.local.StrollDataBase
import com.example.stroll.data.repository.StrollRepositoryImpl
import com.example.stroll.domain.repository.StrollRepository
import com.example.stroll.other.Constants.KEY_FIRST_TIME_TOGGLE
import com.example.stroll.other.Constants.KEY_NAME
import com.example.stroll.other.Constants.SHARED_PREFERENCES_NAME
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
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
        ).build()
    }

    @Singleton
    @Provides
    fun provideStrollRepository(db: StrollDataBase): StrollRepository {
        return StrollRepositoryImpl(db.dao)
    }

    @Singleton
    @Provides
    fun provideSharedPreferences(@ApplicationContext app: Context) =
        app.getSharedPreferences(SHARED_PREFERENCES_NAME, MODE_PRIVATE)!!

    @Singleton
    @Provides
    fun provideName(sharedPref: SharedPreferences) = sharedPref.getString(KEY_NAME, "") ?: ""

    @Singleton
    @Provides
    fun provideFirstTimeToggle(sharedPref: SharedPreferences) =
        sharedPref.getBoolean(KEY_FIRST_TIME_TOGGLE, true)


}

package com.example.choreboo_habittrackerfriend.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.example.choreboo_habittrackerfriend.data.datastore.UserPreferences
import com.example.choreboo_habittrackerfriend.data.datastore.dataStore
import com.example.choreboo_habittrackerfriend.data.local.ChorebooDatabase
import com.example.choreboo_habittrackerfriend.data.local.dao.HabitDao
import com.example.choreboo_habittrackerfriend.data.local.dao.HabitLogDao
import com.example.choreboo_habittrackerfriend.data.local.dao.InventoryDao
import com.example.choreboo_habittrackerfriend.data.local.dao.ItemDao
import com.example.choreboo_habittrackerfriend.data.local.dao.ChorebooDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ChorebooDatabase {
        return ChorebooDatabase.getInstance(context)
    }

    @Provides
    fun provideHabitDao(db: ChorebooDatabase): HabitDao = db.habitDao()

    @Provides
    fun provideHabitLogDao(db: ChorebooDatabase): HabitLogDao = db.habitLogDao()

    @Provides
    fun provideChorebooDao(db: ChorebooDatabase): ChorebooDao = db.chorebooDao()

    @Provides
    fun provideItemDao(db: ChorebooDatabase): ItemDao = db.itemDao()

    @Provides
    fun provideInventoryDao(db: ChorebooDatabase): InventoryDao = db.inventoryDao()

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return context.dataStore
    }

    @Provides
    @Singleton
    fun provideUserPreferences(dataStore: DataStore<Preferences>): UserPreferences {
        return UserPreferences(dataStore)
    }
}


package com.example.weeboo_habittrackerfriend.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.example.weeboo_habittrackerfriend.data.datastore.UserPreferences
import com.example.weeboo_habittrackerfriend.data.datastore.dataStore
import com.example.weeboo_habittrackerfriend.data.local.WeebooDatabase
import com.example.weeboo_habittrackerfriend.data.local.dao.HabitDao
import com.example.weeboo_habittrackerfriend.data.local.dao.HabitLogDao
import com.example.weeboo_habittrackerfriend.data.local.dao.InventoryDao
import com.example.weeboo_habittrackerfriend.data.local.dao.ItemDao
import com.example.weeboo_habittrackerfriend.data.local.dao.WeebooDao
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
    fun provideDatabase(@ApplicationContext context: Context): WeebooDatabase {
        return WeebooDatabase.getInstance(context)
    }

    @Provides
    fun provideHabitDao(db: WeebooDatabase): HabitDao = db.habitDao()

    @Provides
    fun provideHabitLogDao(db: WeebooDatabase): HabitLogDao = db.habitLogDao()

    @Provides
    fun provideWeebooDao(db: WeebooDatabase): WeebooDao = db.weebooDao()

    @Provides
    fun provideItemDao(db: WeebooDatabase): ItemDao = db.itemDao()

    @Provides
    fun provideInventoryDao(db: WeebooDatabase): InventoryDao = db.inventoryDao()

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


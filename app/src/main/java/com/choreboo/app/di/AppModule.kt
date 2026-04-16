package com.choreboo.app.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.choreboo.app.data.datastore.UserPreferences
import com.choreboo.app.data.datastore.dataStore
import com.choreboo.app.data.local.ChorebooDatabase
import com.choreboo.app.data.local.dao.HabitDao
import com.choreboo.app.data.local.dao.HabitLogDao
import com.choreboo.app.data.local.dao.ChorebooDao
import com.choreboo.app.data.local.dao.HouseholdMemberDao
import com.choreboo.app.data.local.dao.HouseholdDao
import com.choreboo.app.data.local.dao.HouseholdHabitStatusDao
import com.choreboo.app.data.local.dao.PurchasedBackgroundDao
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage

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
    fun provideHouseholdMemberDao(db: ChorebooDatabase): HouseholdMemberDao = db.householdMemberDao()

    @Provides
    fun provideHouseholdDao(db: ChorebooDatabase): HouseholdDao = db.householdDao()

    @Provides
    fun provideHouseholdHabitStatusDao(db: ChorebooDatabase): HouseholdHabitStatusDao = db.householdHabitStatusDao()

    @Provides
    fun providePurchasedBackgroundDao(db: ChorebooDatabase): PurchasedBackgroundDao = db.purchasedBackgroundDao()

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

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseStorage(): FirebaseStorage = FirebaseStorage.getInstance()
}


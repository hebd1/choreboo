package com.example.choreboo_habittrackerfriend.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.choreboo_habittrackerfriend.data.local.converter.Converters
import com.example.choreboo_habittrackerfriend.data.local.dao.HabitDao
import com.example.choreboo_habittrackerfriend.data.local.dao.HabitLogDao
import com.example.choreboo_habittrackerfriend.data.local.dao.ChorebooDao
import com.example.choreboo_habittrackerfriend.data.local.dao.HouseholdMemberDao
import com.example.choreboo_habittrackerfriend.data.local.dao.HouseholdDao
import com.example.choreboo_habittrackerfriend.data.local.dao.HouseholdHabitStatusDao
import com.example.choreboo_habittrackerfriend.data.local.entity.HabitEntity
import com.example.choreboo_habittrackerfriend.data.local.entity.HabitLogEntity
import com.example.choreboo_habittrackerfriend.data.local.entity.ChorebooEntity
import com.example.choreboo_habittrackerfriend.data.local.entity.HouseholdMemberEntity
import com.example.choreboo_habittrackerfriend.data.local.entity.HouseholdEntity
import com.example.choreboo_habittrackerfriend.data.local.entity.HouseholdHabitStatusEntity

@Database(
    entities = [
        HabitEntity::class,
        HabitLogEntity::class,
        ChorebooEntity::class,
        HouseholdMemberEntity::class,
        HouseholdEntity::class,
        HouseholdHabitStatusEntity::class,
    ],
    version = 14,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class ChorebooDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao
    abstract fun habitLogDao(): HabitLogDao
    abstract fun chorebooDao(): ChorebooDao
    abstract fun householdMemberDao(): HouseholdMemberDao
    abstract fun householdDao(): HouseholdDao
    abstract fun householdHabitStatusDao(): HouseholdHabitStatusDao

    companion object {
        @Volatile
        private var INSTANCE: ChorebooDatabase? = null

        fun getInstance(context: Context): ChorebooDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ChorebooDatabase::class.java,
                    "choreboo_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

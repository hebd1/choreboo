package com.example.choreboo_habittrackerfriend.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.choreboo_habittrackerfriend.data.local.converter.Converters
import com.example.choreboo_habittrackerfriend.data.local.dao.HabitDao
import com.example.choreboo_habittrackerfriend.data.local.dao.HabitLogDao
import com.example.choreboo_habittrackerfriend.data.local.dao.ChorebooDao
import com.example.choreboo_habittrackerfriend.data.local.dao.HouseholdMemberDao
import com.example.choreboo_habittrackerfriend.data.local.dao.HouseholdDao
import com.example.choreboo_habittrackerfriend.data.local.dao.HouseholdHabitStatusDao
import com.example.choreboo_habittrackerfriend.data.local.dao.PurchasedBackgroundDao
import com.example.choreboo_habittrackerfriend.data.local.entity.HabitEntity
import com.example.choreboo_habittrackerfriend.data.local.entity.HabitLogEntity
import com.example.choreboo_habittrackerfriend.data.local.entity.ChorebooEntity
import com.example.choreboo_habittrackerfriend.data.local.entity.HouseholdMemberEntity
import com.example.choreboo_habittrackerfriend.data.local.entity.HouseholdEntity
import com.example.choreboo_habittrackerfriend.data.local.entity.HouseholdHabitStatusEntity
import com.example.choreboo_habittrackerfriend.data.local.entity.PurchasedBackgroundEntity

val MIGRATION_17_18 = object : Migration(17, 18) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Recreate the habits table with ownerUid as TEXT NOT NULL (default '')
        // to fix the nullable ownerUid that caused rows to be invisible in UID-filtered queries.
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `habits_new` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `title` TEXT NOT NULL,
                `description` TEXT,
                `iconName` TEXT NOT NULL,
                `customDays` TEXT NOT NULL,
                `difficulty` INTEGER NOT NULL,
                `baseXp` INTEGER NOT NULL,
                `reminderEnabled` INTEGER NOT NULL,
                `reminderTime` TEXT,
                `createdAt` INTEGER NOT NULL,
                `isArchived` INTEGER NOT NULL,
                `isHouseholdHabit` INTEGER NOT NULL,
                `ownerUid` TEXT NOT NULL,
                `householdId` TEXT,
                `assignedToUid` TEXT,
                `assignedToName` TEXT,
                `remoteId` TEXT,
                `pendingSync` INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            INSERT INTO `habits_new`
            SELECT
                `id`, `title`, `description`, `iconName`, `customDays`,
                `difficulty`, `baseXp`, `reminderEnabled`, `reminderTime`,
                `createdAt`, `isArchived`, `isHouseholdHabit`,
                COALESCE(`ownerUid`, '') AS `ownerUid`,
                `householdId`, `assignedToUid`, `assignedToName`,
                `remoteId`, `pendingSync`
            FROM `habits`
            """.trimIndent(),
        )
        db.execSQL("DROP TABLE `habits`")
        db.execSQL("ALTER TABLE `habits_new` RENAME TO `habits`")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_habits_remoteId` ON `habits` (`remoteId`)")
    }
}

@Database(
    entities = [
        HabitEntity::class,
        HabitLogEntity::class,
        ChorebooEntity::class,
        HouseholdMemberEntity::class,
        HouseholdEntity::class,
        HouseholdHabitStatusEntity::class,
        PurchasedBackgroundEntity::class,
    ],
    version = 18, // v18: ownerUid made non-null in habits table (P4-11)
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
    abstract fun purchasedBackgroundDao(): PurchasedBackgroundDao

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
                    .addMigrations(MIGRATION_17_18)
                    .fallbackToDestructiveMigrationOnDowngrade()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

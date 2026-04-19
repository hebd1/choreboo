package com.choreboo.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.choreboo.app.data.local.converter.Converters
import com.choreboo.app.data.local.dao.HabitDao
import com.choreboo.app.data.local.dao.HabitLogDao
import com.choreboo.app.data.local.dao.ChorebooDao
import com.choreboo.app.data.local.dao.HouseholdMemberDao
import com.choreboo.app.data.local.dao.HouseholdDao
import com.choreboo.app.data.local.dao.HouseholdHabitStatusDao
import com.choreboo.app.data.local.dao.PurchasedBackgroundDao
import com.choreboo.app.data.local.entity.HabitEntity
import com.choreboo.app.data.local.entity.HabitLogEntity
import com.choreboo.app.data.local.entity.ChorebooEntity
import com.choreboo.app.data.local.entity.HouseholdMemberEntity
import com.choreboo.app.data.local.entity.HouseholdEntity
import com.choreboo.app.data.local.entity.HouseholdHabitStatusEntity
import com.choreboo.app.data.local.entity.PurchasedBackgroundEntity

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

val MIGRATION_18_19 = object : Migration(18, 19) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Add index on cachedDate in household_habit_statuses for faster date-filtered queries.
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_household_habit_statuses_cachedDate` " +
                "ON `household_habit_statuses` (`cachedDate`)",
        )
    }
}

val MIGRATION_19_20 = object : Migration(19, 20) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Add index on completedByUid in habit_logs for faster badge-related queries
        // (getTotalCompletionCount and getMaxStreakEver both filter on this column).
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_habit_logs_completedByUid` " +
                "ON `habit_logs` (`completedByUid`)",
        )
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
    version = 23, // v23: multi-pet support with active choreboo flag
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
                    .addMigrations(MIGRATION_17_18, MIGRATION_18_19, MIGRATION_19_20)
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

package com.example.weeboo_habittrackerfriend.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.weeboo_habittrackerfriend.data.local.converter.Converters
import com.example.weeboo_habittrackerfriend.data.local.dao.HabitDao
import com.example.weeboo_habittrackerfriend.data.local.dao.HabitLogDao
import com.example.weeboo_habittrackerfriend.data.local.dao.InventoryDao
import com.example.weeboo_habittrackerfriend.data.local.dao.ItemDao
import com.example.weeboo_habittrackerfriend.data.local.dao.WeebooDao
import com.example.weeboo_habittrackerfriend.data.local.entity.EquippedItemEntity
import com.example.weeboo_habittrackerfriend.data.local.entity.HabitEntity
import com.example.weeboo_habittrackerfriend.data.local.entity.HabitLogEntity
import com.example.weeboo_habittrackerfriend.data.local.entity.InventoryItemEntity
import com.example.weeboo_habittrackerfriend.data.local.entity.ItemEntity
import com.example.weeboo_habittrackerfriend.data.local.entity.WeebooEntity

@Database(
    entities = [
        HabitEntity::class,
        HabitLogEntity::class,
        WeebooEntity::class,
        ItemEntity::class,
        InventoryItemEntity::class,
        EquippedItemEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class WeebooDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao
    abstract fun habitLogDao(): HabitLogDao
    abstract fun weebooDao(): WeebooDao
    abstract fun itemDao(): ItemDao
    abstract fun inventoryDao(): InventoryDao

    companion object {
        @Volatile
        private var INSTANCE: WeebooDatabase? = null

        fun getInstance(context: Context): WeebooDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WeebooDatabase::class.java,
                    "weeboo_database"
                )
                    .addCallback(SeedDatabaseCallback())
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

private class SeedDatabaseCallback : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        val seeds = listOf(
            // Food items
            "INSERT INTO items (name, description, type, rarity, price, effectValue, effectStat) VALUES ('Apple', 'A fresh, crunchy apple', 'FOOD', 'COMMON', 5, 15, 'HUNGER')",
            "INSERT INTO items (name, description, type, rarity, price, effectValue, effectStat) VALUES ('Cookie', 'A delicious chocolate chip cookie', 'FOOD', 'COMMON', 8, 10, 'HAPPINESS')",
            "INSERT INTO items (name, description, type, rarity, price, effectValue, effectStat) VALUES ('Energy Drink', 'Instant energy boost!', 'FOOD', 'COMMON', 8, 20, 'ENERGY')",
            "INSERT INTO items (name, description, type, rarity, price, effectValue, effectStat) VALUES ('Pizza', 'A hot slice of pizza', 'FOOD', 'COMMON', 12, 25, 'HUNGER')",
            "INSERT INTO items (name, description, type, rarity, price, effectValue, effectStat) VALUES ('Ice Cream', 'Sweet and creamy treat', 'FOOD', 'RARE', 20, 15, 'HAPPINESS')",
            "INSERT INTO items (name, description, type, rarity, price, effectValue, effectStat) VALUES ('Golden Feast', 'A legendary meal fit for royalty', 'FOOD', 'LEGENDARY', 50, 30, 'HUNGER')",
            "INSERT INTO items (name, description, type, rarity, price, effectValue, effectStat) VALUES ('Vitamins', 'Boosts overall health', 'FOOD', 'RARE', 15, 10, 'ENERGY')",
            "INSERT INTO items (name, description, type, rarity, price, effectValue, effectStat) VALUES ('Candy', 'A sweet little treat', 'FOOD', 'COMMON', 3, 5, 'HAPPINESS')",
            // Hats
            "INSERT INTO items (name, description, type, rarity, price) VALUES ('Top Hat', 'A classy top hat', 'HAT', 'COMMON', 30)",
            "INSERT INTO items (name, description, type, rarity, price) VALUES ('Crown', 'A golden crown for royalty', 'HAT', 'RARE', 80)",
            "INSERT INTO items (name, description, type, rarity, price) VALUES ('Wizard Hat', 'A mystical wizard hat', 'HAT', 'LEGENDARY', 200)",
            // Clothes
            "INSERT INTO items (name, description, type, rarity, price) VALUES ('T-Shirt', 'A comfy casual tee', 'CLOTHES', 'COMMON', 25)",
            "INSERT INTO items (name, description, type, rarity, price) VALUES ('Tuxedo', 'Looking sharp!', 'CLOTHES', 'RARE', 100)",
            "INSERT INTO items (name, description, type, rarity, price) VALUES ('Cape', 'A legendary hero cape', 'CLOTHES', 'LEGENDARY', 250)",
            // Backgrounds
            "INSERT INTO items (name, description, type, rarity, price) VALUES ('Meadow', 'A peaceful green meadow', 'BACKGROUND', 'COMMON', 40)",
            "INSERT INTO items (name, description, type, rarity, price) VALUES ('Beach', 'Sunny beach vibes', 'BACKGROUND', 'RARE', 90)",
            "INSERT INTO items (name, description, type, rarity, price) VALUES ('Space', 'Among the stars!', 'BACKGROUND', 'LEGENDARY', 300)",
        )
        seeds.forEach { db.execSQL(it) }
    }
}


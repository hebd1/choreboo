package com.example.choreboo_habittrackerfriend.data.local.dao
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.choreboo_habittrackerfriend.data.local.entity.ItemEntity
import kotlinx.coroutines.flow.Flow
@Dao
interface ItemDao {
    @Query("SELECT * FROM items")
    fun getAllItems(): Flow<List<ItemEntity>>
    @Query("SELECT * FROM items WHERE type = :type")
    fun getItemsByType(type: String): Flow<List<ItemEntity>>
    @Query("SELECT * FROM items WHERE id = :id")
    suspend fun getItemById(id: Long): ItemEntity?
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(items: List<ItemEntity>)
    @Query("SELECT * FROM items WHERE type = 'FOOD'")
    suspend fun getAllFoodItems(): List<ItemEntity>
    @Query("SELECT * FROM items WHERE rarity = :rarity")
    suspend fun getItemsByRarity(rarity: String): List<ItemEntity>
}

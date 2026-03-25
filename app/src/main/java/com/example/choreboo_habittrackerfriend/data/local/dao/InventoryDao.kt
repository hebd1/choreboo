package com.example.choreboo_habittrackerfriend.data.local.dao
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.choreboo_habittrackerfriend.data.local.entity.EquippedItemEntity
import com.example.choreboo_habittrackerfriend.data.local.entity.InventoryItemEntity
import kotlinx.coroutines.flow.Flow
@Dao
interface InventoryDao {
    @Query("""
        SELECT inventory_items.*, items.name AS itemName, items.type AS itemType, 
               items.rarity AS itemRarity, items.effectValue, items.effectStat,
               items.description AS itemDescription, items.animationAsset
        FROM inventory_items 
        INNER JOIN items ON inventory_items.itemId = items.id
        ORDER BY items.type, items.name
    """)
    fun getInventoryWithDetails(): Flow<List<InventoryItemWithDetails>>
    @Query("SELECT * FROM inventory_items WHERE itemId = :itemId LIMIT 1")
    suspend fun getInventoryItemByItemId(itemId: Long): InventoryItemEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInventoryItem(item: InventoryItemEntity): Long
    @Query("UPDATE inventory_items SET quantity = quantity + :amount WHERE itemId = :itemId")
    suspend fun incrementQuantity(itemId: Long, amount: Int)
    @Query("UPDATE inventory_items SET quantity = quantity - :amount WHERE itemId = :itemId AND quantity >= :amount")
    suspend fun decrementQuantity(itemId: Long, amount: Int): Int
    @Query("DELETE FROM inventory_items WHERE itemId = :itemId")
    suspend fun removeItem(itemId: Long)
    @Query("SELECT quantity FROM inventory_items WHERE itemId = :itemId")
    suspend fun getItemQuantity(itemId: Long): Int?
    // Equipped items
    @Query("SELECT * FROM equipped_items WHERE chorebooId = :chorebooId")
    fun getEquippedItems(chorebooId: Long): Flow<List<EquippedItemEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun equipItem(item: EquippedItemEntity): Long
    @Query("DELETE FROM equipped_items WHERE chorebooId = :chorebooId AND slot = :slot")
    suspend fun unequipItem(chorebooId: Long, slot: String)
}
data class InventoryItemWithDetails(
    val id: Long,
    val itemId: Long,
    val quantity: Int,
    val acquiredAt: Long,
    val itemName: String,
    val itemType: String,
    val itemRarity: String,
    val effectValue: Int?,
    val effectStat: String?,
    val itemDescription: String,
    val animationAsset: String?,
)

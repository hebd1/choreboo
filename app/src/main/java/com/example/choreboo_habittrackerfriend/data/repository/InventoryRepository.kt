package com.example.choreboo_habittrackerfriend.data.repository

import com.example.choreboo_habittrackerfriend.data.local.dao.InventoryDao
import com.example.choreboo_habittrackerfriend.data.local.dao.InventoryItemWithDetails
import com.example.choreboo_habittrackerfriend.data.local.dao.ItemDao
import com.example.choreboo_habittrackerfriend.data.local.entity.EquippedItemEntity
import com.example.choreboo_habittrackerfriend.data.local.entity.InventoryItemEntity
import com.example.choreboo_habittrackerfriend.data.datastore.UserPreferences
import com.example.choreboo_habittrackerfriend.domain.model.Item
import com.example.choreboo_habittrackerfriend.domain.model.ItemType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InventoryRepository @Inject constructor(
    private val inventoryDao: InventoryDao,
    private val itemDao: ItemDao,
) {
    fun getInventoryWithDetails(): Flow<List<InventoryItemWithDetails>> {
        return inventoryDao.getInventoryWithDetails()
    }

    suspend fun addItemToInventory(itemId: Long, quantity: Int = 1) {
        val existing = inventoryDao.getInventoryItemByItemId(itemId)
        if (existing != null) {
            inventoryDao.incrementQuantity(itemId, quantity)
        } else {
            inventoryDao.insertInventoryItem(
                InventoryItemEntity(itemId = itemId, quantity = quantity)
            )
        }
    }

    suspend fun consumeItem(itemId: Long): Boolean {
        val quantity = inventoryDao.getItemQuantity(itemId) ?: return false
        if (quantity <= 0) return false

        if (quantity == 1) {
            inventoryDao.removeItem(itemId)
        } else {
            inventoryDao.decrementQuantity(itemId, 1)
        }
        return true
    }

    fun getEquippedItems(chorebooId: Long): Flow<List<EquippedItemEntity>> {
        return inventoryDao.getEquippedItems(chorebooId)
    }

    suspend fun equipItem(chorebooId: Long, itemId: Long, slot: String) {
        inventoryDao.equipItem(EquippedItemEntity(chorebooId = chorebooId, itemId = itemId, slot = slot))
    }

    suspend fun unequipItem(chorebooId: Long, slot: String) {
        inventoryDao.unequipItem(chorebooId, slot)
    }

    suspend fun getItemById(id: Long) = itemDao.getItemById(id)
}


package com.example.weeboo_habittrackerfriend.data.repository

import com.example.weeboo_habittrackerfriend.data.local.dao.ItemDao
import com.example.weeboo_habittrackerfriend.data.local.entity.ItemEntity
import com.example.weeboo_habittrackerfriend.data.datastore.UserPreferences
import com.example.weeboo_habittrackerfriend.domain.model.Item
import com.example.weeboo_habittrackerfriend.domain.model.ItemType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

sealed class PurchaseResult {
    data class Success(val item: Item) : PurchaseResult()
    data object InsufficientFunds : PurchaseResult()
    data object ItemNotFound : PurchaseResult()
}

@Singleton
class ShopRepository @Inject constructor(
    private val itemDao: ItemDao,
    private val inventoryRepository: InventoryRepository,
    private val userPreferences: UserPreferences,
) {
    fun getAllShopItems(): Flow<List<Item>> = itemDao.getAllItems().map { entities ->
        entities.map { it.toDomain() }
    }

    fun getItemsByType(type: String): Flow<List<Item>> = itemDao.getItemsByType(type).map { entities ->
        entities.map { it.toDomain() }
    }

    suspend fun purchaseItem(itemId: Long): PurchaseResult {
        val item = itemDao.getItemById(itemId) ?: return PurchaseResult.ItemNotFound
        val deducted = userPreferences.deductPoints(item.price)
        if (!deducted) return PurchaseResult.InsufficientFunds

        inventoryRepository.addItemToInventory(itemId)
        return PurchaseResult.Success(item.toDomain())
    }

    suspend fun getRandomFoodItem(): ItemEntity? {
        val foods = itemDao.getAllFoodItems()
        if (foods.isEmpty()) return null

        // Weighted random: 70% COMMON, 25% RARE, 5% LEGENDARY
        val roll = (1..100).random()
        val rarity = when {
            roll <= 70 -> "COMMON"
            roll <= 95 -> "RARE"
            else -> "LEGENDARY"
        }
        val filtered = foods.filter { it.rarity == rarity }
        return (filtered.ifEmpty { foods }).random()
    }
}

fun ItemEntity.toDomain() = Item(
    id = id,
    name = name,
    description = description,
    type = try { ItemType.valueOf(type) } catch (_: Exception) { ItemType.FOOD },
    rarity = rarity,
    price = price,
    effectValue = effectValue,
    effectStat = effectStat,
    animationAsset = animationAsset,
)


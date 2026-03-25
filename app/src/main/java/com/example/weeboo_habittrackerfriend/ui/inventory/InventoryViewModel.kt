package com.example.weeboo_habittrackerfriend.ui.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.weeboo_habittrackerfriend.data.local.dao.InventoryItemWithDetails
import com.example.weeboo_habittrackerfriend.data.local.entity.EquippedItemEntity
import com.example.weeboo_habittrackerfriend.data.repository.InventoryRepository
import com.example.weeboo_habittrackerfriend.data.repository.WeebooRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InventoryViewModel @Inject constructor(
    private val inventoryRepository: InventoryRepository,
    private val weebooRepository: WeebooRepository,
) : ViewModel() {

    val inventoryItems: StateFlow<List<InventoryItemWithDetails>> =
        inventoryRepository.getInventoryWithDetails()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val foodItems: StateFlow<List<InventoryItemWithDetails>> =
        inventoryRepository.getInventoryWithDetails()
            .map { list -> list.filter { it.itemType == "FOOD" } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val hatItems: StateFlow<List<InventoryItemWithDetails>> =
        inventoryRepository.getInventoryWithDetails()
            .map { list -> list.filter { it.itemType == "HAT" } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val clothesItems: StateFlow<List<InventoryItemWithDetails>> =
        inventoryRepository.getInventoryWithDetails()
            .map { list -> list.filter { it.itemType == "CLOTHES" } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val backgroundItems: StateFlow<List<InventoryItemWithDetails>> =
        inventoryRepository.getInventoryWithDetails()
            .map { list -> list.filter { it.itemType == "BACKGROUND" } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val equippedItems: StateFlow<List<EquippedItemEntity>> =
        weebooRepository.getWeeboo()
            .flatMapLatest { weeboo ->
                if (weeboo != null) inventoryRepository.getEquippedItems(weeboo.id)
                else flowOf(emptyList())
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _events = MutableSharedFlow<InventoryEvent>()
    val events = _events.asSharedFlow()

    fun useFood(item: InventoryItemWithDetails) {
        viewModelScope.launch {
            val consumed = inventoryRepository.consumeItem(item.itemId)
            if (consumed && item.effectStat != null && item.effectValue != null) {
                weebooRepository.feedWeeboo(item.effectStat, item.effectValue)
                _events.emit(InventoryEvent.UsedFood(item.itemName))
            }
        }
    }

    fun equipItem(item: InventoryItemWithDetails) {
        viewModelScope.launch {
            val weeboo = weebooRepository.getWeebooSync() ?: return@launch
            val slot = item.itemType // "HAT", "CLOTHES", "BACKGROUND"
            inventoryRepository.equipItem(weeboo.id, item.itemId, slot)
            _events.emit(InventoryEvent.Equipped(item.itemName))
        }
    }

    fun unequipItem(slot: String) {
        viewModelScope.launch {
            val weeboo = weebooRepository.getWeebooSync() ?: return@launch
            inventoryRepository.unequipItem(weeboo.id, slot)
            _events.emit(InventoryEvent.Unequipped(slot))
        }
    }
}

sealed class InventoryEvent {
    data class UsedFood(val itemName: String) : InventoryEvent()
    data class Equipped(val itemName: String) : InventoryEvent()
    data class Unequipped(val slot: String) : InventoryEvent()
}


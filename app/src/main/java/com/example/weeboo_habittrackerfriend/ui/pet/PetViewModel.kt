package com.example.weeboo_habittrackerfriend.ui.pet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.weeboo_habittrackerfriend.data.datastore.UserPreferences
import com.example.weeboo_habittrackerfriend.data.local.dao.InventoryItemWithDetails
import com.example.weeboo_habittrackerfriend.data.local.entity.EquippedItemEntity
import com.example.weeboo_habittrackerfriend.data.local.entity.ItemEntity
import com.example.weeboo_habittrackerfriend.data.repository.InventoryRepository
import com.example.weeboo_habittrackerfriend.data.repository.WeebooRepository
import com.example.weeboo_habittrackerfriend.domain.model.WeebooMood
import com.example.weeboo_habittrackerfriend.domain.model.WeebooStats
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EquippedItemInfo(
    val slot: String,
    val itemName: String,
    val itemEmoji: String,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PetViewModel @Inject constructor(
    private val weebooRepository: WeebooRepository,
    private val inventoryRepository: InventoryRepository,
    private val userPreferences: UserPreferences,
) : ViewModel() {

    val weebooState: StateFlow<WeebooStats?> = weebooRepository.getWeeboo()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val currentMood: StateFlow<WeebooMood> = weebooRepository.getWeeboo()
        .map { it?.mood ?: WeebooMood.IDLE }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), WeebooMood.IDLE)

    val totalPoints: StateFlow<Int> = userPreferences.totalPoints
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val foodInventory: StateFlow<List<InventoryItemWithDetails>> =
        inventoryRepository.getInventoryWithDetails()
            .map { items -> items.filter { it.itemType == "FOOD" } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _equippedItemInfos = MutableStateFlow<List<EquippedItemInfo>>(emptyList())
    val equippedItemInfos: StateFlow<List<EquippedItemInfo>> = _equippedItemInfos.asStateFlow()

    private val _events = MutableSharedFlow<PetEvent>()
    val events = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            weebooRepository.applyStatDecay()
        }
        // Watch equipped items and resolve their names
        viewModelScope.launch {
            weebooRepository.getWeeboo()
                .flatMapLatest { weeboo ->
                    if (weeboo != null) inventoryRepository.getEquippedItems(weeboo.id)
                    else flowOf(emptyList())
                }
                .collect { equippedEntities ->
                    val infos = equippedEntities.mapNotNull { equipped ->
                        val item = inventoryRepository.getItemById(equipped.itemId)
                        if (item != null) {
                            val emoji = when (equipped.slot) {
                                "HAT" -> "🎩"
                                "CLOTHES" -> "👕"
                                "BACKGROUND" -> "🖼️"
                                else -> "📦"
                            }
                            EquippedItemInfo(
                                slot = equipped.slot,
                                itemName = item.name,
                                itemEmoji = emoji,
                            )
                        } else null
                    }
                    _equippedItemInfos.value = infos
                }
        }
    }

    fun feedWeeboo(inventoryItem: InventoryItemWithDetails) {
        viewModelScope.launch {
            val consumed = inventoryRepository.consumeItem(inventoryItem.itemId)
            if (consumed && inventoryItem.effectStat != null && inventoryItem.effectValue != null) {
                weebooRepository.feedWeeboo(inventoryItem.effectStat, inventoryItem.effectValue)
                _events.emit(PetEvent.Fed(inventoryItem.itemName))
            }
        }
    }
}

sealed class PetEvent {
    data class Fed(val itemName: String) : PetEvent()
}


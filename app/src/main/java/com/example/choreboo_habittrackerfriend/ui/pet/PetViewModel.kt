package com.example.choreboo_habittrackerfriend.ui.pet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.choreboo_habittrackerfriend.data.datastore.UserPreferences
import com.example.choreboo_habittrackerfriend.data.local.dao.InventoryItemWithDetails
import com.example.choreboo_habittrackerfriend.data.local.entity.EquippedItemEntity
import com.example.choreboo_habittrackerfriend.data.local.entity.ItemEntity
import com.example.choreboo_habittrackerfriend.data.repository.InventoryRepository
import com.example.choreboo_habittrackerfriend.data.repository.ChorebooRepository
import com.example.choreboo_habittrackerfriend.domain.model.ChorebooMood
import com.example.choreboo_habittrackerfriend.domain.model.ChorebooStats
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
    private val chorebooRepository: ChorebooRepository,
    private val inventoryRepository: InventoryRepository,
    private val userPreferences: UserPreferences,
) : ViewModel() {

    val chorebooState: StateFlow<ChorebooStats?> = chorebooRepository.getChoreboo()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val currentMood: StateFlow<ChorebooMood> = chorebooRepository.getChoreboo()
        .map { it?.mood ?: ChorebooMood.IDLE }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ChorebooMood.IDLE)

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
            chorebooRepository.applyStatDecay()
        }
        // Watch equipped items and resolve their names
        viewModelScope.launch {
            chorebooRepository.getChoreboo()
                .flatMapLatest { choreboo ->
                    if (choreboo != null) inventoryRepository.getEquippedItems(choreboo.id)
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

    fun feedChoreboo(inventoryItem: InventoryItemWithDetails) {
        viewModelScope.launch {
            val consumed = inventoryRepository.consumeItem(inventoryItem.itemId)
            if (consumed && inventoryItem.effectStat != null && inventoryItem.effectValue != null) {
                chorebooRepository.feedChoreboo(inventoryItem.effectStat, inventoryItem.effectValue)
                _events.emit(PetEvent.Fed(inventoryItem.itemName))
            }
        }
    }
}

sealed class PetEvent {
    data class Fed(val itemName: String) : PetEvent()
}


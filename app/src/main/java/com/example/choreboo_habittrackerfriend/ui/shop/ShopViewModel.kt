package com.example.choreboo_habittrackerfriend.ui.shop

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.choreboo_habittrackerfriend.data.datastore.UserPreferences
import com.example.choreboo_habittrackerfriend.data.repository.PurchaseResult
import com.example.choreboo_habittrackerfriend.data.repository.ShopRepository
import com.example.choreboo_habittrackerfriend.domain.model.Item
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ShopViewModel @Inject constructor(
    private val shopRepository: ShopRepository,
    private val userPreferences: UserPreferences,
) : ViewModel() {

    val shopItems: StateFlow<List<Item>> = shopRepository.getAllShopItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalPoints: StateFlow<Int> = userPreferences.totalPoints
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _events = MutableSharedFlow<ShopEvent>()
    val events = _events.asSharedFlow()

    fun purchaseItem(itemId: Long) {
        viewModelScope.launch {
            when (val result = shopRepository.purchaseItem(itemId)) {
                is PurchaseResult.Success -> _events.emit(ShopEvent.Purchased(result.item.name))
                is PurchaseResult.InsufficientFunds -> _events.emit(ShopEvent.InsufficientFunds)
                is PurchaseResult.ItemNotFound -> _events.emit(ShopEvent.Error("Item not found"))
            }
        }
    }
}

sealed class ShopEvent {
    data class Purchased(val itemName: String) : ShopEvent()
    data object InsufficientFunds : ShopEvent()
    data class Error(val message: String) : ShopEvent()
}


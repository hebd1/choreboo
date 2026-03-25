package com.example.choreboo_habittrackerfriend.ui.inventory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.choreboo_habittrackerfriend.data.local.dao.InventoryItemWithDetails

private val tabLabels = listOf("All", "Food", "Hats", "Clothes", "Backgrounds")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    onNavigateBack: () -> Unit,
    viewModel: InventoryViewModel = hiltViewModel(),
) {
    val allItems by viewModel.inventoryItems.collectAsState()
    val foodItems by viewModel.foodItems.collectAsState()
    val hatItems by viewModel.hatItems.collectAsState()
    val clothesItems by viewModel.clothesItems.collectAsState()
    val backgroundItems by viewModel.backgroundItems.collectAsState()
    val equippedItems by viewModel.equippedItems.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Set of currently equipped item IDs
    val equippedItemIds = equippedItems.map { it.itemId }.toSet()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is InventoryEvent.UsedFood -> snackbarHostState.showSnackbar(
                    "Used ${event.itemName}! 😋", duration = SnackbarDuration.Short,
                )
                is InventoryEvent.Equipped -> snackbarHostState.showSnackbar(
                    "Equipped ${event.itemName}! 🎩", duration = SnackbarDuration.Short,
                )
                is InventoryEvent.Unequipped -> snackbarHostState.showSnackbar(
                    "Unequipped ${event.slot}!", duration = SnackbarDuration.Short,
                )
            }
        }
    }

    val displayItems = when (selectedTab) {
        1 -> foodItems
        2 -> hatItems
        3 -> clothesItems
        4 -> backgroundItems
        else -> allItems
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Inventory", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                edgePadding = 16.dp,
                containerColor = Color.Transparent,
                indicator = {},
                divider = {},
            ) {
                tabLabels.forEachIndexed { index, label ->
                    FilterChip(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        label = { Text(label) },
                        modifier = Modifier.padding(horizontal = 4.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        ),
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (displayItems.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🎒", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Nothing here yet!",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "Complete habits or visit the shop",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(displayItems, key = { it.id }) { item ->
                        val isEquipped = item.itemId in equippedItemIds
                        InventoryItemCard(
                            item = item,
                            isEquipped = isEquipped,
                            onUse = if (item.itemType == "FOOD") {
                                { viewModel.useFood(item) }
                            } else null,
                            onEquip = if (item.itemType != "FOOD") {
                                {
                                    if (isEquipped) viewModel.unequipItem(item.itemType)
                                    else viewModel.equipItem(item)
                                }
                            } else null,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryItemCard(
    item: InventoryItemWithDetails,
    isEquipped: Boolean = false,
    onUse: (() -> Unit)? = null,
    onEquip: (() -> Unit)? = null,
) {
    val typeEmoji = when (item.itemType) {
        "FOOD" -> "🍽️"
        "HAT" -> "🎩"
        "CLOTHES" -> "👕"
        "BACKGROUND" -> "🖼️"
        else -> "📦"
    }
    val rarityColor = when (item.itemRarity) {
        "LEGENDARY" -> Color(0xFFFFD54F)
        "RARE" -> Color(0xFF7C4DFF)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isEquipped)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        onClick = { onUse?.invoke() ?: onEquip?.invoke() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = typeEmoji, fontSize = 36.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = item.itemName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = item.itemRarity,
                style = MaterialTheme.typography.labelSmall,
                color = rarityColor,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "x${item.quantity}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            if (item.itemType == "FOOD") {
                Text(
                    text = "Tap to use",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else if (isEquipped) {
                Text(
                    text = "✅ Equipped",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            } else {
                Text(
                    text = "Tap to equip",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}


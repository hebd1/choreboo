package com.example.choreboo_habittrackerfriend.ui.shop

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.choreboo_habittrackerfriend.domain.model.Item
import com.example.choreboo_habittrackerfriend.domain.model.ItemType

private val tabLabels = listOf("All", "Food", "Hats", "Clothes", "Backgrounds")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopScreen(
    viewModel: ShopViewModel = hiltViewModel(),
) {
    val shopItems by viewModel.shopItems.collectAsState()
    val totalPoints by viewModel.totalPoints.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTab by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ShopEvent.Purchased -> snackbarHostState.showSnackbar(
                    "Purchased ${event.itemName}! 🎉", duration = SnackbarDuration.Short,
                )
                is ShopEvent.InsufficientFunds -> snackbarHostState.showSnackbar(
                    "Not enough points! 💸", duration = SnackbarDuration.Short,
                )
                is ShopEvent.Error -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    val filteredItems = when (selectedTab) {
        0 -> shopItems
        1 -> shopItems.filter { it.type == ItemType.FOOD }
        2 -> shopItems.filter { it.type == ItemType.HAT }
        3 -> shopItems.filter { it.type == ItemType.CLOTHES }
        4 -> shopItems.filter { it.type == ItemType.BACKGROUND }
        else -> shopItems
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Shop", fontWeight = FontWeight.Bold) },
                actions = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 12.dp),
                    ) {
                        Icon(
                            Icons.Default.Stars,
                            contentDescription = "Points",
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "$totalPoints",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.tertiary,
                            style = MaterialTheme.typography.titleMedium,
                        )
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
            // Tab row
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

            // Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(filteredItems, key = { it.id }) { item ->
                    ShopItemCard(
                        item = item,
                        canAfford = totalPoints >= item.price,
                        onBuy = { viewModel.purchaseItem(item.id) },
                    )
                }
            }
        }
    }
}

@Composable
fun ShopItemCard(
    item: Item,
    canAfford: Boolean,
    onBuy: () -> Unit,
) {
    val rarityColor = when (item.rarity) {
        "LEGENDARY" -> Color(0xFFFFD54F)
        "RARE" -> Color(0xFF7C4DFF)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val typeEmoji = when (item.type) {
        ItemType.FOOD -> "🍽️"
        ItemType.HAT -> "🎩"
        ItemType.CLOTHES -> "👕"
        ItemType.BACKGROUND -> "🖼️"
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Icon
            Text(text = typeEmoji, fontSize = 36.sp)

            Spacer(modifier = Modifier.height(8.dp))

            // Name
            Text(
                text = item.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            // Rarity
            Text(
                text = item.rarity,
                style = MaterialTheme.typography.labelSmall,
                color = rarityColor,
                fontWeight = FontWeight.Bold,
            )

            // Description
            if (item.description.isNotBlank()) {
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            // Effect (food only)
            if (item.type == ItemType.FOOD && item.effectValue != null) {
                Text(
                    text = "+${item.effectValue} ${item.effectStat ?: ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Buy button
            Button(
                onClick = onBuy,
                enabled = canAfford,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp),
                shape = RoundedCornerShape(10.dp),
                contentPadding = ButtonDefaults.ContentPadding,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            ) {
                Icon(Icons.Default.Stars, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${item.price}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}


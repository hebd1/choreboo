package com.example.choreboo_habittrackerfriend.ui.pet.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.example.choreboo_habittrackerfriend.R
import com.example.choreboo_habittrackerfriend.domain.model.BackgroundItem
import com.example.choreboo_habittrackerfriend.domain.model.BackgroundTier
import com.example.choreboo_habittrackerfriend.ui.theme.GoldGlow
import com.example.choreboo_habittrackerfriend.ui.theme.StitchTertiary
import androidx.compose.ui.res.stringResource
import com.example.choreboo_habittrackerfriend.ui.util.labelRes

/**
 * Bottom sheet that lets the user browse, purchase, and select backgrounds.
 *
 * @param catalogue         Full ordered list of [BackgroundItem]s (from [BACKGROUND_REGISTRY]).
 * @param unlockedIds       Set of IDs the user already owns (always includes "default").
 * @param currentBackgroundId The ID currently applied to the Choreboo (null = default).
 * @param totalPoints       User's current star-point balance (for affordability check).
 * @param isPremium         Whether the user has an active premium subscription.
 * @param onSelect          Called when the user taps an already-owned item to apply it.
 * @param onPurchase        Called when the user taps "Buy" on a locked item.
 * @param onDismiss         Called when the sheet should be dismissed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackgroundPickerSheet(
    catalogue: List<BackgroundItem>,
    unlockedIds: Set<String>,
    currentBackgroundId: String?,
    totalPoints: Int,
    isPremium: Boolean = false,
    onSelect: (String?) -> Unit,
    onPurchase: (BackgroundItem) -> Unit,
    onDismiss: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.bg_picker_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                // Points balance pill
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Icon(
                        Icons.Default.Stars,
                        contentDescription = stringResource(R.string.bg_picker_points_cd),
                        tint = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "$totalPoints",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp),
            ) {
                items(catalogue, key = { it.id }) { item ->
                    val isUnlocked = item.id in unlockedIds || (item.requiresPremium && isPremium)
                    val isSelected = (currentBackgroundId ?: "default") == item.id
                    val canAfford = totalPoints >= item.cost
                    val isPremiumLocked = item.requiresPremium && !isPremium

                    BackgroundCell(
                        item = item,
                        isUnlocked = isUnlocked,
                        isSelected = isSelected,
                        canAfford = canAfford,
                        isPremiumLocked = isPremiumLocked,
                        onSelect = { onSelect(if (item.isDefault) null else item.id) },
                        onPurchase = { onPurchase(item) },
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun BackgroundCell(
    item: BackgroundItem,
    isUnlocked: Boolean,
    isSelected: Boolean,
    canAfford: Boolean,
    isPremiumLocked: Boolean,
    onSelect: () -> Unit,
    onPurchase: () -> Unit,
) {
    val context = LocalContext.current
    val borderColor = when {
        isSelected -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outlineVariant
    }
    val borderWidth = if (isSelected) 2.5.dp else 1.dp

    Box(
        modifier = Modifier
            .height(120.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(borderWidth, borderColor, RoundedCornerShape(12.dp))
            .clickable(enabled = !isPremiumLocked) {
                if (isUnlocked) onSelect() else if (canAfford) onPurchase()
            },
    ) {
        // Background thumbnail — asset image or emoji fallback
        if (item.assetPath != null) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data("file:///android_asset/${item.assetPath}")
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )
            // Scrim so text is readable over image
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color.Black.copy(alpha = 0.3f)),
            )
        } else {
            // Default mood-gradient placeholder
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(MaterialTheme.colorScheme.surfaceContainerLow),
                contentAlignment = Alignment.Center,
            ) {
                Text(item.emoji, fontSize = 28.sp)
            }
        }

        // Top-right: tier badge or "✓ selected" checkmark
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp),
        ) {
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(RoundedCornerShape(50.dp))
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = stringResource(R.string.bg_picker_selected_cd),
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(14.dp),
                    )
                }
            } else if (!item.isDefault) {
                TierBadge(tier = item.tier, isUnlocked = isUnlocked)
            }
        }

        // Bottom: label + action
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = stringResource(item.labelRes()),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (item.assetPath != null) Color.White else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )
            if (!isUnlocked) {
                if (isPremiumLocked) {
                    // Crown "Premium" lock pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50.dp))
                            .background(MaterialTheme.colorScheme.tertiaryContainer)
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Icon(
                                Icons.Default.WorkspacePremium,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.size(10.dp),
                            )
                            Text(
                                text = stringResource(R.string.bg_picker_premium_label),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                            )
                        }
                    }
                } else {
                    // Buy button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50.dp))
                            .background(
                                if (canAfford) MaterialTheme.colorScheme.secondary
                                else MaterialTheme.colorScheme.surfaceContainerHighest,
                            )
                            .clickable(enabled = canAfford) { onPurchase() }
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Icon(
                                Icons.Default.Stars,
                                contentDescription = null,
                                tint = if (canAfford) MaterialTheme.colorScheme.onSecondary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(10.dp),
                            )
                            Text(
                                text = "${item.cost}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (canAfford) MaterialTheme.colorScheme.onSecondary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            } else if (!isSelected) {
                Text(
                    text = stringResource(R.string.bg_picker_tap_to_use),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (item.assetPath != null) Color.White.copy(alpha = 0.85f)
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Lock icon overlay when not affordable or premium-locked
        if (!isUnlocked && (!canAfford || isPremiumLocked)) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color.Black.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (isPremiumLocked) Icons.Default.WorkspacePremium else Icons.Default.Lock,
                    contentDescription = if (isPremiumLocked) stringResource(R.string.bg_picker_premium_only_cd) else stringResource(R.string.bg_picker_not_enough_points_cd),
                    tint = if (isPremiumLocked) MaterialTheme.colorScheme.tertiary else Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(28.dp),
                )
            }
        }
    }
}

@Composable
private fun TierBadge(tier: BackgroundTier?, isUnlocked: Boolean) {
    val (label, color) = when {
        isUnlocked -> return // no badge for owned items
        tier == null -> return // default — always unlocked, no badge
        tier == BackgroundTier.COMMON -> stringResource(R.string.tier_common) to MaterialTheme.colorScheme.primary
        tier == BackgroundTier.RARE -> stringResource(R.string.tier_rare) to StitchTertiary
        else -> stringResource(R.string.tier_premium) to GoldGlow
    }
    Box(
        modifier = Modifier
            .size(18.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(color),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Black,
            color = Color.White,
            fontSize = 9.sp,
        )
    }
}

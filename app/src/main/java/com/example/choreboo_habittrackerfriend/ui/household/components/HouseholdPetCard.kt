package com.example.choreboo_habittrackerfriend.ui.household.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.choreboo_habittrackerfriend.domain.model.ChorebooStage
import com.example.choreboo_habittrackerfriend.domain.model.HouseholdPet

private val stageEmoji = mapOf(
    ChorebooStage.EGG to "\uD83E\uDD5A",
    ChorebooStage.BABY to "\uD83D\uDC23",
    ChorebooStage.CHILD to "\uD83D\uDC25",
    ChorebooStage.TEEN to "\uD83D\uDC24",
    ChorebooStage.ADULT to "\uD83D\uDC14",
    ChorebooStage.LEGENDARY to "\uD83E\uDD85",
)

/**
 * Compact card designed for a 2-column grid.
 * Shows the pet emoji, level badge, owner avatar (photo or initial), owner name,
 * and three mini stat bars (hunger / happiness / energy).
 */
@Composable
fun HouseholdPetCard(
    pet: HouseholdPet,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // ── Pet emoji + level badge ──────────────────────────────────
            Box(contentAlignment = Alignment.BottomEnd) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                ) {
                    Text(
                        text = stageEmoji[pet.stage] ?: "\uD83D\uDC3E",
                        fontSize = 32.sp,
                    )
                }
                // Level badge overlaid on bottom-end of the pet circle
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(horizontal = 4.dp, vertical = 1.dp),
                ) {
                    Text(
                        text = "Lv.${pet.level}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ── Pet name ────────────────────────────────────────────────
            Text(
                text = pet.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(modifier = Modifier.height(4.dp))

            // ── Owner row: photo/initial + display name ──────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                // Avatar: photo if available, otherwise coloured initial
                if (!pet.ownerPhotoUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = pet.ownerPhotoUrl,
                        contentDescription = "Profile photo of ${pet.ownerName}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape),
                    )
                } else {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondaryContainer),
                    ) {
                        Text(
                            text = pet.ownerName.firstOrNull()?.uppercase() ?: "?",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))

                Text(
                    text = pet.ownerName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ── Mini stat bars ───────────────────────────────────────────
            MiniStatBar(emoji = "\uD83C\uDF56", value = pet.hunger)
            Spacer(modifier = Modifier.height(4.dp))
            MiniStatBar(emoji = "\uD83D\uDE0A", value = pet.happiness)
            Spacer(modifier = Modifier.height(4.dp))
            MiniStatBar(emoji = "\u26A1", value = pet.energy)
        }
    }
}

@Composable
private fun MiniStatBar(
    emoji: String,
    value: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = emoji,
            fontSize = 12.sp,
        )
        LinearProgressIndicator(
            progress = { value / 100f },
            modifier = Modifier
                .weight(1f)
                .height(5.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = when {
                value < 20 -> MaterialTheme.colorScheme.error
                value < 50 -> MaterialTheme.colorScheme.tertiary
                else -> MaterialTheme.colorScheme.primary
            },
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        )
    }
}

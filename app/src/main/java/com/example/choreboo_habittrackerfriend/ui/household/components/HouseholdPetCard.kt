package com.example.choreboo_habittrackerfriend.ui.household.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.rememberLottieAnimatable
import com.airbnb.lottie.compose.rememberLottieComposition
import com.example.choreboo_habittrackerfriend.domain.model.ChorebooMood
import com.example.choreboo_habittrackerfriend.domain.model.HouseholdPet
import com.example.choreboo_habittrackerfriend.domain.model.PetType

private enum class HouseholdAnimPhase { MOOD, IDLE }

/**
 * Compact card designed for a 2-column grid.
 * Shows the pet Lottie idle animation (mood-idle alternating loop for FOX; emoji
 * fallback for other pet types), a level badge, owner avatar overlaid in the
 * upper-left corner of the card, the pet name, owner row, and mini stat bars.
 */
@Composable
fun HouseholdPetCard(
    pet: HouseholdPet,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    // Outer Box allows the owner avatar badge to be overlaid on the card's top-left.
    Box(modifier = modifier) {
        Card(
            onClick = { onClick?.invoke() },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    // Extra top padding so column content clears the avatar overlay.
                    .padding(start = 12.dp, end = 12.dp, bottom = 12.dp, top = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // ── Animation area + level badge ─────────────────────────
                Box(contentAlignment = Alignment.BottomEnd) {
                    HouseholdPetAnimation(
                        petType = pet.petType,
                        mood = pet.mood,
                        modifier = Modifier.size(88.dp),
                    )
                    // Level badge overlaid on bottom-end of the animation area
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

                // ── Pet name ─────────────────────────────────────────────
                Text(
                    text = pet.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Spacer(modifier = Modifier.height(4.dp))

                // ── Owner row: photo/initial + display name ───────────────
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    var ownerPhotoFailed by remember { mutableStateOf(false) }

                    if (!pet.ownerPhotoUrl.isNullOrBlank() && !ownerPhotoFailed) {
                        AsyncImage(
                            model = pet.ownerPhotoUrl,
                            contentDescription = "Profile photo of ${pet.ownerName}",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape),
                            onError = { ownerPhotoFailed = true },
                        )
                    } else {
                        // Fallback: green AccountCircle icon
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "Profile photo of ${pet.ownerName}",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
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

                // ── Mini stat bars ────────────────────────────────────────
                MiniStatBar(emoji = "\uD83C\uDF56", value = pet.hunger)
                Spacer(modifier = Modifier.height(4.dp))
                MiniStatBar(emoji = "\uD83D\uDE0A", value = pet.happiness)
                Spacer(modifier = Modifier.height(4.dp))
                MiniStatBar(emoji = "\u26A1", value = pet.energy)
            }
        }

        // ── Owner avatar badge – upper-left corner overlay ───────────────
        var avatarPhotoFailed by remember { mutableStateOf(false) }

        if (!pet.ownerPhotoUrl.isNullOrBlank() && !avatarPhotoFailed) {
            AsyncImage(
                model = pet.ownerPhotoUrl,
                contentDescription = "Profile photo of ${pet.ownerName}",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .size(28.dp)
                    .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape)
                    .clip(CircleShape),
                onError = { avatarPhotoFailed = true },
            )
        } else {
            // Fallback: green AccountCircle icon with border
            Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = "Profile photo of ${pet.ownerName}",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .size(28.dp)
                    .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape),
            )
        }
    }
}

/**
 * Pet animation area for the household card.
 *
 * FOX: Lottie mood-idle alternating loop — plays the mood-appropriate animation
 * once (happy/hungry/sad), then the idle animation 3 times, then repeats.
 *
 * All other pet types (AXOLOTL, CAPYBARA, PANDA): emoji placeholder until
 * their Lottie assets are added under assets/animations/<type>/.
 */
@Composable
private fun HouseholdPetAnimation(
    petType: PetType,
    mood: ChorebooMood,
    modifier: Modifier = Modifier,
) {
    if (petType == PetType.FOX) {
        var phase by remember { mutableStateOf(HouseholdAnimPhase.MOOD) }

        val happyComposition by rememberLottieComposition(
            LottieCompositionSpec.Asset("animations/fox/fox_happy_lottie.json"),
        )
        val hungryComposition by rememberLottieComposition(
            LottieCompositionSpec.Asset("animations/fox/fox_hungry_lottie.json"),
        )
        val sadComposition by rememberLottieComposition(
            LottieCompositionSpec.Asset("animations/fox/fox_sad_lottie.json"),
        )
        val idleComposition by rememberLottieComposition(
            LottieCompositionSpec.Asset("animations/fox/fox_idle_lottie.json"),
        )

        val lottieAnimatable = rememberLottieAnimatable()

        val currentPhaseComposition = when (phase) {
            HouseholdAnimPhase.MOOD -> when (mood) {
                ChorebooMood.HAPPY,
                ChorebooMood.CONTENT -> happyComposition
                ChorebooMood.HUNGRY -> hungryComposition
                else -> sadComposition
            }
            HouseholdAnimPhase.IDLE -> idleComposition
        }

        LaunchedEffect(phase, currentPhaseComposition) {
            if (currentPhaseComposition != null) {
                lottieAnimatable.animate(
                    composition = currentPhaseComposition,
                    iterations = when (phase) {
                        HouseholdAnimPhase.MOOD -> 1
                        HouseholdAnimPhase.IDLE -> 3
                    },
                )
                phase = when (phase) {
                    HouseholdAnimPhase.MOOD -> HouseholdAnimPhase.IDLE
                    HouseholdAnimPhase.IDLE -> HouseholdAnimPhase.MOOD
                }
            }
        }

        Crossfade(
            targetState = phase,
            animationSpec = tween(durationMillis = 100),
            label = "householdPetAnimCrossfade",
        ) { animPhase ->
            val composition = when (animPhase) {
                HouseholdAnimPhase.MOOD -> when (mood) {
                    ChorebooMood.HAPPY,
                    ChorebooMood.CONTENT -> happyComposition
                    ChorebooMood.HUNGRY -> hungryComposition
                    else -> sadComposition
                }
                HouseholdAnimPhase.IDLE -> idleComposition
            }
            LottieAnimation(
                composition = composition,
                progress = { lottieAnimatable.progress },
                modifier = modifier,
            )
        }
    } else {
        // Emoji placeholder for AXOLOTL, CAPYBARA, PANDA until their Lottie
        // assets are added under assets/animations/<pettype>/.
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text(
                text = petType.emoji,
                fontSize = 44.sp,
            )
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

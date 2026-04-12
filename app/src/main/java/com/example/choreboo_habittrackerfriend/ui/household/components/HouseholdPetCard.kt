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
import com.example.choreboo_habittrackerfriend.R
import com.example.choreboo_habittrackerfriend.domain.model.ChorebooMood
import com.example.choreboo_habittrackerfriend.domain.model.HouseholdPet
import com.example.choreboo_habittrackerfriend.domain.model.PetType
import com.example.choreboo_habittrackerfriend.ui.components.PetBackgroundImage
import com.example.choreboo_habittrackerfriend.ui.components.WebmAnimationView
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.res.stringResource
import com.example.choreboo_habittrackerfriend.ui.theme.PetMoodContentStart
import com.example.choreboo_habittrackerfriend.ui.theme.PetMoodDarkContentStart
import com.example.choreboo_habittrackerfriend.ui.theme.PetMoodDarkHappyStart
import com.example.choreboo_habittrackerfriend.ui.theme.PetMoodDarkHungryStart
import com.example.choreboo_habittrackerfriend.ui.theme.PetMoodDarkSadStart
import com.example.choreboo_habittrackerfriend.ui.theme.PetMoodDarkTiredStart
import com.example.choreboo_habittrackerfriend.ui.theme.PetMoodHappyStart
import com.example.choreboo_habittrackerfriend.ui.theme.PetMoodHungryStart
import com.example.choreboo_habittrackerfriend.ui.theme.PetMoodSadStart
import com.example.choreboo_habittrackerfriend.ui.theme.PetMoodTiredStart

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
    animationOffsetMs: Long = 0L,
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
                    // Background + animation stacked in a clipped rounded box
                    Box(
                        modifier = Modifier
                            .size(88.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        val isDark = isSystemInDarkTheme()
                        val moodBg = when (pet.mood) {
                            ChorebooMood.HAPPY, ChorebooMood.CONTENT ->
                                if (isDark) PetMoodDarkHappyStart else PetMoodHappyStart
                            ChorebooMood.HUNGRY ->
                                if (isDark) PetMoodDarkHungryStart else PetMoodHungryStart
                            ChorebooMood.TIRED ->
                                if (isDark) PetMoodDarkTiredStart else PetMoodTiredStart
                            ChorebooMood.SAD ->
                                if (isDark) PetMoodDarkSadStart else PetMoodSadStart
                            else ->
                                if (isDark) PetMoodDarkContentStart else PetMoodContentStart
                        }
                        PetBackgroundImage(
                            backgroundId = pet.backgroundId,
                            mood = pet.mood,
                            moodColor = moodBg,
                        )
                        HouseholdPetAnimation(
                            petType = pet.petType,
                            mood = pet.mood,
                            animationOffsetMs = animationOffsetMs,
                            modifier = Modifier.size(88.dp),
                        )
                    }
                    // Level badge overlaid on bottom-end of the animation area
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.primary)
                            .padding(horizontal = 4.dp, vertical = 1.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.stats_level_label, pet.level),
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

                // ── Owner name ─────────────────────────────────────────────
                Text(
                    text = pet.ownerName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

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
        var avatarPhotoFailed by remember(pet.ownerPhotoUrl) { mutableStateOf(false) }

        if (!pet.ownerPhotoUrl.isNullOrBlank() && !avatarPhotoFailed) {
            AsyncImage(
                model = pet.ownerPhotoUrl,
                contentDescription = stringResource(R.string.household_profile_photo_cd, pet.ownerName),
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
                contentDescription = stringResource(R.string.household_profile_photo_cd, pet.ownerName),
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
 * FOX: WebM mood-idle alternating loop — plays the mood-appropriate animation
 * once (happy/hungry/sad), then the idle animation 3 times, then repeats.
 *
 * All other pet types (AXOLOTL, CAPYBARA, PANDA): emoji placeholder until
 * their WebM assets are added under assets/animations/<type>/.
 */
@Composable
private fun HouseholdPetAnimation(
    petType: PetType,
    mood: ChorebooMood,
    animationOffsetMs: Long = 0L,
    modifier: Modifier = Modifier,
) {
    if (petType == PetType.FOX) {
        var phase by remember { mutableStateOf(HouseholdAnimPhase.MOOD) }
        var started by remember { mutableStateOf(animationOffsetMs == 0L) }

        // If an offset is specified, delay before starting animations
        LaunchedEffect(Unit) {
            if (animationOffsetMs > 0) {
                kotlinx.coroutines.delay(animationOffsetMs)
                started = true
            }
        }

        val (assetPath, iterations) = when (phase) {
            HouseholdAnimPhase.MOOD -> {
                val moodAsset = when (mood) {
                    ChorebooMood.HAPPY,
                    ChorebooMood.CONTENT -> "animations/fox/fox_happy.webp"
                    ChorebooMood.HUNGRY -> "animations/fox/fox_hungry.webp"
                    else -> "animations/fox/fox_sad.webp"
                }
                moodAsset to 1
            }
            HouseholdAnimPhase.IDLE -> "animations/fox/fox_idle.webp" to 3
        }

        Crossfade(
            targetState = phase,
            animationSpec = tween(durationMillis = 100),
            label = "householdPetAnimCrossfade",
        ) { animPhase ->
            val (currentAsset, currentIterations) = when (animPhase) {
                HouseholdAnimPhase.MOOD -> {
                    val moodAsset = when (mood) {
                        ChorebooMood.HAPPY,
                        ChorebooMood.CONTENT -> "animations/fox/fox_happy.webp"
                        ChorebooMood.HUNGRY -> "animations/fox/fox_hungry.webp"
                        else -> "animations/fox/fox_sad.webp"
                    }
                    moodAsset to 1
                }
                HouseholdAnimPhase.IDLE -> "animations/fox/fox_idle.webp" to 3
            }

            if (started) {
                WebmAnimationView(
                    assetPath = currentAsset,
                    iterations = currentIterations,
                    onComplete = {
                        phase = when (phase) {
                            HouseholdAnimPhase.MOOD -> HouseholdAnimPhase.IDLE
                            HouseholdAnimPhase.IDLE -> HouseholdAnimPhase.MOOD
                        }
                    },
                    modifier = modifier,
                )
            } else {
                // Show a blank box while waiting for the animation to start
                Box(modifier = modifier)
            }
        }
    } else {
        // Emoji placeholder for AXOLOTL, CAPYBARA, PANDA until their WebM
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

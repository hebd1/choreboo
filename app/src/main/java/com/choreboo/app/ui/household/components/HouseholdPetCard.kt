package com.choreboo.app.ui.household.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.choreboo.app.R
import com.choreboo.app.domain.model.ChorebooMood
import com.choreboo.app.domain.model.HouseholdPet
import com.choreboo.app.domain.model.PetType
import com.choreboo.app.ui.components.PetBackgroundImage
import com.choreboo.app.ui.components.PetSceneSwipeMask
import com.choreboo.app.ui.components.WebmAnimationView
import com.choreboo.app.ui.components.FOX_ANIM_IDLE
import com.choreboo.app.ui.components.FOX_IDLE_ITERATIONS
import com.choreboo.app.ui.components.foxMoodAssetPath
import com.choreboo.app.ui.components.PANDA_ANIM_IDLE
import com.choreboo.app.ui.components.PANDA_IDLE_ITERATIONS
import com.choreboo.app.ui.components.pandaMoodAssetPath
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.res.stringResource
import com.choreboo.app.ui.theme.PetMoodContentStart
import com.choreboo.app.ui.theme.PetMoodDarkContentStart
import com.choreboo.app.ui.theme.PetMoodDarkHappyStart
import com.choreboo.app.ui.theme.PetMoodDarkHungryStart
import com.choreboo.app.ui.theme.PetMoodDarkSadStart
import com.choreboo.app.ui.theme.PetMoodDarkTiredStart
import com.choreboo.app.ui.theme.PetMoodHappyStart
import com.choreboo.app.ui.theme.PetMoodHungryStart
import com.choreboo.app.ui.theme.PetMoodSadStart
import com.choreboo.app.ui.theme.PetMoodTiredStart
import com.choreboo.app.ui.theme.softGlassSurface

private enum class HouseholdAnimPhase { MOOD, IDLE }

private val HOUSEHOLD_PET_SCENE_OFFSET_X = 0.dp
private val HOUSEHOLD_PET_SCENE_OFFSET_Y = 12.dp

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
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = androidx.compose.ui.graphics.Color.Transparent,
            ),
            border = BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.16f),
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .softGlassSurface(
                        shape = RoundedCornerShape(20.dp),
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.74f),
                        borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.18f),
                    )
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(112.dp)
                        .clip(RoundedCornerShape(18.dp)),
                ) {
                    var petSceneTransitionKey by remember { mutableIntStateOf(0) }
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
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.surface.copy(alpha = 0.08f),
                                        Color.Transparent,
                                        MaterialTheme.colorScheme.surface.copy(alpha = 0.22f),
                                    ),
                                ),
                            ),
                    )
                    HouseholdPetAnimation(
                        petType = pet.petType,
                        mood = pet.mood,
                        animationOffsetMs = animationOffsetMs,
                        onTransition = { petSceneTransitionKey++ },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .offset(x = HOUSEHOLD_PET_SCENE_OFFSET_X, y = HOUSEHOLD_PET_SCENE_OFFSET_Y)
                            .size(94.dp),
                    )
                    PetSceneSwipeMask(
                        transitionKey = petSceneTransitionKey,
                        modifier = Modifier.fillMaxSize(),
                    )

                    OwnerAvatarBadge(
                        ownerName = pet.ownerName,
                        ownerPhotoUrl = pet.ownerPhotoUrl,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(10.dp),
                    )

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(10.dp)
                            .softGlassSurface(
                                shape = RoundedCornerShape(10.dp),
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.88f),
                                borderColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.08f),
                            )
                            .padding(horizontal = 9.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.household_level_badge, pet.level),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = pet.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Spacer(modifier = Modifier.height(6.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = pet.ownerName,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                MiniStatBar(emoji = "\uD83C\uDF56", value = pet.hunger)
                Spacer(modifier = Modifier.height(6.dp))
                MiniStatBar(emoji = "\uD83D\uDE0A", value = pet.happiness)
                Spacer(modifier = Modifier.height(6.dp))
                MiniStatBar(emoji = "\u26A1", value = pet.energy)
            }
        }
    }
}

@Composable
private fun OwnerAvatarBadge(
    ownerName: String,
    ownerPhotoUrl: String?,
    modifier: Modifier = Modifier,
) {
    var avatarPhotoFailed by remember(ownerPhotoUrl) { mutableStateOf(false) }

    Box(
        modifier = modifier
            .size(34.dp)
            .softGlassSurface(
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
                borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.18f),
            )
            .padding(3.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (!ownerPhotoUrl.isNullOrBlank() && !avatarPhotoFailed) {
            AsyncImage(
                model = ownerPhotoUrl,
                contentDescription = stringResource(R.string.household_profile_photo_cd, ownerName),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape),
                onError = { avatarPhotoFailed = true },
            )
        } else {
            Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = stringResource(R.string.household_profile_photo_cd, ownerName),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxSize(),
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
 * All other pet types (AXOLOTL, CAPYBARA): emoji placeholder until their
 * animated assets are added under assets/animations/<type>/.
 */
@Composable
private fun HouseholdPetAnimation(
    petType: PetType,
    mood: ChorebooMood,
    animationOffsetMs: Long = 0L,
    onTransition: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (petType == PetType.FOX) {
        var phase by remember { mutableStateOf(HouseholdAnimPhase.MOOD) }
        var animationKey by remember { mutableIntStateOf(0) }
        var started by remember { mutableStateOf(animationOffsetMs == 0L) }

        fun transitionTo(nextPhase: HouseholdAnimPhase) {
            if (phase != nextPhase) {
                phase = nextPhase
                animationKey++
                onTransition()
            }
        }

        // If an offset is specified, delay before starting animations
        LaunchedEffect(Unit) {
            if (animationOffsetMs > 0) {
                kotlinx.coroutines.delay(animationOffsetMs)
                started = true
            }
        }

        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            if (started) {
                key(animationKey, phase, mood) {
                    val renderedPhase = phase
                    val (assetPath, iterations) = when (phase) {
                        HouseholdAnimPhase.MOOD -> foxMoodAssetPath(mood) to 1
                        HouseholdAnimPhase.IDLE -> FOX_ANIM_IDLE to FOX_IDLE_ITERATIONS
                    }

                    WebmAnimationView(
                        assetPath = assetPath,
                        iterations = iterations,
                        onComplete = {
                            if (phase != renderedPhase) return@WebmAnimationView

                            when (renderedPhase) {
                                HouseholdAnimPhase.MOOD -> transitionTo(HouseholdAnimPhase.IDLE)
                                HouseholdAnimPhase.IDLE -> transitionTo(HouseholdAnimPhase.MOOD)
                            }
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            } else {
                // Show a blank box while waiting for the animation to start
                Box(modifier = Modifier.fillMaxSize())
            }
        }
    } else if (petType == PetType.PANDA) {
        var phase by remember { mutableStateOf(HouseholdAnimPhase.MOOD) }
        var animationKey by remember { mutableIntStateOf(0) }
        var started by remember { mutableStateOf(animationOffsetMs == 0L) }

        fun transitionTo(nextPhase: HouseholdAnimPhase) {
            if (phase != nextPhase) {
                phase = nextPhase
                animationKey++
                onTransition()
            }
        }

        LaunchedEffect(Unit) {
            if (animationOffsetMs > 0) {
                kotlinx.coroutines.delay(animationOffsetMs)
                started = true
            }
        }

        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            if (started) {
                key(animationKey, phase, mood) {
                    val renderedPhase = phase
                    val (assetPath, iterations) = when (phase) {
                        HouseholdAnimPhase.MOOD -> pandaMoodAssetPath(mood) to 1
                        HouseholdAnimPhase.IDLE -> PANDA_ANIM_IDLE to PANDA_IDLE_ITERATIONS
                    }

                    WebmAnimationView(
                        assetPath = assetPath,
                        iterations = iterations,
                        onComplete = {
                            if (phase != renderedPhase) return@WebmAnimationView

                            when (renderedPhase) {
                                HouseholdAnimPhase.MOOD -> transitionTo(HouseholdAnimPhase.IDLE)
                                HouseholdAnimPhase.IDLE -> transitionTo(HouseholdAnimPhase.MOOD)
                            }
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            } else {
                Box(modifier = Modifier.fillMaxSize())
            }
        }
    } else {
        // Emoji placeholder for AXOLOTL, CAPYBARA until their WebM
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
                .height(6.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = when {
                value < 20 -> MaterialTheme.colorScheme.error
                value < 50 -> MaterialTheme.colorScheme.tertiary
                else -> MaterialTheme.colorScheme.primary
            },
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.72f),
        )
    }
}

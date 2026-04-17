package com.choreboo.app.ui.household

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import com.choreboo.app.ui.components.SnackbarType
import com.choreboo.app.ui.components.showStitchSnackbar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.choreboo.app.R
import com.choreboo.app.domain.model.HouseholdHabitStatus
import com.choreboo.app.domain.model.HouseholdPet
import com.choreboo.app.ui.components.StitchSnackbar
import com.choreboo.app.ui.household.components.HouseholdHabitCard
import com.choreboo.app.ui.household.components.HouseholdPetCard
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HouseholdScreen(
    onNavigateToSettings: () -> Unit = {},
    viewModel: HouseholdViewModel = hiltViewModel(),
) {
    val household by viewModel.currentHousehold.collectAsStateWithLifecycle()
    val pets by viewModel.householdPets.collectAsStateWithLifecycle()
    val habits by viewModel.householdHabits.collectAsStateWithLifecycle()
    val selectedPet by viewModel.selectedPet.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is HouseholdEvent.ShowSnackbar -> {
                    scope.launch { snackbarHostState.showStitchSnackbar(event.message, type = SnackbarType.Info) }
                }
            }
        }
    }

    // ── Member habits popup ──────────────────────────────────────────
    selectedPet?.let { pet ->
        val memberHabits = habits.filter { it.assignedToUid == pet.ownerUid }
        MemberHabitsDialog(
            pet = pet,
            habits = memberHabits,
            onDismiss = { viewModel.clearSelectedPet() },
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refreshData() },
            modifier = Modifier.fillMaxSize(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .padding(top = 16.dp, bottom = 96.dp),
            ) {
                // Header
                Text(
                    text = household?.name ?: stringResource(R.string.household_title_fallback),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (household == null) {
                    EmptyHouseholdState(onInvite = onNavigateToSettings)
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(0.dp),
                        contentPadding = PaddingValues(bottom = 8.dp),
                    ) {
                        // ── Pet grid (up to 5) ───────────────────────────────────
                        if (pets.isEmpty()) {
                            item {
                                QuietHouseholdState()
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        } else {
                            // Cap at 5 pets. Display as a 2-column grid using chunked rows.
                            val displayPets = pets.take(5)
                            val rows = displayPets.chunked(2)
                            items(rows, key = { row -> "pet_row_${row[0].chorebooId}" }) { row ->
                                val rowIndex = rows.indexOf(row)
                                PetGridRow(
                                    row = row,
                                    rowIndex = rowIndex,
                                    onPetClick = { viewModel.selectPet(it) },
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        }

                        // ── Household chores section ─────────────────────────────
                        if (habits.isNotEmpty()) {
                            // Create a map of uid -> photoUrl for quick lookup
                            val memberPhotoMap = pets.associate { pet ->
                                pet.ownerUid to pet.ownerPhotoUrl
                            }

                            item(key = "chores_header") {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = stringResource(R.string.household_chores_section),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            items(habits, key = { habit -> "habit_${habit.habitId}" }) { habit ->
                                HouseholdHabitCard(habit = habit)
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }
            } // end Column
        } // end PullToRefreshBox

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp),
        ) { data -> StitchSnackbar(data) }
    } // end Box
}

// ── Member habits dialog ─────────────────────────────────────────────────

/**
 * Popup shown when the user taps a pet card. Displays the pet owner's name,
 * profile photo, and all household chores they own with today's completion status.
 */
@Composable
private fun MemberHabitsDialog(
    pet: HouseholdPet,
    habits: List<HouseholdHabitStatus>,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.household_close_button))
            }
        },
        icon = {
            var photoFailed by remember(pet.ownerPhotoUrl) { mutableStateOf(false) }

            if (!pet.ownerPhotoUrl.isNullOrBlank() && !photoFailed) {
                AsyncImage(
                    model = pet.ownerPhotoUrl,
                    contentDescription = stringResource(R.string.household_profile_photo_cd, pet.ownerName),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape),
                    onError = { photoFailed = true },
                )
            } else {
                // Fallback: green AccountCircle icon
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = stringResource(R.string.household_profile_photo_cd, pet.ownerName),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp),
                )
            }
        },
        title = {
            Text(
                text = stringResource(R.string.household_chores_title, pet.ownerName),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            if (habits.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "\uD83C\uDF3F",
                        fontSize = 36.sp,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.household_no_chores),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    habits.forEach { habit ->
                        MemberHabitRow(habit = habit)
                    }
                }
            }
        },
    )
}

/**
 * Compact row for a single habit inside the member habits dialog.
 * Shows the emoji icon, title, and a small status indicator.
 */
@Composable
private fun MemberHabitRow(
    habit: HouseholdHabitStatus,
    modifier: Modifier = Modifier,
) {
    val isCompleted = habit.completedByName != null

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Emoji icon
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(
                    if (isCompleted)
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    else
                        MaterialTheme.colorScheme.surfaceContainerHighest,
                ),
        ) {
            Text(
                text = habit.iconName,
                fontSize = 18.sp,
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = habit.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (isCompleted) {
                Text(
                    text = stringResource(R.string.household_done_by, habit.completedByName),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            } else {
                Text(
                    text = stringResource(R.string.household_pending),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * A single row in the 2-column pet grid.
 * If the row has only 1 pet (last row when there's an odd number), that card
 * fills half the width to avoid a stretched single card.
 * 
 * Each pet gets a staggered animation offset based on its position (600ms per pet).
 */
@Composable
private fun PetGridRow(
    row: List<HouseholdPet>,
    rowIndex: Int,
    onPetClick: (HouseholdPet) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        row.forEachIndexed { itemIndex, pet ->
            val globalIndex = rowIndex * 2 + itemIndex
            val animationOffsetMs = globalIndex * 600L
            
            HouseholdPetCard(
                pet = pet,
                onClick = { onPetClick(pet) },
                animationOffsetMs = animationOffsetMs,
                modifier = Modifier.weight(1f),
            )
        }
        // If only 1 item in this row, add an invisible spacer to keep column width
        if (row.size == 1) {
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun QuietHouseholdState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
     ) {
         Column(horizontalAlignment = Alignment.CenterHorizontally) {
             Text(
                 text = "\uD83C\uDFE0",
                 fontSize = 48.sp,
             )
             Spacer(modifier = Modifier.height(12.dp))
             Text(
                 text = stringResource(R.string.household_quiet_title),
                 style = MaterialTheme.typography.bodyLarge,
                 color = MaterialTheme.colorScheme.onSurfaceVariant,
             )
             Text(
                 text = stringResource(R.string.household_quiet_body),
                 style = MaterialTheme.typography.bodyMedium,
                 color = MaterialTheme.colorScheme.onSurfaceVariant,
                 textAlign = TextAlign.Center,
             )
         }
     }
}

@Composable
private fun EmptyHouseholdState(
    onInvite: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Home icon
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(64.dp)
                        .background(
                            MaterialTheme.colorScheme.primaryContainer,
                            RoundedCornerShape(16.dp),
                        ),
                ) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = stringResource(R.string.household_invite_cd),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(32.dp),
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.household_empty_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.household_empty_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onInvite,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                    ),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = stringResource(R.string.household_invite_housemate),
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = stringResource(R.string.household_invite_housemate),
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

package com.example.choreboo_habittrackerfriend.ui.household

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.choreboo_habittrackerfriend.domain.model.HouseholdHabitStatus
import com.example.choreboo_habittrackerfriend.domain.model.HouseholdPet
import com.example.choreboo_habittrackerfriend.ui.household.components.HouseholdHabitCard
import com.example.choreboo_habittrackerfriend.ui.household.components.HouseholdPetCard

@Composable
fun HouseholdScreen(
    onNavigateToSettings: () -> Unit = {},
    viewModel: HouseholdViewModel = hiltViewModel(),
) {
    val household by viewModel.currentHousehold.collectAsStateWithLifecycle()
    val pets by viewModel.householdPets.collectAsStateWithLifecycle()
    val habits by viewModel.householdHabits.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp, bottom = 96.dp),
    ) {
        // Header
        Text(
            text = household?.name ?: "Household",
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
                        PetGridRow(row = row)
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                // ── Household chores section ─────────────────────────────
                if (habits.isNotEmpty()) {
                    item(key = "chores_header") {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Household Chores",
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
    }
}

/**
 * A single row in the 2-column pet grid.
 * If the row has only 1 pet (last row when there's an odd number), that card
 * fills half the width to avoid a stretched single card.
 */
@Composable
private fun PetGridRow(
    row: List<HouseholdPet>,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        row.forEach { pet ->
            HouseholdPetCard(
                pet = pet,
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
                text = "Your household is quiet...",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "Invite someone to see their Choreboo here!",
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
                        contentDescription = "Household",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(32.dp),
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Join the Neighborhood",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Create or join a household to see your housemates' Choreboos and share habits together!",
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
                            contentDescription = "Invite",
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = "Invite Housemate",
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

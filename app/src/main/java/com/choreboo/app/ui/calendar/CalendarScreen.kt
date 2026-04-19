package com.choreboo.app.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.choreboo.app.R
import com.choreboo.app.ui.theme.HeatmapHigh
import com.choreboo.app.ui.theme.HeatmapLow
import com.choreboo.app.ui.theme.XpPurple
import com.choreboo.app.ui.components.ChorebooTopAppBar
import com.choreboo.app.ui.habits.components.getEmojiForIconName
import com.choreboo.app.ui.theme.softGlassSurface
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel = hiltViewModel(),
) {
    val selectedMonth by viewModel.selectedMonth.collectAsStateWithLifecycle()
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val completions by viewModel.completionsForMonth.collectAsStateWithLifecycle()
    val selectedDateLogs by viewModel.selectedDateLogs.collectAsStateWithLifecycle()
    val totalPoints by viewModel.totalPoints.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val profilePhotoUri by viewModel.profilePhotoUri.collectAsStateWithLifecycle()
    val googlePhotoUrl by viewModel.googlePhotoUrl.collectAsStateWithLifecycle()

    val today = remember { LocalDate.now() }
    var todayDate by rememberSaveable(
        stateSaver = Saver<LocalDate, Long>(
            save = { date -> date.toEpochDay() },
            restore = { epoch -> LocalDate.ofEpochDay(epoch) },
        ),
    ) { mutableStateOf(today) }

    // Refresh today's date and ViewModel date state on every screen resume
    // so that the "today" highlight is correct after midnight crossings.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        todayDate = LocalDate.now()
        viewModel.refreshTodayDate()
    }

    // Calendar grid metrics
    val totalDaysWithAny = completions.values.count { it > 0 }
    val daysInMonth = selectedMonth.lengthOfMonth()
    val totalXp = selectedDateLogs.sumOf { it.xpEarned }

    Scaffold(
        topBar = {
            ChorebooTopAppBar(
                profilePhotoUri = profilePhotoUri,
                googlePhotoUrl = googlePhotoUrl,
                totalPoints = totalPoints,
                pointsContentDescription = stringResource(R.string.calendar_points_cd),
            ) {
                Text(
                    stringResource(R.string.calendar_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        },
        containerColor = Color.Transparent,
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refreshData() },
            modifier = Modifier.fillMaxSize(),
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
            // Calendar card
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                ) {
                    Column(
                        modifier = Modifier
                            .softGlassSurface(
                                shape = RoundedCornerShape(16.dp),
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.84f),
                                borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.22f),
                            )
                            .padding(20.dp),
                    ) {
                        // Month navigation
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            IconButton(
                                onClick = { viewModel.previousMonth() },
                                modifier = Modifier
                                    .softGlassSurface(
                                        shape = CircleShape,
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.78f),
                                        borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.18f),
                                    ),
                            ) {
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = stringResource(R.string.calendar_prev_month_cd))
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${selectedMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${selectedMonth.year}",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = stringResource(R.string.calendar_days_complete, totalDaysWithAny, daysInMonth),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                            IconButton(
                                onClick = { viewModel.nextMonth() },
                                modifier = Modifier
                                    .softGlassSurface(
                                        shape = CircleShape,
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.78f),
                                        borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.18f),
                                    ),
                            ) {
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = stringResource(R.string.calendar_next_month_cd))
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Day of week headers
                        Row(modifier = Modifier.fillMaxWidth()) {
                            DayOfWeek.entries.forEach { dow ->
                                Box(
                                    modifier = Modifier.weight(1f),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = dow.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.outline,
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Calendar grid
                        val firstDayOfMonth = selectedMonth.atDay(1)
                        val startOffset = firstDayOfMonth.dayOfWeek.value - 1
                        val totalCells = startOffset + daysInMonth
                        val rows = (totalCells + 6) / 7

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            repeat(rows) { row ->
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    for (col in 0..6) {
                                        val cellIndex = row * 7 + col
                                        val dayNum = cellIndex - startOffset + 1
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .aspectRatio(1f)
                                                .padding(2.dp),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            if (dayNum in 1..daysInMonth) {
                                                val date = selectedMonth.atDay(dayNum)
                                                val isToday = date == todayDate
                                                val isSelected = date == selectedDate
                                                val count = completions[date] ?: 0

                                                val bgColor = when {
                                                    isSelected -> MaterialTheme.colorScheme.primary
                                                    count > 3 -> HeatmapHigh.copy(alpha = 0.3f)
                                                    count > 0 -> HeatmapLow.copy(alpha = 0.3f)
                                                    else -> Color.Transparent
                                                }

                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .clip(RoundedCornerShape(10.dp))
                                                        .background(bgColor)
                                                        .then(
                                                            if (isToday && !isSelected)
                                                                Modifier.border(
                                                                    3.dp,
                                                                    MaterialTheme.colorScheme.primary,
                                                                    RoundedCornerShape(10.dp),
                                                                )
                                                            else Modifier
                                                        )
                                                        .then(
                                                            if (isSelected)
                                                                Modifier.shadow(
                                                                    4.dp,
                                                                    RoundedCornerShape(10.dp),
                                                                    ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                                                )
                                                            else Modifier
                                                        )
                                                        .clickable { viewModel.selectDate(date) },
                                                    contentAlignment = Alignment.Center,
                                                ) {
                                                    Text(
                                                        text = "$dayNum",
                                                        style = MaterialTheme.typography.labelMedium,
                                                        fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
                                                        color = when {
                                                            isSelected -> MaterialTheme.colorScheme.onPrimary
                                                            count == 0 && !isToday -> MaterialTheme.colorScheme.outlineVariant
                                                            else -> MaterialTheme.colorScheme.onSurface
                                                        },
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Legend
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            LegendDot(color = HeatmapHigh)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.calendar_legend_4_plus), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.width(16.dp))
                            LegendDot(color = HeatmapLow)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.calendar_legend_1_3), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.width(16.dp))
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.calendar_legend_none), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // Selected date details
            if (selectedDate != null) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = selectedDate!!.let {
                                "${it.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())}, " +
                                "${it.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())} ${it.dayOfMonth}"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        if (totalXp > 0) {
                            Text(
                                text = stringResource(R.string.calendar_total_xp, totalXp),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = XpPurple,
                            )
                        }
                    }
                }

                if (selectedDateLogs.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .softGlassSurface(
                                    shape = RoundedCornerShape(16.dp),
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.82f),
                                    borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.22f),
                                )
                                .padding(32.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                stringResource(R.string.calendar_no_completions),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                } else {
                    items(selectedDateLogs, key = { it.id }) { log ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.Transparent,
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .softGlassSurface(
                                        shape = RoundedCornerShape(16.dp),
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.82f),
                                        borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.22f),
                                    )
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            text = getEmojiForIconName(log.habitIcon),
                                            fontSize = 24.sp,
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = log.habitTitle,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                        if (log.streakAtCompletion > 1) {
                                            Text(
                                                text = stringResource(R.string.calendar_day_streak, log.streakAtCompletion),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.secondary,
                                            )
                                        }
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50.dp))
                                        .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f))
                                        .padding(horizontal = 10.dp, vertical = 5.dp),
                                ) {
                                    Text(
                                        text = stringResource(R.string.calendar_xp_earned, log.xpEarned),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            } // end LazyColumn
            } // end else (isLoading)
        } // end PullToRefreshBox
    }
}

@Composable
private fun LegendDot(color: Color) {
    Box(
        modifier = Modifier
            .size(12.dp)
            .clip(CircleShape)
            .background(color),
    )
}

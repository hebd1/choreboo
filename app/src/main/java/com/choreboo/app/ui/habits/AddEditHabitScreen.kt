package com.choreboo.app.ui.habits

import android.Manifest
import android.os.Build
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import com.choreboo.app.ui.components.SnackbarType
import com.choreboo.app.ui.components.StitchSnackbar
import com.choreboo.app.ui.components.showStitchSnackbar
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.res.stringResource
import androidx.emoji2.emojipicker.EmojiPickerView
import androidx.hilt.navigation.compose.hiltViewModel
import com.choreboo.app.R
import coil3.compose.AsyncImage
import com.choreboo.app.domain.model.HouseholdMember
import com.choreboo.app.ui.components.ProfileAvatar
import com.choreboo.app.ui.theme.softGlassSurface
import androidx.compose.ui.layout.ContentScale
import java.time.LocalTime

private data class EmojiIcon(val id: String, val emoji: String, val label: String)

// Note: emoji icon labels are loaded inside the composable to use stringResource()
private val emojiIconIds = listOf(
    "emoji_salad" to "🥗" to R.string.emoji_icon_healthy,
    "emoji_water" to "💧" to R.string.emoji_icon_water,
    "emoji_running" to "🏃" to R.string.emoji_icon_running,
    "emoji_book" to "📚" to R.string.emoji_icon_reading,
    "emoji_meditate" to "🧘" to R.string.emoji_icon_meditate,
    "emoji_cleaning" to "🧹" to R.string.emoji_icon_cleaning,
    "emoji_cooking" to "🍳" to R.string.emoji_icon_cooking,
    "emoji_music" to "🎵" to R.string.emoji_icon_music,
    "emoji_sleep" to "😴" to R.string.emoji_icon_sleep,
    "emoji_code" to "💻" to R.string.emoji_icon_coding,
    "emoji_art" to "🎨" to R.string.emoji_icon_art,
    "emoji_strength" to "💪" to R.string.emoji_icon_strength,
    "emoji_yoga" to "🧘" to R.string.emoji_icon_yoga,
    "emoji_walk" to "🚶" to R.string.emoji_icon_walk,
    "emoji_study" to "📖" to R.string.emoji_icon_study,
)

private val daysOfWeek = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")

private val dayDisplayLabels = mapOf(
    "MON" to R.string.day_chip_mon,
    "TUE" to R.string.day_chip_tue,
    "WED" to R.string.day_chip_wed,
    "THU" to R.string.day_chip_thu,
    "FRI" to R.string.day_chip_fri,
    "SAT" to R.string.day_chip_sat,
    "SUN" to R.string.day_chip_sun,
)

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AddEditHabitScreen(
    onNavigateBack: () -> Unit,
    onSavedBack: (isNew: Boolean) -> Unit = { onNavigateBack() },
    onDeletedBack: () -> Unit = { onNavigateBack() },
    viewModel: AddEditHabitViewModel = hiltViewModel(),
) {
    val formState by viewModel.formState.collectAsStateWithLifecycle()
    val isOwner by viewModel.isOwner.collectAsStateWithLifecycle()
    val profilePhotoUri by viewModel.profilePhotoUri.collectAsStateWithLifecycle()
    val googlePhotoUrl by viewModel.googlePhotoUrl.collectAsStateWithLifecycle()
    val householdMembers by viewModel.householdMembers.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showTimePicker by rememberSaveable { mutableStateOf(false) }
    var showEmojiPicker by rememberSaveable { mutableStateOf(false) }
    var showDeleteConfirm by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    
    val emojiIcons = emojiIconIds.map { (idAndEmoji, labelRes) ->
        val (id, emoji) = idAndEmoji
        EmojiIcon(id, emoji, stringResource(labelRes))
     }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is AddEditHabitEvent.Saved -> onSavedBack(event.isNew)
                is AddEditHabitEvent.Deleted -> onDeletedBack()
                is AddEditHabitEvent.ValidationError -> {
                    snackbarHostState.showStitchSnackbar(
                        message = context.getString(event.messageResId),
                        type = SnackbarType.Error,
                    )
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) { data -> StitchSnackbar(data) } },
        containerColor = Color.Transparent,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            // Choreboo branded top bar
            ChorebooTopBar(
                onNavigateBack = onNavigateBack,
                profilePhotoUri = profilePhotoUri,
                googlePhotoUrl = googlePhotoUrl,
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // Title Section
                Text(
                    text = if (formState.isEditing) stringResource(R.string.add_habit_title_edit) else stringResource(R.string.add_habit_title_new),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.add_habit_motivational),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Habit Icon & Name Input
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    // Large icon preview
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .softGlassSurface(
                                shape = RoundedCornerShape(12.dp),
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.82f),
                                borderColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        val selectedIcon = emojiIcons.find { it.id == formState.iconName }
                        Text(
                            text = selectedIcon?.emoji ?: formState.iconName,
                            fontSize = 44.sp,
                        )
                    }

                    // Name input
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                    ) {
                        Text(
                            text = stringResource(R.string.add_habit_name_label),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 0.5.sp,
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = formState.title,
                            onValueChange = { if (it.length <= 100) viewModel.updateTitle(it) },
                            placeholder = { Text(stringResource(R.string.add_habit_name_placeholder)) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                            trailingIcon = if (formState.title.length > 80) {
                                {
                                    Text(
                                        text = stringResource(R.string.add_habit_name_counter_format, formState.title.length),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (formState.title.length >= 100)
                                            MaterialTheme.colorScheme.error
                                        else
                                            MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(end = 8.dp),
                                    )
                                }
                            } else null,
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                                unfocusedBorderColor = Color.Transparent,
                                focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(
                                onDone = { focusManager.clearFocus() },
                            ),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Icon Picker — horizontal scroll
                Text(
                    text = stringResource(R.string.add_habit_pick_icon),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.5.sp,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Spacer(modifier = Modifier.width(4.dp))
                    emojiIconIds.forEach { (idAndEmoji, stringId) ->
                        val (id, emoji) = idAndEmoji
                        val label = stringResource(stringId)
                        val selected = formState.iconName == id
                         Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(
                                    if (selected) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceContainerLow,
                                )
                                .then(
                                    if (selected) Modifier.border(
                                        2.dp,
                                        MaterialTheme.colorScheme.primary,
                                        CircleShape,
                                    )
                                    else Modifier,
                                )
                                .clickable { viewModel.updateIconName(id) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = emoji,
                                fontSize = 28.sp,
                            )
                        }
                    }
                    // Custom emoji "+" button
                    val isCustomEmoji = emojiIconIds.none { (idAndEmoji, _) -> idAndEmoji.first == formState.iconName }
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(
                                if (isCustomEmoji) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceContainerLow,
                            )
                            .then(
                                if (isCustomEmoji) Modifier.border(
                                    2.dp,
                                    MaterialTheme.colorScheme.primary,
                                    CircleShape,
                                )
                                else Modifier,
                            )
                            .clickable { showEmojiPicker = true },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (isCustomEmoji) {
                            Text(
                                text = formState.iconName,
                                fontSize = 28.sp,
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = stringResource(R.string.add_habit_custom_emoji_cd),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Frequency card
                FrequencyCard(
                    frequencyMode = formState.frequencyMode,
                    selectedDays = formState.customDays,
                    onFrequencyModeChange = viewModel::updateFrequencyMode,
                    onDayToggle = viewModel::toggleCustomDay,
                    onMonthlyDayToggle = viewModel::toggleMonthlyDay,
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Difficulty card (consolidates XP reward)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .softGlassSurface(
                            shape = RoundedCornerShape(16.dp),
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.84f),
                            borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.22f),
                        )
                        .padding(16.dp),
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.add_habit_difficulty_label),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.outline,
                            letterSpacing = 1.sp,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                Text(
                                    text = stringResource(R.string.add_habit_lightning_emoji),
                                    fontSize = 24.sp,
                                )
                            }
                            Column(
                                horizontalAlignment = Alignment.End,
                            ) {
                                Text(
                                    text = getDifficultyLabel(formState.difficulty),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(
                                    verticalAlignment = Alignment.Bottom,
                                    horizontalArrangement = Arrangement.End,
                                ) {
                                    Text(
                                        text = "${formState.baseXp}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.tertiary,
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = stringResource(R.string.add_habit_xp_label),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.tertiary,
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Slider(
                            value = formState.difficulty.toFloat(),
                            onValueChange = { viewModel.updateDifficulty(it.toInt()) },
                            valueRange = 1f..3f,
                            steps = 1,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                            ),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Reminder card
                ReminderCard(
                    reminderEnabled = formState.reminderEnabled,
                    reminderTime = formState.reminderTime,
                    onToggle = viewModel::updateReminderEnabled,
                    onTimeClick = { showTimePicker = true },
                )

                if (showTimePicker) {
                    TimePickerDialog(
                        initialTime = formState.reminderTime,
                        onTimeSelected = { time ->
                            viewModel.updateReminderTime(time)
                            showTimePicker = false
                        },
                        onDismiss = { showTimePicker = false },
                    )
                }

                if (showEmojiPicker) {
                    EmojiPickerDialog(
                        onEmojiSelected = { emoji ->
                            viewModel.updateIconName(emoji)
                        },
                        onDismiss = { showEmojiPicker = false },
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Household Habit toggle — only the owner can change this setting.
                // Assignees see the habit as-is and cannot promote/demote it.
                if (isOwner) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .softGlassSurface(
                                shape = RoundedCornerShape(16.dp),
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.84f),
                                borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.22f),
                            )
                            .border(
                                width = 0.dp,
                                color = Color.Transparent,
                                shape = RoundedCornerShape(16.dp),
                            ),
                    ) {
                        // Secondary-container left border accent
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .width(4.dp)
                                .height(56.dp)
                                .background(
                                    MaterialTheme.colorScheme.secondaryContainer,
                                    RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp),
                                ),
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Groups,
                                contentDescription = stringResource(R.string.add_habit_household_habit),
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(24.dp),
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.add_habit_household_habit),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = stringResource(R.string.add_habit_household_visible),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Switch(
                                checked = formState.isHouseholdHabit,
                                onCheckedChange = viewModel::updateIsHouseholdHabit,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    // Assignee picker — shown when Household Habit is on (owner only)
                    AnimatedVisibility(
                        visible = formState.isHouseholdHabit,
                        enter = expandVertically(),
                        exit = shrinkVertically(),
                    ) {
                        Column {
                            AssigneePicker(
                                members = householdMembers,
                                selectedUid = formState.assignedToUid,
                                onSelect = { uid, name -> viewModel.updateAssignedTo(uid, name) },
                            )
                            Spacer(modifier = Modifier.height(28.dp))
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.height(28.dp))
                }

                // CTA buttons
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .softGlassSurface(
                            shape = RoundedCornerShape(16.dp),
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.94f),
                            borderColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.08f),
                        )
                        .clickable { viewModel.saveHabit() },
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.TaskAlt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(20.dp),
                        )
                        Text(
                            text = if (formState.isEditing) stringResource(R.string.add_habit_save_update) else stringResource(R.string.add_habit_save_create),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Cancel button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clickable { onNavigateBack() },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.add_habit_cancel_back),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                // Delete button — only when editing an existing habit
                if (formState.isEditing) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clickable { showDeleteConfirm = true },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(R.string.add_habit_delete_button),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }

    }

    // Delete confirmation dialog — shown outside Scaffold Column to avoid scroll issues
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.pet_delete_habit_title), fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(R.string.add_habit_delete_confirm_body)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    viewModel.deleteHabit()
                }) {
                    Text(stringResource(R.string.pet_delete_button), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(R.string.pet_cancel_button))
                }
            },
        )
    }
}

@Composable
private fun ChorebooTopBar(
    onNavigateBack: () -> Unit,
    profilePhotoUri: String?,
    googlePhotoUrl: String?,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .softGlassSurface(
                shape = RoundedCornerShape(28.dp),
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.8f),
                borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.22f),
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f),
        ) {
            // Profile avatar circle
            ProfileAvatar(
                profilePhotoUri = profilePhotoUri,
                googlePhotoUrl = googlePhotoUrl,
                size = 40.dp,
            )


        }
    }
}

@Composable
private fun FrequencyCard(
    frequencyMode: FrequencyMode,
    selectedDays: List<String>,
    onFrequencyModeChange: (FrequencyMode) -> Unit,
    onDayToggle: (String) -> Unit,
    onMonthlyDayToggle: (Int) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .softGlassSurface(
                shape = RoundedCornerShape(16.dp),
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.84f),
                borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
            )
            .border(
                4.dp,
                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                RoundedCornerShape(16.dp),
            )
            .padding(16.dp),
    ) {
        Column {
            Text(
                text = stringResource(R.string.add_habit_frequency_label),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Frequency mode selector (Weekly / Monthly)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FrequencyMode.entries.forEach { mode ->
                    val isSelected = mode == frequencyMode
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else Color.Transparent,
                            )
                            .clickable { onFrequencyModeChange(mode) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(
                                when (mode) {
                                    FrequencyMode.WEEKLY -> R.string.add_habit_mode_weekly
                                    FrequencyMode.MONTHLY -> R.string.add_habit_mode_monthly
                                }
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Day/Month selector based on mode
            when (frequencyMode) {
                FrequencyMode.WEEKLY -> {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        daysOfWeek.forEach { day ->
                            val selected = day in selectedDays
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50.dp))
                                    .background(
                                        if (selected) MaterialTheme.colorScheme.primary
                                         else MaterialTheme.colorScheme.surfaceContainerHighest,
                                     )
                                     .clickable { onDayToggle(day) }
                                     .padding(horizontal = 12.dp, vertical = 8.dp),
                                 contentAlignment = Alignment.Center,
                             ) {
                                 Text(
                                     text = stringResource(dayDisplayLabels[day] ?: R.string.day_chip_mon),
                                     style = MaterialTheme.typography.labelSmall,
                                     fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                     color = if (selected) MaterialTheme.colorScheme.onPrimary
                                     else MaterialTheme.colorScheme.onSurfaceVariant,
                                 )
                             }
                         }
                     }
                 }
                FrequencyMode.MONTHLY -> {
                    MonthlyDaySelector(
                        selectedDays = selectedDays,
                        onDayToggle = onMonthlyDayToggle,
                    )
                }
            }
        }
    }
}

@Composable
private fun MonthlyDaySelector(
    selectedDays: List<String>,
    onDayToggle: (Int) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
    ) {
        // Preset options
        Text(
            text = stringResource(R.string.add_habit_quick_select),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf(stringResource(R.string.add_habit_monthly_1st) to 1, stringResource(R.string.add_habit_monthly_15th) to 15, stringResource(R.string.add_habit_monthly_last) to 31).forEach { (label, day) ->
                val selected = "D$day" in selectedDays
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(50.dp))
                        .background(
                            if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceContainerHighest,
                        )
                        .clickable { onDayToggle(day) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        color = if (selected) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Full day grid
        Text(
            text = stringResource(R.string.add_habit_pick_days),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(8.dp))

        val columns = 7
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            (1..31).chunked(columns).forEach { daysInRow ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    daysInRow.forEach { day ->
                        val selected = "D$day" in selectedDays
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (selected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceContainerHighest,
                                )
                                .clickable { onDayToggle(day) }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "$day",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                color = if (selected) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                            )
                        }
                    }
                    // Empty space for alignment
                    repeat(columns - daysInRow.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun ReminderCard(
    reminderEnabled: Boolean,
    reminderTime: LocalTime,
    onToggle: (Boolean) -> Unit,
    onTimeClick: () -> Unit,
) {
    // Permission launcher for POST_NOTIFICATIONS (Android 13+)
    var showPermissionDialog by rememberSaveable { mutableStateOf(false) }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        if (isGranted) {
            onToggle(true)
        } else {
            showPermissionDialog = true
        }
    }

    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = { Text(stringResource(R.string.add_habit_notification_permission_title)) },
            text = { Text(stringResource(R.string.add_habit_notification_permission_body)) },
            confirmButton = {
                Button(onClick = { showPermissionDialog = false }) {
                    Text(stringResource(R.string.add_habit_ok_button))
                }
            },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .softGlassSurface(
                shape = RoundedCornerShape(16.dp),
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.84f),
                borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.22f),
            )
            .padding(12.dp),
    ) {
        // Toggle row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
            ) {
                // Alarm icon in circle
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.Alarm,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(20.dp),
                    )
                }

                // Text
                Column {
                    Text(
                        text = stringResource(R.string.add_habit_daily_reminder),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (reminderEnabled) stringResource(R.string.add_habit_reminder_enabled) else stringResource(R.string.add_habit_reminder_not_set),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Toggle switch with permission handling
            Switch(
                checked = reminderEnabled,
                onCheckedChange = { isChecked ->
                    if (isChecked && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        // Request notification permission on Android 13+
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        onToggle(isChecked)
                    }
                },
            )
        }

        // Expandable time selector
        AnimatedVisibility(
            visible = reminderEnabled,
            enter = expandVertically(),
            exit = shrinkVertically(),
        ) {
            Column {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 10.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.15f))
                        .clickable(onClick = onTimeClick)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Default.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = stringResource(R.string.add_habit_remind_at),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = reminderTime.format(
                                java.time.format.DateTimeFormatter.ofPattern("hh:mm a"),
                            ),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = stringResource(R.string.add_habit_change_time_cd),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun getDifficultyLabel(difficulty: Int): String {
    return when (difficulty) {
        1 -> stringResource(R.string.add_habit_difficulty_easy)
        2 -> stringResource(R.string.add_habit_difficulty_medium)
        3 -> stringResource(R.string.add_habit_difficulty_hard)
        else -> stringResource(R.string.add_habit_difficulty_easy)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    initialTime: LocalTime,
    onTimeSelected: (LocalTime) -> Unit,
    onDismiss: () -> Unit,
) {
    val state = rememberTimePickerState(
        initialHour = initialTime.hour,
        initialMinute = initialTime.minute,
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_habit_select_time)) },
        text = {
            TimePicker(state = state)
        },
        confirmButton = {
            Button(
                onClick = {
                    onTimeSelected(LocalTime.of(state.hour, state.minute))
                },
            ) {
                Text(stringResource(R.string.add_habit_ok_button))
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text(stringResource(R.string.add_habit_cancel_time))
            }
        },
    )
}

@Composable
private fun EmojiPickerDialog(    onEmojiSelected: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_habit_custom_emoji_dialog)) },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
            ) {
                AndroidView(
                    factory = { ctx ->
                        EmojiPickerView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT,
                            )
                            setOnEmojiPickedListener { emojiViewItem ->
                                onEmojiSelected(emojiViewItem.emoji)
                                onDismiss()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.add_habit_emoji_cancel))
            }
        },
    )
}

/**
 * Assignee picker card shown when a habit is marked as a Household Habit.
 * Displays all household members as selectable chips. Selecting "Anyone" clears the assignee.
 */
@Composable
private fun AssigneePicker(
    members: List<HouseholdMember>,
    selectedUid: String?,
    onSelect: (uid: String?, name: String?) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .softGlassSurface(
                shape = RoundedCornerShape(16.dp),
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.84f),
                borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.22f),
            )
            .padding(16.dp),
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.PersonOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(20.dp),
                )
            Text(
                text = stringResource(R.string.add_habit_assign_to_label),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.outline,
                letterSpacing = 1.sp,
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.add_habit_assign_to_body),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
            Spacer(modifier = Modifier.height(12.dp))

            // "Anyone" chip
            val anyoneSelected = selectedUid == null
            AssigneeChip(
                label = stringResource(R.string.add_habit_assign_anyone),
                emoji = stringResource(R.string.add_habit_assignee_anyone_emoji),
                isSelected = anyoneSelected,
                onClick = { onSelect(null, null) },
            )

            if (members.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                members.forEach { member ->
                    val isSelected = member.uid == selectedUid
                    AssigneeChip(
                        label = member.displayName,
                        emoji = null,
                        photoUrl = member.photoUrl,
                        isSelected = isSelected,
                        onClick = { onSelect(member.uid, member.displayName) },
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }
    }
}

@Composable
private fun AssigneeChip(
    label: String,
    emoji: String?,
    isSelected: Boolean,
    onClick: () -> Unit,
    photoUrl: String? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceContainerHighest,
            )
            .then(
                if (isSelected) Modifier.border(
                    1.5.dp,
                    MaterialTheme.colorScheme.primary,
                    RoundedCornerShape(12.dp),
                ) else Modifier,
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Avatar: photo > initial circle > emoji
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(
                    if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    else MaterialTheme.colorScheme.surfaceContainerHigh,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (emoji != null) {
                Text(text = emoji, fontSize = 16.sp)
            } else if (photoUrl != null) {
                AsyncImage(
                    model = photoUrl,
                    contentDescription = label,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Text(
                    text = label.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
            else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )

        if (isSelected) {
            Text(
                text = "✓",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

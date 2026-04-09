package com.example.choreboo_habittrackerfriend.ui.util

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.choreboo_habittrackerfriend.R
import com.example.choreboo_habittrackerfriend.domain.model.BadgeDefinition
import com.example.choreboo_habittrackerfriend.domain.model.BackgroundItem
import com.example.choreboo_habittrackerfriend.domain.model.ChorebooMood
import com.example.choreboo_habittrackerfriend.domain.model.ChorebooStage
import com.example.choreboo_habittrackerfriend.domain.model.PetType
import com.example.choreboo_habittrackerfriend.ui.onboarding.BiggestStruggle
import com.example.choreboo_habittrackerfriend.ui.onboarding.UsageIntent

// ------------------------------------------------------------------------------------------------
// ChorebooMood
// ------------------------------------------------------------------------------------------------

@StringRes
fun ChorebooMood.displayNameRes(): Int = when (this) {
    ChorebooMood.HAPPY -> R.string.mood_happy
    ChorebooMood.CONTENT -> R.string.mood_content
    ChorebooMood.HUNGRY -> R.string.mood_hungry
    ChorebooMood.TIRED -> R.string.mood_tired
    ChorebooMood.SAD -> R.string.mood_sad
    ChorebooMood.IDLE -> R.string.mood_idle
}

@Composable
fun ChorebooMood.displayName(): String = stringResource(displayNameRes())

/** "Feeling Loved", "Feeling Good", etc. — used on the Stats screen. */
@StringRes
fun ChorebooMood.feelingLabelRes(): Int = when (this) {
    ChorebooMood.HAPPY -> R.string.mood_feeling_loved
    ChorebooMood.CONTENT -> R.string.mood_feeling_good
    ChorebooMood.HUNGRY -> R.string.mood_feeling_hungry
    ChorebooMood.TIRED -> R.string.mood_feeling_tired
    ChorebooMood.SAD -> R.string.mood_feeling_sad
    ChorebooMood.IDLE -> R.string.mood_feeling_idle
}

@Composable
fun ChorebooMood.feelingLabel(): String = stringResource(feelingLabelRes())

// ------------------------------------------------------------------------------------------------
// ChorebooStage
// ------------------------------------------------------------------------------------------------

@StringRes
fun ChorebooStage.displayNameRes(): Int = when (this) {
    ChorebooStage.EGG -> R.string.stage_egg
    ChorebooStage.BABY -> R.string.stage_baby
    ChorebooStage.CHILD -> R.string.stage_child
    ChorebooStage.TEEN -> R.string.stage_teen
    ChorebooStage.ADULT -> R.string.stage_adult
    ChorebooStage.LEGENDARY -> R.string.stage_legendary
}

@Composable
fun ChorebooStage.displayName(): String = stringResource(displayNameRes())

// ------------------------------------------------------------------------------------------------
// PetType
// ------------------------------------------------------------------------------------------------

@StringRes
fun PetType.displayNameRes(): Int = when (this) {
    PetType.FOX -> R.string.pet_type_fox
    PetType.AXOLOTL -> R.string.pet_type_axolotl
    PetType.CAPYBARA -> R.string.pet_type_capybara
    PetType.PANDA -> R.string.pet_type_panda
}

@Composable
fun PetType.displayName(): String = stringResource(displayNameRes())

// ------------------------------------------------------------------------------------------------
// BadgeDefinition
// ------------------------------------------------------------------------------------------------

@StringRes
fun BadgeDefinition.displayNameRes(): Int = when (this) {
    BadgeDefinition.FIRST_STEP -> R.string.badge_first_step
    BadgeDefinition.ON_FIRE -> R.string.badge_on_fire
    BadgeDefinition.WEEK_WARRIOR -> R.string.badge_week_warrior
    BadgeDefinition.FORTNIGHT_FORCE -> R.string.badge_fortnight_force
    BadgeDefinition.MONTHLY_MASTER -> R.string.badge_monthly_master
    BadgeDefinition.HABIT_STARTER -> R.string.badge_habit_starter
    BadgeDefinition.HABIT_COLLECTOR -> R.string.badge_habit_collector
    BadgeDefinition.CENTURION -> R.string.badge_centurion
    BadgeDefinition.XP_MACHINE -> R.string.badge_xp_machine
    BadgeDefinition.LEGENDARY_GRINDER -> R.string.badge_legendary_grinder
}

@Composable
fun BadgeDefinition.localizedDisplayName(): String = stringResource(displayNameRes())

@StringRes
fun BadgeDefinition.descriptionRes(): Int = when (this) {
    BadgeDefinition.FIRST_STEP -> R.string.badge_first_step_desc
    BadgeDefinition.ON_FIRE -> R.string.badge_on_fire_desc
    BadgeDefinition.WEEK_WARRIOR -> R.string.badge_week_warrior_desc
    BadgeDefinition.FORTNIGHT_FORCE -> R.string.badge_fortnight_force_desc
    BadgeDefinition.MONTHLY_MASTER -> R.string.badge_monthly_master_desc
    BadgeDefinition.HABIT_STARTER -> R.string.badge_habit_starter_desc
    BadgeDefinition.HABIT_COLLECTOR -> R.string.badge_habit_collector_desc
    BadgeDefinition.CENTURION -> R.string.badge_centurion_desc
    BadgeDefinition.XP_MACHINE -> R.string.badge_xp_machine_desc
    BadgeDefinition.LEGENDARY_GRINDER -> R.string.badge_legendary_grinder_desc
}

@Composable
fun BadgeDefinition.localizedDescription(): String = stringResource(descriptionRes())

// ------------------------------------------------------------------------------------------------
// BackgroundItem
// ------------------------------------------------------------------------------------------------

@StringRes
fun BackgroundItem.labelRes(): Int = when (id) {
    "default" -> R.string.bg_label_default
    "meadow" -> R.string.bg_label_meadow
    "sunset" -> R.string.bg_label_sunset
    "ocean" -> R.string.bg_label_ocean
    "night_sky" -> R.string.bg_label_night_sky
    "autumn" -> R.string.bg_label_autumn
    "cherry_blossom" -> R.string.bg_label_cherry_blossom
    "galaxy" -> R.string.bg_label_galaxy
    "underwater" -> R.string.bg_label_underwater
    "aurora" -> R.string.bg_label_aurora
    // Fallback: unknown future backgrounds return the default label
    else -> R.string.bg_label_default
}

/** Returns true when this background has a known localized label resource. */
fun BackgroundItem.hasLocalizedLabel(): Boolean = id in setOf(
    "default", "meadow", "sunset", "ocean", "night_sky",
    "autumn", "cherry_blossom", "galaxy", "underwater", "aurora",
)

@Composable
fun BackgroundItem.localizedLabel(): String =
    stringResource(labelRes())

// ------------------------------------------------------------------------------------------------
// UsageIntent
// ------------------------------------------------------------------------------------------------

@StringRes
fun UsageIntent.labelRes(): Int = when (this) {
    UsageIntent.CHORES_WITH_FRIENDS -> R.string.usage_intent_chores
    UsageIntent.HABITS_ROUTINES -> R.string.usage_intent_habits
    UsageIntent.TASK_MANAGER -> R.string.usage_intent_tasks
}

@Composable
fun UsageIntent.localizedLabel(): String = stringResource(labelRes())

// ------------------------------------------------------------------------------------------------
// BiggestStruggle
// ------------------------------------------------------------------------------------------------

@StringRes
fun BiggestStruggle.labelRes(): Int = when (this) {
    BiggestStruggle.MOTIVATION -> R.string.struggle_motivation
    BiggestStruggle.TIME -> R.string.struggle_time
    BiggestStruggle.REMEMBERING -> R.string.struggle_remembering
    BiggestStruggle.GETTING_STARTED -> R.string.struggle_getting_started
}

@Composable
fun BiggestStruggle.localizedLabel(): String = stringResource(labelRes())

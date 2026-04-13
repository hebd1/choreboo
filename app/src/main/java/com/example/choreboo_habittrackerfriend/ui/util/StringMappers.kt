package com.example.choreboo_habittrackerfriend.ui.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
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
// Context utilities
// ------------------------------------------------------------------------------------------------

/**
 * Walks the [Context] wrapper chain to find the underlying [Activity].
 * Returns null if no Activity is found (e.g. in a service or application context).
 * Prefer this over a direct `context as? Activity` cast, which fails when the context
 * is wrapped (e.g. [ContextThemeWrapper]).
 */
fun Context.findActivity(): Activity? {
    var ctx: Context = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

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

/**
 * Resolves the [BadgeDefinition.iconName] string key to its corresponding Material
 * [ImageVector]. Kept in the UI layer so the domain model stays free of Compose dependencies.
 */
fun BadgeDefinition.resolveIcon(): ImageVector = when (iconName) {
    "CheckCircle" -> Icons.Default.CheckCircle
    "LocalFireDepartment" -> Icons.Default.LocalFireDepartment
    "EmojiEvents" -> Icons.Default.EmojiEvents
    "Whatshot" -> Icons.Default.Whatshot
    "WorkspacePremium" -> Icons.Default.WorkspacePremium
    "FitnessCenter" -> Icons.Default.FitnessCenter
    "Inventory2" -> Icons.Default.Inventory2
    "Stars" -> Icons.Default.Stars
    "Bolt" -> Icons.Default.Bolt
    "Diamond" -> Icons.Default.Diamond
    else -> Icons.Default.Stars
}

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

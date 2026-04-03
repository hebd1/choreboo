package com.example.choreboo_habittrackerfriend.domain.model

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
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Represents a badge that the user has earned (or can earn).
 */
data class Badge(
    val definition: BadgeDefinition,
    val isUnlocked: Boolean,
)

/**
 * All available badges in the app. Each badge has a display name, icon, description,
 * and a category that determines which stat is checked for unlocking.
 */
enum class BadgeDefinition(
    val displayName: String,
    val icon: ImageVector,
    val description: String,
    val category: BadgeCategory,
    val threshold: Int,
) {
    FIRST_STEP(
        displayName = "First Step",
        icon = Icons.Default.CheckCircle,
        description = "Complete your first habit",
        category = BadgeCategory.TOTAL_COMPLETIONS,
        threshold = 1,
    ),
    ON_FIRE(
        displayName = "On Fire",
        icon = Icons.Default.LocalFireDepartment,
        description = "Reach a 3-day streak",
        category = BadgeCategory.MAX_STREAK,
        threshold = 3,
    ),
    WEEK_WARRIOR(
        displayName = "Week Warrior",
        icon = Icons.Default.EmojiEvents,
        description = "Reach a 7-day streak",
        category = BadgeCategory.MAX_STREAK,
        threshold = 7,
    ),
    FORTNIGHT_FORCE(
        displayName = "Fortnight Force",
        icon = Icons.Default.Whatshot,
        description = "Reach a 14-day streak",
        category = BadgeCategory.MAX_STREAK,
        threshold = 14,
    ),
    MONTHLY_MASTER(
        displayName = "Monthly Master",
        icon = Icons.Default.WorkspacePremium,
        description = "Reach a 30-day streak",
        category = BadgeCategory.MAX_STREAK,
        threshold = 30,
    ),
    HABIT_STARTER(
        displayName = "Habit Starter",
        icon = Icons.Default.FitnessCenter,
        description = "Create 3 habits",
        category = BadgeCategory.HABITS_CREATED,
        threshold = 3,
    ),
    HABIT_COLLECTOR(
        displayName = "Habit Collector",
        icon = Icons.Default.Inventory2,
        description = "Create 5 habits",
        category = BadgeCategory.HABITS_CREATED,
        threshold = 5,
    ),
    CENTURION(
        displayName = "Centurion",
        icon = Icons.Default.Stars,
        description = "Earn 100 total XP",
        category = BadgeCategory.LIFETIME_XP,
        threshold = 100,
    ),
    XP_MACHINE(
        displayName = "XP Machine",
        icon = Icons.Default.Bolt,
        description = "Earn 500 total XP",
        category = BadgeCategory.LIFETIME_XP,
        threshold = 500,
    ),
    LEGENDARY_GRINDER(
        displayName = "Legendary Grinder",
        icon = Icons.Default.Diamond,
        description = "Earn 1000 total XP",
        category = BadgeCategory.LIFETIME_XP,
        threshold = 1000,
    ),
}

enum class BadgeCategory {
    TOTAL_COMPLETIONS,
    MAX_STREAK,
    HABITS_CREATED,
    LIFETIME_XP,
}

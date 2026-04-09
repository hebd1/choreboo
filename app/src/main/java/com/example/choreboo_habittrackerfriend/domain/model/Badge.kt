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
 * All available badges in the app. Each badge has an icon and a category that determines
 * which stat is checked for unlocking. Display name and description are managed as string resources.
 */
enum class BadgeDefinition(
    val icon: ImageVector,
    val category: BadgeCategory,
    val threshold: Int,
) {
    FIRST_STEP(
        icon = Icons.Default.CheckCircle,
        category = BadgeCategory.TOTAL_COMPLETIONS,
        threshold = 1,
    ),
    ON_FIRE(
        icon = Icons.Default.LocalFireDepartment,
        category = BadgeCategory.MAX_STREAK,
        threshold = 3,
    ),
    WEEK_WARRIOR(
        icon = Icons.Default.EmojiEvents,
        category = BadgeCategory.MAX_STREAK,
        threshold = 7,
    ),
    FORTNIGHT_FORCE(
        icon = Icons.Default.Whatshot,
        category = BadgeCategory.MAX_STREAK,
        threshold = 14,
    ),
    MONTHLY_MASTER(
        icon = Icons.Default.WorkspacePremium,
        category = BadgeCategory.MAX_STREAK,
        threshold = 30,
    ),
    HABIT_STARTER(
        icon = Icons.Default.FitnessCenter,
        category = BadgeCategory.HABITS_CREATED,
        threshold = 3,
    ),
    HABIT_COLLECTOR(
        icon = Icons.Default.Inventory2,
        category = BadgeCategory.HABITS_CREATED,
        threshold = 5,
    ),
    CENTURION(
        icon = Icons.Default.Stars,
        category = BadgeCategory.LIFETIME_XP,
        threshold = 100,
    ),
    XP_MACHINE(
        icon = Icons.Default.Bolt,
        category = BadgeCategory.LIFETIME_XP,
        threshold = 500,
    ),
    LEGENDARY_GRINDER(
        icon = Icons.Default.Diamond,
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

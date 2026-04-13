package com.example.choreboo_habittrackerfriend.domain.model

/**
 * Represents a badge that the user has earned (or can earn).
 */
data class Badge(
    val definition: BadgeDefinition,
    val isUnlocked: Boolean,
)

/**
 * All available badges in the app. Each badge has an iconName (a string key resolved to an
 * [androidx.compose.ui.graphics.vector.ImageVector] by [com.example.choreboo_habittrackerfriend.ui.util.resolveIcon])
 * and a category that determines which stat is checked for unlocking.
 * Display name and description are managed as string resources.
 *
 * The [iconName] field is a plain String to avoid coupling the domain layer to Compose/Material
 * icon APIs. Call [com.example.choreboo_habittrackerfriend.ui.util.resolveIcon] in UI code to
 * obtain the actual [androidx.compose.ui.graphics.vector.ImageVector].
 */
enum class BadgeDefinition(
    val iconName: String,
    val category: BadgeCategory,
    val threshold: Int,
) {
    FIRST_STEP(
        iconName = "CheckCircle",
        category = BadgeCategory.TOTAL_COMPLETIONS,
        threshold = 1,
    ),
    ON_FIRE(
        iconName = "LocalFireDepartment",
        category = BadgeCategory.MAX_STREAK,
        threshold = 3,
    ),
    WEEK_WARRIOR(
        iconName = "EmojiEvents",
        category = BadgeCategory.MAX_STREAK,
        threshold = 7,
    ),
    FORTNIGHT_FORCE(
        iconName = "Whatshot",
        category = BadgeCategory.MAX_STREAK,
        threshold = 14,
    ),
    MONTHLY_MASTER(
        iconName = "WorkspacePremium",
        category = BadgeCategory.MAX_STREAK,
        threshold = 30,
    ),
    HABIT_STARTER(
        iconName = "FitnessCenter",
        category = BadgeCategory.HABITS_CREATED,
        threshold = 3,
    ),
    HABIT_COLLECTOR(
        iconName = "Inventory2",
        category = BadgeCategory.HABITS_CREATED,
        threshold = 5,
    ),
    CENTURION(
        iconName = "Stars",
        category = BadgeCategory.LIFETIME_XP,
        threshold = 100,
    ),
    XP_MACHINE(
        iconName = "Bolt",
        category = BadgeCategory.LIFETIME_XP,
        threshold = 500,
    ),
    LEGENDARY_GRINDER(
        iconName = "Diamond",
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


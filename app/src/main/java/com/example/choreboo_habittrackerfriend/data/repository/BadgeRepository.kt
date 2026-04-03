package com.example.choreboo_habittrackerfriend.data.repository

import com.example.choreboo_habittrackerfriend.data.local.dao.HabitDao
import com.example.choreboo_habittrackerfriend.data.local.dao.HabitLogDao
import com.example.choreboo_habittrackerfriend.data.datastore.UserPreferences
import com.example.choreboo_habittrackerfriend.domain.model.Badge
import com.example.choreboo_habittrackerfriend.domain.model.BadgeCategory
import com.example.choreboo_habittrackerfriend.domain.model.BadgeDefinition
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BadgeRepository @Inject constructor(
    private val habitDao: HabitDao,
    private val habitLogDao: HabitLogDao,
    private val userPreferences: UserPreferences,
) {

    /**
     * Emits the full list of badges with their unlocked state.
     * Combines four reactive sources: total completions, max streak ever,
     * total habits created, and lifetime XP.
     */
    fun getAllBadges(): Flow<List<Badge>> = combine(
        habitLogDao.getTotalCompletionCount(),
        habitLogDao.getMaxStreakEver(),
        habitDao.getTotalHabitCount(),
        userPreferences.totalLifetimeXp,
    ) { totalCompletions, maxStreak, habitsCreated, lifetimeXp ->
        BadgeDefinition.entries.map { definition ->
            val statValue = when (definition.category) {
                BadgeCategory.TOTAL_COMPLETIONS -> totalCompletions
                BadgeCategory.MAX_STREAK -> maxStreak
                BadgeCategory.HABITS_CREATED -> habitsCreated
                BadgeCategory.LIFETIME_XP -> lifetimeXp
            }
            Badge(
                definition = definition,
                isUnlocked = statValue >= definition.threshold,
            )
        }
    }

    /** Convenience flow that emits the count of unlocked badges. */
    fun getEarnedBadgeCount(): Flow<Int> = getAllBadges()
        .map { badges -> badges.count { it.isUnlocked } }
}

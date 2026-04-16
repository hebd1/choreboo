package com.choreboo.app.data.repository

import com.choreboo.app.data.local.dao.HabitDao
import com.choreboo.app.data.local.dao.HabitLogDao
import com.choreboo.app.data.datastore.UserPreferences
import com.choreboo.app.domain.model.Badge
import com.choreboo.app.domain.model.BadgeCategory
import com.choreboo.app.domain.model.BadgeDefinition
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BadgeRepository @Inject constructor(
    private val habitDao: HabitDao,
    private val habitLogDao: HabitLogDao,
    private val userPreferences: UserPreferences,
    private val firebaseAuth: FirebaseAuth,
) {

    /**
     * Emits the full list of badges with their unlocked state.
     * Combines four reactive sources: total completions, max streak ever,
     * total habits created, and lifetime XP.
     *
     * All badge stats are scoped to the current user's UID so household members'
     * synced logs and habits are not counted toward this user's badges.
     */
    fun getAllBadges(): Flow<List<Badge>> {
        val uid = firebaseAuth.currentUser?.uid
            ?: return flowOf(emptyList())

        return combine(
            habitLogDao.getTotalCompletionCount(uid),
            habitLogDao.getMaxStreakEver(uid),
            habitDao.getTotalHabitCount(uid),
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
    }

    /** Convenience flow that emits the count of unlocked badges. */
    fun getEarnedBadgeCount(): Flow<Int> = getAllBadges()
        .map { badges -> badges.count { it.isUnlocked } }
}

package com.example.choreboo_habittrackerfriend.data.repository

import android.util.Log
import com.example.choreboo_habittrackerfriend.data.datastore.UserPreferences
import com.example.choreboo_habittrackerfriend.data.local.dao.ChorebooDao
import com.example.choreboo_habittrackerfriend.data.local.entity.ChorebooEntity
import com.example.choreboo_habittrackerfriend.dataconnect.ChorebooConnector
import com.example.choreboo_habittrackerfriend.dataconnect.execute
import com.example.choreboo_habittrackerfriend.dataconnect.instance
import com.example.choreboo_habittrackerfriend.domain.model.ChorebooStage
import com.example.choreboo_habittrackerfriend.domain.model.ChorebooStats
import com.example.choreboo_habittrackerfriend.domain.model.PetType
import com.google.firebase.Timestamp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.Date
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max

private const val TAG = "ChorebooRepository"

data class XpResult(
    val levelsGained: Int = 0,
    val newLevel: Int = 1,
    val evolved: Boolean = false,
    val newStage: ChorebooStage? = null,
)

@Singleton
class ChorebooRepository @Inject constructor(
    private val chorebooDao: ChorebooDao,
) {
    private val connector by lazy { ChorebooConnector.instance }

    /** Fire-and-forget scope for silent write-through calls. */
    private val writeScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun getChoreboo(): Flow<ChorebooStats?> = chorebooDao.getChoreboo().map { it?.toDomain() }

    suspend fun getChorebooSync(): ChorebooStats? = chorebooDao.getChorebooSync()?.toDomain()

    suspend fun getOrCreateChoreboo(name: String = "Choreboo", petType: PetType = PetType.FOX): ChorebooEntity {
        val existing = chorebooDao.getChorebooSync()
        if (existing != null) return existing

        val newChoreboo = ChorebooEntity(
            name = name,
            stage = ChorebooStage.EGG.name,
            hunger = 10,
            happiness = 80,
            energy = 80,
            petType = petType.name,
        )
        val id = chorebooDao.insertChoreboo(newChoreboo)
        val created = newChoreboo.copy(id = id)

        // Write-through: insert into Data Connect
        try {
            val result = connector.insertChoreboo.execute(
                name = name,
                stage = ChorebooStage.EGG.name,
                level = 1,
                xp = 0,
                hunger = 10,
                happiness = 80,
                energy = 80,
                petType = petType.name,
                lastInteractionAt = Timestamp(Date(created.lastInteractionAt)),
            )
            val remoteId = result.data.choreboo_insert.id.toString()
            chorebooDao.updateChoreboo(created.copy(remoteId = remoteId))
            Log.d(TAG, "Created choreboo in cloud: $remoteId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync new choreboo to cloud", e)
        }

        return created
    }

    suspend fun applyStatDecay() {
        val choreboo = chorebooDao.getChorebooSync() ?: return
        val now = System.currentTimeMillis()

        // If sleeping, keep lastInteractionAt current to prevent decay accumulation
        if (choreboo.sleepUntil > now) {
            // Pet is still sleeping, update lastInteractionAt to now to prevent decay
            chorebooDao.updateChoreboo(choreboo.copy(lastInteractionAt = now))
            return
        }

        // If sleep just expired, set lastInteractionAt to sleepUntil time for proper decay calc
        val decayFromTime = if (choreboo.sleepUntil > 0 && choreboo.sleepUntil <= now) {
            choreboo.sleepUntil
        } else {
            choreboo.lastInteractionAt
        }

        val hoursSinceInteraction = (now - decayFromTime) / (1000 * 60 * 60)

        if (hoursSinceInteraction <= 0) {
            // Clear sleep if it's expired
            if (choreboo.sleepUntil > 0 && choreboo.sleepUntil <= now) {
                val updated = choreboo.copy(sleepUntil = 0, lastInteractionAt = now)
                chorebooDao.updateChoreboo(updated)
                // Sync cleared sleep state to cloud
                syncSleepToCloud(updated)
            }
            return
        }

        val decayAmount = hoursSinceInteraction.toInt().coerceAtMost(50) // cap decay
        val updated = choreboo.copy(
            hunger = max(0, choreboo.hunger - decayAmount),
            happiness = max(0, choreboo.happiness - (decayAmount / 2)),
            energy = max(0, choreboo.energy - (decayAmount / 2)),
            lastInteractionAt = now,
            sleepUntil = 0, // Clear sleep when it expires
        )
        chorebooDao.updateChoreboo(updated)

        // Write-through: update stats in Data Connect (fire-and-forget)
        writeScope.launch { syncStatsToCloud(updated) }
        // Sync cleared sleep state to cloud (fire-and-forget)
        if (choreboo.sleepUntil > 0) {
            writeScope.launch { syncSleepToCloud(updated) }
        }
    }

    suspend fun addXp(amount: Int): XpResult {
        val choreboo = chorebooDao.getChorebooSync() ?: return XpResult()
        val oldLevel = choreboo.level
        val oldStage = try { ChorebooStage.valueOf(choreboo.stage) } catch (_: Exception) { ChorebooStage.EGG }
        var newXp = choreboo.xp + amount
        var newLevel = choreboo.level

        // Level up logic
        var xpNeeded = newLevel * 50
        while (newXp >= xpNeeded) {
            newXp -= xpNeeded
            newLevel++
            xpNeeded = newLevel * 50
        }

        // Stage evolution
        val totalXpEarned = (1 until newLevel).sumOf { it * 50 } + newXp
        val newStage = ChorebooStage.fromTotalXp(totalXpEarned)

        val updated = choreboo.copy(
            xp = newXp,
            level = newLevel,
            stage = newStage.name,
            lastInteractionAt = System.currentTimeMillis(),
        )
        chorebooDao.updateChoreboo(updated)

        // Write-through: update XP in Data Connect (fire-and-forget)
        updated.remoteId?.let { remoteId ->
            writeScope.launch {
                try {
                    connector.updateChorebooXp.execute(
                        chorebooId = UUID.fromString(remoteId),
                        level = newLevel,
                        xp = newXp,
                        stage = newStage.name,
                    )
                    Log.d(TAG, "Synced XP to cloud: level=$newLevel, xp=$newXp")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to sync XP to cloud", e)
                }
            }
        }

        return XpResult(
            levelsGained = newLevel - oldLevel,
            newLevel = newLevel,
            evolved = newStage != oldStage,
            newStage = if (newStage != oldStage) newStage else null,
        )
    }

    /** Manual feed from pet screen: +20 hunger, costs 10 points (already deducted by caller). */
    suspend fun feedChoreboo() {
        val choreboo = chorebooDao.getChorebooSync() ?: return
        val updated = choreboo.copy(
            hunger = (choreboo.hunger + 20).coerceAtMost(100),
            lastInteractionAt = System.currentTimeMillis(),
        )
        chorebooDao.updateChoreboo(updated)

        // Write-through: update stats in Data Connect (fire-and-forget)
        writeScope.launch { syncStatsToCloud(updated) }
    }

    /** Put pet to sleep for 24 hours -- freezes all stat decay during sleep. */
    suspend fun putToSleep() {
        val choreboo = chorebooDao.getChorebooSync() ?: return
        val now = System.currentTimeMillis()
        val sleepUntilTime = now + (24 * 60 * 60 * 1000) // 24 hours from now
        val updated = choreboo.copy(
            sleepUntil = sleepUntilTime,
            lastInteractionAt = now,
        )
        chorebooDao.updateChoreboo(updated)

        // Write-through: update sleep in Data Connect (fire-and-forget)
        updated.remoteId?.let { remoteId ->
            writeScope.launch {
                try {
                    connector.updateChorebooSleep.execute(
                        chorebooId = UUID.fromString(remoteId),
                    ) {
                        sleepUntil = Timestamp(Date(sleepUntilTime))
                    }
                    Log.d(TAG, "Synced sleep to cloud")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to sync sleep to cloud", e)
                }
            }
        }
    }

    /**
     * Auto-feed: called silently after habit completion.
     * If hunger < 30 AND user has >= 10 points, deduct 10 points and add +20 hunger.
     * No animation triggered -- purely background operation.
     */
    suspend fun autoFeedIfNeeded(userPreferences: UserPreferences) {
        val choreboo = chorebooDao.getChorebooSync() ?: return
        if (choreboo.hunger >= 30) return
        val points = userPreferences.totalPoints.first()
        if (points < 10) return
        val deducted = userPreferences.deductPoints(10)
        if (deducted) {
            val updated = choreboo.copy(
                hunger = (choreboo.hunger + 20).coerceAtMost(100),
                lastInteractionAt = System.currentTimeMillis(),
            )
            chorebooDao.updateChoreboo(updated)

            // Write-through (fire-and-forget)
            writeScope.launch { syncStatsToCloud(updated) }
        }
    }

    suspend fun updateName(name: String) {
        val choreboo = chorebooDao.getChorebooSync() ?: return
        val updated = choreboo.copy(name = name)
        chorebooDao.updateChoreboo(updated)

        // Write-through: full update to Data Connect (fire-and-forget)
        updated.remoteId?.let { remoteId ->
            writeScope.launch {
                try {
                    connector.updateChorebooFull.execute(
                        chorebooId = UUID.fromString(remoteId),
                        name = name,
                        stage = updated.stage,
                        level = updated.level,
                        xp = updated.xp,
                        hunger = updated.hunger,
                        happiness = updated.happiness,
                        energy = updated.energy,
                        petType = updated.petType,
                        lastInteractionAt = Timestamp(Date(updated.lastInteractionAt)),
                    ) {
                        sleepUntil = if (updated.sleepUntil > 0) Timestamp(Date(updated.sleepUntil)) else null
                    }
                    Log.d(TAG, "Synced name update to cloud")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to sync name update to cloud", e)
                }
            }
        }
    }

    /**
     * Sync stats (hunger, happiness, energy, lastInteractionAt) to Data Connect.
     */
    private suspend fun syncStatsToCloud(entity: ChorebooEntity) {
        entity.remoteId?.let { remoteId ->
            try {
                connector.updateChorebooStats.execute(
                    chorebooId = UUID.fromString(remoteId),
                    hunger = entity.hunger,
                    happiness = entity.happiness,
                    energy = entity.energy,
                    lastInteractionAt = Timestamp(Date(entity.lastInteractionAt)),
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to sync stats to cloud", e)
            }
        }
    }

    /**
     * Sync sleepUntil state to Data Connect (used when sleep expires during decay).
     */
    private suspend fun syncSleepToCloud(entity: ChorebooEntity) {
        entity.remoteId?.let { remoteId ->
            try {
                connector.updateChorebooSleep.execute(
                    chorebooId = UUID.fromString(remoteId),
                ) {
                    sleepUntil = if (entity.sleepUntil > 0) Timestamp(Date(entity.sleepUntil)) else null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to sync sleep state to cloud", e)
            }
        }
    }

    /**
     * Pull choreboo from Data Connect and merge into Room (cloud wins).
     * Called once after successful authentication.
     */
    suspend fun syncFromCloud() {
        try {
            val result = connector.getMyChoreboo.execute()
            val cloudPet = result.data.choreboos.firstOrNull()
            if (cloudPet == null) {
                Log.d(TAG, "No choreboo found in cloud — skipping sync")
                return
            }

            val remoteId = cloudPet.id.toString()
            val existing = chorebooDao.getChorebooByRemoteId(remoteId)
                ?: chorebooDao.getChorebooSync()

            val lastInteractionMs = cloudPet.lastInteractionAt.toDate().time
            val createdAtMs = cloudPet.createdAt.toDate().time
            val sleepUntilMs = cloudPet.sleepUntil?.toDate()?.time ?: 0L

            val entity = ChorebooEntity(
                id = existing?.id ?: 0,
                name = cloudPet.name,
                stage = cloudPet.stage,
                level = cloudPet.level,
                xp = cloudPet.xp,
                hunger = cloudPet.hunger,
                happiness = cloudPet.happiness,
                energy = cloudPet.energy,
                petType = cloudPet.petType,
                lastInteractionAt = lastInteractionMs,
                createdAt = createdAtMs,
                sleepUntil = sleepUntilMs,
                ownerUid = cloudPet.owner.id,
                remoteId = remoteId,
            )

            if (existing != null) {
                chorebooDao.updateChoreboo(entity)
            } else {
                chorebooDao.insertChoreboo(entity)
            }
            Log.d(TAG, "Synced choreboo from cloud: $remoteId (level=${cloudPet.level})")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync choreboo from cloud", e)
            throw e
        }
    }

    /**
     * Clear all local choreboo data — used for sign-out cleanup.
     */
    suspend fun clearLocalData() {
        chorebooDao.deleteAllChoreboos()
    }
}

private fun ChorebooEntity.toDomain() = ChorebooStats(
    id = id,
    name = name,
    stage = try { ChorebooStage.valueOf(stage) } catch (_: Exception) { ChorebooStage.EGG },
    level = level,
    xp = xp,
    hunger = hunger,
    happiness = happiness,
    energy = energy,
    petType = try { PetType.valueOf(petType) } catch (_: Exception) { PetType.FOX },
    lastInteractionAt = lastInteractionAt,
    createdAt = createdAt,
    sleepUntil = sleepUntil,
)

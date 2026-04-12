package com.example.choreboo_habittrackerfriend.data.repository

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
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import java.util.Date
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.roundToInt

private const val CLOUD_TIMEOUT_MS = 5000L
private const val SLEEP_DURATION_MS = 24 * 60 * 60 * 1000L // 24 hours

/**
 * D2: Retry delays for write-through failures. 3 attempts total:
 * 1st attempt immediate, 2nd after 1 s, 3rd after 3 s.
 */
private val CHOREBOO_WRITE_THROUGH_RETRY_DELAYS_MS = listOf(1_000L, 3_000L)

data class XpResult(
    val levelsGained: Int = 0,
    val newLevel: Int = 1,
    val evolved: Boolean = false,
    val newStage: ChorebooStage? = null,
)

@Singleton
class ChorebooRepository @Inject constructor(
    private val chorebooDao: ChorebooDao,
    private val userRepository: UserRepository,
) {
    private val connector by lazy { ChorebooConnector.instance }

    /** Fire-and-forget scope for silent write-through calls. */
    @Volatile
    private var writeScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Prevents concurrent auto-feed attempts from both passing the hunger/points check. */
    private val autoFeedMutex = Mutex()

    /**
     * Cancel all pending write-through coroutines and create a fresh scope.
     * Called on sign-out and account reset to prevent stale writes executing after
     * the user's data has been cleared.
     *
     * Thread-safety: must be called from the main thread (SettingsViewModel/ResetRepository
     * always invoke this from viewModelScope which is confined to Main). The cancel() and
     * scope reassignment are therefore sequentially ordered — no new coroutines can be
     * launched on the old scope after this returns because all callers hold a reference to
     * the *current* writeScope at launch time.
     */
    fun cancelPendingWrites() {
        writeScope.coroutineContext[Job]?.cancel()
        writeScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }

    /**
     * D2: Execute [block] with up to 3 attempts (1 immediate + 2 retries with backoff).
     * Returns true on success, false after all attempts fail.
     * Callers set pendingSync=true before calling, clear it after this returns.
     */
    private suspend fun retryWithBackoff(tag: String, block: suspend () -> Unit): Boolean {
        var attempt = 0
        while (true) {
            try {
                block()
                return true
            } catch (e: Exception) {
                if (attempt < CHOREBOO_WRITE_THROUGH_RETRY_DELAYS_MS.size) {
                    val delayMs = CHOREBOO_WRITE_THROUGH_RETRY_DELAYS_MS[attempt]
                    Timber.w(e, "Write-through [$tag] attempt ${attempt + 1} failed, retrying in ${delayMs}ms")
                    delay(delayMs)
                    attempt++
                } else {
                    Timber.e(e, "Write-through [$tag] exhausted all retries")
                    return false
                }
            }
        }
    }

    fun getChoreboo(): Flow<ChorebooStats?> = chorebooDao.getChoreboo().map { it?.toDomain() }

    suspend fun getChorebooSync(): ChorebooStats? = chorebooDao.getChorebooSync()?.toDomain()

    /** Returns just the name of the local choreboo, or null if none exists. */
    suspend fun getChorebooNameSync(): String? = chorebooDao.getChorebooSync()?.name

    suspend fun getOrCreateChoreboo(name: String = "Choreboo", petType: PetType = PetType.FOX): ChorebooEntity {
        require(name.isNotBlank()) { "Choreboo name must not be blank" }
        require(name.length <= 20) { "Choreboo name must be 20 characters or fewer, was ${name.length}" }

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

        // Write-through: insert into Data Connect (fire-and-forget)
        writeScope.launch {
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
                Timber.d("Created choreboo in cloud: %s", remoteId)
            } catch (e: Exception) {
                Timber.e(e, "Failed to sync new choreboo to cloud")
            }
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

        // Use floating-point division so sub-hour decay (≥ 30 min rounds to 1) is applied.
        // Integer division would give 0 for elapsed < 1 hour, causing stats to never decay
        // for users who open the app frequently.
        val hoursSinceInteraction = (now - decayFromTime).toFloat() / (1000f * 60f * 60f)

        if (hoursSinceInteraction < 0.01f) {
            // Clear sleep if it's expired
            if (choreboo.sleepUntil > 0 && choreboo.sleepUntil <= now) {
                val updated = choreboo.copy(sleepUntil = 0, lastInteractionAt = now)
                chorebooDao.updateChoreboo(updated)
                // Sync cleared sleep state to cloud
                syncSleepToCloud(updated)
            }
            return
        }

        val decayAmount = hoursSinceInteraction.roundToInt().coerceAtMost(50) // cap decay
        val updated = choreboo.copy(
            hunger = max(0, choreboo.hunger - decayAmount),
            happiness = max(0, choreboo.happiness - (decayAmount / 2)),
            energy = max(0, choreboo.energy - (decayAmount / 2)),
            lastInteractionAt = now,
            sleepUntil = 0, // Clear sleep when it expires
        )
        chorebooDao.updateChoreboo(updated)

        // Write-through: update stats in Data Connect (fire-and-forget)
        // D2: Mark pendingSync so a concurrent cloud-wins sync doesn't overwrite the decayed stats
        // before the write-through reaches the cloud.
        if (updated.remoteId != null) {
            chorebooDao.markPendingSync(updated.id)
            writeScope.launch {
                retryWithBackoff("applyStatDecayStats:${updated.id}") { syncStatsToCloud(updated) }
                // Sync cleared sleep state to cloud if sleep just expired
                if (choreboo.sleepUntil > 0) {
                    retryWithBackoff("applyStatDecaySleep:${updated.id}") { syncSleepToCloud(updated) }
                }
                chorebooDao.clearPendingSync(updated.id)
            }
        }
    }

    suspend fun addXp(amount: Int): XpResult {
        require(amount > 0) { "XP amount must be positive, was $amount" }

        val choreboo = chorebooDao.getChorebooSync() ?: return XpResult()
        val oldLevel = choreboo.level
        val oldStage = try { ChorebooStage.valueOf(choreboo.stage) } catch (e: Exception) { Timber.w(e, "Unknown ChorebooStage value: ${choreboo.stage}"); ChorebooStage.EGG }
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
        // D2: Set pendingSync=true to prevent cloud-wins sync from overwriting the new XP
        // before the write-through reaches the cloud.
        updated.remoteId?.let { remoteId ->
            chorebooDao.markPendingSync(updated.id)
            writeScope.launch {
                val success = retryWithBackoff("updateChorebooXp:${updated.id}") {
                    connector.updateChorebooXp.execute(
                        chorebooId = UUID.fromString(remoteId),
                        level = newLevel,
                        xp = newXp,
                        stage = newStage.name,
                    )
                }
                if (success) {
                    Timber.d("Synced XP to cloud: level=%d, xp=%d", newLevel, newXp)
                }
                chorebooDao.clearPendingSync(updated.id)
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
        // D2: Mark pendingSync so a concurrent sync doesn't overwrite the hunger increase.
        if (updated.remoteId != null) {
            chorebooDao.markPendingSync(updated.id)
            writeScope.launch {
                retryWithBackoff("feedChoreboo:${updated.id}") { syncStatsToCloud(updated) }
                chorebooDao.clearPendingSync(updated.id)
            }
        }
    }

    /** Put pet to sleep for 24 hours -- freezes all stat decay during sleep. */
    suspend fun putToSleep() {
        val choreboo = chorebooDao.getChorebooSync() ?: return
        val now = System.currentTimeMillis()
        val sleepUntilTime = now + SLEEP_DURATION_MS
        val updated = choreboo.copy(
            sleepUntil = sleepUntilTime,
            lastInteractionAt = now,
        )
        chorebooDao.updateChoreboo(updated)

        // Write-through: update sleep in Data Connect (fire-and-forget)
        // D2: Mark pendingSync so a concurrent sync doesn't overwrite the sleep state.
        updated.remoteId?.let { remoteId ->
            chorebooDao.markPendingSync(updated.id)
            writeScope.launch {
                val success = retryWithBackoff("putToSleep:${updated.id}") {
                    connector.updateChorebooSleep.execute(
                        chorebooId = UUID.fromString(remoteId),
                    ) {
                        sleepUntil = Timestamp(Date(sleepUntilTime))
                    }
                }
                if (success) {
                    Timber.d("Synced sleep to cloud")
                }
                chorebooDao.clearPendingSync(updated.id)
            }
        }
    }

    /**
     * Auto-feed: called silently after habit completion.
     * If hunger < 30 AND user has >= 10 points, deduct 10 points and add +20 hunger.
     * No animation triggered -- purely background operation.
     */
    suspend fun autoFeedIfNeeded(userPreferences: UserPreferences) {
        autoFeedMutex.withLock {
            val choreboo = chorebooDao.getChorebooSync() ?: return@withLock
            if (choreboo.hunger >= 30) return@withLock
            val points = userPreferences.totalPoints.first()
            if (points < 10) return@withLock
            val deducted = userPreferences.deductPoints(10)
            if (deducted) {
                val updated = choreboo.copy(
                    hunger = (choreboo.hunger + 20).coerceAtMost(100),
                    lastInteractionAt = System.currentTimeMillis(),
                )
                chorebooDao.updateChoreboo(updated)

                // Write-through: sync pet stats (fire-and-forget)
                // D2: Mark pendingSync so a concurrent sync doesn't overwrite the hunger increase.
                if (updated.remoteId != null) {
                    chorebooDao.markPendingSync(updated.id)
                    writeScope.launch {
                        retryWithBackoff("autoFeedStats:${updated.id}") { syncStatsToCloud(updated) }
                        chorebooDao.clearPendingSync(updated.id)
                    }
                }

                // Write-through: sync deducted points so cloud stays current
                writeScope.launch {
                    val newPoints = userPreferences.totalPoints.first()
                    val newLifetimeXp = userPreferences.totalLifetimeXp.first()
                    userRepository.syncPointsToCloud(newPoints, newLifetimeXp)
                }
            }
        }
    }

    suspend fun updateName(name: String) {
        require(name.isNotBlank()) { "Choreboo name must not be blank" }
        require(name.length <= 20) { "Choreboo name must be 20 characters or fewer, was ${name.length}" }

        val choreboo = chorebooDao.getChorebooSync() ?: return
        val updated = choreboo.copy(name = name)
        chorebooDao.updateChoreboo(updated)

        // Write-through: full update to Data Connect (fire-and-forget)
        // D2: Mark pendingSync so a concurrent sync doesn't overwrite the new name.
        updated.remoteId?.let { remoteId ->
            chorebooDao.markPendingSync(updated.id)
            writeScope.launch {
                val success = retryWithBackoff("updateChorebooName:${updated.id}") {
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
                        backgroundId = updated.backgroundId
                    }
                }
                if (success) {
                    Timber.d("Synced name update to cloud")
                }
                chorebooDao.clearPendingSync(updated.id)
            }
        }
    }

    /**
     * Update the background for this user's Choreboo locally and write-through to cloud.
     * Pass null (or [BACKGROUND_DEFAULT_ID]) to revert to the free mood-gradient default.
     */
    suspend fun updateBackground(backgroundId: String?) {
        val choreboo = chorebooDao.getChorebooSync() ?: return
        // Store null for "default" so cloud schema stays clean
        val cloudId = if (backgroundId == com.example.choreboo_habittrackerfriend.domain.model.BACKGROUND_DEFAULT_ID) null else backgroundId
        val updated = choreboo.copy(backgroundId = cloudId)
        chorebooDao.updateChoreboo(updated)

        // D2: Mark pendingSync so a concurrent sync doesn't overwrite the background change.
        updated.remoteId?.let { remoteId ->
            chorebooDao.markPendingSync(updated.id)
            writeScope.launch {
                val success = retryWithBackoff("updateBackground:${updated.id}") {
                    connector.updateChorebooBackground.execute(
                        chorebooId = UUID.fromString(remoteId),
                    ) {
                        this.backgroundId = cloudId
                    }
                }
                if (success) {
                    Timber.d("Synced background update to cloud: %s", cloudId)
                }
                chorebooDao.clearPendingSync(updated.id)
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
                Timber.e(e, "Failed to sync stats to cloud")
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
                Timber.e(e, "Failed to sync sleep state to cloud")
            }
        }
    }

    /**
     * Pull choreboo from Data Connect and merge into Room (cloud wins).
     * Called once after successful authentication.
     */
    suspend fun syncFromCloud() {
        try {
            val result = withTimeoutOrNull(CLOUD_TIMEOUT_MS) { connector.getMyChoreboo.execute() }
            if (result == null) {
                Timber.w("syncFromCloud: timed out")
                return
            }
            val cloudPet = result.data.choreboos.firstOrNull()
            if (cloudPet == null) {
                Timber.d("No choreboo found in cloud — skipping sync")
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
                backgroundId = cloudPet.backgroundId,
            )

            if (existing != null) {
                // D2: Skip overwriting if a write-through is in flight. The local state is
                // ahead of cloud; overwriting it would revert the user's change.
                if (existing.pendingSync) {
                    Timber.d("syncFromCloud: skipping pendingSync choreboo remoteId=$remoteId")
                    return
                }
                chorebooDao.updateChoreboo(entity)
            } else {
                chorebooDao.insertChoreboo(entity)
            }
            Timber.d("Synced choreboo from cloud: %s (level=%d)", remoteId, cloudPet.level)
        } catch (e: Exception) {
            Timber.e(e, "Failed to sync choreboo from cloud")
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
    stage = try { ChorebooStage.valueOf(stage) } catch (e: Exception) { Timber.w(e, "Unknown ChorebooStage value: $stage"); ChorebooStage.EGG },
    level = level,
    xp = xp,
    hunger = hunger,
    happiness = happiness,
    energy = energy,
    petType = try { PetType.valueOf(petType) } catch (e: Exception) { Timber.w(e, "Unknown PetType value: $petType"); PetType.FOX },
    lastInteractionAt = lastInteractionAt,
    createdAt = createdAt,
    sleepUntil = sleepUntil,
    backgroundId = backgroundId,
)

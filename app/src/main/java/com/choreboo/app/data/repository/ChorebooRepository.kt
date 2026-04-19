package com.choreboo.app.data.repository

import com.choreboo.app.data.datastore.UserPreferences
import com.choreboo.app.data.local.dao.ChorebooDao
import com.choreboo.app.data.local.entity.ChorebooEntity
import com.choreboo.app.dataconnect.ChorebooConnector
import com.choreboo.app.dataconnect.execute
import com.choreboo.app.dataconnect.instance
import com.choreboo.app.domain.model.ChorebooStage
import com.choreboo.app.domain.model.ChorebooStats
import com.choreboo.app.domain.model.PetType
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
private const val SLEEP_DURATION_MS = 24 * 60 * 60 * 1000L
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

    @Volatile
    private var writeScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val autoFeedMutex = Mutex()

    fun cancelPendingWrites() {
        writeScope.coroutineContext[Job]?.cancel()
        writeScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }

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

    fun getChoreboo(): Flow<ChorebooStats?> = chorebooDao.getActiveChoreboo().map { it?.toDomain() }

    fun getAllChoreboos(): Flow<List<ChorebooStats>> = chorebooDao.getAllChoreboos().map { entities ->
        entities.map { it.toDomain() }
    }

    suspend fun getChorebooSync(): ChorebooStats? = chorebooDao.getActiveChorebooSync()?.toDomain()

    suspend fun getAllChoreboosSync(): List<ChorebooStats> = chorebooDao.getAllChoreboosSync().map { it.toDomain() }

    suspend fun getChorebooNameSync(): String? = chorebooDao.getActiveChorebooSync()?.name

    suspend fun hasAnyChoreboo(): Boolean = chorebooDao.getAllChoreboosSync().isNotEmpty()

    suspend fun hasPetType(petType: PetType): Boolean = chorebooDao.getChorebooByPetType(petType.name) != null

    suspend fun getChorebooForPetType(petType: PetType): ChorebooStats? =
        chorebooDao.getChorebooByPetType(petType.name)?.toDomain()

    suspend fun ensureActiveChoreboo(): ChorebooStats? {
        val active = chorebooDao.getActiveChorebooSync()
        if (active != null) return active.toDomain()

        val first = chorebooDao.getAllChoreboosSync().firstOrNull() ?: return null
        chorebooDao.setActiveChoreboo(first.id)
        syncActiveChorebooToCloud(first.remoteId)
        return first.copy(isActive = true).toDomain()
    }

    suspend fun getOrCreateChoreboo(name: String = "Choreboo", petType: PetType = PetType.FOX): ChorebooStats {
        return createOrActivatePetType(name, petType)
    }

    suspend fun createOrActivatePetType(name: String, petType: PetType): ChorebooStats {
        val trimmed = name.trim()
        require(trimmed.isNotBlank()) { "Choreboo name must not be blank" }
        require(trimmed.length <= 20) { "Choreboo name must be 20 characters or fewer, was ${trimmed.length}" }

        val existing = chorebooDao.getChorebooByPetType(petType.name)
        if (existing != null) {
            if (!existing.isActive) {
                chorebooDao.setActiveChoreboo(existing.id)
                syncActiveChorebooToCloud(existing.remoteId)
            }
            return existing.copy(isActive = true).toDomain()
        }

        val ownerUid = userRepository.getCurrentUid()
        val newChoreboo = ChorebooEntity(
            name = trimmed,
            stage = ChorebooStage.EGG.name,
            hunger = 10,
            happiness = 80,
            energy = 80,
            petType = petType.name,
            ownerUid = ownerUid,
            isActive = true,
        )

        chorebooDao.clearActiveChoreboo()
        val id = chorebooDao.insertChoreboo(newChoreboo)
        val created = newChoreboo.copy(id = id)

        writeScope.launch {
            retryWithBackoff("insertChoreboo:${created.id}") {
                val result = connector.insertChoreboo.execute(
                    name = trimmed,
                    stage = ChorebooStage.EGG.name,
                    level = 1,
                    xp = 0,
                    hunger = 10,
                    happiness = 80,
                    energy = 80,
                    petType = petType.name,
                    lastInteractionAt = Timestamp(Date(created.lastInteractionAt)),
                ) {
                    sleepUntil = null
                }

                val remoteId = result.data.choreboo_insert.id.toString()
                val synced = created.copy(remoteId = remoteId)
                chorebooDao.updateChoreboo(synced)
                syncActiveChorebooToCloud(remoteId)
                Timber.d("Created choreboo in cloud: %s", remoteId)
            }
        }

        return created.toDomain()
    }

    suspend fun switchActiveChoreboo(localId: Long) {
        val target = chorebooDao.getChorebooById(localId) ?: return
        if (target.isActive) return

        chorebooDao.setActiveChoreboo(localId)
        syncActiveChorebooToCloud(target.remoteId)
    }

    suspend fun switchActiveChoreboo(petType: PetType) {
        val target = chorebooDao.getChorebooByPetType(petType.name) ?: return
        switchActiveChoreboo(target.id)
    }

    suspend fun renameChoreboo(id: Long, name: String) {
        val trimmed = name.trim()
        require(trimmed.isNotBlank()) { "Choreboo name must not be blank" }
        require(trimmed.length <= 20) { "Choreboo name must be 20 characters or fewer, was ${trimmed.length}" }

        val choreboo = chorebooDao.getChorebooById(id) ?: return
        val updated = choreboo.copy(name = trimmed)
        chorebooDao.updateChoreboo(updated)
        syncFullChoreboo(updated, "renameChoreboo:${updated.id}")
    }

    suspend fun updateName(name: String) {
        val active = chorebooDao.getActiveChorebooSync() ?: return
        renameChoreboo(active.id, name)
    }

    suspend fun applyStatDecay() {
        val choreboo = chorebooDao.getActiveChorebooSync() ?: return
        val now = System.currentTimeMillis()

        if (choreboo.sleepUntil > now) {
            chorebooDao.updateChoreboo(choreboo.copy(lastInteractionAt = now))
            return
        }

        val decayFromTime = if (choreboo.sleepUntil > 0 && choreboo.sleepUntil <= now) {
            choreboo.sleepUntil
        } else {
            choreboo.lastInteractionAt
        }

        val hoursSinceInteraction = (now - decayFromTime).toFloat() / (1000f * 60f * 60f)
        if (hoursSinceInteraction < 0.01f) {
            if (choreboo.sleepUntil > 0 && choreboo.sleepUntil <= now) {
                val updated = choreboo.copy(sleepUntil = 0, lastInteractionAt = now)
                chorebooDao.updateChoreboo(updated)
                syncSleepToCloud(updated)
            }
            return
        }

        val decayAmount = hoursSinceInteraction.roundToInt().coerceAtMost(50)
        val updated = choreboo.copy(
            hunger = max(0, choreboo.hunger - decayAmount),
            happiness = max(0, choreboo.happiness - (decayAmount / 2)),
            energy = max(0, choreboo.energy - (decayAmount / 2)),
            lastInteractionAt = now,
            sleepUntil = 0,
        )
        chorebooDao.updateChoreboo(updated)

        if (updated.remoteId != null) {
            chorebooDao.markPendingSync(updated.id)
            writeScope.launch {
                retryWithBackoff("applyStatDecayStats:${updated.id}") { syncStatsToCloud(updated) }
                if (choreboo.sleepUntil > 0) {
                    retryWithBackoff("applyStatDecaySleep:${updated.id}") { syncSleepToCloud(updated) }
                }
                chorebooDao.clearPendingSync(updated.id)
            }
        }
    }

    suspend fun addXp(amount: Int): XpResult {
        require(amount > 0) { "XP amount must be positive, was $amount" }

        val choreboo = chorebooDao.getActiveChorebooSync() ?: return XpResult()
        val oldLevel = choreboo.level
        val oldStage = try {
            ChorebooStage.valueOf(choreboo.stage)
        } catch (e: Exception) {
            Timber.w(e, "Unknown ChorebooStage value: ${choreboo.stage}")
            ChorebooStage.EGG
        }

        var newXp = choreboo.xp + amount
        var newLevel = choreboo.level
        var xpNeeded = newLevel * 50
        while (newXp >= xpNeeded) {
            newXp -= xpNeeded
            newLevel++
            xpNeeded = newLevel * 50
        }

        val totalXpEarned = (1 until newLevel).sumOf { it * 50 } + newXp
        val newStage = ChorebooStage.fromTotalXp(totalXpEarned)

        val updated = choreboo.copy(
            xp = newXp,
            level = newLevel,
            stage = newStage.name,
            lastInteractionAt = System.currentTimeMillis(),
        )
        chorebooDao.updateChoreboo(updated)

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

    suspend fun feedChoreboo() {
        val choreboo = chorebooDao.getActiveChorebooSync() ?: return
        val updated = choreboo.copy(
            hunger = (choreboo.hunger + 20).coerceAtMost(100),
            lastInteractionAt = System.currentTimeMillis(),
        )
        chorebooDao.updateChoreboo(updated)

        if (updated.remoteId != null) {
            chorebooDao.markPendingSync(updated.id)
            writeScope.launch {
                retryWithBackoff("feedChoreboo:${updated.id}") { syncStatsToCloud(updated) }
                chorebooDao.clearPendingSync(updated.id)
            }
        }
    }

    suspend fun putToSleep() {
        val choreboo = chorebooDao.getActiveChorebooSync() ?: return
        val now = System.currentTimeMillis()
        val sleepUntilTime = now + SLEEP_DURATION_MS
        val updated = choreboo.copy(
            sleepUntil = sleepUntilTime,
            lastInteractionAt = now,
        )
        chorebooDao.updateChoreboo(updated)

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

    suspend fun autoFeedIfNeeded(userPreferences: UserPreferences) {
        autoFeedMutex.withLock {
            val choreboo = chorebooDao.getActiveChorebooSync() ?: return@withLock
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

                if (updated.remoteId != null) {
                    chorebooDao.markPendingSync(updated.id)
                    writeScope.launch {
                        retryWithBackoff("autoFeedStats:${updated.id}") { syncStatsToCloud(updated) }
                        chorebooDao.clearPendingSync(updated.id)
                    }
                }

                writeScope.launch {
                    val newPoints = userPreferences.totalPoints.first()
                    val newLifetimeXp = userPreferences.totalLifetimeXp.first()
                    userRepository.syncPointsToCloud(newPoints, newLifetimeXp)
                }
            }
        }
    }

    suspend fun updateBackground(backgroundId: String?) {
        val choreboo = chorebooDao.getActiveChorebooSync() ?: return
        val cloudId = if (backgroundId == com.choreboo.app.domain.model.BACKGROUND_DEFAULT_ID) null else backgroundId
        val updated = choreboo.copy(backgroundId = cloudId)
        chorebooDao.updateChoreboo(updated)

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

    suspend fun syncFromCloud() {
        try {
            val cloudUser = withTimeoutOrNull(CLOUD_TIMEOUT_MS) { connector.getCurrentUser.execute() }
            if (cloudUser == null) {
                Timber.w("syncFromCloud: user query timed out")
                return
            }
            val activeRemoteId = cloudUser.data.user?.activeChoreboo?.id?.toString()

            val result = withTimeoutOrNull(CLOUD_TIMEOUT_MS) { connector.getMyChoreboos.execute() }
            if (result == null) {
                Timber.w("syncFromCloud: choreboos query timed out")
                return
            }

            val cloudPets = result.data.choreboos
            if (cloudPets.isEmpty()) {
                chorebooDao.deleteAllChoreboos()
                Timber.d("No choreboos found in cloud — cleared local cache")
                return
            }

            val remoteIds = cloudPets.map { it.id.toString() }
            cloudPets.forEach { cloudPet ->
                val remoteId = cloudPet.id.toString()
                val existing = chorebooDao.getChorebooByRemoteId(remoteId)
                    ?: chorebooPetByOwnerAndType(cloudPet.owner.id, cloudPet.petType)

                if (existing?.pendingSync == true) {
                    Timber.d("syncFromCloud: skipping pendingSync choreboo remoteId=%s", remoteId)
                    return@forEach
                }

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
                    lastInteractionAt = cloudPet.lastInteractionAt.toDate().time,
                    createdAt = cloudPet.createdAt.toDate().time,
                    sleepUntil = cloudPet.sleepUntil?.toDate()?.time ?: 0L,
                    ownerUid = cloudPet.owner.id,
                    remoteId = remoteId,
                    isActive = remoteId == activeRemoteId,
                    backgroundId = cloudPet.backgroundId,
                )

                if (existing != null) {
                    chorebooDao.updateChoreboo(entity)
                } else {
                    chorebooDao.insertChoreboo(entity)
                }
            }

            chorebooDao.deleteRemoteChoreboosNotIn(remoteIds)

            val resolvedActiveRemoteId = activeRemoteId ?: remoteIds.firstOrNull()
            if (resolvedActiveRemoteId != null) {
                val activeEntity = chorebooDao.getChorebooByRemoteId(resolvedActiveRemoteId)
                if (activeEntity != null) {
                    chorebooDao.setActiveChoreboo(activeEntity.id)
                    if (activeRemoteId == null) {
                        syncActiveChorebooToCloud(resolvedActiveRemoteId)
                    }
                }
            }

            Timber.d("Synced %d choreboos from cloud", cloudPets.size)
        } catch (e: Exception) {
            Timber.e(e, "Failed to sync choreboos from cloud")
            throw e
        }
    }

    suspend fun clearLocalData() {
        chorebooDao.deleteAllChoreboos()
    }

    private suspend fun chorebooPetByOwnerAndType(ownerUid: String, petType: String): ChorebooEntity? {
        return chorebooDao.getChorebooByOwnerAndPetType(ownerUid, petType)
    }

    private suspend fun syncFullChoreboo(entity: ChorebooEntity, tag: String) {
        entity.remoteId?.let { remoteId ->
            chorebooDao.markPendingSync(entity.id)
            writeScope.launch {
                val success = retryWithBackoff(tag) {
                    connector.updateChorebooFull.execute(
                        chorebooId = UUID.fromString(remoteId),
                        name = entity.name,
                        stage = entity.stage,
                        level = entity.level,
                        xp = entity.xp,
                        hunger = entity.hunger,
                        happiness = entity.happiness,
                        energy = entity.energy,
                        petType = entity.petType,
                        lastInteractionAt = Timestamp(Date(entity.lastInteractionAt)),
                    ) {
                        sleepUntil = if (entity.sleepUntil > 0) Timestamp(Date(entity.sleepUntil)) else null
                        backgroundId = entity.backgroundId
                    }
                }
                if (success) {
                    Timber.d("Synced full choreboo update to cloud")
                }
                chorebooDao.clearPendingSync(entity.id)
            }
        }
    }

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

    private fun syncActiveChorebooToCloud(remoteId: String?) {
        writeScope.launch {
            try {
                if (remoteId == null) {
                    connector.clearActiveChoreboo.execute()
                } else {
                    connector.setActiveChoreboo.execute {
                        chorebooId = UUID.fromString(remoteId)
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to sync active choreboo to cloud")
            }
        }
    }
}

private fun ChorebooEntity.toDomain() = ChorebooStats(
    id = id,
    name = name,
    stage = try {
        ChorebooStage.valueOf(stage)
    } catch (e: Exception) {
        Timber.w(e, "Unknown ChorebooStage value: $stage")
        ChorebooStage.EGG
    },
    level = level,
    xp = xp,
    hunger = hunger,
    happiness = happiness,
    energy = energy,
    petType = try {
        PetType.valueOf(petType)
    } catch (e: Exception) {
        Timber.w(e, "Unknown PetType value: $petType")
        PetType.FOX
    },
    lastInteractionAt = lastInteractionAt,
    createdAt = createdAt,
    sleepUntil = sleepUntil,
    isActive = isActive,
    backgroundId = backgroundId,
)

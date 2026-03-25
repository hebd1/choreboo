package com.example.weeboo_habittrackerfriend.data.repository

import com.example.weeboo_habittrackerfriend.data.local.dao.WeebooDao
import com.example.weeboo_habittrackerfriend.data.local.entity.WeebooEntity
import com.example.weeboo_habittrackerfriend.domain.model.WeebooStage
import com.example.weeboo_habittrackerfriend.domain.model.WeebooStats
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max

data class XpResult(
    val levelsGained: Int = 0,
    val newLevel: Int = 1,
    val evolved: Boolean = false,
    val newStage: WeebooStage? = null,
)

@Singleton
class WeebooRepository @Inject constructor(
    private val weebooDao: WeebooDao,
) {
    fun getWeeboo(): Flow<WeebooStats?> = weebooDao.getWeeboo().map { it?.toDomain() }

    suspend fun getWeebooSync(): WeebooStats? = weebooDao.getWeebooSync()?.toDomain()

    suspend fun getOrCreateWeeboo(name: String = "Weeboo"): WeebooEntity {
        val existing = weebooDao.getWeebooSync()
        if (existing != null) return existing

        val newWeeboo = WeebooEntity(
            name = name,
            stage = WeebooStage.EGG.name,
            hunger = 80,
            happiness = 80,
            energy = 80,
        )
        val id = weebooDao.insertWeeboo(newWeeboo)
        return newWeeboo.copy(id = id)
    }

    suspend fun applyStatDecay() {
        val weeboo = weebooDao.getWeebooSync() ?: return
        val now = System.currentTimeMillis()
        val hoursSinceInteraction = (now - weeboo.lastInteractionAt) / (1000 * 60 * 60)

        if (hoursSinceInteraction <= 0) return

        val decayAmount = hoursSinceInteraction.toInt().coerceAtMost(50) // cap decay
        val updated = weeboo.copy(
            hunger = max(0, weeboo.hunger - decayAmount),
            happiness = max(0, weeboo.happiness - (decayAmount / 2)),
            energy = max(0, weeboo.energy - (decayAmount / 2)),
            lastInteractionAt = now,
        )
        weebooDao.updateWeeboo(updated)
    }

    suspend fun addXp(amount: Int): XpResult {
        val weeboo = weebooDao.getWeebooSync() ?: return XpResult()
        val oldLevel = weeboo.level
        val oldStage = try { WeebooStage.valueOf(weeboo.stage) } catch (_: Exception) { WeebooStage.EGG }
        var newXp = weeboo.xp + amount
        var newLevel = weeboo.level

        // Level up logic
        var xpNeeded = newLevel * 50
        while (newXp >= xpNeeded) {
            newXp -= xpNeeded
            newLevel++
            xpNeeded = newLevel * 50
        }

        // Stage evolution
        val totalXpEarned = (1 until newLevel).sumOf { it * 50 } + newXp
        val newStage = WeebooStage.fromTotalXp(totalXpEarned)

        val updated = weeboo.copy(
            xp = newXp,
            level = newLevel,
            stage = newStage.name,
            lastInteractionAt = System.currentTimeMillis(),
        )
        weebooDao.updateWeeboo(updated)

        return XpResult(
            levelsGained = newLevel - oldLevel,
            newLevel = newLevel,
            evolved = newStage != oldStage,
            newStage = if (newStage != oldStage) newStage else null,
        )
    }

    suspend fun feedWeeboo(stat: String, value: Int) {
        val weeboo = weebooDao.getWeebooSync() ?: return
        val updated = when (stat.uppercase()) {
            "HUNGER" -> weeboo.copy(hunger = (weeboo.hunger + value).coerceAtMost(100))
            "HAPPINESS" -> weeboo.copy(happiness = (weeboo.happiness + value).coerceAtMost(100))
            "ENERGY" -> weeboo.copy(energy = (weeboo.energy + value).coerceAtMost(100))
            else -> weeboo
        }.copy(lastInteractionAt = System.currentTimeMillis())
        weebooDao.updateWeeboo(updated)
    }

    suspend fun updateName(name: String) {
        val weeboo = weebooDao.getWeebooSync() ?: return
        weebooDao.updateWeeboo(weeboo.copy(name = name))
    }
}

private fun WeebooEntity.toDomain() = WeebooStats(
    id = id,
    name = name,
    stage = try { WeebooStage.valueOf(stage) } catch (_: Exception) { WeebooStage.EGG },
    level = level,
    xp = xp,
    hunger = hunger,
    happiness = happiness,
    energy = energy,
    lastInteractionAt = lastInteractionAt,
    createdAt = createdAt,
)


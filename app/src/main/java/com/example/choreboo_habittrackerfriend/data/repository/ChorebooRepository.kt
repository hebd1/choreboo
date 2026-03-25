package com.example.choreboo_habittrackerfriend.data.repository

import com.example.choreboo_habittrackerfriend.data.local.dao.ChorebooDao
import com.example.choreboo_habittrackerfriend.data.local.entity.ChorebooEntity
import com.example.choreboo_habittrackerfriend.domain.model.ChorebooStage
import com.example.choreboo_habittrackerfriend.domain.model.ChorebooStats
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max

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
    fun getChoreboo(): Flow<ChorebooStats?> = chorebooDao.getChoreboo().map { it?.toDomain() }

    suspend fun getChorebooSync(): ChorebooStats? = chorebooDao.getChorebooSync()?.toDomain()

    suspend fun getOrCreateChoreboo(name: String = "Choreboo"): ChorebooEntity {
        val existing = chorebooDao.getChorebooSync()
        if (existing != null) return existing

        val newChoreboo = ChorebooEntity(
            name = name,
            stage = ChorebooStage.EGG.name,
            hunger = 80,
            happiness = 80,
            energy = 80,
        )
        val id = chorebooDao.insertChoreboo(newChoreboo)
        return newChoreboo.copy(id = id)
    }

    suspend fun applyStatDecay() {
        val choreboo = chorebooDao.getChorebooSync() ?: return
        val now = System.currentTimeMillis()
        val hoursSinceInteraction = (now - choreboo.lastInteractionAt) / (1000 * 60 * 60)

        if (hoursSinceInteraction <= 0) return

        val decayAmount = hoursSinceInteraction.toInt().coerceAtMost(50) // cap decay
        val updated = choreboo.copy(
            hunger = max(0, choreboo.hunger - decayAmount),
            happiness = max(0, choreboo.happiness - (decayAmount / 2)),
            energy = max(0, choreboo.energy - (decayAmount / 2)),
            lastInteractionAt = now,
        )
        chorebooDao.updateChoreboo(updated)
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

        return XpResult(
            levelsGained = newLevel - oldLevel,
            newLevel = newLevel,
            evolved = newStage != oldStage,
            newStage = if (newStage != oldStage) newStage else null,
        )
    }

    suspend fun feedChoreboo(stat: String, value: Int) {
        val choreboo = chorebooDao.getChorebooSync() ?: return
        val updated = when (stat.uppercase()) {
            "HUNGER" -> choreboo.copy(hunger = (choreboo.hunger + value).coerceAtMost(100))
            "HAPPINESS" -> choreboo.copy(happiness = (choreboo.happiness + value).coerceAtMost(100))
            "ENERGY" -> choreboo.copy(energy = (choreboo.energy + value).coerceAtMost(100))
            else -> choreboo
        }.copy(lastInteractionAt = System.currentTimeMillis())
        chorebooDao.updateChoreboo(updated)
    }

    suspend fun updateName(name: String) {
        val choreboo = chorebooDao.getChorebooSync() ?: return
        chorebooDao.updateChoreboo(choreboo.copy(name = name))
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
    lastInteractionAt = lastInteractionAt,
    createdAt = createdAt,
)


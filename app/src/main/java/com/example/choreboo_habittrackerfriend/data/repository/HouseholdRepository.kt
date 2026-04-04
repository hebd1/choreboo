package com.example.choreboo_habittrackerfriend.data.repository

import android.util.Log
import com.example.choreboo_habittrackerfriend.data.local.dao.HouseholdMemberDao
import com.example.choreboo_habittrackerfriend.data.local.entity.HouseholdMemberEntity
import com.example.choreboo_habittrackerfriend.dataconnect.ChorebooConnector
import com.example.choreboo_habittrackerfriend.dataconnect.execute
import com.example.choreboo_habittrackerfriend.dataconnect.instance
import com.example.choreboo_habittrackerfriend.domain.model.ChorebooStage
import com.example.choreboo_habittrackerfriend.domain.model.Household
import com.example.choreboo_habittrackerfriend.domain.model.HouseholdHabitStatus
import com.example.choreboo_habittrackerfriend.domain.model.HouseholdMember
import com.example.choreboo_habittrackerfriend.domain.model.HouseholdPet
import com.example.choreboo_habittrackerfriend.domain.model.PetType
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "HouseholdRepository"

sealed class HouseholdResult {
    data class Success(val household: Household) : HouseholdResult()
    data class Error(val message: String) : HouseholdResult()
}

@Singleton
class HouseholdRepository @Inject constructor(
    private val householdMemberDao: HouseholdMemberDao,
    private val userRepository: UserRepository,
    private val habitRepository: HabitRepository,
) {
    private val connector by lazy { ChorebooConnector.instance }

    private val _currentHousehold = MutableStateFlow<Household?>(null)
    val currentHousehold: Flow<Household?> = _currentHousehold.asStateFlow()

    private val _householdMembers = MutableStateFlow<List<HouseholdMember>>(emptyList())
    val householdMembers: Flow<List<HouseholdMember>> = _householdMembers.asStateFlow()

    /**
     * Household pets backed by Room — persists across process death and is available
     * immediately on cold start without a network round-trip. [refreshHouseholdPets]
     * writes cloud data into this table; Room emits updates reactively.
     */
    val householdPets: Flow<List<HouseholdPet>> = householdMemberDao.getAllMembers()
        .map { entities -> entities.map { it.toDomain() } }

    private val _householdHabits = MutableStateFlow<List<HouseholdHabitStatus>>(emptyList())
    val householdHabits: Flow<List<HouseholdHabitStatus>> = _householdHabits.asStateFlow()

    /**
     * Generate a random 6-character alphanumeric invite code.
     */
    private fun generateInviteCode(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..6).map { chars.random() }.joinToString("")
    }

    /**
     * Create a new household and set the current user as creator.
     */
    suspend fun createHousehold(name: String): HouseholdResult {
        require(name.isNotBlank()) { "Household name must not be blank" }
        require(name.length <= 50) { "Household name must be 50 characters or fewer, was ${name.length}" }

        val uid = userRepository.getCurrentUid()
            ?: return HouseholdResult.Error("Not authenticated")

        val inviteCode = generateInviteCode()

        return try {
            // Create the household in Data Connect
            val createResult = connector.createHousehold.execute(
                name = name,
                inviteCode = inviteCode,
            )
            val householdId = createResult.data.household_insert.id

            // Assign the household to the current user
            connector.updateUserHousehold.execute {
                this.householdId = householdId
            }

            val household = Household(
                id = householdId.toString(),
                name = name,
                inviteCode = inviteCode,
                createdByUid = uid,
            )
            _currentHousehold.value = household
            Log.d(TAG, "Created household: $name ($householdId)")
            HouseholdResult.Success(household)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create household", e)
            HouseholdResult.Error(e.message ?: "Failed to create household")
        }
    }

    /**
     * Join an existing household by invite code.
     */
    suspend fun joinHousehold(inviteCode: String): HouseholdResult {
        require(inviteCode.isNotBlank()) { "Invite code must not be blank" }

        val uid = userRepository.getCurrentUid()
            ?: return HouseholdResult.Error("Not authenticated")

        // Guard: prevent joining when already a member of another household
        if (_currentHousehold.value != null) {
            return HouseholdResult.Error("You are already in a household. Leave it first before joining another.")
        }

        return try {
            // Look up the household by invite code
            val queryResult = connector.getHouseholdByInviteCode.execute(
                inviteCode = inviteCode.uppercase(),
            )
            val found = queryResult.data.households.firstOrNull()
                ?: return HouseholdResult.Error("No household found with that invite code")

            // Assign the household to the current user
            connector.updateUserHousehold.execute {
                this.householdId = found.id
            }

            val household = Household(
                id = found.id.toString(),
                name = found.name,
                inviteCode = found.inviteCode,
                createdByUid = found.createdBy.id,
                createdByName = found.createdBy.displayName,
            )
            _currentHousehold.value = household
            Log.d(TAG, "Joined household: ${found.name} (${found.id})")

            // Refresh members and pets (pets write-through to Room)
            refreshHouseholdMembers()
            refreshHouseholdPets()

            HouseholdResult.Success(household)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to join household", e)
            HouseholdResult.Error(e.message ?: "Failed to join household")
        }
    }

    /**
     * Leave the current household.
     * Converts the user's own household habits to personal and removes other members'
     * synced habits from Room.
     */
    suspend fun leaveHousehold(): HouseholdResult {
        val uid = userRepository.getCurrentUid()
            ?: return HouseholdResult.Error("Not authenticated")
        return try {
            // Clean up household habits before clearing cloud membership
            habitRepository.convertHouseholdHabitsToPersonal(uid)

            // Set user's householdId to null
            connector.updateUserHousehold.execute {
                this.householdId = null
            }

            _currentHousehold.value = null
            _householdMembers.value = emptyList()
            householdMemberDao.deleteAll()
            _householdHabits.value = emptyList()
            Log.d(TAG, "Left household")
            HouseholdResult.Success(
                Household(id = "", name = "", inviteCode = "", createdByUid = ""),
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to leave household", e)
            HouseholdResult.Error(e.message ?: "Failed to leave household")
        }
    }

    /**
     * Fetch household members from Data Connect.
     * Auth-scoped: traverses from the authenticated user to their household's members.
     */
    suspend fun refreshHouseholdMembers() {
        try {
            val result = connector.getMyHouseholdMembers.execute()
            val members = result.data.user?.household?.users_on_household
            if (members != null) {
                _householdMembers.value = members.map { user ->
                    HouseholdMember(
                        uid = user.id,
                        displayName = user.displayName,
                        photoUrl = user.photoUrl,
                        email = user.email,
                    )
                }
                Log.d(TAG, "Refreshed ${members.size} household members")
            } else {
                _householdMembers.value = emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to refresh household members", e)
        }
    }

    /**
     * Fetch all Choreboos belonging to household members from Data Connect and persist
     * them to Room. Auth-scoped: traverses from the authenticated user to their
     * household's members' choreboos. Only members who have created a Choreboo are
     * stored — members without pets are excluded. Departed members are reconciled out of
     * the local cache.
     */
    suspend fun refreshHouseholdPets() {
        try {
            val result = connector.getMyHouseholdChoreboos.execute()
            val users = result.data.user?.household?.users_on_household
            if (users != null) {
                val entities = users.mapNotNull { user ->
                    val pet = user.choreboo_on_owner ?: return@mapNotNull null
                    HouseholdMemberEntity(
                        uid = user.id,
                        displayName = user.displayName,
                        photoUrl = user.photoUrl,
                        chorebooId = pet.id.toString(),
                        chorebooName = pet.name,
                        chorebooStage = pet.stage,
                        chorebooLevel = pet.level,
                        chorebooXp = pet.xp,
                        chorebooHunger = pet.hunger,
                        chorebooHappiness = pet.happiness,
                        chorebooEnergy = pet.energy,
                        chorebooPetType = pet.petType,
                        lastSyncedAt = System.currentTimeMillis(),
                    )
                }
                if (entities.isEmpty()) {
                    householdMemberDao.deleteAll()
                } else {
                    householdMemberDao.upsertAll(entities)
                    // Reconcile: remove any cached member no longer in the household
                    householdMemberDao.deleteMembersNotIn(entities.map { it.uid })
                }
                Log.d(TAG, "Persisted ${entities.size} household pets to Room")
            } else {
                // User has no household — clear the local cache
                householdMemberDao.deleteAll()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to refresh household pets", e)
        }
    }

    /**
     * Fetch household habits + today's completion status from Data Connect.
     * Calls GetMyHouseholdHabits for the habit list, then GetHouseholdHabitLogsForDate
     * to overlay who (if anyone) has completed each habit today.
     */
    suspend fun refreshHouseholdHabits() {
        try {
            val today = LocalDate.now().toString() // "YYYY-MM-DD"

            // Run both network calls in parallel
            val (habitNodes, logsByHabitId) = coroutineScope {
                val habitsDeferred = async {
                    val habitsResult = connector.getMyHouseholdHabits.execute()
                    habitsResult.data.user?.household?.habits_on_household
                }
                val logsDeferred = async {
                    val logsResult = connector.getHouseholdHabitLogsForDate.execute(date = today)
                    buildMap<String, Pair<String, String>> {
                        logsResult.data.user?.household?.habits_on_household?.forEach { habitNode ->
                            habitNode.habitLogs_on_habit.firstOrNull()?.let { log ->
                                put(
                                    habitNode.id.toString(),
                                    Pair(log.completedBy.id, log.completedBy.displayName),
                                )
                            }
                        }
                    }
                }
                Pair(habitsDeferred.await(), logsDeferred.await())
            }

            if (habitNodes == null) {
                _householdHabits.value = emptyList()
                return
            }

            _householdHabits.value = habitNodes.map { h ->
                val completedBy = logsByHabitId[h.id.toString()]
                HouseholdHabitStatus(
                    habitId = h.id.toString(),
                    title = h.title,
                    iconName = h.iconName,
                    ownerName = h.owner.displayName,
                    ownerUid = h.owner.id,
                    baseXp = h.baseXp,
                    assignedToUid = h.assignedTo?.id,
                    assignedToName = h.assignedTo?.displayName,
                    completedByUid = completedBy?.first,
                    completedByName = completedBy?.second,
                )
            }
            Log.d(TAG, "Refreshed ${_householdHabits.value.size} household habits")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to refresh household habits", e)
        }
    }

    /**
     * Clear all in-memory household state and the Room household_members cache.
     * Called on sign-out and account reset so no stale data bleeds into the next session.
     */
    suspend fun clearState() {
        _currentHousehold.value = null
        _householdMembers.value = emptyList()
        householdMemberDao.deleteAll()
        _householdHabits.value = emptyList()
    }

    /**
     * Refresh all household data (household info, members, pets, and habits).
     * Auth-scoped: uses GetMyHousehold which traverses from auth.uid.
     */
    suspend fun refreshAll() {
        try {
            val result = connector.getMyHousehold.execute()
            val h = result.data.user?.household
            if (h != null) {
                _currentHousehold.value = Household(
                    id = h.id.toString(),
                    name = h.name,
                    inviteCode = h.inviteCode,
                    createdByUid = h.createdBy.id,
                    createdByName = h.createdBy.displayName,
                )
            } else {
                _currentHousehold.value = null
            }
            // Members, pets, and habits are independent — run in parallel
            coroutineScope {
                launch { refreshHouseholdMembers() }
                launch { refreshHouseholdPets() }
                launch { refreshHouseholdHabits() }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to refresh all household data", e)
        }
    }
}

// ── Mapping ──────────────────────────────────────────────────────────────────────────────────

private fun HouseholdMemberEntity.toDomain(): HouseholdPet = HouseholdPet(
    chorebooId = chorebooId,
    name = chorebooName,
    stage = try { ChorebooStage.valueOf(chorebooStage) } catch (_: Exception) { ChorebooStage.EGG },
    level = chorebooLevel,
    xp = chorebooXp,
    hunger = chorebooHunger,
    happiness = chorebooHappiness,
    energy = chorebooEnergy,
    petType = try { PetType.valueOf(chorebooPetType) } catch (_: Exception) { PetType.FOX },
    ownerName = displayName,
    ownerUid = uid,
    ownerPhotoUrl = photoUrl,
)

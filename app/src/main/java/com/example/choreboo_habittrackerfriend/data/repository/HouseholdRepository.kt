package com.example.choreboo_habittrackerfriend.data.repository

import android.util.Log
import com.example.choreboo_habittrackerfriend.data.local.dao.HouseholdMemberDao
import com.example.choreboo_habittrackerfriend.data.local.dao.HouseholdDao
import com.example.choreboo_habittrackerfriend.data.local.dao.HouseholdHabitStatusDao
import com.example.choreboo_habittrackerfriend.data.local.entity.HouseholdEntity
import com.example.choreboo_habittrackerfriend.data.local.entity.HouseholdHabitStatusEntity
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "HouseholdRepository"
private const val CLOUD_TIMEOUT_MS = 5000L

sealed class HouseholdResult {
    data class Success(val household: Household) : HouseholdResult()
    data class Error(val message: String) : HouseholdResult()
}

@Singleton
class HouseholdRepository @Inject constructor(
    private val householdMemberDao: HouseholdMemberDao,
    private val householdDao: HouseholdDao,
    private val householdHabitStatusDao: HouseholdHabitStatusDao,
    private val userRepository: UserRepository,
    private val habitRepository: HabitRepository,
) {
    private val connector by lazy { ChorebooConnector.instance }

    /**
     * Current household — Room-backed. Reactive and persists across process death.
     * At most one row exists (the current user's household).
     */
    val currentHousehold: Flow<Household?> = householdDao.getHousehold()
        .map { it?.toDomain() }

    /**
     * Household members — mapped from household_members table.
     * Only includes members with Choreboos. For full member identity, use householdMembers field below.
     */
    val householdPets: Flow<List<HouseholdPet>> = householdMemberDao.getAllMembers()
        .map { entities -> entities.map { it.toDomain() } }

    /**
     * Household members with identity info (uid, displayName, photoUrl, email).
     * Mapped from household_members table.
     */
    val householdMembers: Flow<List<HouseholdMember>> = householdMemberDao.getAllMembers()
        .map { entities ->
            entities.map { entity ->
                HouseholdMember(
                    uid = entity.uid,
                    displayName = entity.displayName,
                    photoUrl = entity.photoUrl,
                    email = entity.email,
                )
            }
        }

    /**
     * Household habit statuses — Room-backed. Includes today's completion overlay.
     */
    val householdHabits: Flow<List<HouseholdHabitStatus>> = householdHabitStatusDao.getAllHabitStatuses()
        .map { entities -> entities.map { it.toDomain() } }

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
            val createResult = withTimeoutOrNull(CLOUD_TIMEOUT_MS) {
                connector.createHousehold.execute(
                    name = name,
                    inviteCode = inviteCode,
                )
            } ?: return HouseholdResult.Error("Request timed out. Please try again.")
            val householdId = createResult.data.household_insert.id

            // Assign the household to the current user
            withTimeoutOrNull(CLOUD_TIMEOUT_MS) {
                connector.updateUserHousehold.execute {
                    this.householdId = householdId
                }
            } ?: return HouseholdResult.Error("Request timed out. Please try again.")

            val household = HouseholdEntity(
                id = householdId.toString(),
                name = name,
                inviteCode = inviteCode,
                createdByUid = uid,
            )
            householdDao.upsertHousehold(household)
            Log.d(TAG, "Created household: $name ($householdId)")
            HouseholdResult.Success(household.toDomain())
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

        return try {
            // Look up the household by invite code
            val queryResult = withTimeoutOrNull(CLOUD_TIMEOUT_MS) {
                connector.getHouseholdByInviteCode.execute(
                    inviteCode = inviteCode.uppercase(),
                )
            } ?: return HouseholdResult.Error("Request timed out. Please try again.")
            val found = queryResult.data.households.firstOrNull()
                ?: return HouseholdResult.Error("No household found with that invite code")

            // Assign the household to the current user
            withTimeoutOrNull(CLOUD_TIMEOUT_MS) {
                connector.updateUserHousehold.execute {
                    this.householdId = found.id
                }
            } ?: return HouseholdResult.Error("Request timed out. Please try again.")

            val household = HouseholdEntity(
                id = found.id.toString(),
                name = found.name,
                inviteCode = found.inviteCode,
                createdByUid = found.createdBy.id,
                createdByName = found.createdBy.displayName,
            )
            householdDao.upsertHousehold(household)
            Log.d(TAG, "Joined household: ${found.name} (${found.id})")

            // Refresh members and pets (pets write-through to Room)
            refreshHouseholdMembers()
            refreshHouseholdPets()
            refreshHouseholdHabits()

            HouseholdResult.Success(household.toDomain())
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
            withTimeoutOrNull(CLOUD_TIMEOUT_MS) {
                connector.updateUserHousehold.execute {
                    this.householdId = null
                }
            } ?: return HouseholdResult.Error("Request timed out. Please try again.")

            householdDao.deleteAll()
            householdMemberDao.deleteAll()
            householdHabitStatusDao.deleteAll()
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
     * Fetch household members from Data Connect and write identity columns to Room.
     * Auth-scoped: traverses from the authenticated user to their household's members.
     *
     * Only updates (displayName, photoUrl, email, lastSyncedAt). Pet columns written by
     * [refreshHouseholdPets] are never touched here — INSERT OR IGNORE + UPDATE ensures
     * existing pet data is never overwritten by this call.
     */
    suspend fun refreshHouseholdMembers() {
        try {
            val result = withTimeoutOrNull(CLOUD_TIMEOUT_MS) { connector.getMyHouseholdMembers.execute() }
            if (result == null) {
                Log.w(TAG, "refreshHouseholdMembers: timed out")
                return
            }
            val members = result.data.user?.household?.users_on_household
            if (members != null) {
                if (members.isEmpty()) {
                    householdMemberDao.deleteAll()
                } else {
                    val now = System.currentTimeMillis()
                    members.forEach { user ->
                        householdMemberDao.upsertIdentityColumns(
                            uid = user.id,
                            displayName = user.displayName,
                            photoUrl = user.photoUrl,
                            email = user.email,
                            lastSyncedAt = now,
                        )
                    }
                    // Reconcile: remove any cached member no longer in the household
                    householdMemberDao.deleteMembersNotIn(members.map { it.id })
                }
                Log.d(TAG, "Refreshed ${members.size} household members")
            } else {
                householdMemberDao.deleteAll()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to refresh household members", e)
        }
    }

    /**
     * Fetch all Choreboos belonging to household members from Data Connect and persist
     * pet columns to Room. Auth-scoped: traverses from the authenticated user to their
     * household's members' choreboos. Only members who have created a Choreboo are
     * stored — members without pets are excluded.
     *
     * Only updates pet-related columns (choreboo*, displayName, photoUrl, lastSyncedAt).
     * The email column is never touched here — INSERT OR IGNORE + UPDATE ensures email
     * written by [refreshHouseholdMembers] is preserved.
     */
    suspend fun refreshHouseholdPets() {
        try {
            val result = withTimeoutOrNull(CLOUD_TIMEOUT_MS) { connector.getMyHouseholdChoreboos.execute() }
            if (result == null) {
                Log.w(TAG, "refreshHouseholdPets: timed out")
                return
            }
            val users = result.data.user?.household?.users_on_household
            if (users != null) {
                val petUsers = users.filter { it.choreboo_on_owner != null }
                if (petUsers.isEmpty()) {
                    householdMemberDao.deleteAll()
                } else {
                    val now = System.currentTimeMillis()
                    petUsers.forEach { user ->
                        val pet = user.choreboo_on_owner!!
                        householdMemberDao.upsertPetColumns(
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
                            lastSyncedAt = now,
                        )
                    }
                    // Reconcile: remove any cached member no longer in the household
                    householdMemberDao.deleteMembersNotIn(petUsers.map { it.id })
                }
                Log.d(TAG, "Persisted ${petUsers.size} household pets to Room")
            } else {
                // User has no household — clear the local cache
                householdMemberDao.deleteAll()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to refresh household pets", e)
        }
    }

    /**
     * Fetch household habits + today's completion status from Data Connect and persist to Room.
     * Calls GetMyHouseholdHabits for the habit list, then GetHouseholdHabitLogsForDate
     * to overlay who (if anyone) has completed each habit today.
     */
    suspend fun refreshHouseholdHabits() {
        try {
            val today = LocalDate.now().toString() // "YYYY-MM-DD"

            // Run both network calls in parallel
            val (habitNodes, logsByHabitId) = coroutineScope {
                val habitsDeferred = async {
                    val habitsResult = withTimeoutOrNull(CLOUD_TIMEOUT_MS) { connector.getMyHouseholdHabits.execute() }
                    if (habitsResult == null) {
                        Log.w(TAG, "refreshHouseholdHabits: habits timed out")
                        return@async null
                    }
                    habitsResult.data.user?.household?.habits_on_household
                }
                val logsDeferred = async {
                    val logsResult = withTimeoutOrNull(CLOUD_TIMEOUT_MS) { connector.getHouseholdHabitLogsForDate.execute(date = today) }
                    if (logsResult == null) {
                        Log.w(TAG, "refreshHouseholdHabits: logs timed out")
                        return@async emptyMap<String, Pair<String, String>>()
                    }
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
                householdHabitStatusDao.deleteAll()
                return
            }

            val entities = habitNodes.map { h ->
                val completedBy = logsByHabitId[h.id.toString()]
                HouseholdHabitStatusEntity(
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
                    cachedDate = today,
                )
            }
            householdHabitStatusDao.replaceAll(entities)
            Log.d(TAG, "Persisted ${entities.size} household habits to Room")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to refresh household habits", e)
        }
    }

    /**
     * Clear all household state and Room caches.
     * Called on sign-out and account reset so no stale data bleeds into the next session.
     */
    suspend fun clearState() {
        householdDao.deleteAll()
        householdMemberDao.deleteAll()
        householdHabitStatusDao.deleteAll()
    }

    /**
     * Refresh all household data (household info, members, pets, and habits).
     * Auth-scoped: uses GetMyHousehold which traverses from auth.uid.
     */
    suspend fun refreshAll() {
        try {
            val result = withTimeoutOrNull(CLOUD_TIMEOUT_MS) { connector.getMyHousehold.execute() }
            if (result == null) {
                Log.w(TAG, "refreshAll: timed out")
                return
            }
            val h = result.data.user?.household
            if (h != null) {
                val household = HouseholdEntity(
                    id = h.id.toString(),
                    name = h.name,
                    inviteCode = h.inviteCode,
                    createdByUid = h.createdBy.id,
                    createdByName = h.createdBy.displayName,
                )
                householdDao.upsertHousehold(household)
            } else {
                householdDao.deleteAll()
            }
            // Members must run before pets (members writes email; pets must not wipe it).
            // Habits are independent — run in parallel with the member/pet chain.
            coroutineScope {
                launch {
                    refreshHouseholdMembers()
                    refreshHouseholdPets()
                }
                launch { refreshHouseholdHabits() }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to refresh all household data", e)
        }
    }
}

// ── Mapping ──────────────────────────────────────────────────────────────────────────────────

private fun HouseholdEntity.toDomain(): Household = Household(
    id = id,
    name = name,
    inviteCode = inviteCode,
    createdByUid = createdByUid,
    createdByName = createdByName,
)

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

private fun HouseholdHabitStatusEntity.toDomain(): HouseholdHabitStatus = HouseholdHabitStatus(
    habitId = habitId,
    title = title,
    iconName = iconName,
    ownerName = ownerName,
    ownerUid = ownerUid,
    baseXp = baseXp,
    assignedToUid = assignedToUid,
    assignedToName = assignedToName,
    completedByName = completedByName,
    completedByUid = completedByUid,
)

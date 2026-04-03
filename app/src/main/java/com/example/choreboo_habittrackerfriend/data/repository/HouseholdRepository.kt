package com.example.choreboo_habittrackerfriend.data.repository

import android.util.Log
import com.example.choreboo_habittrackerfriend.dataconnect.ChorebooConnector
import com.example.choreboo_habittrackerfriend.dataconnect.execute
import com.example.choreboo_habittrackerfriend.dataconnect.instance
import com.example.choreboo_habittrackerfriend.domain.model.ChorebooStage
import com.example.choreboo_habittrackerfriend.domain.model.Household
import com.example.choreboo_habittrackerfriend.domain.model.HouseholdMember
import com.example.choreboo_habittrackerfriend.domain.model.HouseholdPet
import com.example.choreboo_habittrackerfriend.domain.model.PetType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "HouseholdRepository"

sealed class HouseholdResult {
    data class Success(val household: Household) : HouseholdResult()
    data class Error(val message: String) : HouseholdResult()
}

@Singleton
class HouseholdRepository @Inject constructor(
    private val userRepository: UserRepository,
) {
    private val connector by lazy { ChorebooConnector.instance }

    private val _currentHousehold = MutableStateFlow<Household?>(null)
    val currentHousehold: Flow<Household?> = _currentHousehold.asStateFlow()

    private val _householdMembers = MutableStateFlow<List<HouseholdMember>>(emptyList())
    val householdMembers: Flow<List<HouseholdMember>> = _householdMembers.asStateFlow()

    private val _householdPets = MutableStateFlow<List<HouseholdPet>>(emptyList())
    val householdPets: Flow<List<HouseholdPet>> = _householdPets.asStateFlow()

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
        val uid = userRepository.getCurrentUid()
            ?: return HouseholdResult.Error("Not authenticated")

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

            // Refresh members and pets
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
     */
    suspend fun leaveHousehold(): HouseholdResult {
        return try {
            // Set user's householdId to null
            connector.updateUserHousehold.execute {
                this.householdId = null
            }

            _currentHousehold.value = null
            _householdMembers.value = emptyList()
            _householdPets.value = emptyList()
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
     */
    suspend fun refreshHouseholdMembers() {
        val household = _currentHousehold.value ?: return
        try {
            val householdUuid = UUID.fromString(household.id)
            val result = connector.getHouseholdMembers.execute(
                householdId = householdUuid,
            )
            _householdMembers.value = result.data.users.map { user ->
                HouseholdMember(
                    uid = user.id,
                    displayName = user.displayName,
                    photoUrl = user.photoUrl,
                    email = user.email,
                )
            }
            Log.d(TAG, "Refreshed ${result.data.users.size} household members")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to refresh household members", e)
        }
    }

    /**
     * Fetch all Choreboos belonging to household members from Data Connect.
     */
    suspend fun refreshHouseholdPets() {
        val household = _currentHousehold.value ?: return
        try {
            val householdUuid = UUID.fromString(household.id)
            val result = connector.getChoreboosByHousehold.execute(
                householdId = householdUuid,
            )
            _householdPets.value = result.data.choreboos.map { pet ->
                HouseholdPet(
                    chorebooId = pet.id.toString(),
                    name = pet.name,
                    stage = try { ChorebooStage.valueOf(pet.stage) } catch (_: Exception) { ChorebooStage.EGG },
                    level = pet.level,
                    xp = pet.xp,
                    hunger = pet.hunger,
                    happiness = pet.happiness,
                    energy = pet.energy,
                    petType = try { PetType.valueOf(pet.petType) } catch (_: Exception) { PetType.FOX },
                    ownerName = pet.owner.displayName,
                    ownerUid = pet.owner.id,
                    ownerPhotoUrl = pet.owner.photoUrl,
                )
            }
            Log.d(TAG, "Refreshed ${result.data.choreboos.size} household pets")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to refresh household pets", e)
        }
    }

    /**
     * Refresh all household data (household info from user profile, then members and pets).
     */
    suspend fun refreshAll() {
        try {
            // Fetch user's current household from their cloud profile
            val cloudUser = userRepository.fetchCurrentUserFromCloud()
            if (cloudUser?.householdId != null) {
                val householdUuid = UUID.fromString(cloudUser.householdId)
                val result = connector.getHouseholdById.execute(
                    householdId = householdUuid,
                )
                val h = result.data.household
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
            } else {
                _currentHousehold.value = null
            }
            refreshHouseholdMembers()
            refreshHouseholdPets()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to refresh all household data", e)
        }
    }
}

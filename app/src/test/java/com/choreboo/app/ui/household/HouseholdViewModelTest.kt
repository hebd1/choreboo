package com.choreboo.app.ui.household

import com.choreboo.app.TestDispatcherRule
import com.choreboo.app.data.datastore.UserPreferences
import com.choreboo.app.data.repository.AuthRepository
import com.choreboo.app.data.repository.HouseholdRepository
import com.choreboo.app.data.repository.UserRepository
import com.choreboo.app.domain.model.Household
import com.choreboo.app.domain.model.HouseholdPet
import com.choreboo.app.domain.model.ChorebooStage
import com.choreboo.app.domain.model.PetType
import app.cash.turbine.test
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Unit tests for [HouseholdViewModel].
 *
 * Covers: initial data exposure, selectedPet state, refreshData loading state, and
 * photo enrichment for the current user's pet card.
 */
class HouseholdViewModelTest {

    @get:Rule
    val dispatcherRule = TestDispatcherRule()

    private lateinit var householdRepository: HouseholdRepository
    private lateinit var userRepository: UserRepository
    private lateinit var userPreferences: UserPreferences
    private lateinit var authRepository: AuthRepository

    private val householdFlow = MutableStateFlow<Household?>(null)
    private val petsFlow = MutableStateFlow<List<HouseholdPet>>(emptyList())
    private val profilePhotoFlow = MutableStateFlow<String?>(null)
    private val currentUserFlow = MutableStateFlow<com.google.firebase.auth.FirebaseUser?>(null)

    private fun createViewModel() = HouseholdViewModel(
        householdRepository = householdRepository,
        userRepository = userRepository,
        userPreferences = userPreferences,
        authRepository = authRepository,
    )

    private fun fakePet(
        ownerUid: String = "uid-1",
        ownerPhotoUrl: String? = null,
    ) = HouseholdPet(
        chorebooId = "choreboo-1",
        name = "Foxy",
        stage = ChorebooStage.BABY,
        level = 1,
        xp = 0,
        hunger = 80,
        happiness = 80,
        energy = 80,
        petType = PetType.FOX,
        ownerName = "Alice",
        ownerUid = ownerUid,
        ownerPhotoUrl = ownerPhotoUrl,
        backgroundId = null,
    )

    @Before
    fun setUp() {
        householdRepository = mockk(relaxed = true)
        userRepository = mockk(relaxed = true)
        userPreferences = mockk(relaxed = true)
        authRepository = mockk(relaxed = true)

        every { householdRepository.currentHousehold } returns householdFlow
        every { householdRepository.householdPets } returns petsFlow
        every { householdRepository.householdMembers } returns flowOf(emptyList())
        every { householdRepository.householdHabits } returns flowOf(emptyList())
        every { userPreferences.profilePhotoUri } returns profilePhotoFlow
        every { authRepository.currentUser } returns currentUserFlow
        every { userRepository.getCurrentUid() } returns "uid-1"
    }

    // ── initial state ────────────────────────────────────────────────────────

    @Test
    fun `currentHousehold emits null initially`() = runTest {
        val vm = createViewModel()
        vm.currentHousehold.test {
            assertNull(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `currentHousehold reflects repository updates`() = runTest {
        val vm = createViewModel()
        val household = Household(id = "h1", name = "The Squad", inviteCode = "ABC12345", createdByUid = "uid-1")
        householdFlow.value = household

        vm.currentHousehold.test {
            assertEquals(household, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── selectedPet ──────────────────────────────────────────────────────────

    @Test
    fun `selectedPet is null initially`() = runTest {
        val vm = createViewModel()
        assertNull(vm.selectedPet.value)
    }

    @Test
    fun `selectPet sets selectedPet`() = runTest {
        val vm = createViewModel()
        val pet = fakePet()
        vm.selectPet(pet)
        assertEquals(pet, vm.selectedPet.value)
    }

    @Test
    fun `clearSelectedPet nullifies selectedPet`() = runTest {
        val vm = createViewModel()
        vm.selectPet(fakePet())
        vm.clearSelectedPet()
        assertNull(vm.selectedPet.value)
    }

    // ── isRefreshing ─────────────────────────────────────────────────────────

    @Test
    fun `isRefreshing is false initially`() = runTest {
        val vm = createViewModel()
        assertFalse(vm.isRefreshing.value)
    }

    @Test
    fun `refreshData calls householdRepository refreshAll`() = runTest {
        val vm = createViewModel()
        vm.refreshData()
        coVerify { householdRepository.refreshAll() }
        assertFalse(vm.isRefreshing.value)
    }

    // ── photo enrichment ─────────────────────────────────────────────────────

    @Test
    fun `householdPets patches current user photo from local DataStore when cloud photo is null`() = runTest {
        profilePhotoFlow.value = "file:///local/photo.jpg"
        val petWithoutPhoto = fakePet(ownerUid = "uid-1", ownerPhotoUrl = null)
        petsFlow.value = listOf(petWithoutPhoto)

        val vm = createViewModel()

        vm.householdPets.test {
            val pets = awaitItem()
            assertEquals("file:///local/photo.jpg", pets.firstOrNull()?.ownerPhotoUrl)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `householdPets does not overwrite existing cloud photo for current user`() = runTest {
        profilePhotoFlow.value = "file:///local/photo.jpg"
        val petWithCloudPhoto = fakePet(ownerUid = "uid-1", ownerPhotoUrl = "https://cloud/photo.jpg")
        petsFlow.value = listOf(petWithCloudPhoto)

        val vm = createViewModel()

        vm.householdPets.test {
            val pets = awaitItem()
            assertEquals("https://cloud/photo.jpg", pets.firstOrNull()?.ownerPhotoUrl)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `householdPets does not modify other users pets`() = runTest {
        profilePhotoFlow.value = "file:///local/photo.jpg"
        val otherUserPet = fakePet(ownerUid = "uid-other", ownerPhotoUrl = null)
        petsFlow.value = listOf(otherUserPet)

        val vm = createViewModel()

        vm.householdPets.test {
            val pets = awaitItem()
            assertNull(pets.firstOrNull()?.ownerPhotoUrl)
            cancelAndIgnoreRemainingEvents()
        }
    }
}

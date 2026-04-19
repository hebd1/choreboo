package com.choreboo.app.data.repository

import com.choreboo.app.data.local.dao.HouseholdDao
import com.choreboo.app.data.local.dao.HouseholdHabitStatusDao
import com.choreboo.app.data.local.dao.HouseholdMemberDao
import com.google.firebase.auth.FirebaseAuth
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class HouseholdRepositoryRefreshTest {

    private lateinit var householdMemberDao: HouseholdMemberDao
    private lateinit var repo: HouseholdRepository

    @Before
    fun setUp() {
        householdMemberDao = mockk(relaxed = true)

        repo = HouseholdRepository(
            householdMemberDao = householdMemberDao,
            householdDao = mockk<HouseholdDao>(relaxed = true),
            householdHabitStatusDao = mockk<HouseholdHabitStatusDao>(relaxed = true),
            userRepository = mockk(relaxed = true),
            habitRepository = mockk(relaxed = true),
            firebaseAuth = mockk<FirebaseAuth>(relaxed = true),
        )
    }

    @Test
    fun `upsertHouseholdPets preserves identity-only members during reconciliation`() = runTest {
        repo.upsertHouseholdPets(
            petUsers = listOf(
                HouseholdPetSnapshot(
                    uid = "member-with-pet",
                    displayName = "Pet Owner",
                    photoUrl = null,
                    chorebooId = "pet-1",
                    chorebooName = "Boo",
                    chorebooStage = "BABY",
                    chorebooLevel = 2,
                    chorebooXp = 10,
                    chorebooHunger = 80,
                    chorebooHappiness = 90,
                    chorebooEnergy = 70,
                    chorebooPetType = "FOX",
                    chorebooBackgroundId = null,
                ),
            ),
            allMemberUids = listOf("member-with-pet", "member-without-pet"),
        )

        coVerify { householdMemberDao.upsertPetColumns(uid = "member-with-pet", displayName = any(), photoUrl = any(), chorebooId = any(), chorebooName = any(), chorebooStage = any(), chorebooLevel = any(), chorebooXp = any(), chorebooHunger = any(), chorebooHappiness = any(), chorebooEnergy = any(), chorebooPetType = any(), chorebooBackgroundId = any(), lastSyncedAt = any()) }
        coVerify { householdMemberDao.deleteMembersNotIn(listOf("member-with-pet", "member-without-pet")) }
    }

    @Test
    fun `upsertHouseholdPets deletes all when household is empty`() = runTest {
        repo.upsertHouseholdPets(
            petUsers = emptyList(),
            allMemberUids = emptyList(),
        )

        coVerify { householdMemberDao.deleteAll() }
        coVerify(exactly = 0) { householdMemberDao.deleteMembersNotIn(any()) }
    }

    @Test
    fun `upsertHouseholdPets reconciles even when no members have pets`() = runTest {
        repo.upsertHouseholdPets(
            petUsers = emptyList(),
            allMemberUids = listOf("member-a", "member-b"),
        )

        coVerify(exactly = 0) { householdMemberDao.upsertPetColumns(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) }
        coVerify { householdMemberDao.deleteMembersNotIn(listOf("member-a", "member-b")) }
    }
}

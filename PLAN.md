# Choreboo Audit & Remediation Plan

## Status: COMPLETE ✅

All phases of the original audit are done. A verification audit identified 6 remaining issues — all 6 are fixed. A subsequent code quality pass expanded the test suite from 265 to 455 tests. The build passes and all 455 unit tests pass.

---

## Code Quality Pass (2026-04-15)

Expanded the test suite from 265 tests (14 files) to **455 tests (34 files)**. All three workstreams complete:

### 1. New Test Files Written (20 new files)

| File | Tests | Coverage |
|------|-------|----------|
| `AuthRepositoryTest.kt` | 15 | signIn/signUp validation, Google sign-in, password reset |
| `BackgroundRepositoryTest.kt` | 9 | purchaseBackground, syncFromCloud, selectBackground |
| `BadgeRepositoryTest.kt` | 12 | getAvailableBadges, getEarnedBadges, badge criteria |
| `ChorebooRepositoryStatDecayTest.kt` | 19 | Stat decay calculation, time-based hunger/happiness/energy |
| `ChorebooRepositoryStatsTest.kt` | 21 | getOrCreateChoreboo, updateName, feed, sleep, stats |
| `HabitRepositorySyncTest.kt` | 9 | syncHabitsFromCloud, syncHabitLogsFromCloud, deletion reconciliation |
| `HouseholdRepositoryLeaveTest.kt` | 5 | leaveHousehold success/error paths, creator vs member |
| `ResetRepositoryTest.kt` | 7 | resetAll sequence, partial failures, cancelPendingWrites |
| `UserRepositorySyncTest.kt` | 6 | syncPointsToCloud, syncPointsFromCloud, max-wins merge |
| `UserRepositoryUpdateDisplayNameTest.kt` | 7 | updateDisplayName validation: blank, >30 chars, trim, no auth |
| `HouseholdPetTest.kt` | 14 | HouseholdPet domain model fields, mood derivation |
| `PetMoodSchedulerTest.kt` | 11 | Alarm scheduling, exact alarm permission, window fallback |
| `HouseholdViewModelTest.kt` | 10 | Household data loading, member habits, refresh |
| `OnboardingViewModelTest.kt` | 15 | Name validation, pet type selection, hatch flow |
| `SettingsViewModelSignOutTest.kt` | 14 | Sign-out flow, cancelPendingWrites, local cleanup |
| `SettingsViewModelHouseholdTest.kt` | 12 | createHousehold, joinHousehold, leaveHousehold, loading flags |
| `StatsViewModelTest.kt` | 10 | Stats aggregation, habit filtering, date ranges |
| `MainViewModelTest.kt` | 11 | isAppReady, fast-path, full startup, timeout, sync |
| `AppLifecycleObserverTest.kt` | 4 | Cold-start skip, warm-resume sync, exception handling |

### 2. Existing Test Files Updated

| File | Before | After | Changes |
|------|--------|-------|---------|
| `HabitTest.kt` | 16 | 18 | +2 tests for isScheduledForToday edge cases |
| `BillingRepositoryTest.kt` | 4 | 7 | +3 tests for billing guards |
| `HabitRepositoryTest.kt` | 19 | 20 | +1 test for validation |
| `AddEditHabitViewModelTest.kt` | 34 | 35 | +1 test |
| `PetViewModelTest.kt` | 22 | 23 | +1 test |

### 3. Test Suite Summary (455 total)

**Domain models (82):** HabitTest 18, ChorebooStatsTest 33, ChorebooStageTest 17, HouseholdPetTest 14

**Repositories (175):** AuthRepositoryTest 15, BackgroundRepositoryTest 9, BadgeRepositoryTest 12, BillingRepositoryTest 7, ChorebooRepositoryAddXpTest 12, ChorebooRepositoryStatDecayTest 19, ChorebooRepositoryStatsTest 21, HabitRepositorySyncTest 9, HabitRepositoryTest 20, HouseholdRepositoryLeaveTest 5, HouseholdRepositoryValidationTest 6, ResetRepositoryTest 7, SyncManagerTest 11, UserRepositorySyncTest 6, UserRepositoryTest 10, UserRepositoryUpdateDisplayNameTest 7

**ViewModels (182):** AuthViewModelTest 23, CalendarViewModelTest 18, AddEditHabitViewModelTest 35, HouseholdViewModelTest 10, OnboardingViewModelTest 15, PetViewModelTest 23, SettingsViewModelTest 10, SettingsViewModelSignOutTest 14, SettingsViewModelHouseholdTest 12, StatsViewModelTest 10, MainViewModelTest 11

**Infrastructure (16):** AppLifecycleObserverTest 4, PetMoodSchedulerTest 11, ExampleUnitTest 1

---

## Verification Audit Issues

| ID | Description | Status |
|----|-------------|--------|
| P3-11 | `HabitReminderScheduler` and `PetMoodScheduler` used `setAndAllowWhileIdle()` as fallback instead of `setWindow()` | ✅ Fixed |
| P4-03 | `UserPreferences` had unused `setPoints()` and `setLifetimeXp()` individual methods | ✅ Fixed |
| P4-05 | `BillingRepository.release()` had no documentation explaining why it is never called | ✅ Fixed |
| P4-10 | `DeleteHousehold` mutation did not null out `User.household` FK before deleting the household row | ✅ Fixed |
| P9-17 | `PetScreen` and `StatsScreen` called `isScheduledForToday()` with the default `LocalDate.now()` instead of the reactive `todayDate` from the ViewModel | ✅ Fixed |
| P9-18 | `calculateNextTriggerTime()` in `HabitReminderScheduler` called `ZonedDateTime.now()` / `LocalDate.now()` multiple times (TOCTOU) | ✅ Fixed |

---

## Fix Details

### P3-11 + P9-18 — `HabitReminderScheduler.kt` & `PetMoodScheduler.kt`
- Added `ALARM_WINDOW_MS = 15 * 60 * 1000L` constant.
- Changed both fallback alarm paths from `setAndAllowWhileIdle()` → `setWindow(AlarmManager.RTC_WAKEUP, triggerTime, ALARM_WINDOW_MS, pendingIntent)`.
- `PetMoodScheduler`: added `canScheduleExactAlarms()` guard (API 31+) and defensive `SecurityException` catch, both using `setWindow()`.
- Captured `val now = ZonedDateTime.now()` once in `calculateNextTriggerTime()`; derived `today` from `now.toLocalDate()`. All sub-functions (`calculateNextWeeklyTrigger`, `calculateNextMonthlyTrigger`) now accept `now: ZonedDateTime` as a parameter instead of calling `ZonedDateTime.now()` independently.
- Replaced standalone `ZoneId.systemDefault()` usages to use the correct top-level import.

### P4-03 — `UserPreferences.kt`
- Removed unused `setPoints(amount: Int)` and `setLifetimeXp(amount: Int)` methods.
- Only the combined `setPointsAndLifetimeXp()` method remains.

### P4-05 — `BillingRepository.kt`
- Updated KDoc on `release()` to explain it is intentionally never called from any ViewModel: the `@Singleton` outlives all ViewModels, so calling it from `onCleared()` would kill the billing connection on every navigation away. Play Billing best practice is to keep the connection alive for the process lifetime.

### P4-10 — `mutations.gql`
- Added a `user_updateMany` step to the `DeleteHousehold` mutation (as Step 2, between `habit_updateMany` and `household_deleteMany`) to null out `User.household` FK for all household members before deleting the household row.
- SDK regenerated: `npx firebase-tools@latest dataconnect:sdk:generate`.
- Deployed to production: `npx firebase-tools@latest deploy --only dataconnect --project choreboo-7f36c`.

### P9-17 — `PetViewModel.kt`, `StatsViewModel.kt`, `PetScreen.kt`, `StatsScreen.kt`
- Added `todayLocalDate: StateFlow<LocalDate>` to both ViewModels (derived by mapping the existing `_todayDate` string → `LocalDate.parse()`).
- `PetScreen`: collects `todayLocalDate`; passes it explicitly to both `isScheduledForToday()` calls; updated `remember` key to include `todayLocalDate`.
- `StatsScreen`: collects `todayLocalDate`; passes it explicitly to `isScheduledForToday()` call.

---

## Build & Test Results (final)

- `assembleDebug` — **BUILD SUCCESSFUL**
- `testDebugUnitTest` — **BUILD SUCCESSFUL** (455 tests pass)
- Warnings present are pre-existing (`enablePendingPurchases()` deprecation, `ExperimentalCoroutinesApi` opt-in suggestions) — none introduced by our changes.

# Choreboo Audit & Remediation Plan

## Status: COMPLETE ✅

All phases of the original audit are done. A verification audit identified 6 remaining issues — all 6 are fixed, the build passes, and all 265+ unit tests pass.

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
- `testDebugUnitTest` — **BUILD SUCCESSFUL** (all tests pass)
- Warnings present are pre-existing (`enablePendingPurchases()` deprecation, `ExperimentalCoroutinesApi` opt-in suggestions) — none introduced by our changes.

# PLAN.md — Choreboo Comprehensive Audit & Remediation Plan

This file tracks all actionable findings from a full 6-agent parallel audit (2026-04-12)
covering repositories, ViewModels, UI, Data Connect backend, build/DI/security, domain
models, workers, theme, components, and test coverage. Phases 1–2 (prior security fixes
S1–S9) are complete. Phases 3–9 below are the remaining work.

**Severity legend:** CRITICAL = crash/data-loss/security-hole; HIGH = incorrect behavior
or significant risk; MEDIUM = reliability/UX degradation; LOW = cleanup/polish.

---

## Completed (Phases 1–2)

All items from the original Phase 1–2 security audit (S1–S9) are done:

| ID | Fix | Status |
|----|-----|--------|
| S1 | `.gitignore` — add `google-services.json`, keystores, service account keys | Done |
| S2 | Replace `android.util.Log` with Timber; `DebugTree` under `BuildConfig.DEBUG` | Done |
| S3 | `network_security_config.xml` — disable cleartext, restrict to system CAs | Done |
| S5 | `backup_rules.xml` / `data_extraction_rules.xml` — exclude Room DB, DataStore | Done |
| S6 | Remove `GetUserById` query from `queries.gql` | Done |
| S7 | Split `UpdateHabit` into `UpdateOwnHabit` + `UpdateAssignedHabit` | Done |
| S8 | Remove `IS_PREMIUM` DataStore cache; derive from BillingClient at runtime | Done |
| S9 | `generateInviteCode()` — `SecureRandom`, 8 chars, retry on collision | Done |

## Completed (Phase 3 — Critical Security & Data Integrity)

All 11 Phase 3 items are done (committed 2026-04-12):

| ID | Fix | Status |
|----|-----|--------|
| P3-01 | `google-services.json` removed from git tracking | Done |
| P3-02 | R8/ProGuard enabled (`isMinifyEnabled=true`, `isShrinkResources=true`), comprehensive `proguard-rules.pro` | Done |
| P3-03 | `POST_NOTIFICATIONS` permission check in all 4 receivers/workers via `NotificationUtils.notifyIfPermitted()` | Done |
| P3-04 | `cancelReminder()` now mirrors `scheduleReminder()` Intent action string so `FLAG_NO_CREATE` matches | Done |
| P3-05 | `@Stable` removed from `ChorebooStats`; `isSleeping` changed from computed property to `fun isSleeping()` | Done |
| P3-06 | `ChorebooStats` default params changed from `System.currentTimeMillis()` to `0L` | Done |
| P3-07 | `CreateHabitLog` — `@transaction` + embedded `@check` verifying caller is owner, assignee, or household member | Done |
| P3-08 | `CreateHabit` — `@transaction` + embedded `@check` verifying caller's `User.householdId` matches `$householdId` | Done |
| P3-09 | `NullifyHouseholdForMembers` — replaced `insecureReason` with WHERE filter traversing `household.createdById == auth.uid` | Done |
| P3-10 | `DeleteLogsForHabit` — now deletes ALL logs for the habit (not just caller's) scoped to habit owner via `habit.ownerId == auth.uid` | Done |
| P3-11 | `canScheduleExactAlarms()` runtime check added; falls back to `setWindow()` on API 31+ when denied | Done |

---

## Phase 3: Critical Security & Data Integrity

**All 11 items complete — see Completed (Phase 3) table above.**

---

## Phase 4: High-Priority Data Integrity & Reliability

Incorrect behavior, data corruption, or significant risk in production.

### P4-01 — Destructive migration wipes all local data (HIGH)
- **File:** `data/local/ChorebooDatabase.kt:58`
- **Problem:** `fallbackToDestructiveMigration()` silently deletes all Room data on any
  schema version change. In production, this means app updates that change the DB schema
  wipe the user's local cache without warning.
- **Fix:** Write proper Room migrations for each schema change. At minimum, replace with
  `fallbackToDestructiveMigrationOnDowngrade()` and add migration objects for upgrades.

### P4-02 — `refreshHouseholdPets()` wipes member identity data (HIGH)
- **File:** `data/repository/HouseholdRepository.kt:304-305`
- **Problem:** If no household members have created a Choreboo, the pet query returns empty.
  The method then calls `householdMemberDao.deleteAll()` which wipes ALL member rows
  including identity columns (name, email, photo) written by `refreshHouseholdMembers()`.
- **Fix:** Only delete pet-related columns (set to null) instead of deleting entire rows.
  Or skip the delete when the cloud response is empty.

### P4-03 — Non-atomic `setPoints()` / `setLifetimeXp()` in DataStore (HIGH)
- **File:** `data/datastore/UserPreferences.kt:79-85`
- **Problem:** Two separate `dataStore.edit {}` calls — if the app crashes between them,
  `totalPoints` and `totalLifetimeXp` get out of sync.
- **Fix:** Combine into a single `dataStore.edit {}` block that sets both values atomically.

### P4-04 — `deleteHabit()` cloud write-through is fire-and-forget (HIGH)
- **File:** `data/repository/HabitRepository.kt:206-219`
- **Problem:** Habit is deleted from Room immediately, then cloud deletion is attempted
  without retry. If cloud delete fails, the next sync re-downloads the habit from the cloud.
  The habit "reappears" after the user thought they deleted it.
- **Fix:** Either retry cloud deletion with backoff, or mark the habit as
  "pending-delete" locally and reconcile on next sync.

### P4-05 — `BillingRepository` never calls `endConnection()` (HIGH)
- **File:** `data/repository/BillingRepository.kt:63-70`
- **Problem:** `BillingClient.startConnection()` is called but `endConnection()` is never
  called. This leaks the service connection and can cause ANRs.
- **Fix:** Add `endConnection()` in a lifecycle-aware cleanup method. Consider tying the
  billing client lifecycle to the Activity or Application lifecycle.

### P4-06 — Data Connect: no unique constraint on `HabitLog(habit, date)` (HIGH)
- **File:** `dataconnect/schema/schema.gql:91-104`
- **Problem:** Room has `UNIQUE(habitId, date)` but the cloud schema does not. A race
  condition or retry can create duplicate completion logs in the cloud.
- **Fix:** Add `@@unique(fields: ["habit", "date", "completedBy"])` to the `HabitLog` table.

### P4-07 — Data Connect: `UpdateUserPoints` no server-side validation (HIGH)
- **File:** `dataconnect/choreboo-connector/mutations.gql:28-33`
- **Problem:** Accepts arbitrary `totalPoints` and `totalLifetimeXp` values. A compromised
  client can set points to any value.
- **Fix:** Add `@check` constraints (e.g., `totalPoints >= 0`, `totalLifetimeXp >= 0`).
  Consider server-side increment mutations instead of absolute-value updates.

### P4-08 — Data Connect: `UpdateAssignedHabit` allows changing `difficulty` and `baseXp` (HIGH)
- **File:** `dataconnect/choreboo-connector/mutations.gql:293-322`
- **Problem:** An assignee can change `difficulty` and `baseXp`, which affects XP earned
  on completion. These should be owner-only fields.
- **Fix:** Remove `difficulty` and `baseXp` from the `UpdateAssignedHabit` mutation's
  settable fields.

### P4-09 — Data Connect: `UpdateOwnHabit` no `householdId` verification (HIGH)
- **File:** `dataconnect/choreboo-connector/mutations.gql:254-289`
- **Problem:** Owner can set `householdId` to any household UUID without verifying
  membership. Could move a habit into an arbitrary household.
- **Fix:** Add a membership check for the target `householdId`.

### P4-10 — Data Connect: `DeleteHousehold` leaves orphaned habits (HIGH)
- **File:** `dataconnect/choreboo-connector/mutations.gql:63-67`
- **Problem:** Deleting a household doesn't null out or delete habits that reference it.
  Those habits become orphans with a dangling `householdId` FK.
- **Fix:** Before deleting, null out `householdId` on all habits referencing the household,
  or delete them. Also null out `household` on all member `User` records.

### P4-11 — `HabitEntity.ownerUid` is nullable but queries filter with `=` (HIGH)
- **File:** `data/local/entity/HabitEntity.kt:22`
- **Problem:** `ownerUid` is `String?` (nullable). DAO queries that filter
  `WHERE ownerUid = :uid` will never match rows where `ownerUid` is null (SQL null != null).
  Any habit inserted without an `ownerUid` becomes invisible in queries.
- **Fix:** Make `ownerUid` non-null with a default of `""`, or use `IS` comparison in
  queries where null matching is intended.

### P4-12 — `Habit.isScheduledForToday()` D31 overflow for months < 31 days (HIGH)
- **File:** `domain/model/Habit.kt:43-50`
- **Problem:** Monthly schedule special-case checks for "D31" but doesn't handle D28, D29,
  D30 in months with fewer days. A habit scheduled for D31 won't trigger in February (28/29
  days), April (30 days), etc.
- **Fix:** If the scheduled day exceeds the month's max days, treat the last day of the
  month as the trigger day.

### P4-13 — `HouseholdHabitStatusDao.replaceAll()` never removes deleted habits (HIGH)
- **File:** `data/local/dao/HouseholdHabitStatusDao.kt:34-35`
- **Problem:** `replaceAll()` uses `INSERT OR REPLACE` for incoming statuses but never
  deletes statuses for habits that no longer exist in the cloud. Stale rows accumulate.
- **Fix:** Wrap in a `@Transaction`: delete all rows for the given date, then insert
  the fresh set. Or delete rows whose `habitId` is not in the incoming set.

### P4-14 — No release signing configuration (HIGH)
- **File:** `app/build.gradle.kts`
- **Problem:** No `signingConfigs` block for release builds. Cannot produce a signed
  release APK/AAB for Play Store.
- **Fix:** Add a `signingConfigs` block reading keystore path/passwords from environment
  variables or `local.properties` (not checked into git).

### P4-15 — Duplicate `calculateNextCriticalTime` methods in `PetMoodScheduler` (HIGH)
- **File:** `worker/PetMoodScheduler.kt:122-158` and `171-207`
- **Problem:** Two nearly identical methods exist. If one is fixed but not the other,
  behavior diverges silently.
- **Fix:** Delete the duplicate and have all call sites use the single remaining method.

### P4-16 — Workers bypass Hilt DI for database access (HIGH)
- **Files:**
  - `worker/HabitReminderReceiver.kt:38` — calls `ChorebooDatabase.getInstance()` directly
  - `worker/PetMoodReceiver.kt:42` — accesses DB directly despite `@AndroidEntryPoint`
  - `worker/PetMoodCheckWorker.kt:36` — bypasses injected dependencies
- **Problem:** These components create their own DB instances outside the Hilt DI graph,
  potentially causing multiple Room database instances (and WAL corruption).
- **Fix:** Use `@AndroidEntryPoint` with `@Inject` fields for receivers, and
  `@HiltWorker` with `@AssistedInject` for workers.

---

## Phase 5: Cloud Security & Schema Hardening ✅ COMPLETE

Server-side validation gaps and schema improvements.

### P5-01 — Data Connect: `PurchaseBackground` no server-side point verification (MEDIUM) ✅
- **File:** `dataconnect/choreboo-connector/mutations.gql:199-206`
- **Problem:** Client deducts points locally and then calls the purchase mutation. A
  compromised client can skip the deduction and purchase for free.
- **Fix:** Add a server-side check that verifies `User.totalPoints >= cost` before inserting
  the `PurchasedBackground` row and decrementing points atomically.

### P5-02 — Data Connect: `InsertChoreboo` no stat validation (MEDIUM) ✅
- **File:** `dataconnect/choreboo-connector/mutations.gql:71-98`
- **Problem:** Accepts arbitrary values for hunger, happiness, energy, xp, level. A
  compromised client can create a max-stat Choreboo.
- **Fix:** Add `@check` constraints or ignore client-provided stats and use server defaults.

### P5-03 — Data Connect: missing indexes on frequently queried columns (MEDIUM) ✅
- **File:** `dataconnect/schema/schema.gql`
- **Problem:** No `@@index` on `HabitLog.date`, `Habit.owner`, `HabitLog.completedBy`.
  These columns appear in WHERE clauses of multiple queries.
- **Fix:** Add `@@index(fields: ["date"])` on HabitLog, `@@index(fields: ["owner"])` on
  Habit, `@@index(fields: ["completedBy"])` on HabitLog.

### P5-04 — `connector.yaml` `authMode: PUBLIC` is overly permissive (MEDIUM) ✅
- **File:** `dataconnect/choreboo-connector/connector.yaml:2`
- **Problem:** `authMode: PUBLIC` means unauthenticated requests are accepted at the
  connector level (each operation has its own `@auth` check). If any operation accidentally
  omits `@auth`, it's exposed publicly.
- **Fix:** Change to `authMode: USER_REQUIRED` so the connector rejects unauthenticated
  requests by default.

### P5-05 — Data Connect: `CreateHabitLog` TOCTOU race on completion (MEDIUM) ✅
- **File:** `dataconnect/choreboo-connector/mutations.gql:380-397`
- **Problem:** Without a cloud-side unique constraint (see P4-06), two concurrent
  `CreateHabitLog` calls for the same habit+date can both succeed, awarding double XP.
- **Fix:** Addressed by P4-06 (add unique constraint). Also consider server-side
  idempotency key.

### P5-06 — Dead queries in `queries.gql` (LOW) ✅
- **File:** `dataconnect/choreboo-connector/queries.gql`
- **Queries:** `GetMyHabits` (133-158), `GetHabitById` (249-290), `GetLogsForHabit`
  (296-328), `GetMyLogsForDate` (330-341)
- **Problem:** These queries are not called from any client code. They increase the
  connector's surface area and maintenance burden.
- **Fix:** Remove unused queries. Deploy with `--force` to acknowledge breaking changes.

### P5-07 — Dead mutation `DeleteHabitLog` (LOW) ✅
- **File:** `dataconnect/choreboo-connector/mutations.gql:400-404`
- **Problem:** Not called from any client code.
- **Fix:** Remove. Deploy with `--force`.

### P5-08 — String-typed enums in cloud schema (LOW) ✅
- **File:** `dataconnect/schema/schema.gql`
- **Problem:** `stage`, `petType`, `difficulty` are plain `String` columns. No server-side
  validation of allowed values.
- **Fix:** Consider adding `@check` constraints or switching to GraphQL enums if Data
  Connect supports them.

---

## Phase 6: Medium-Priority Reliability & UX ✅

Bugs, degraded UX, and reliability improvements.

### P6-01 ✅ — `SettingsViewModel.resetAccount()` missing try/finally for `_isResetting` (MEDIUM)
### P6-02 ✅ — `OnboardingScreen` uses `remember` instead of `rememberSaveable` (MEDIUM)
### P6-03 ✅ — Onboarding Hatch button no double-tap debounce (already guarded, no change needed) (MEDIUM)
### P6-04 ✅ — `SettingsScreen` CredentialManager created with Activity context in `remember` (MEDIUM)
### P6-05 ✅ — `BannerAdView` holds Activity context, no cleanup (MEDIUM)
### P6-06 ✅ — `WebmAnimationView` ImageView holds Activity context (MEDIUM)
### P6-07 ✅ — `CalendarViewModel.isLoading` cleared prematurely (MEDIUM)
### P6-08 ✅ — `applyStatDecay()` integer division (sub-hour decay always 0) (MEDIUM)
### P6-09 ✅ — `refreshHouseholdHabits()` uses `LocalDate.now()` not reactive date (MEDIUM)
### P6-10 ✅ — `SettingsScreen` snackbar host passed as empty lambda (MEDIUM)
### P6-11 ✅ — Fragile Activity casts for Google sign-in (MEDIUM)
### P6-12 ✅ — `AppLifecycleObserver` uses unscoped CoroutineScope (MEDIUM)
### P6-13 ✅ — `BackgroundRepository` not reactive to auth state changes (MEDIUM)
### P6-14 ✅ — `BillingRepository` unbounded retry on connection failure (MEDIUM)
### P6-15 ✅ — `writeScope` in repositories never properly cancelled (MEDIUM)
### P6-16 ✅ — Standalone mood colors not theme-aware (MEDIUM)
### P6-17 ✅ — `StitchSnackbar` uses `actionLabel` as type discriminator (MEDIUM)

---

## Phase 7: Build Optimization & Code Quality

Non-blocking improvements to build, dependencies, and code cleanliness.

### P7-01 — Remove unused Gson `Converters.kt` if confirmed unused (MEDIUM)
- **File:** `data/local/converter/Converters.kt`
- **Problem:** The Gson TypeConverter for `List<String>` may be unused if `customDays` is
  stored as a comma-separated string. Verify usage before removing.
- **Action:** Grep for `Converters` in `ChorebooDatabase.kt` `@TypeConverters` annotation.
  If used, keep. If not, remove and delete Gson dependency if no other usage.

### P7-02 — Gradle parallel builds and caching disabled (MEDIUM)
- **File:** `gradle.properties:13`
- **Problem:** `org.gradle.parallel` and `org.gradle.caching` are commented out. Builds
  are slower than necessary.
- **Fix:** Uncomment `org.gradle.parallel=true` and `org.gradle.caching=true`.

### P7-03 — `com.example.*` applicationId not changed (MEDIUM)
- **File:** `app/build.gradle.kts`
- **Problem:** `applicationId = "com.example.choreboo_habittrackerfriend"` — the `com.example`
  prefix cannot be published to the Play Store.
- **Fix:** Change to a proper reverse-domain (e.g., `com.choreboo.habittracker`). This
  requires a fresh install on all devices (different app identity).

### P7-04 — Static `versionCode = 1` (MEDIUM)
- **File:** `app/build.gradle.kts:19-20`
- **Problem:** `versionCode` never incremented. Play Store requires monotonically increasing
  version codes for each upload.
- **Fix:** Automate via CI (e.g., build number) or manually increment on release.

### P7-05 — `material-icons-extended` included without R8 (MEDIUM)
- **File:** `app/build.gradle.kts:114`
- **Problem:** `material-icons-extended` adds ~24MB of icon classes. Without R8 tree-shaking
  (P3-02), all icons ship in the APK.
- **Fix:** Enabling R8 (P3-02) will tree-shake unused icons. Alternatively, copy only the
  needed icon files directly.

### P7-06 — `dataconnectCompile` task swallows failures (MEDIUM)
- **File:** `build.gradle.kts:24` (project-level)
- **Problem:** `isIgnoreExitValue = true` means SDK generation failures are silently ignored.
  Stale generated code may be used without warning.
- **Fix:** Remove `isIgnoreExitValue = true`. Let the build fail if SDK gen fails.

### P7-07 — AdMob production App ID hardcoded in manifest (MEDIUM)
- **File:** `AndroidManifest.xml:26-28`
- **Problem:** Production AdMob App ID is hardcoded. Should use a build config field to
  separate debug/release IDs.
- **Fix:** Use `manifestPlaceholders` or `buildConfigField` to inject the ID per build type.

### P7-08 — Test ad unit ID hardcoded in `BannerAdView` (MEDIUM)
- **File:** `ui/components/BannerAdView.kt:27`
- **Problem:** Test ad unit ID (`ca-app-pub-3940256099942544/...`) is hardcoded. Needs to
  be swapped for production ID in release builds.
- **Fix:** Use `BuildConfig` field to switch between test and production ad unit IDs.

### P7-09 — Missing `hilt-compiler` KSP for `@HiltWorker` (MEDIUM)
- **File:** `app/build.gradle.kts`
- **Problem:** `@HiltWorker` annotation is used but `androidx.hilt:hilt-compiler` KSP
  processor may not be configured. Verify and add if missing.
- **Fix:** Add `ksp("androidx.hilt:hilt-compiler:1.2.0")` to dependencies.

### P7-10 — `network_security_config.xml` no debug overrides (LOW)
- **File:** `app/src/main/res/xml/network_security_config.xml`
- **Problem:** No `<debug-overrides>` section. Developers can't use HTTP proxies
  (e.g., Charles) for debugging API calls.
- **Fix:** Add `<debug-overrides trustUserCerts="true">` section.

### P7-11 — Unused Glance dependencies (LOW)
- **File:** `app/build.gradle.kts:109-111`
- **Problem:** Glance widget dependencies are included but the widget is not implemented.
  Adds unnecessary APK size and build time.
- **Fix:** Remove until the widget feature is implemented.

### P7-12 — `build.ps1` hardcoded Windows path (LOW)
- **File:** `build.ps1:31`
- **Problem:** Hardcoded `C:\Program Files\Android\Android Studio\jbr` as `JAVA_HOME`
  fallback. Fails if Android Studio is installed elsewhere.
- **Fix:** Make the path configurable or search common installation locations.

### P7-13 — `settings.gradle.kts` includes `foojay-resolver-convention` plugin (LOW)
- **File:** `settings.gradle.kts:15`
- **Problem:** The `foojay-resolver-convention` plugin auto-downloads JDK toolchains, but
  the project uses Android Studio's bundled JBR. Unnecessary plugin.
- **Fix:** Remove the plugin declaration.

### P7-14 — Explicit `buildToolsVersion` pinned (LOW)
- **File:** `app/build.gradle.kts:14`
- **Problem:** `buildToolsVersion` is explicitly set. AGP auto-selects the appropriate
  version; pinning can cause version mismatch issues.
- **Fix:** Remove the explicit `buildToolsVersion` line.

### P7-15 — `popUpTo(0)` fragile navigation (LOW)
- **File:** `navigation/ChorebooNavGraph.kt:124`
- **Problem:** `popUpTo(0)` is an undocumented/fragile way to pop the entire back stack.
  May break if the nav graph structure changes.
- **Fix:** Use `popUpTo(navController.graph.startDestinationId) { inclusive = true }`.

### P7-16 — `kotlinx-serialization-core` without `json` (LOW)
- **File:** `gradle/libs.versions.toml` or `app/build.gradle.kts`
- **Problem:** `kotlinx-serialization-core` is included but `kotlinx-serialization-json`
  is not. Without the json module, serialization is limited.
- **Fix:** If serialization is actually used, add `json` module. If not, remove both.

### P7-17 — AdMob initialized in debug builds (LOW)
- **File:** `ChorebooApplication.kt:33`
- **Problem:** `MobileAds.initialize()` runs in debug builds, making network calls to
  AdMob servers during development.
- **Fix:** Guard with `if (!BuildConfig.DEBUG)` or use test device configuration.

### P7-18 — Stale backup rule references (LOW)
- **Files:** `app/src/main/res/xml/backup_rules.xml`, `data_extraction_rules.xml`
- **Problem:** References to GMS auth shared preferences (`com.google.android.gms.signin`)
  that are no longer used (migrated to Credential Manager).
- **Fix:** Remove the stale exclusion entries.

### P7-19 — `BootReceiver.kt` unused imports (LOW)
- **File:** `worker/BootReceiver.kt:9-11`
- **Fix:** Remove unused import statements.

---

## Phase 8: Test Coverage & Reliability

Expand the test suite from 265 tests (~50% coverage) toward comprehensive coverage.

### P8-01 — Add `AuthRepository` tests (HIGH)
- **Missing:** Login, register, Google sign-in, password reset, sign-out flows.
  Validation guards for email/password non-blank.
- **File to create:** `src/test/.../data/repository/AuthRepositoryTest.kt`

### P8-02 — Add `HouseholdViewModel` tests (HIGH)
- **Missing:** Create household, join household, leave household, refresh members,
  refresh pets, refresh habits, member habit dialog, event emission.
- **File to create:** `src/test/.../ui/household/HouseholdViewModelTest.kt`

### P8-03 — Add `BackgroundRepository` tests (MEDIUM)
- **Missing:** Purchase background, sync from cloud, get purchased backgrounds.
- **File to create:** `src/test/.../data/repository/BackgroundRepositoryTest.kt`

### P8-04 — Add `ResetRepository` tests (MEDIUM)
- **Missing:** Full reset sequence, partial failure handling, cleanup ordering.
- **File to create:** `src/test/.../data/repository/ResetRepositoryTest.kt`

### P8-05 — Add `OnboardingViewModel` tests (MEDIUM)
- **Missing:** Name validation, pet type selection, hatching flow, event emission.
- **File to create:** `src/test/.../ui/onboarding/OnboardingViewModelTest.kt`

### P8-06 — Add `StatsViewModel` tests (LOW)
- **Missing:** Stat aggregation, date range filtering, refresh flow.
- **File to create:** `src/test/.../ui/stats/StatsViewModelTest.kt`

### P8-07 — Add `BadgeRepository` tests (LOW)
- **Missing:** Badge unlock logic, badge listing.
- **File to create:** `src/test/.../data/repository/BadgeRepositoryTest.kt`

### P8-08 — Fix flaky `Thread.sleep` in `AppLifecycleObserverTest` (MEDIUM)
- **File:** `src/test/.../di/AppLifecycleObserverTest.kt:65,75,88`
- **Problem:** Uses `Thread.sleep(200)` to wait for coroutine completion. Flaky on slow CI.
- **Fix:** Use `advanceUntilIdle()` or `runCurrent()` from `TestCoroutineScheduler`.

### P8-09 — Fix flaky `LocalDate.now()` in test assertions (MEDIUM)
- **Files:**
  - `HabitRepositoryTest.kt:195-196,243-245,285-287`
  - `HabitTest.kt:28-37`
  - `CalendarViewModelTest.kt:78-84`
- **Problem:** Tests use `LocalDate.now()` for assertions. Can fail if the test runs at
  midnight (date rolls over between setup and assertion).
- **Fix:** Inject a `Clock` or fixed date into the classes under test.

### P8-10 — Replace `flowOf()` with `MutableStateFlow` in mock flows (MEDIUM)
- **Files:** Multiple test files (4+)
- **Problem:** `flowOf()` completes immediately. Downstream `stateIn(WhileSubscribed)`
  may not collect the value before the flow completes.
- **Fix:** Use `MutableStateFlow(value)` which stays active.

### P8-11 — Add missing critical test scenarios (MEDIUM)
- **Scenarios not currently tested:**
  - `ChorebooRepository.feedChoreboo()` — point deduction, stat increase, write-through
  - `ChorebooRepository.applyStatDecay()` — decay calculation, boundary values
  - `ChorebooRepository.syncFromCloud()` — conflict resolution, cloud-wins merge
  - `HabitRepository.syncHabitsFromCloud()` — deletion reconciliation
  - `HouseholdRepository.createHousehold()` — success path (only validation tested)
  - `HouseholdRepository.joinHousehold()` — success path
  - `HouseholdRepository.leaveHousehold()` — full flow
  - `BackgroundRepository.purchaseBackground()` — point deduction, purchase record
- **Fix:** Add test cases for each scenario.

### P8-12 — `BillingRepositoryTest` tests simulated helpers not real class (LOW)
- **File:** `src/test/.../data/repository/BillingRepositoryTest.kt`
- **Problem:** Tests exercise helper functions rather than the actual `BillingRepository`
  class methods with mocked `BillingClient`.
- **Fix:** Rewrite to test the actual repository with a mocked `BillingClient`.

---

## Phase 9: Low-Priority Polish

Cleanup items that improve code quality but don't affect functionality.

### P9-01 — Archive/unarchive habit has no cloud retry (LOW)
- **File:** `data/repository/HabitRepository.kt`
- **Fix:** Add retry logic consistent with other write-through patterns.

### P9-02 — UID variable shadowing in repositories (LOW)
- **Files:** Multiple repository files
- **Fix:** Rename shadowed variables to avoid confusion.

### P9-03 — Missing `cachedDate` index on `household_habit_statuses` (LOW)
- **File:** `data/local/entity/HouseholdHabitStatusEntity.kt`
- **Fix:** Add `@Index` annotation on `cachedDate` column.

### P9-04 — Hardcoded snackbar bottom positioning (LOW)
- **Files:** Multiple Screen composables
- **Fix:** Use proper `SnackbarHost` positioning via Scaffold.

### P9-05 — Hardcoded background price strings (LOW)
- **Files:** UI composables showing background prices
- **Fix:** Move to `strings.xml` with quantity formatting.

### P9-06 — `startDestination` not reactive to auth changes (LOW)
- **File:** `MainActivity.kt:65-70`
- **Problem:** Start destination is computed once. If auth state changes while the activity
  is alive, the nav graph doesn't react.
- **Fix:** Navigate programmatically on auth state change rather than relying on
  `startDestination`.

### P9-07 — `PetMoodScheduler.kt` stat==20 edge case (LOW)
- **File:** `worker/PetMoodScheduler.kt:129,178`
- **Problem:** Critical threshold check uses `<= 20` but the notification logic may not
  trigger exactly at 20 (off-by-one).
- **Fix:** Verify threshold logic and add a test case.

### P9-08 — `HabitReminderReceiver` LocalDate.now() TOCTOU in reschedule (LOW)
- **File:** `worker/HabitReminderReceiver.kt:36,60-68`
- **Problem:** Two `LocalDate.now()` calls — if date rolls between them, the wrong day
  is used for schedule vs. rescheduling.
- **Fix:** Capture `LocalDate.now()` once at the top and reuse.

### P9-09 — `WebmAnimationView` onComplete callback capture (LOW)
- **File:** `ui/components/WebmAnimationView.kt:75,101`
- **Problem:** `onComplete` lambda is captured in a `remember` block and may go stale.
- **Fix:** Use `rememberUpdatedState(onComplete)` pattern.

### P9-10 — Duplicated notification ID logic across receivers/workers (LOW)
- **Files:** `HabitReminderReceiver`, `HabitCompleteReceiver`, `PetMoodCheckWorker`,
  `PetMoodReceiver`
- **Fix:** Extract shared notification constants and builder to a common utility class.

### P9-11 — Typography name shadowing (LOW)
- **File:** `ui/theme/Type.kt:23`
- **Fix:** Rename to avoid shadowing the `Typography` class name.

### P9-12 — Color aliases could be removed (LOW)
- **File:** `ui/theme/Color.kt`
- **Fix:** Consolidate redundant color aliases.

### P9-13 — `Badge.kt` imports `ImageVector` in domain layer (LOW)
- **File:** `domain/model/Badge.kt:3-14`
- **Problem:** Domain model depends on `androidx.compose.ui.graphics.vector.ImageVector`,
  coupling the domain layer to the UI framework.
- **Fix:** Use a `String` icon reference and resolve to `ImageVector` in the UI layer.

### P9-14 — Dark theme scrim color incorrect (LOW)
- **File:** `ui/theme/Theme.kt:59,95`
- **Fix:** Set proper `scrim` color for dark scheme.

### P9-15 — Missing `surfaceBright` in dark color scheme (LOW)
- **File:** `ui/theme/Theme.kt`
- **Fix:** Add `surfaceBright` token to dark scheme.

### P9-16 — `Background.kt` KDoc inconsistency (LOW)
- **File:** `domain/model/Background.kt:5-6,11`
- **Fix:** Update KDoc to match actual field purposes.

### P9-17 — `Habit.isScheduledForToday()` is impure (LOW)
- **File:** `domain/model/Habit.kt:31,40,41`
- **Problem:** Calls `LocalDate.now()` inside a domain model method. Makes testing harder.
- **Fix:** Accept a `today: LocalDate` parameter.

### P9-18 — `HabitReminderScheduler` TOCTOU between LocalDate and ZonedDateTime (LOW)
- **File:** `worker/HabitReminderScheduler.kt:109,115`
- **Fix:** Capture the current instant once and derive both date and zoned time from it.

### P9-19 — `PetMoodReceiver.kt` unused import (LOW)
- **File:** `worker/PetMoodReceiver.kt:8`
- **Fix:** Remove unused import.

### P9-20 — Redundant `UpdateChorebooFull` mutation (LOW)
- **File:** `dataconnect/choreboo-connector/mutations.gql`
- **Fix:** Remove if `UpdateChoreboo` covers all use cases.

---

## Deferred (Out of Scope)

| ID | Description |
|----|-------------|
| G14 | 30-day habit log sync limit — older logs never synced to new devices |
| L5 | No pagination on habit log queries |
| L11 | No deep link support for household invite codes |

---

## Not Yet Implemented Features

| Feature | Notes |
|---------|-------|
| Glance widget | Today's habits + pet mood on home screen |
| Sound effects | Play on habit completion, feeding, level-up |
| WebP animations for AXOLOTL, CAPYBARA, PANDA | Currently emoji placeholders |
| Multiple Choreboos | Architecture supports it; UI does not |
| Inventory screen | `ui/inventory/` placeholder exists |
| Shop screen | `ui/shop/` placeholder exists |
| IME keyboard padding | `imePadding()` not applied to text-input screens |

---

## Build Notes

- Build: `powershell.exe -File build.ps1 assembleDebug`
- Tests: `powershell.exe -File build.ps1 testDebugUnitTest` (265 tests as of 2026-04-12)
- SDK regen: `npx firebase-tools@latest dataconnect:sdk:generate`
- Deploy: `npx firebase-tools@latest deploy --only dataconnect --project choreboo-7f36c`
- Always use `--force` on deploy when removing operations (breaking change acknowledgement).

---

*Last updated: 2026-04-12 — Phase 3 complete (all 11 CRITICAL items done). Phases 4–9 remain (~109 findings).*

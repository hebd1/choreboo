# AGENTS.md

## Project

Tamagotchi-style habit tracker Android app (Kotlin, Jetpack Compose, Material3). Users complete habits to earn XP and evolve a digital pet ("Choreboo"). Supports **households** — shared groups where members can see each other's pets and habits. Firebase Auth (email + Google sign-in) for identity; **Firebase Data Connect** (PostgreSQL) as cloud backend with **write-through sync** and **cloud-to-local sync on auth + app foreground**. Room is the local cache; DataStore for preferences.

## Architecture

MVVM: `Screen composable → @HiltViewModel → @Singleton Repository → @Dao → Room`

Repositories also call the **Firebase Data Connect generated SDK** for cloud write-through and cloud-to-local sync.

- **Entities** live in `data/local/entity/` — Room-annotated data classes with String-typed enums, `Long` timestamps, and `remoteId`/`ownerUid` fields for cloud sync.
- **Domain models** live in `domain/model/` — mirror entities but use Kotlin enums and typed fields. `Habit` has `isScheduledForToday()` helper.
- **Mapping** uses extension functions (`toDomain()` / `toEntity()`) defined at the bottom of each Repository file (private).
- **DI**: Single `AppModule` provides DB, all DAOs, DataStore, `UserPreferences`, and `FirebaseAuth`. All wiring is constructor injection.
- **Desugaring**: `isCoreLibraryDesugaringEnabled = true` — `java.time` APIs work on minSdk 24.
- **TypeConverters**: `data/local/converter/Converters.kt` uses Gson for `List<String>` conversion. Enums are stored as plain Strings (no TypeConverter).
- **Typography**: Custom `PlusJakartaSans` font family (5 weights: Light, Regular, Medium, SemiBold, Bold) applied to all Material3 typography styles in `Type.kt`.

## Cloud Sync Architecture

- **Backend**: Firebase Data Connect (PostgreSQL) — project `choreboo-7f36c`.
- **SDK generation**: `npx firebase-tools@latest dataconnect:sdk:generate` — a `dataconnectCompile` Exec task in root `build.gradle.kts` runs it. Generated SDK goes to `app/build/generated/sources/`.
- **Auth**: Firebase Auth (email/password + Google sign-in). `AuthRepository` wraps `FirebaseAuth`. `AuthViewModel` orchestrates login/register flows.
- **Write-through**: All Room mutations (habit CRUD, choreboo updates, habit log inserts) also fire the corresponding Data Connect mutation. Failures are **silent** — no user-facing error for write-through.
- **Cloud-to-local sync**: Triggered **after auth** (`force = true`, bypasses cooldown) and **on every app foreground** (`force = false`, 5-minute cooldown). `SyncManager` orchestrates all sync with a `Mutex` to prevent concurrent runs. `SyncManager.syncAll()` calls `getIdToken(false).await()` before any Data Connect calls to ensure the auth token is cached and available for the gRPC interceptor. Order: habits + choreboo + user points (parallel) → habit logs (sequential, needs habit remoteIds) → household habit logs (best-effort). Conflict resolution: **cloud wins**.
- **Error visibility**: Only the post-auth sync shows errors (snackbar). Write-through failures are silent.
- **Security**: All 16 queries and 16 mutations have `@auth(level: USER)` directives with auth-scoped filters. `connector.yaml` has `authMode: PUBLIC` (each operation has its own auth check). 4 household queries (`GetMyHousehold`, `GetMyHouseholdMembers`, `GetMyHouseholdChoreboos`, `GetMyHouseholdHabits`) are **inherently auth-scoped** — they traverse from `auth.uid` to the user's household, so no `householdId` parameter is needed and callers can only see their own household data.

### Data Connect Schema (5 cloud tables)

Defined in `dataconnect/schema/schema.gql`:

| Table | Key Fields |
|-------|------------|
| **User** | uid (String PK from Firebase Auth), displayName, email, photoUrl, household FK, createdAt, totalPoints, totalLifetimeXp |
| **Household** | UUID PK, name, inviteCode (unique), createdBy FK→User, createdAt |
| **Choreboo** | UUID PK, owner FK→User (unique), name, stage, level, xp, hunger, happiness, energy, petType, lastInteractionAt, createdAt, sleepUntil |
| **Habit** | UUID PK, owner FK→User, household FK→Household (nullable), assignedTo FK→User (nullable), title, description, iconName, customDays, difficulty, baseXp, reminderEnabled, reminderTime, isHouseholdHabit, isArchived, createdAt |
| **HabitLog** | UUID PK, habit FK→Habit, completedBy FK→User, completedAt, date, xpEarned, streakAtCompletion |

### Data Connect Files

- `dataconnect/dataconnect.yaml` — project config
- `dataconnect/schema/schema.gql` — 5 tables
- `dataconnect/choreboo-connector/connector.yaml` — authMode: PUBLIC
- `dataconnect/choreboo-connector/queries.gql` — 16 queries, all auth-scoped
- `dataconnect/choreboo-connector/mutations.gql` — 16 mutations, all auth-scoped

## Data Flow Patterns

- **Reactive reads**: DAOs return `Flow<>` → Repositories expose `Flow<>` → ViewModels expose `StateFlow` via `.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), default)`.
- **One-shot writes**: DAO methods are `suspend`; called from `viewModelScope.launch {}` in ViewModels. Write-through to Data Connect happens in the same suspend function.
- **One-shot events** (snackbars, navigation, level-up): `MutableSharedFlow` exposed as `events` in ViewModels (see `HabitListEvent`, `AddEditHabitEvent`, `PetEvent`, `AuthEvent`, `SettingsEvent`, `HouseholdEvent`).
- **Stat decay**: Calculated on PetScreen open via `chorebooRepository.applyStatDecay()` based on `lastInteractionAt`.
- **XP/Level-up**: `ChorebooRepository.addXp()` returns `XpResult(levelsGained, newLevel, evolved, newStage)` so callers can trigger celebration UI.
- **Habit completion**: `HabitRepository.completeHabit()` returns `CompletionResult(xpEarned, newStreak, alreadyComplete)` with targetCount enforcement.
- **Streaks**: `HabitRepository.getStreaksForToday()` returns `Flow<Map<Long, Int>>` for StreakBadge display.
- **Cloud-to-local sync**: `HabitRepository.syncHabitsFromCloud()`, `HabitRepository.syncHabitLogsFromCloud()`, `ChorebooRepository.syncFromCloud()`. Called from `SyncManager.syncAll()`.

## Room Database (v10, 3 entities)

| Table | Columns | Indexes |
|-------|---------|---------|
| **habits** | id, title, description, iconName, customDays, difficulty, baseXp, reminderEnabled, reminderTime, createdAt, isArchived, isHouseholdHabit, ownerUid, householdId, remoteId | `remoteId` |
| **habit_logs** | id, habitId (FK→habits CASCADE), completedAt, date (ISO string), xpEarned, streakAtCompletion, completedByUid, remoteId | `remoteId`, UNIQUE(`habitId`, `date`) |
| **choreboos** | id, name, stage, level, xp, hunger, happiness, energy, petType, lastInteractionAt, createdAt, sleepUntil, ownerUid, remoteId | `remoteId` |

- `remoteId` maps to the Data Connect UUID for cloud sync. Indexed on all 3 entities.
- `ownerUid` / `completedByUid` / `householdId` map to Firebase Auth UIDs and household references.
- `habit_logs` has a UNIQUE(`habitId`, `date`) index for atomic duplicate prevention on habit completion.
- `insertLog` uses `OnConflictStrategy.IGNORE` — returns -1L when duplicate is ignored.
- Uses `fallbackToDestructiveMigration()` during development.
- Schemas exported to `app/schemas/`.

## Build & Run

- **Gradle**: 9.3.1 wrapper, AGP 8.9.1, Kotlin 2.0.21, KSP 2.0.21-1.0.27
- **SDK**: minSdk 24, compileSdk 36, targetSdk 36
- **Version catalog**: All dependency versions in `gradle/libs.versions.toml`
- **Firebase project**: `choreboo-7f36c`
- **Data Connect SDK regen**: `npx firebase-tools@latest dataconnect:sdk:generate`

### Build Environment (WSL + Windows Android SDK)

> **IMPORTANT**: This project runs in WSL but Android Studio and the Android SDK are installed on Windows. All builds **must** go through `powershell.exe` calling `gradlew.bat`. Running `./gradlew` directly from WSL will fail because the SDK's build-tools contain Windows `.exe` binaries (`aapt2.exe`, etc.) that Linux cannot execute.
>
> Use `build.ps1` (project root) as a clean wrapper. `JAVA_HOME` is set persistently as a Windows User environment variable pointing to Android Studio's bundled JBR (`C:\Program Files\Android\Android Studio\jbr`); the script also auto-detects it as a fallback.

| Task | Command (run from WSL or any terminal) |
|------|----------------------------------------|
| Debug build | `powershell.exe -File build.ps1 assembleDebug` |
| Release build | `powershell.exe -File build.ps1 assembleRelease` |
| All unit tests | `powershell.exe -File build.ps1 testDebugUnitTest` |
| Single test class | `powershell.exe -File build.ps1 testDebugUnitTest --tests "com.example.choreboo_habittrackerfriend.ExampleUnitTest"` |
| Single test method | `powershell.exe -File build.ps1 testDebugUnitTest --tests "com.example.choreboo_habittrackerfriend.ExampleUnitTest.addition_isCorrect"` |
| Instrumented tests | `powershell.exe -File build.ps1 connectedDebugAndroidTest` (requires device/emulator) |
| Lint | `powershell.exe -File build.ps1 lint` |
| Gradle version check | `powershell.exe -File build.ps1 --version` |
| SDK regen | `npx firebase-tools@latest dataconnect:sdk:generate` |

> **Note**: No real tests exist yet — only default Android Studio template stubs. JUnit 4 for unit tests, AndroidX Test + Espresso + Compose UI Test JUnit4 for instrumented tests. No lint configuration, detekt, or ktlint is set up.

## Code Style

**General**: `kotlin.code.style=official` is set in `gradle.properties`. No formatter or linter is configured — follow existing patterns.

### Formatting
- **Indentation**: 4 spaces, no tabs.
- **Braces**: Opening brace on same line as declaration.
- **Trailing commas**: Always use on multiline parameter lists, function arguments, annotation arguments, and list literals.
- **Modifier chains**: Line-break per modifier call:
  ```kotlin
  Modifier
      .fillMaxSize()
      .padding(padding)
      .padding(horizontal = 16.dp)
  ```

### Naming Conventions
- **Classes**: PascalCase. Suffixes: `Entity`, `Dao`, `Screen`, `ViewModel`, `Repository`.
- **Functions**: camelCase. Composable functions use PascalCase.
- **Constants**: `SCREAMING_SNAKE_CASE` (e.g., `REMINDER_CHANNEL_ID`).
- **Private mutable state**: Prefix with `_` (e.g., `_events`, `_formState`, `_selectedMonth`).
- **Result types**: `<Action>Result` (e.g., `CompletionResult`, `XpResult`).
- **Event sealed classes**: `<Feature>Event` (e.g., `HabitListEvent`, `AuthEvent`, `HouseholdEvent`).

### Imports
- No enforced ordering — loosely grouped by Android/Compose, project, stdlib, javax.
- No wildcard imports. No blank lines between import groups.

### Error Handling
- **Enum parsing**: `try { EnumType.valueOf(field) } catch (_: Exception) { DEFAULT }` in all `toDomain()` functions. Always use `_` for the ignored exception.
- **Null safety**: Use `?.` and early `return` instead of throwing:
  ```kotlin
  val choreboo = chorebooDao.getChorebooSync() ?: return
  ```
- **Sealed class results** for operation outcomes — never throw for business logic errors:
  - `CompletionResult` (with `alreadyComplete` flag)
  - `XpResult` (levelsGained, newLevel, evolved, newStage)
- **No try/catch in ViewModels or Screen composables** — coroutine failures propagate.
- **Write-through failures** are silently caught in Repositories (no user-facing error).

## File Organization

- **Result/sealed classes** for operation outcomes: defined at **top** of Repository file, before the class.
- **Event sealed classes**: defined at **bottom** of ViewModel file, after the class.
- **Helper data classes** (e.g., `HabitStreak`): at **bottom** of DAO files.
- **Mapping functions** (`toDomain()` / `toEntity()`): private extension fns at **bottom** of Repository files.
- **Private val constants** (e.g., `iconOptions`, `daysOfWeek`): at **top** of Screen files, before the composable.
- Some small composables live inline in their Screen file (e.g., `FeedBottomSheet`) rather than in the `components/` subfolder.

## Key Conventions

- **Package**: `com.example.choreboo_habittrackerfriend`
- **Enums stored as `String` in Room** — parse with `try/catch` in `toDomain()`.
- **Dates**: ISO-8601 strings (`"2026-03-24"`) in `habit_logs.date`; timestamps (`Long`) for `createdAt`, `lastInteractionAt`.
- **`customDays`**: Comma-separated string in entity (`"MON,TUE,WED"`), `List<String>` in domain model.
- **Navigation**: `Screen` sealed class in `navigation/ChorebooNavGraph.kt`. Start destination is dynamic: Auth → Onboarding → HabitList. 5 bottom tabs (`habits_list`, `pet`, `household`, `calendar`, `settings`); bottom bar hidden on `auth`, `onboarding`, `add_edit_habit`.
- **Destructive actions** (delete habit) require `AlertDialog` confirmation.
- **Level-up celebrations** shown via `AlertDialog` when XP causes level/stage change.
- **Cloud sync**: `remoteId` field on all 3 Room entities links local rows to Data Connect UUIDs. Write-through on every mutation; cloud-to-local on auth + app foreground (via `SyncManager`).

## UI Rules

- Material3 only — use `MaterialTheme.colorScheme.*` and `MaterialTheme.typography.*`. No hardcoded text styles.
- Dynamic color is **disabled** (`dynamicColor = false`) — custom Choreboo palette always applied. Primary `#006E1C` (deep green), Secondary `#8B5000` (warm orange), Tertiary `#6833EA` (purple). See `Color.kt` for full Stitch design system.
- Card corners: `RoundedCornerShape(16.dp)`. Input corners: `RoundedCornerShape(12.dp)`.
- Always handle `innerPadding` from `Scaffold`. Use `Modifier.fillMaxWidth()` over hardcoded widths.
- Habit icons: **emoji-based** system (`EmojiIcon` data class with 15 preset emoji + custom emoji input). Not Material Icons.
- Touch targets >= 48dp. Empty states show emoji + friendly message + CTA.
- Use `AlertDialog` for confirmations; `ModalBottomSheet` for selection lists (feed).
- Pet animations: emoji placeholders per `ChorebooMood`; designed to swap to Lottie JSON in `res/raw/`.
- Pet size scales by `ChorebooStage`.
- **Auth screen**: Syncing overlay blocks interaction during cloud-to-local sync after login.

## Package Structure

```
com.example.choreboo_habittrackerfriend/
├── MainActivity.kt                  # @AndroidEntryPoint, dynamic startDestination (Auth/Onboarding/HabitList)
├── ChorebooApplication.kt           # @HiltAndroidApp, notification channels
├── navigation/                      # ChorebooNavGraph.kt, Screen sealed class (8 routes)
├── data/
│   ├── local/
│   │   ├── ChorebooDatabase.kt      # Room DB v10, 3 entities, fallbackToDestructiveMigration
│   │   ├── converter/
│   │   │   └── Converters.kt        # Gson TypeConverter for List<String>
│   │   ├── entity/                  # HabitEntity, HabitLogEntity, ChorebooEntity
│   │   └── dao/                     # HabitDao, HabitLogDao, ChorebooDao
│   ├── datastore/                   # UserPreferences (theme, onboarding, sound, totalPoints, totalLifetimeXp, profilePhotoUri, householdNotifications)
│   └── repository/                  # HabitRepository, ChorebooRepository, AuthRepository, HouseholdRepository, UserRepository, BadgeRepository
├── di/                              # AppModule (DB, DAOs, DataStore, UserPreferences, FirebaseAuth), SyncManager, AppLifecycleObserver
├── domain/model/                    # Habit, ChorebooStats, ChorebooMood, ChorebooStage, PetType, Household, AppUser, Badge
├── ui/
│   ├── theme/                       # Color.kt, Theme.kt, Type.kt
│   ├── components/                  # BottomNavBar (5 tabs with dynamic pet mood icon)
│   ├── auth/                        # AuthScreen, AuthViewModel (login/register/sync orchestration)
│   ├── habits/                      # HabitListScreen, AddEditHabitScreen, components/ (HabitCard, StreakBadge)
│   ├── pet/                         # PetScreen, PetViewModel, components/StatBar
│   ├── household/                   # HouseholdScreen, HouseholdViewModel, components/HouseholdPetCard
│   ├── calendar/                    # CalendarScreen, CalendarViewModel
│   ├── onboarding/                  # OnboardingScreen, OnboardingViewModel
│   └── settings/                    # SettingsScreen, SettingsViewModel
└── worker/                          # AlarmManager-based reminders (see below)
    ├── HabitReminderScheduler.kt    # Schedules per-habit alarms via AlarmManager
    ├── HabitReminderReceiver.kt     # BroadcastReceiver that shows reminder notification
    ├── HabitCompleteReceiver.kt     # BroadcastReceiver for "Complete" action on notification
    ├── ReminderRescheduleWorker.kt  # WorkManager job that reschedules alarms after reboot/update
    └── BootReceiver.kt              # BOOT_COMPLETED receiver that triggers ReminderRescheduleWorker
```

## Adding a New Feature (Screen)

1. Create `ui/<feature>/` folder with `FeatureScreen.kt`, `FeatureViewModel.kt`, and optional `components/` subfolder.
2. ViewModel: `@HiltViewModel class FeatureViewModel @Inject constructor(...)`.
3. Add route to `Screen` sealed class and composable to `ChorebooNavGraph`.
4. If it needs a bottom tab, add to `bottomNavRoutes` in `MainActivity.kt` and `BottomNavBar.kt`.
5. If it needs new data: add Entity → DAO → provide DAO in `AppModule` → create/update Repository.
6. If it needs cloud persistence: add table to `schema.gql`, add queries/mutations to `.gql` files, run `npx firebase-tools@latest dataconnect:sdk:generate`, add write-through calls in Repository, add sync logic if needed.

## Copilot Instructions

Additional context lives in `.github/copilot-instructions.md` (color palette hex codes, Room schema columns, pet animation strategy). Key code-gen rules from that file:
- Always use Material3 APIs (not Material2).
- Room queries: `Flow<>` for observable, `suspend` for one-shot.
- For level-up detection, use `XpResult` from `ChorebooRepository.addXp()`.
- When adding habit icons, update `iconOptions` in `AddEditHabitScreen` (emoji-based `EmojiIcon` system).
- Write-through: any Room mutation should also call the corresponding Data Connect mutation.

## Known Issues (as of 2026-04-03)

### Bugs (all resolved)

All bugs B1–B17 have been fixed:
- **B1**: Sign-out now clears Room tables, DataStore, and household state.
- **B2**: `completeHabit()` saves cloud `remoteId` back to local log.
- **B3**: `completeHabit()` sets `completedByUid` on local log entity.
- **B4/B5**: `HabitCompleteReceiver` rewritten — uses Hilt `@AndroidEntryPoint`, delegates to repositories instead of duplicating logic. Now syncs to cloud and updates `TOTAL_LIFETIME_XP`.
- **B6**: `sendPasswordReset()` returns `AuthResult.ResetEmailSent`.
- **B7**: Habit completion race condition fixed — UNIQUE(`habitId`, `date`) index + `OnConflictStrategy.IGNORE` with -1L check.
- **B8**: `applyStatDecay()` syncs cleared `sleepUntil` to cloud.
- **B9**: `autoFeedIfNeeded()` updates `lastInteractionAt`.
- **B10**: `customDays` split filters empty strings.
- **B11**: `HabitReminderScheduler` uses `today.lengthOfMonth()`.
- **B12**: `saveHabit()` now preserves `remoteId`, `ownerUid`, `householdId`, `createdAt`, and `isArchived` from the existing habit when editing. Previously these fields were reset to defaults on every edit, causing cloud duplicates and ownership loss.
- **B13**: `householdId` is now resolved from `HouseholdRepository.currentHousehold` when `isHouseholdHabit` is toggled on. Previously the toggle set a boolean flag but never associated the habit with the user's actual household.
- **B14**: `HabitReminderScheduler.calculateNextMonthlyTrigger()` now sorts `normalizedDays` so the loop finds the nearest scheduled day, not an arbitrary one. Dead `validDaysThisMonth` variable and unused `ChronoUnit` import removed.
- **B15**: `HabitRepository` now receives `FirebaseAuth` via Hilt constructor injection instead of calling `FirebaseAuth.getInstance()` directly in 3 places.
- **B16**: `SyncManager.syncAll()` now calls `getIdToken(false).await()` before any Data Connect calls. Fixes `UNAUTHENTICATED` gRPC errors caused by a race condition where `FirebaseAuth.currentUser` is non-null but the Data Connect SDK's internal `IdTokenListener` hasn't received the token yet.
- **B17**: `HabitRepository.syncHouseholdHabitLogsForToday()` now checks `hasHouseholdHabits()` via `HabitDao` and returns early if the user has none. Prevents `NOT_FOUND` errors from `GetHouseholdHabitLogsForDate` when the user has no household habits.

### Security Gaps (resolved)

- **G20**: 4 household queries restructured to be inherently auth-scoped (traverse from `auth.uid`). No `householdId` parameter needed. `HouseholdRepository` updated to use new SDK query names and response shapes.

### Sync Gaps (resolved)

- **G11**: `SyncManager` singleton (with 5-minute cooldown + `Mutex`) orchestrates all sync. `AppLifecycleObserver` (`ProcessLifecycleOwner`) triggers `syncAll(force = false)` on every app foreground. Auth-triggered sync calls `syncAll(force = true)` to bypass cooldown. `AuthViewModel` now delegates to `SyncManager` instead of calling repositories directly.
- **G12**: `syncHabitsFromCloud()` now uses `getAllMyHabits` (no `isArchived` filter) instead of `getMyHabits`, so archived habits are correctly propagated across devices.
- **G13**: Deletion reconciliation added to `syncHabitsFromCloud()` and `syncHabitLogsFromCloud()`. After upserting cloud data, any local habit (with non-null `remoteId`, owned by current user) or local log (within the 30-day sync window, with non-null `remoteId`) not present in the cloud response is deleted from Room.
- **G16**: `totalPoints` and `totalLifetimeXp` added to `User` cloud table and `AppUser` domain model. `completeHabit()` calls `userRepository.syncPointsToCloud()` as write-through. `SyncManager` calls `userRepository.syncPointsFromCloud()` (max-wins merge, pushes back if merged values differ) on every sync.

### Remaining Sync Gaps

| ID | Description |
|----|-------------|
| G14 | 30-day habit log sync limit means older logs are never synced |

## Not Yet Implemented

- Glance widget (today's habits + pet mood)
- Sound effects
- Lottie animations (replace emoji placeholders)
- Multiple Choreboos

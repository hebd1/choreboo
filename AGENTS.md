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
- **Logging**: Timber (5.0.1) — `DebugTree` planted in `ChorebooApplication.onCreate()` under `BuildConfig.DEBUG`. No `android.util.Log` calls anywhere in the codebase; all logging uses `Timber.d/w/e/wtf()`.

## Cloud Sync Architecture

- **Backend**: Firebase Data Connect (PostgreSQL) — project `choreboo-7f36c`.
- **SDK generation**: `npx firebase-tools@latest dataconnect:sdk:generate` — a `dataconnectCompile` Exec task in root `build.gradle.kts` runs it. Generated SDK goes to `app/build/generated/sources/`.
- **Auth**: Firebase Auth (email/password + Google sign-in). `AuthRepository` wraps `FirebaseAuth`. `AuthViewModel` orchestrates login/register flows. Google sign-in uses the **Android Credential Manager API** (`androidx.credentials` 1.5.0 + `googleid` 1.1.1) — the legacy `GoogleSignIn` GMS API is not used. `AuthScreen` builds a `GetCredentialRequest` with `GetGoogleIdOption`, calls `CredentialManager.getCredential()`, extracts the `GoogleIdTokenCredential`, and passes the raw ID token to `AuthRepository.signInWithGoogle(idToken)`.
- **Write-through**: All Room mutations (habit CRUD, choreboo updates, habit log inserts) also fire the corresponding Data Connect mutation. Failures are **silent** — no user-facing error for write-through.
- **Cloud-to-local sync**: Triggered **after auth** (`force = true`, bypasses cooldown) and **on every app foreground** (`force = false`, 5-minute cooldown). `SyncManager` orchestrates all sync with a `Mutex` to prevent concurrent runs. `SyncManager.syncAll()` calls `getIdToken(false).await()` before any Data Connect calls to ensure the auth token is cached and available for the gRPC interceptor. Order: habits + choreboo + user points + purchased backgrounds (parallel) → habit logs (sequential, needs habit remoteIds) → household habit logs (best-effort). Conflict resolution: **cloud wins**.
- **App foreground sync**: `AppLifecycleObserver` (registered via `ProcessLifecycleOwner`) calls `syncManager.syncAll(force = false)` on every `onStart`. Cold-start sync is **skipped** here — `MainViewModel` handles it as part of the coordinated splash-screen startup sequence. Only subsequent warm resumes trigger the observer's sync.
- **Error visibility**: Only the post-auth sync shows errors (snackbar). Write-through failures are silent.
- **Security**: All 16 queries and 27 mutations have `@auth(level: USER)` directives with auth-scoped filters. `connector.yaml` has `authMode: PUBLIC` (each operation has its own auth check). 4 household queries (`GetMyHousehold`, `GetMyHouseholdMembers`, `GetMyHouseholdChoreboos`, `GetMyHouseholdHabits`) are **inherently auth-scoped** — they traverse from `auth.uid` to the user's household, so no `householdId` parameter is needed and callers can only see their own household data. 3 habit/log queries (`GetHabitById`, `GetLogsForHabit`, `GetLogsForHabitAndDate`) that allow access to household habits now require household membership verification — the `isHouseholdHabit` branch checks that the caller's `auth.uid` is a member of the habit's household. Habit updates are split: `UpdateOwnHabit` (owner only, all fields) and `UpdateAssignedHabit` (assignee, safe fields only — no ownership or household fields).

### Data Connect Schema (6 cloud tables)

Defined in `dataconnect/schema/schema.gql`:

| Table | Key Fields |
|-------|------------|
| **User** | uid (String PK from Firebase Auth), displayName, email, photoUrl, household FK, createdAt, totalPoints, totalLifetimeXp |
| **Household** | UUID PK, name, inviteCode (unique), createdBy FK→User, createdAt |
| **Choreboo** | UUID PK, owner FK→User (unique), name, stage, level, xp, hunger, happiness, energy, petType, lastInteractionAt, createdAt, sleepUntil, backgroundId (nullable) |
| **PurchasedBackground** | composite PK (owner FK→User, backgroundId String), purchasedAt |
| **Habit** | UUID PK, owner FK→User, household FK→Household (nullable), assignedTo FK→User (nullable), title, description, iconName, customDays, difficulty, baseXp, reminderEnabled, reminderTime, isHouseholdHabit, isArchived, createdAt |
| **HabitLog** | UUID PK, habit FK→Habit, completedBy FK→User, completedAt, date, xpEarned, streakAtCompletion |

### Data Connect Files

- `dataconnect/dataconnect.yaml` — project config
- `dataconnect/schema/schema.gql` — 6 tables
- `dataconnect/choreboo-connector/connector.yaml` — authMode: PUBLIC
- `dataconnect/choreboo-connector/queries.gql` — 16 queries, all auth-scoped
- `dataconnect/choreboo-connector/mutations.gql` — 27 mutations, all auth-scoped (22 operational + 5 dev reset). Operational mutations include `UnarchiveHabit`, `NullifyHouseholdForMembers`, and `DeleteLogsForHabit` added since initial implementation.

## Data Flow Patterns

- **Reactive reads**: DAOs return `Flow<>` → Repositories expose `Flow<>` → ViewModels expose `StateFlow` via `.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), default)`.
- **Lifecycle-aware collection**: All Screen composables collect `StateFlow` via `collectAsStateWithLifecycle()` (from `androidx.lifecycle.compose`) — not `collectAsState()`. This ensures recomposition only occurs when the lifecycle is at least `STARTED`, preventing wasted work while the app is in the background.
- **Config-change survival**: `rememberSaveable` (from `androidx.compose.runtime.saveable`) is used for UI state that must survive configuration changes — dialog visibility flags, form text, selected tabs, and similar ephemeral UI state. Import path is `androidx.compose.runtime.saveable.rememberSaveable` (not `androidx.compose.runtime.rememberSaveable`).
- **One-shot writes**: DAO methods are `suspend`; called from `viewModelScope.launch {}` in ViewModels. Write-through to Data Connect happens in the same suspend function. Point deductions (feeding) also write-through to cloud via `UserRepository.syncPointsToCloud()`.
- **One-shot events** (snackbars, navigation, level-up): `MutableSharedFlow` exposed as `events` in ViewModels (see `PetEvent`, `AddEditHabitEvent`, `AuthEvent`, `SettingsEvent`, `HouseholdEvent`).
- **Loading states**: ViewModels expose `isSaving` / `isRefreshing` / `isHatching` / `isCreatingHousehold` / `isJoiningHousehold` / `isLeavingHousehold` as `StateFlow<Boolean>`, guarded by `try/finally` to ensure reset on error. Screens collect these to show `CircularProgressIndicator` and disable interaction.
- **Pull-to-refresh**: `PetScreen`, `HouseholdScreen`, and `CalendarScreen` use `PullToRefreshBox` (Material3 experimental). Refresh calls `viewModel.refreshData()` which sets `_isRefreshing`, calls `syncManager.syncAll(force = true)` + stat decay, then clears the flag in `finally`.
- **Habit creation snackbar**: `AddEditHabitEvent.Saved(isNew: Boolean)` carries a flag. In the nav graph, `isNew == true` sets `"habitCreated" = true` on the Pet back stack entry via `SavedStateHandle`. `PetScreen` reads this via `backStackEntry.savedStateHandle.getStateFlow("habitCreated", false)` and shows a `StitchSnackbar` confirmation, clearing the flag via `onHabitCreatedConsumed`.
- **Cloud timeouts**: Every `withTimeoutOrNull(CLOUD_TIMEOUT_MS)` (5 000 ms) wraps all Data Connect `.execute()` calls across all repositories. On timeout the call returns `null` and the caller handles it gracefully (logs warning, no user-facing error for write-through).
- **Stat decay**: Calculated on PetScreen open via `chorebooRepository.applyStatDecay()` based on `lastInteractionAt`.
- **XP/Level-up**: `ChorebooRepository.addXp()` returns `XpResult(levelsGained, newLevel, evolved, newStage)` so callers can trigger celebration UI.
- **Habit completion**: `HabitRepository.completeHabit()` returns `CompletionResult(xpEarned, newStreak, alreadyComplete)` with targetCount enforcement.
- **Streaks**: `HabitRepository.getStreaksForToday()` returns `Flow<Map<Long, Int>>` for StreakBadge display.
- **Cloud-to-local sync**: `HabitRepository.syncHabitsFromCloud()`, `HabitRepository.syncHabitLogsFromCloud()`, `ChorebooRepository.syncFromCloud()`, `BackgroundRepository.syncFromCloud()`. Called from `SyncManager.syncAll()`.

## Room Database (v16, 7 entities)

| Table | Columns | Indexes |
|-------|---------|---------|
| **habits** | id, title, description, iconName, customDays, difficulty, baseXp, reminderEnabled, reminderTime, createdAt, isArchived, isHouseholdHabit, ownerUid, householdId, assignedToUid, assignedToName, remoteId | `remoteId` |
| **habit_logs** | id, habitId (FK→habits CASCADE), completedAt, date (ISO string), xpEarned, streakAtCompletion, completedByUid, remoteId | `remoteId`, `date`, UNIQUE(`habitId`, `date`) |
| **choreboos** | id, name, stage, level, xp, hunger, happiness, energy, petType, lastInteractionAt, createdAt, sleepUntil, ownerUid, remoteId, backgroundId | `remoteId` |
| **household_members** | uid (String PK = Firebase Auth UID), displayName, photoUrl, email, chorebooId, chorebooName, chorebooStage, chorebooLevel, chorebooXp, chorebooHunger, chorebooHappiness, chorebooEnergy, chorebooPetType, chorebooBackgroundId, lastSyncedAt | — |
| **households** | id (String PK = Data Connect UUID), name, inviteCode, createdByUid, createdByName | — |
| **household_habit_statuses** | habitId (String PK = Data Connect UUID), title, iconName, ownerName, ownerUid, baseXp, assignedToUid, assignedToName, completedByName, completedByUid, cachedDate | — |
| **purchased_backgrounds** | ownerUid + backgroundId (composite PK), purchasedAt | — |

- `remoteId` maps to the Data Connect UUID for cloud sync. Indexed on `habits`, `habit_logs`, and `choreboos`.
- `ownerUid` / `completedByUid` / `householdId` map to Firebase Auth UIDs and household references.
- `habit_logs` has a UNIQUE(`habitId`, `date`) index for atomic duplicate prevention on habit completion.
- `insertLog` uses `OnConflictStrategy.IGNORE` — returns -1L when duplicate is ignored.
- `household_members` is a **read-only cloud cache** — written by `HouseholdRepository.refreshHouseholdMembers()` (identity columns) and `refreshHouseholdPets()` (pet columns) using partial-update `@Transaction` methods (`INSERT OR IGNORE` + targeted `UPDATE`). Cleared on sign-out/leave. Only members who have created a Choreboo appear in pet data. Uses `uid` as PK. No `remoteId` needed (uid is the stable cloud key).
- `households` is a **read-only cloud cache** for the user's current household. Written by `HouseholdRepository`, cleared on sign-out/leave.
- `household_habit_statuses` caches today's household habit completion statuses for the household screen. Uses Data Connect habit UUID as PK.
- Uses `fallbackToDestructiveMigration()` during development.
- Schemas exported to `app/schemas/`.

## Build & Run

- **Gradle**: 9.3.1 wrapper, AGP 8.9.1, Kotlin 2.0.21, KSP 2.0.21-1.0.27
- **SDK**: minSdk 24, compileSdk 36, targetSdk 36
- **Version catalog**: All dependency versions in `gradle/libs.versions.toml`
- **Firebase project**: `choreboo-7f36c`
- **Data Connect SDK regen**: `npx firebase-tools@latest dataconnect:sdk:generate`
- **Firebase Storage**: Configured for profile photo uploads (`storage.rules` in project root)
- **Key build features**: `buildConfig = true` (required for `BuildConfig.DEBUG` access), `compose = true`, `isCoreLibraryDesugaringEnabled = true`
- **Key dependencies**: Timber 5.0.1 (logging), Compose BOM 2025.05.00, `lifecycle-runtime-compose` 2.10.0 (`collectAsStateWithLifecycle`), Credential Manager 1.5.0 + `googleid` 1.1.1 (Google sign-in)

### Firebase Deploy

Deploy the backend (Data Connect schema/connectors + Storage rules) to the `choreboo-7f36c` project:

| Task | Command (run from WSL or any terminal) |
|------|----------------------------------------|
| Full deploy (Data Connect + Storage) | `npx firebase-tools@latest deploy --project choreboo-7f36c` |
| Data Connect only | `npx firebase-tools@latest deploy --only dataconnect --project choreboo-7f36c` |
| Storage rules only | `npx firebase-tools@latest deploy --only storage --project choreboo-7f36c` |
| Check deploy status | `npx firebase-tools@latest --version` |

- **Config files**: `firebase.json` (defines `dataconnect` + `storage` targets), `.firebaserc` (default project = `choreboo-7f36c`)
- **Storage rules**: `storage.rules` — profile photos readable by all authenticated users, writable/deletable only by the owner (`/profile_photos/{userId}.jpg`)
- **Data Connect console**: https://console.firebase.google.com/project/choreboo-7f36c/dataconnect/locations/us-central1/services/choreboo-dataconnect/schema
- **Project console**: https://console.firebase.google.com/project/choreboo-7f36c/overview

> **Note**: Always deploy Data Connect after modifying `schema.gql`, `queries.gql`, or `mutations.gql`. The deploy validates schema compatibility with the existing Cloud SQL database and applies migrations automatically.

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

> **Note**: 265 unit tests across domain models, repositories, and ViewModels. See the [Testing](#testing) section for details. No lint configuration, detekt, or ktlint is set up.

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
- **Event sealed classes**: `<Feature>Event` (e.g., `PetEvent`, `AuthEvent`, `HouseholdEvent`).

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
- **Exception to try/catch rule**: `PetViewModel.completeHabit()` wraps its body in `try/catch` and emits `PetEvent.CompletionError` so the screen can show a snackbar — this is the only VM-level catch.
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
- **Navigation**: `Screen` sealed class in `navigation/ChorebooNavGraph.kt`. Start destination is dynamic: Auth → Onboarding → Pet. 5 bottom tabs (`pet`, `stats`, `household`, `calendar`, `settings`); bottom bar hidden on `auth`, `onboarding`, `add_edit_habit`. `MainViewModel.isAppReady` gates the splash screen before the first destination renders.
- **Destructive actions** (delete habit) require `AlertDialog` confirmation.
- **Level-up celebrations** shown via `AlertDialog` when XP causes level/stage change.
- **Cloud sync**: `remoteId` field on all 3 Room entities links local rows to Data Connect UUIDs. Write-through on every mutation; cloud-to-local on auth + app foreground (via `SyncManager`).
- **Network security**: `network_security_config.xml` disables cleartext HTTP in release builds. `backup_rules.xml` and `data_extraction_rules.xml` exclude the Room DB, DataStore prefs, and shared prefs from Android Auto Backup.
- **StateFlow collection**: Always use `collectAsStateWithLifecycle()` (from `androidx.lifecycle.compose`) in Screen composables — never `collectAsState()`. This is lifecycle-aware and prevents unnecessary recomposition when the app is backgrounded.
- **Saveable state**: Use `rememberSaveable` (import: `androidx.compose.runtime.saveable.rememberSaveable`) for any UI state that must survive configuration changes (dialog flags, form input, selected tabs). Do NOT use the incorrect `androidx.compose.runtime.rememberSaveable` path.
- **String resources**: All user-facing strings use `stringResource(R.string.*)`. No hardcoded string literals in composables. Add new strings to `res/values/strings.xml` only — do not update `values-de/` or `values-es/` (those are deferred).

## UI Rules

- Material3 only — use `MaterialTheme.colorScheme.*` and `MaterialTheme.typography.*`. No hardcoded text styles or hardcoded colors anywhere in the UI.
- Dynamic color is **disabled** (`dynamicColor = false`) — custom Choreboo palette always applied. Primary `#006E1C` (deep green), Secondary `#8B5000` (warm orange), Tertiary `#6833EA` (purple). See `Color.kt` for full Stitch design system.
- Card corners: `RoundedCornerShape(16.dp)`. Input corners: `RoundedCornerShape(12.dp)`.
- Always handle `innerPadding` from `Scaffold`. Use `Modifier.fillMaxWidth()` over hardcoded widths.
- **Bottom nav bar** lives in `Scaffold`'s `bottomBar` slot (in `MainActivity`). No hardcoded bottom padding or 80dp spacers in any screen — `innerPadding` from `Scaffold` propagates to content via `Modifier.padding(innerPadding).consumeWindowInsets(innerPadding)` on the nav graph.
- **Strings**: All user-facing text uses `stringResource(R.string.*)` — no hardcoded strings in composables. `res/values/strings.xml` contains ~548 string resources + 3 plurals. Auth error types use `@StringRes` fields. Only `values/` (English) is maintained; `values-de/` and `values-es/` are not yet updated.
- Habit icons: **emoji-based** system (`EmojiIcon` data class with 15 preset emoji + custom emoji input). Not Material Icons.
- Touch targets >= 48dp. Empty states show emoji + friendly message + CTA.
- Use `AlertDialog` for confirmations; `ModalBottomSheet` for selection lists (feed, background picker).
- **`PetBackgroundImage`** (`ui/components/PetBackgroundImage.kt`) — renders the selected background behind the pet. If `backgroundId` is null or `"default"`, shows the mood-based color gradient. Otherwise loads the image from `assets/backgrounds/<id>.webp` via Coil's `rememberAsyncImagePainter`. Used in `PetScreen` and `HouseholdPetCard`.
- **`ShimmerPlaceholder`** (`ui/components/ShimmerPlaceholder.kt`) — reusable loading skeleton. Renders a rounded rectangle pulsing between alpha 0.3–0.7 with a 1-second tween. Parameters: `width: Dp = 100.dp`, `height: Dp = 24.dp`, `modifier`. Used in `PetScreen` while the Choreboo name is loading.
- **Pet animations**: Animated WebP files with alpha transparency. FOX pet type has 9 animations (mood-based + action-based). Other pet types (AXOLOTL, CAPYBARA, PANDA) use emoji placeholders until WebP assets are added. `WebmAnimationView` composable in `ui/components/` handles playback via `AnimatedImageDrawable` (API 28+); API 24–27 gets a transparent placeholder. No ExoPlayer dependency.
- Pet size scales by `ChorebooStage`.
- **Auth screen**: Syncing overlay blocks interaction during cloud-to-local sync after login.
- **Splash screen**: `MainActivity` shows a branded splash until `MainViewModel.isAppReady` becomes `true`. For fully authenticated+onboarded users, it waits for Room warmup only (sub-second). Cloud sync and WebP animation loading run in the background without blocking the splash.
- **CalendarScreen loading**: `CalendarViewModel` exposes `isLoading: StateFlow<Boolean>` (starts `true`, cleared after first data emission). `CalendarScreen` shows a `CircularProgressIndicator` while `isLoading` is true.

## Package Structure

```
com.example.choreboo_habittrackerfriend/
├── MainActivity.kt                  # @AndroidEntryPoint, dynamic startDestination (Auth/Onboarding/Pet)
├── MainViewModel.kt                 # @HiltViewModel — startup sequencer (DataStore → Room warmup → sync); exposes isAppReady StateFlow
├── ChorebooApplication.kt           # @HiltAndroidApp, notification channels, Timber DebugTree (DEBUG builds only)
├── navigation/                      # ChorebooNavGraph.kt, Screen sealed class (8 routes)
├── data/
│   ├── local/
│   │   ├── ChorebooDatabase.kt      # Room DB v16, 7 entities, fallbackToDestructiveMigration
│   │   ├── converter/
│   │   │   └── Converters.kt        # Gson TypeConverter for List<String>
│   │   ├── entity/                  # HabitEntity, HabitLogEntity, ChorebooEntity, HouseholdMemberEntity, HouseholdEntity, HouseholdHabitStatusEntity, PurchasedBackgroundEntity
│   │   └── dao/                     # HabitDao, HabitLogDao, ChorebooDao, HouseholdMemberDao, HouseholdDao, HouseholdHabitStatusDao, PurchasedBackgroundDao
│   ├── datastore/                   # UserPreferences (theme, onboarding, sound, totalPoints, totalLifetimeXp, profilePhotoUri)
│   └── repository/                  # HabitRepository, ChorebooRepository, AuthRepository, HouseholdRepository, UserRepository, BadgeRepository, BackgroundRepository, BillingRepository, ResetRepository, SyncManager
├── di/                              # AppModule (DB, DAOs, DataStore, UserPreferences, FirebaseAuth), AppLifecycleObserver
├── domain/model/                    # Habit, ChorebooStats, ChorebooMood, ChorebooStage, PetType, Household, AppUser, Badge, Background
├── ui/
│   ├── theme/                       # Color.kt, Theme.kt, Type.kt
│   ├── components/                  # BottomNavBar (5 tabs with dynamic pet mood icon), ProfileAvatar, ShimmerPlaceholder, StitchSnackbar, PetBackgroundImage
│   ├── auth/                        # AuthScreen, AuthViewModel (login/register/sync orchestration)
│   ├── habits/                      # AddEditHabitScreen, AddEditHabitViewModel, components/ (HabitCard, StreakBadge)
│   ├── pet/                         # PetScreen (habit list + feed bottom sheet + background picker), PetViewModel (absorbed HabitList functionality), components/ (StatBar, BackgroundPickerSheet)
│   ├── stats/                       # StatsScreen, StatsViewModel
│   ├── household/                   # HouseholdScreen (tap pet card → member habits dialog), HouseholdViewModel (enriches pet photos), components/ (HouseholdPetCard, HouseholdHabitCard)
│   ├── calendar/                    # CalendarScreen, CalendarViewModel
│   ├── onboarding/                  # OnboardingScreen, OnboardingViewModel
│   ├── settings/                    # SettingsScreen, SettingsViewModel (+ dev Reset Account flow)
│   ├── inventory/                   # (placeholder — not yet implemented)
│   └── shop/                        # (placeholder — not yet implemented)
└── worker/                          # AlarmManager-based reminders (see below)
    ├── HabitReminderScheduler.kt    # Schedules per-habit alarms via AlarmManager
    ├── HabitReminderReceiver.kt     # BroadcastReceiver that shows reminder notification
    ├── HabitCompleteReceiver.kt     # BroadcastReceiver for "Complete" action on notification
    ├── ReminderRescheduleWorker.kt  # WorkManager job that reschedules alarms after reboot/update
    └── BootReceiver.kt              # BOOT_COMPLETED receiver that triggers ReminderRescheduleWorker
```

## Dev Reset Account

A dev-only utility for wiping a test account and starting enrollment from scratch. Accessible via **Settings → Account → Reset Account (Dev)**.

### What it does

Deletes all cloud data for the currently signed-in user in FK-safe order, deletes the Firebase Auth user (freeing the email for re-registration), then clears all local Room tables and DataStore. Other users' data is completely untouched — all mutations are scoped to `auth.uid` server-side.

**Deletion order** (respects FK constraints):
1. All `HabitLog` rows where `completedById == auth.uid`
2. All `Habit` rows where `ownerId == auth.uid`
2b. All `PurchasedBackground` rows where `ownerId == auth.uid`
3. `Choreboo` row where `ownerId == auth.uid`
4. Null out `User.household` FK (breaks circular dep with Household)
5. `Household` row — only if the current user is its creator
6. `User` record
7. Firebase Auth user (`currentUser.delete()`)
8. Local Room tables (`habits`, `habit_logs`, `choreboos`, `purchased_backgrounds`) + DataStore

Cloud steps are individually try/caught — a failure at one step logs a warning and continues. Auth deletion and local cleanup always run.

### Key files

| File | Role |
|------|------|
| `dataconnect/choreboo-connector/mutations.gql` | 5 dev reset mutations: `DeleteAllMyHabitLogs`, `DeleteAllMyHabits`, `DeleteAllMyPurchasedBackgrounds`, `DeleteMyChoreboo`, `DeleteMyUser` |
| `data/repository/ResetRepository.kt` | `@Singleton` — orchestrates the full reset sequence; depends on `BackgroundRepository` |
| `ui/settings/SettingsViewModel.kt` | `resetAccount()` + `isResetting: StateFlow<Boolean>` + `SettingsEvent.AccountReset` |
| `ui/settings/SettingsScreen.kt` | "Reset Account (Dev)" button with spinner + two-step confirmation dialog |

### After reset

The app navigates to the Auth screen. Re-register with the same email (or any email) and go through onboarding for a clean slate.

> **`reset-db.sh`** (project root) — dev-only shell script that opens an interactive Cloud SQL shell and prints the `TRUNCATE` command to wipe **all** cloud tables for all users (not just the current account). Use with care — it requires manual confirmation and does not delete Firebase Auth records.

## App Startup / Splash Screen

`MainViewModel` owns the entire startup sequence and exposes `isAppReady: StateFlow<Boolean>`. `MainActivity` shows a branded splash screen until `isAppReady` becomes `true`.

### Startup sequence

1. **Wait for DataStore** — `onboardingComplete.first { it != null }` blocks until the first real DataStore value resolves.
2. **Fast path** (unauthenticated or not-yet-onboarded users) — sets `_startupComplete = true` immediately. Room and sync are never touched.
3. **Full path** (authenticated + onboarded users):
   - **Cloud sync** (fire-and-forget): `syncManager.syncAll(force = false)` is launched in a separate coroutine — it does NOT block the splash screen.
   - **Room warmup** (blocking): `chorebooRepository.getOrCreateChoreboo()` + `applyStatDecay()` ensures the local DB is consistent before the first screen renders. This is the only task that blocks the splash.
   - **WebP animation loading**: `AnimatedImageDrawable` loads animated WebP files when `PetScreen` is rendered — `MainViewModel` does not await it. PetScreen shows emoji fallback until the drawable is ready.
4. After Room warmup completes, `_startupComplete = true` and the splash dismisses.

### Key files

| File | Role |
|------|------|
| `MainViewModel.kt` | `@HiltViewModel` — orchestrates startup, exposes `isAppReady` and `themeMode`/`onboardingComplete`/`petMood` state flows |
| `di/AppLifecycleObserver.kt` | `@Singleton` — skips cold-start `onStart` (handled by `MainViewModel`); triggers `syncAll(force = false)` on subsequent warm resumes |
| `ChorebooApplication.kt` | `@HiltAndroidApp` — app entry point, sets up notification channels, plants Timber `DebugTree` |
| `ui/components/WebmAnimationView.kt` | `AnimatedImageDrawable`-based composable for animated WebP playback with alpha transparency and iteration control |

### isAppReady derivation

```
isAppReady = combine(onboardingComplete, _startupComplete) { onboarding, startup ->
    onboarding != null && startup
}
```

`onboardingComplete` (from DataStore) must resolve to a non-null value AND the startup coroutine must finish before the splash screen dismisses.

## Adding a New Feature (Screen)

1. Create `ui/<feature>/` folder with `FeatureScreen.kt`, `FeatureViewModel.kt`, and optional `components/` subfolder.
2. ViewModel: `@HiltViewModel class FeatureViewModel @Inject constructor(...)`.
3. Add route to `Screen` sealed class and composable to `ChorebooNavGraph`.
4. If it needs a bottom tab, add to `bottomNavRoutes` in `MainActivity.kt` and `BottomNavBar.kt`.
5. If it needs new data: add Entity → DAO → provide DAO in `AppModule` → create/update Repository.
6. If it needs cloud persistence: add table to `schema.gql`, add queries/mutations to `.gql` files, run `npx firebase-tools@latest dataconnect:sdk:generate`, add write-through calls in Repository, add sync logic if needed.
7. Add unit tests: create `<FeatureName>Test.kt` files in mirrored test packages. See [Testing → Adding Tests for a New Feature](#adding-tests-for-a-new-feature) for conventions.

## Copilot Instructions

Additional context lives in `.github/copilot-instructions.md` (color palette hex codes, Room schema columns, pet animation strategy). Key code-gen rules from that file:
- Always use Material3 APIs (not Material2).
- Room queries: `Flow<>` for observable, `suspend` for one-shot.
- For level-up detection, use `XpResult` from `ChorebooRepository.addXp()`.
- When adding habit icons, update `iconOptions` in `AddEditHabitScreen` (emoji-based `EmojiIcon` system).
- Write-through: any Room mutation should also call the corresponding Data Connect mutation.

## Testing

265 unit tests across 3 layers: domain models, repositories, and ViewModels. All tests are JVM-only (no Android emulator required).

### Test Stack

| Library | Version | Purpose |
|---------|---------|---------|
| JUnit 4 | 4.13.2 | Test framework |
| MockK | 1.13.16 | Kotlin-first mocking (relaxed mocks, coEvery/coVerify, slot captures) |
| Turbine | 1.2.0 | Flow/StateFlow testing with `.test {}` blocks |
| kotlinx-coroutines-test | 1.9.0 | `UnconfinedTestDispatcher`, `runTest`, `Dispatchers.setMain` |

### Test Configuration

- **`unitTests.isReturnDefaultValues = true`** in `app/build.gradle.kts` — prevents `android.util.Log` and other Android framework stubs from throwing in JVM tests.
- **`TestDispatcherRule`** (`TestDispatcherRule.kt`) — JUnit 4 `TestWatcher` that swaps `Dispatchers.Main` with `UnconfinedTestDispatcher` for each test. Required by all ViewModel tests and any test that uses `viewModelScope`.

### Test Structure

```
app/src/test/java/com/example/choreboo_habittrackerfriend/
├── TestDispatcherRule.kt                              # Shared JUnit rule for coroutine tests
├── ExampleUnitTest.kt                                 # Template stub (1 test)
├── MainViewModelTest.kt                               # isAppReady, fast-path, full startup, timeout, sync (11)
├── domain/model/
│   ├── HabitTest.kt                                   # isScheduledForToday, calculateStreak, calculateSuggestedXp (16)
│   ├── ChorebooStatsTest.kt                           # fromEntity, mood calculation, stat clamping, decay (33)
│   ├── ChorebooStageTest.kt                           # fromTotalXp thresholds, edge cases, negative XP (17)
│   └── HouseholdPetTest.kt                            # HouseholdPet domain model fields, mood derivation (14)
├── data/repository/
│   ├── BillingRepositoryTest.kt                       # launchPurchaseFlow + verifyPremiumStatus guards (4)
│   ├── ChorebooRepositoryAddXpTest.kt                 # XP addition, level-up, stage evolution, validation (12)
│   ├── HabitRepositoryTest.kt                         # completeHabit, upsertHabit, custom-schedule streaks, validation guards (19)
│   ├── HouseholdRepositoryValidationTest.kt           # createHousehold / joinHousehold validation guards (6)
│   ├── SyncManagerTest.kt                             # Cooldown, mutex, force bypass, partial failure (11)
│   └── UserRepositoryTest.kt                          # Points sync to/from cloud, validation guards (10)
├── di/
│   └── AppLifecycleObserverTest.kt                    # Cold-start skip, warm-resume sync, exception handling (4)
└── ui/
    ├── auth/AuthViewModelTest.kt                       # Form state, validation, Google sign-in, sync, returning-user (23)
    ├── calendar/CalendarViewModelTest.kt               # Month navigation, date parsing, heatmap colors (18)
    ├── habits/AddEditHabitViewModelTest.kt             # Form state, save/load, XP suggestion, validation (34)
    ├── pet/PetViewModelTest.kt                         # Stat display, feeding, sleep, XP events (22)
    └── settings/SettingsViewModelTest.kt               # Reset account, theme changes, settings state (10)
```

### Test Conventions

- **File naming**: `<ClassUnderTest>Test.kt` in a mirrored package under `src/test/`.
- **Test method naming**: backtick-quoted descriptive names: `` `completeHabit returns alreadyComplete when duplicate` ``.
- **Setup**: Use `@Before fun setUp()` to initialize mocks and create the class under test. ViewModel tests often use a `createViewModel()` helper so mock flows can be configured first.
- **Mocking**: `mockk<T>(relaxed = true)` for dependencies. Use `coEvery` for suspend functions, `every` for regular functions. Use `slot<T>()` + `capture(slot)` to assert on arguments passed to mocked functions.
- **Flow testing (Turbine)**: Always use `.test {}` blocks when testing `StateFlow` exposed via `stateIn(WhileSubscribed)` — reading `.value` directly won't activate collection.
- **Mock flows**: Use `MutableStateFlow` (not `flowOf()`) for mocked DAO/Repository flows that feed into `stateIn(WhileSubscribed)`. `flowOf()` completes immediately and the downstream `stateIn` may not collect.
- **ViewModel creation timing**: Set mock return values BEFORE calling `createViewModel()` because `init {}` blocks collect flows immediately with `UnconfinedTestDispatcher`.
- **Assertions**: JUnit `assertEquals`, `assertTrue`, `assertFalse`, `assertNotNull`. MockK `coVerify` / `verify` for interaction assertions.
- **No Android instrumented tests** are included in the current suite — all tests run on JVM.

### Running Tests

| Task | Command |
|------|---------|
| All unit tests | `powershell.exe -File build.ps1 testDebugUnitTest` |
| Single test class | `powershell.exe -File build.ps1 testDebugUnitTest --tests "com.example.choreboo_habittrackerfriend.domain.model.HabitTest"` |
| Single test method | `powershell.exe -File build.ps1 testDebugUnitTest --tests "com.example.choreboo_habittrackerfriend.domain.model.HabitTest.isScheduledForToday returns true for daily habit"` |

### Adding Tests for a New Feature

1. Create `<FeatureName>Test.kt` in the mirrored test package (e.g., `src/test/.../ui/newfeature/NewFeatureViewModelTest.kt`).
2. Add `@get:Rule val dispatcherRule = TestDispatcherRule()` if testing a ViewModel or anything using `Dispatchers.Main`.
3. Mock dependencies with `mockk<T>(relaxed = true)` in `@Before`.
4. For ViewModel tests: create a `createViewModel()` helper, set mock return values before calling it, and use Turbine `.test {}` blocks for `StateFlow` assertions.
5. For Repository tests: mock DAOs and Firebase SDK calls with `coEvery`, test both success and error paths, and verify write-through calls with `coVerify`.
6. For domain model tests: test pure functions directly — no mocking needed.

### Input Validation Guards

19 `require()` guards across 5 repositories validate preconditions at the repository boundary:

| Repository | Method | Guard |
|------------|--------|-------|
| HabitRepository | `upsertHabit()` | title non-blank |
| HabitRepository | `upsertHabit()` | title <= 100 chars |
| HabitRepository | `upsertHabit()` | baseXp > 0 |
| HabitRepository | `completeHabit()` | habitId > 0 |
| ChorebooRepository | `getOrCreateChoreboo()` | name non-blank |
| ChorebooRepository | `getOrCreateChoreboo()` | name <= 20 chars |
| ChorebooRepository | `addXp()` | amount > 0 |
| ChorebooRepository | `updateName()` | name non-blank |
| ChorebooRepository | `updateName()` | name <= 20 chars |
| HouseholdRepository | `createHousehold()` | name non-blank |
| HouseholdRepository | `createHousehold()` | name <= 50 chars |
| HouseholdRepository | `joinHousehold()` | inviteCode non-blank |
| UserRepository | `syncPointsToCloud()` | totalPoints >= 0 |
| UserRepository | `syncPointsToCloud()` | totalLifetimeXp >= 0 |
| UserRepository | `updateDisplayName()` | name non-blank (after trim) |
| UserRepository | `updateDisplayName()` | name <= 30 chars (after trim) |
| AuthRepository | `signInWithEmail()` / `signUpWithEmail()` | email & password non-blank |
| AuthRepository | `sendPasswordReset()` | email non-blank |

## Known Issues (as of 2026-04-11)

### UI/UX Improvements (U1–U9, resolved 2026-04-11)

- **U1**: `rememberSaveable` import corrected to `androidx.compose.runtime.saveable.rememberSaveable` in all 7 affected Screen files. Used for dialog flags, form text, and tab selection throughout the UI.
- **U2**: Google sign-in migrated from legacy GMS `GoogleSignIn` API to **Android Credential Manager** (`androidx.credentials` 1.5.0 + `googleid` 1.1.1). Dead `play-services-auth:21.2.0` dependency removed.
- **U3**: All Screen composables migrated from `collectAsState()` to `collectAsStateWithLifecycle()` (from `lifecycle-runtime-compose` 2.10.0). Zero legacy `collectAsState()` calls remain.
- **U4**: `CalendarViewModel` exposes `isLoading: StateFlow<Boolean>`; `CalendarScreen` shows `CircularProgressIndicator` on first load. `StatsViewModel` exposes `isRefreshing`. `PetViewModel` exposes `initError: StateFlow<Boolean>` with retry UI.
- **U5**: Bottom nav bar moved into `Scaffold`'s `bottomBar` slot in `MainActivity`. All hardcoded 80dp bottom spacers removed from screens; `innerPadding` + `consumeWindowInsets` propagates correct insets to the nav graph.
- **U7**: All hardcoded `Color(...)` literals replaced with `MaterialTheme.colorScheme.*` tokens across `PetBackgroundImage` and `BackgroundPickerSheet`.
- **U8**: Notification permission `IconButton` in `AddEditHabitScreen` given explicit `contentDescription` for accessibility.
- **U9**: ~548 string resources + 3 plurals in `res/values/strings.xml`. All hardcoded user-facing strings in composables replaced with `stringResource(R.string.*)`. Auth error types use `@StringRes` sealed class fields. Compose BOM updated from `2024.12.01` → `2025.05.00`.

### Bugs (all resolved)

All bugs B1–B26 have been fixed. See prior history for details.

### Critical Bugs (all resolved)

All critical bugs C1–C5 found during the 2026-04-05 audit have been fixed. See prior history for details.

### Security Gaps (resolved)

- **G20**: 4 household queries restructured to be inherently auth-scoped (traverse from `auth.uid`). No `householdId` parameter needed. `HouseholdRepository` updated to use new SDK query names and response shapes.

### Security Fixes — Phase 2 (resolved, 2026-04-10)

- **S1**: `network_security_config.xml` added — cleartext HTTP disabled for all domains in release builds. `AndroidManifest.xml` references it via `android:networkSecurityConfig`.
- **S2**: `backup_rules.xml` and `data_extraction_rules.xml` added — Room DB file, DataStore prefs, and shared preferences excluded from Android Auto Backup and cloud backup (API 31+).
- **S3**: `AndroidManifest.xml` hardened — `android:allowBackup="true"` retained with explicit exclusion rules; `android:fullBackupContent` (pre-31) and `android:dataExtractionRules` (31+) set.
- **S4**: `BillingRepository.launchPurchaseFlow()` guards against null `Activity` reference before launching the Play Billing flow. `verifyPremiumStatus()` returns false (not a crash) when the billing client is disconnected.
- **S5**: `UserPreferences.isPremium` DataStore field removed entirely — premium state is now derived at runtime from `BillingRepository.verifyPremiumStatus()` only. No persistent local premium flag.
- **S6**: `GetUserById` query removed from `queries.gql` — it was unused and exposed user records by UID without household-membership verification.
- **S7**: `UpdateHabit` mutation split into `UpdateOwnHabit` (owner only, all fields) and `UpdateAssignedHabit` (assignee, safe fields only — title, description, iconName, customDays, difficulty, baseXp, reminderEnabled, reminderTime). Ownership and household fields cannot be changed by an assignee.
- **S8**: `BillingRepositoryTest` updated — 2 DataStore `isPremium` seed tests removed (field no longer exists); 4 tests remain covering `launchPurchaseFlow` and `verifyPremiumStatus` guards.
- **S9**: `ChorebooApplication.kt` duplicate class body bug fixed — file previously had a second empty class body appended, causing a compile error in edge cases.

### Sync Gaps (resolved)

- **G11**: `SyncManager` singleton (with 5-minute cooldown + `Mutex`) orchestrates all sync. `AppLifecycleObserver` (`ProcessLifecycleOwner`) triggers `syncAll(force = false)` on every app foreground. Auth-triggered sync calls `syncAll(force = true)` to bypass cooldown. `AuthViewModel` now delegates to `SyncManager` instead of calling repositories directly.
- **G12**: `syncHabitsFromCloud()` now uses `getAllMyHabits` (no `isArchived` filter) instead of `getMyHabits`, so archived habits are correctly propagated across devices.
- **G13**: Deletion reconciliation added to `syncHabitsFromCloud()` and `syncHabitLogsFromCloud()`. After upserting cloud data, any local habit (with non-null `remoteId`, owned by current user) or local log (within the 30-day sync window, with non-null `remoteId`) not present in the cloud response is deleted from Room.
- **G16**: `totalPoints` and `totalLifetimeXp` added to `User` cloud table and `AppUser` domain model. `completeHabit()` calls `userRepository.syncPointsToCloud()` as write-through. `SyncManager` calls `userRepository.syncPointsFromCloud()` (max-wins merge, pushes back if merged values differ) on every sync.

### High Priority Issues (all resolved)

- **H1**: Onboarding race condition fixed — `OnboardingViewModel` now uses a `MutableSharedFlow<OnboardingEvent>` for navigation; `OnboardingScreen` collects events and navigates only after the coroutine completes. `SnackbarHost` added for error feedback.
- **H2**: Stale `LocalDate.now()` fixed across `PetViewModel`, `StatsViewModel`, `CalendarViewModel`, and `CalendarScreen`. All now use a reactive `_todayDate: MutableStateFlow` refreshed via `LifecycleEventEffect(ON_RESUME)`.
- **H5**: Stale household habit statuses fixed — `HouseholdHabitStatusDao` now has `getHabitStatusesForDate(date)` with a `WHERE cachedDate = ?` filter. `HouseholdRepository` uses a reactive `_todayDate` + `flatMapLatest` so yesterday's data is never shown as today's.
- **H6**: `BootReceiver` now also handles `MY_PACKAGE_REPLACED` intent — added to both `AndroidManifest.xml` and `BootReceiver.kt` so alarms are rescheduled after app updates, not just reboots.
- **H8**: `HouseholdScreen` now collects `HouseholdViewModel.events` via `LaunchedEffect` and displays them using `SnackbarHost` + `StitchSnackbar` in a `Box` wrapper.

### Medium Priority Issues (all resolved)

- **M1**: `writeScope` in `ChorebooRepository` and `HabitRepository` changed to `private var`. Added `cancelPendingWrites()` that cancels via `coroutineContext[Job]?.cancel()` and creates a fresh scope. Called by `SettingsViewModel.signOut()` and `ResetRepository.resetAll()`.
- **M2**: `SyncManager` now retries each sync step with exponential backoff. Added `RETRY_DELAYS_MS = listOf(1_000L, 2_000L)` constant and `retryWithBackoff {}` helper. All 4 independent sync steps wrapped in `retryWithBackoff`.
- **M3**: `ResetRepository.resetAll()` now calls `chorebooRepository.cancelPendingWrites()` and `habitRepository.cancelPendingWrites()` at the top before cloud cleanup.
- **M8**: Dead `householdNotificationsEnabled` preference removed entirely — from `UserPreferences.kt`, `SettingsViewModel.kt`, `SettingsScreen.kt`, and `SettingsViewModelTest.kt`.

### Low Priority Issues (all resolved)

- **L1**: `isCreatingHousehold`, `isJoiningHousehold`, and `isLeavingHousehold` StateFlows now collected in `SettingsScreen`. Dialog confirm buttons show `CircularProgressIndicator` and are disabled while in-progress. "Create a Household" and "Join with Invite Code" rows are also disabled while either operation is running.
- **L3**: Audited — all `contentDescription = null` instances are on purely decorative icons. No changes needed.
- **L4**: Sleep duration extracted to `private const val SLEEP_DURATION_MS = 24 * 60 * 60 * 1000L` at top of `ChorebooRepository.kt`.
- **L9**: `PetViewModel.completeHabit()` now wraps the entire body in `try/catch`. Added `PetEvent.CompletionError(message: String)` to the sealed class. `PetScreen` event handler shows a snackbar for `CompletionError`.
- **L12**: `StatBar` root `Row` now has `.clearAndSetSemantics { contentDescription = "$label: $value out of $maxValue" }`. `StatBentoCard` and `EnergyBentoCard` outer `Box` modifiers now have `.semantics(mergeDescendants = true) { contentDescription = "..." }`.

### Remaining Sync Gaps

| ID | Description |
|----|-------------|
| G14 | 30-day habit log sync limit means older logs are never synced |

### Deferred (out of scope)

| ID | Description |
|----|-------------|
| L5 | No pagination on habit log queries |
| L11 | No deep link support for household invite codes |

## Pet Animation Asset Pipeline

All fox animations live in `app/src/main/assets/animations/fox/` as animated WebP files with per-frame alpha transparency. `AnimatedImageDrawable` (API 28+) renders them via `ImageDecoder.createSource(assetManager, path)`.

### Static ffmpeg binary

A portable static ffmpeg binary is kept at `/tmp/ffmpeg-static/ffmpeg-7.0.2-amd64-static/ffmpeg` for WSL use. Re-download if the `/tmp` directory is cleared:

```bash
mkdir -p /tmp/ffmpeg-static && cd /tmp/ffmpeg-static
wget https://johnvansickle.com/ffmpeg/releases/ffmpeg-release-amd64-static.tar.xz
tar xf ffmpeg-release-amd64-static.tar.xz
# binary at: /tmp/ffmpeg-static/ffmpeg-7.0.2-amd64-static/ffmpeg
```

### Converting WebM (VP8+alpha) → animated WebP

Android's hardware `MediaCodec` decoders ignore the VP8 alpha plane (output `yuv420p`, black background). Use ffmpeg's software `libvpx` decoder to preserve alpha:

```bash
FFMPEG=/tmp/ffmpeg-static/ffmpeg-7.0.2-amd64-static/ffmpeg
$FFMPEG -vcodec libvpx -i input.webm \
  -vf "fps=15,format=yuva420p" \
  -loop 0 -vcodec libwebp -lossless 0 -quality 70 \
  output.webp
```

- `-vcodec libvpx` **before** `-i` forces the software decoder (reads the alpha plane).
- `fps=15` halves the typical 30 fps source — reduces file size ~50% with negligible quality loss for this art style.
- `quality 70` with `-lossless 0` gives good visual quality at reasonable file sizes.
- Verify output has alpha: the script below checks for `ALPH` inside `ANMF` frames.

### Converting MP4 (green screen) → animated WebP

For MP4 sources with a solid green screen background (`hsl(112.16deg, 82.57%, 52.75%)` ≈ `#3DEA23`):

```bash
FFMPEG=/tmp/ffmpeg-static/ffmpeg-7.0.2-amd64-static/ffmpeg
$FFMPEG -i input.mp4 \
  -vf "colorkey=0x3DEA23:0.3:0.1,fps=15,format=yuva420p" \
  -loop 0 -vcodec libwebp -lossless 0 -quality 70 \
  output.webp
```

- `colorkey=COLOR:SIMILARITY:BLEND` — `0.3` similarity and `0.1` blend work well for this asset set. Increase similarity slightly (e.g. `0.35`) if green fringing remains.
- Adjust the hex color if the source uses a different green screen shade.

### Verifying alpha in output WebP

```python
import struct

with open('output.webp', 'rb') as f:
    data = f.read()

# Find first ANMF and check for ALPH sub-chunk inside it
anmf_pos = data.index(b'ANMF')
frame_data_start = anmf_pos + 8 + 16  # skip ANMF tag+size + 16-byte frame header
tag = data[frame_data_start:frame_data_start+4]
print('First sub-chunk in ANMF:', tag)  # should be b'ALPH'
```

### Current fox animation inventory

| File | Source | Size | Notes |
|------|--------|------|-------|
| `fox_happy.webp` | WebM (VP8+alpha) | 1.1 MB | |
| `fox_idle.webp` | WebM (VP8+alpha) | 842 KB | |
| `fox_sad.webp` | WebM (VP8+alpha) | 864 KB | |
| `fox_eating.webp` | WebM (VP8+alpha) | 1.0 MB | |
| `fox_hungry.webp` | MP4 (green screen) | 2.4 MB | Converted via colorkey filter |
| `fox_interact.webp` | WebM (VP8+alpha) | 1.1 MB | |
| `fox_thumbs_up.webp` | WebM (VP8+alpha) | 986 KB | |
| `fox_start_sleep.webp` | WebM (VP8+alpha) | 863 KB | |
| `fox_loop_sleeping.webp` | WebM (VP8+alpha) | 365 KB | |

### `repeatCount` semantics

`AnimatedImageDrawable.repeatCount` = extra plays **after** the first play:
- `iterations = 1` → `repeatCount = 0`
- `iterations = 3` → `repeatCount = 2`
- `iterations = Int.MAX_VALUE` → `repeatCount = AnimatedImageDrawable.REPEAT_INFINITE` (`-1`)

## Not Yet Implemented

- Glance widget (today's habits + pet mood)
- Sound effects
- WebP animations for other pet types (AXOLOTL, CAPYBARA, PANDA)
- Multiple Choreboos
- IME keyboard padding — `imePadding()` / `WindowInsets.ime` not yet applied to text-input screens (Auth, Onboarding, AddEditHabit, Settings)

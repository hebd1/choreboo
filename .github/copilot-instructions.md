# Choreboo Habit Tracker Friend - Copilot Instructions

## Project Overview
A Tamagotchi-style habit tracker Android app where users complete daily habits to earn XP for their digital pet called a "Choreboo." Choreboos have stats (hunger, happiness, energy) and evolve through stages (Egg → Baby → Child → Teen → Adult → Legendary). Supports **households** — shared groups where members can see each other's pets and habits. Firebase Auth for identity; Firebase Data Connect (PostgreSQL) as cloud backend.

## Build Environment (WSL + Windows Android SDK)

> **IMPORTANT**: This project runs in WSL but Android Studio and the Android SDK are installed on Windows. All builds **must** go through `powershell.exe` calling `gradlew.bat`. Running `./gradlew` directly from WSL will fail because the SDK's build-tools contain Windows `.exe` binaries (`aapt2.exe`, etc.) that Linux cannot execute.
>
> Use `build.ps1` (project root) as a clean wrapper. `JAVA_HOME` is set persistently as a Windows User environment variable pointing to Android Studio's bundled JBR (`C:\Program Files\Android\Android Studio\jbr`); the script also auto-detects it as a fallback.

| Task | Command (run from WSL or any terminal) |
|------|----------------------------------------|
| Debug build | `powershell.exe -File build.ps1 assembleDebug` |
| Release build | `powershell.exe -File build.ps1 assembleRelease` |
| All unit tests | `powershell.exe -File build.ps1 testDebugUnitTest` |
| Instrumented tests | `powershell.exe -File build.ps1 connectedDebugAndroidTest` (requires device/emulator) |
| Lint | `powershell.exe -File build.ps1 lint` |

## Tech Stack
- **Language:** Kotlin
- **UI:** Jetpack Compose + Material3
- **DI:** Hilt (`@HiltAndroidApp`, `@AndroidEntryPoint`, `@HiltViewModel`)
- **Database:** Room (local cache) + Firebase Data Connect (PostgreSQL cloud backend)
- **Auth:** Firebase Auth (email/password + Google sign-in via **Android Credential Manager API** — `androidx.credentials` 1.5.0 + `googleid` 1.1.1; legacy GMS `GoogleSignIn` API not used)
- **Preferences:** DataStore
- **Animations:** Lottie Compose (placeholder emoji for now, swappable to Lottie later)
- **Images:** Coil
- **Background Work:** AlarmManager (per-habit reminders) + WorkManager (reschedule on reboot)
- **Widget:** Glance (not yet implemented)
- **Navigation:** Navigation Compose with bottom tabs
- **Serialization:** Gson (for Room TypeConverters)
- **Desugaring:** Enabled (`isCoreLibraryDesugaringEnabled = true`) for `java.time` on minSdk 24
- **Min SDK:** 24 | **Target SDK:** 36 | **Compile SDK:** 36 | **Compose BOM:** 2025.05.00

## Architecture
- **Pattern:** MVVM (Screen → ViewModel → Repository → DAO → Room), with write-through to Firebase Data Connect
- **Package structure:**
  ```
  com.example.choreboo_habittrackerfriend/
  ├── MainActivity.kt                  # @AndroidEntryPoint, dynamic startDestination (Auth/Onboarding/Pet)
  ├── ChorebooApplication.kt           # @HiltAndroidApp, notification channels
  ├── navigation/                      # ChorebooNavGraph.kt, Screen sealed class (8 routes)
  ├── data/
  │   ├── local/
│   │   ├── ChorebooDatabase.kt      # Room DB v16, 7 entities, fallbackToDestructiveMigration
│   │   ├── converter/
│   │   │   └── Converters.kt        # Gson TypeConverter for List<String>
│   │   ├── entity/                  # HabitEntity, HabitLogEntity, ChorebooEntity, HouseholdMemberEntity, HouseholdEntity, HouseholdHabitStatusEntity, PurchasedBackgroundEntity
│   │   └── dao/                     # HabitDao, HabitLogDao, ChorebooDao, HouseholdMemberDao, HouseholdDao, HouseholdHabitStatusDao, PurchasedBackgroundDao
│   ├── datastore/                   # UserPreferences (theme, onboarding, sound, totalPoints, totalLifetimeXp, profilePhotoUri)
│   └── repository/                  # HabitRepository, ChorebooRepository, AuthRepository, HouseholdRepository, UserRepository, BadgeRepository, BackgroundRepository, ResetRepository, SyncManager
├── di/                              # AppModule (DB, DAOs, DataStore, UserPreferences, FirebaseAuth), AppLifecycleObserver
├── domain/model/                    # Habit, ChorebooStats, ChorebooMood, ChorebooStage, PetType, Household, AppUser, Badge, Background
  ├── ui/
  │   ├── theme/                       # Color.kt (Choreboo palette), Theme.kt (themeMode param), Type.kt
  │   ├── components/                  # BottomNavBar (5 tabs with dynamic pet mood icon), ProfileAvatar, ShimmerPlaceholder, StitchSnackbar, PetBackgroundImage
  │   ├── auth/                        # AuthScreen, AuthViewModel (login/register/sync orchestration)
  │   ├── habits/                      # AddEditHabitScreen, AddEditHabitViewModel, components/ (HabitCard, StreakBadge)
  │   ├── pet/                         # PetScreen (habit list + feed bottom sheet + background picker), PetViewModel (absorbed HabitList functionality), components/ (StatBar, BackgroundPickerSheet)
  │   ├── stats/                       # StatsScreen, StatsViewModel
  │   ├── household/                   # HouseholdScreen (tap pet card → member habits dialog), HouseholdViewModel (enriches pet photos), components/ (HouseholdPetCard, HouseholdHabitCard)
  │   ├── calendar/                    # CalendarScreen (single LazyColumn, heatmap legend), CalendarViewModel
  │   ├── onboarding/                  # OnboardingScreen (name your Choreboo, hatch egg), OnboardingViewModel
  │   ├── settings/                    # SettingsScreen (theme, sound, reminders with permission request), SettingsViewModel (+ dev Reset Account flow)
  │   ├── inventory/                   # (placeholder — not yet implemented)
  │   └── shop/                        # (placeholder — not yet implemented)
  └── worker/                          # AlarmManager-based reminders (see below)
      ├── HabitReminderScheduler.kt    # Schedules per-habit alarms via AlarmManager
      ├── HabitReminderReceiver.kt     # BroadcastReceiver that shows reminder notification
      ├── HabitCompleteReceiver.kt     # BroadcastReceiver for "Complete" action on notification
      ├── ReminderRescheduleWorker.kt  # WorkManager job that reschedules alarms after reboot/update
      └── BootReceiver.kt             # BOOT_COMPLETED receiver that triggers ReminderRescheduleWorker
  ```

## Room Database Schema (v16, 7 entities)
- **habits** – id, title, description, iconName, customDays, difficulty, baseXp, reminderEnabled, reminderTime, createdAt, isArchived, isHouseholdHabit, ownerUid, householdId, assignedToUid, assignedToName, remoteId (`remoteId` indexed)
- **habit_logs** – id, habitId (FK→habits CASCADE), completedAt, date (ISO string), xpEarned, streakAtCompletion, completedByUid, remoteId (`remoteId` indexed, `date` indexed, UNIQUE(`habitId`, `date`))
- **choreboos** – id, name, stage, level, xp, hunger, happiness, energy, petType, lastInteractionAt, createdAt, sleepUntil, ownerUid, remoteId, backgroundId (`remoteId` indexed)
- **household_members** – uid (String PK = Firebase Auth UID), displayName, photoUrl, email, chorebooId, chorebooName, chorebooStage, chorebooLevel, chorebooXp, chorebooHunger, chorebooHappiness, chorebooEnergy, chorebooPetType, chorebooBackgroundId, lastSyncedAt
- **households** – id (String PK = Data Connect UUID), name, inviteCode, createdByUid, createdByName
- **household_habit_statuses** – habitId (String PK = Data Connect UUID), title, iconName, ownerName, ownerUid, baseXp, assignedToUid, assignedToName, completedByName, completedByUid, cachedDate
- **purchased_backgrounds** – ownerUid + backgroundId (composite PK), purchasedAt

- `remoteId` maps to Data Connect UUIDs for cloud sync. Indexed on `habits`, `habit_logs`, and `choreboos`.
- `ownerUid` / `completedByUid` / `householdId` map to Firebase Auth UIDs and household references.
- `habit_logs` has a UNIQUE(`habitId`, `date`) index for atomic duplicate prevention on habit completion.
- `insertLog` uses `OnConflictStrategy.IGNORE` — returns -1L when duplicate is ignored.
- `household_members` is a **read-only cloud cache** — written by `HouseholdRepository.refreshHouseholdMembers()` (identity columns) and `refreshHouseholdPets()` (pet columns) using partial-update `@Transaction` methods (`INSERT OR IGNORE` + targeted `UPDATE`). Cleared on sign-out/leave. Only members who have created a Choreboo appear in pet data. Uses `uid` as PK. No `remoteId` needed (uid is the stable cloud key).
- `households` is a **read-only cloud cache** for the user's current household. Written by `HouseholdRepository`, cleared on sign-out/leave.
- `household_habit_statuses` caches today's household habit completion statuses for the household screen. Uses Data Connect habit UUID as PK.
- Uses `fallbackToDestructiveMigration()` during development.
- Schemas exported to `app/schemas/`.

## Cloud Backend (Firebase Data Connect)

6 cloud tables in `dataconnect/schema/schema.gql`: User, Household, Choreboo, PurchasedBackground, Habit, HabitLog.

- **Write-through**: All Room mutations also fire the corresponding Data Connect mutation. Failures are silent.
- **Cloud-to-local sync**: Triggered **after auth** (`force = true`, bypasses cooldown) and **on every app foreground** (`force = false`, 5-minute cooldown). `SyncManager` orchestrates all sync. Order: habits + choreboo + user points + purchased backgrounds (parallel) → habit logs (sequential, needs habit remoteIds) → household habit logs (best-effort). Cloud wins on conflict.
- **Security**: All 16 queries and 27 mutations have `@auth(level: USER)` directives with auth-scoped filters. 4 household queries (`GetMyHousehold`, `GetMyHouseholdMembers`, `GetMyHouseholdChoreboos`, `GetMyHouseholdHabits`) are **inherently auth-scoped** — they traverse from `auth.uid` to the user's household, so no `householdId` parameter is needed. 3 habit/log queries (`GetHabitById`, `GetLogsForHabit`, `GetLogsForHabitAndDate`) that allow access to household habits now require household membership verification — the `isHouseholdHabit` branch checks that the caller's `auth.uid` is a member of the habit's household.
- **SDK regen**: `npx firebase-tools@latest dataconnect:sdk:generate`
- **Firebase Storage**: Configured for profile photo uploads (`storage.rules` in project root)

### Firebase Deploy

Deploy the backend (Data Connect schema/connectors + Storage rules) to the `choreboo-7f36c` project:

| Task | Command (run from WSL or any terminal) |
|------|----------------------------------------|
| Full deploy (Data Connect + Storage) | `npx firebase-tools@latest deploy --project choreboo-7f36c` |
| Data Connect only | `npx firebase-tools@latest deploy --only dataconnect --project choreboo-7f36c` |
| Storage rules only | `npx firebase-tools@latest deploy --only storage --project choreboo-7f36c` |

- **Config files**: `firebase.json` (defines `dataconnect` + `storage` targets), `.firebaserc` (default project = `choreboo-7f36c`)
- **Storage rules**: `storage.rules` — profile photos readable by all authenticated users, writable/deletable only by the owner
- Always deploy Data Connect after modifying `schema.gql`, `queries.gql`, or `mutations.gql`.

## Navigation
- 5 bottom tabs: Choreboo (`pet`), Stats (`stats`), House (`household`), History (`calendar`), Settings (`settings`)
- Additional routes: `auth`, `onboarding`, `add_edit_habit?habitId={id}`
- Bottom bar hidden on: auth, onboarding, add/edit habit
- Start destination is dynamic: Auth → Onboarding → Pet
- BottomNavBar shows dynamic pet mood icon for the Choreboo tab

## Key Conventions
- All ViewModels use `@HiltViewModel` with `@Inject constructor`
- Repositories are `@Singleton` and return `Flow<>` for reactive data
- UI state exposed as `StateFlow` via `.stateIn(viewModelScope, WhileSubscribed(5000), default)`
- One-shot events use `MutableSharedFlow` (snackbars, navigation, level-up celebrations)
- Domain models are separate from Room entities; mapping via extension functions (`toDomain()` / `toEntity()`)
- Enums stored as Strings in Room (no TypeConverter needed for enums)
- Dates stored as ISO-8601 strings ("2026-03-24") for easy Room queries
- Stat decay calculated on app open based on `lastInteractionAt` timestamp
- Core library desugaring enabled for `java.time` API support on minSdk 24
- Write-through: any Room mutation should also call the corresponding Data Connect mutation
- **StateFlow collection**: Always use `collectAsStateWithLifecycle()` (from `androidx.lifecycle.compose`) in Screen composables — never `collectAsState()`
- **Saveable state**: Use `rememberSaveable` (import `androidx.compose.runtime.saveable.rememberSaveable`) for dialog flags, form text, and tab state that must survive config changes
- **String resources**: All user-facing strings use `stringResource(R.string.*)` — no hardcoded string literals in composables. Add strings to `res/values/strings.xml` only

## Implemented Features
- **Firebase Auth** — Email/password + Google sign-in via Android Credential Manager, syncing overlay on auth screen
- **Households** — Create/join via invite code, view household members' pets and habits
- **Cloud sync** — Write-through on all mutations, cloud-to-local sync on auth + app foreground (via `SyncManager`)
- **Habit completion** — XP earned (base + streak bonus), prevents over-completion via targetCount
- **Streak tracking** — StreakBadge component, streaks displayed per habit from HabitLogDao
- **Scheduling** — `Habit.isScheduledForToday()` disables completion button for CUSTOM habits on non-scheduled days
- **Delete confirmation** — AlertDialog before deleting a habit
- **Level-up celebration** — AlertDialog shown when XP causes level-up or stage evolution
- **Theme wiring** — Settings theme picker (system/light/dark) propagated to Theme.kt via `themeMode` param
- **ReminderWorker** — AlarmManager-based per-habit reminders with notification "Complete" action, POST_NOTIFICATIONS permission handled
- **Calendar heatmap** — Single LazyColumn with color-coded days + legend + detail logs with habit names
- **Customizable pet backgrounds** — 10 backgrounds (1 free default + 9 purchasable with tiered pricing: COMMON=50, RARE=100, PREMIUM=200 star points). Selected via `BackgroundPickerSheet` bottom sheet. Stored on `Choreboo` cloud table (`backgroundId`). Purchases tracked in `PurchasedBackground` cloud table and `purchased_backgrounds` Room table. Background visible in `PetScreen` and household view. `PetBackgroundImage` composable renders mood-gradient for default or loads `assets/backgrounds/<id>.webp` via Coil.

## Color Palette
- **Light:** Primary `#006E1C` (deep green), Secondary `#8B5000` (warm orange), Tertiary `#6833EA` (purple)
- **Dark:** Primary `#78DC77`, Secondary `#FFB870`, Tertiary `#CDBDFF`
- **Accent:** StreakFlame=`#FF6D00`, GoldGlow=`#FFD54F`
- Dynamic color is **disabled** so the custom Choreboo palette always shows

## Style Guidelines
- Modern, rounded UI: `RoundedCornerShape(16.dp)` for cards, `RoundedCornerShape(12.dp)` for inputs
- Use `MaterialTheme.typography.*` and `MaterialTheme.colorScheme.*` consistently — no hardcoded text styles or colors
- Touch targets >= 48dp
- All icons have `contentDescription` for accessibility
- Empty states show emoji + friendly message + call to action
- Use `AnimatedVisibility`, `animateColorAsState` for smooth transitions
- Destructive actions (delete) require confirmation dialog
- **Bottom nav bar** lives in `Scaffold`'s `bottomBar` slot in `MainActivity`; no hardcoded 80dp spacers in screens — `innerPadding` + `consumeWindowInsets` propagates to the nav graph

## Available Habit Icons
Emoji-based system using `EmojiIcon` data class. 15 preset emoji (e.g., "🥗", "💧", "🏃", "📚", "🧘", "🎵", "🔥", "🏋️", "😴", "💻", "🍽️", "🧹", "📖", "🎨", "❤️") plus custom emoji input. Not Material Icons.

## Pet Animation Strategy
- Currently using placeholder emoji per stage
- Designed to be swapped with Lottie JSON files in `res/raw/` (choreboo_idle.json, choreboo_happy.json, etc.)
- `PetAnimationView` composable selects animation based on `ChorebooMood`
- Pet size scales by `ChorebooStage`

## Key Data Flow Patterns
- **`ChorebooRepository.addXp()`** returns `XpResult(levelsGained, newLevel, evolved, newStage)` for celebration UI
- **`HabitRepository.completeHabit()`** returns `CompletionResult(xpEarned, newStreak, alreadyComplete)` with targetCount enforcement
- **`HabitRepository.getStreaksForToday()`** returns `Flow<Map<Long, Int>>` for StreakBadge display
- **Cloud-to-local sync**: `HabitRepository.syncHabitsFromCloud()`, `HabitRepository.syncHabitLogsFromCloud()`, `ChorebooRepository.syncFromCloud()`, `BackgroundRepository.syncFromCloud()`. Called from `SyncManager.syncAll()`.
- **Auth orchestration**: `AuthViewModel` delegates to `SyncManager.syncAll(force = true)` after login/register

## When Generating Code
- Always use the package `com.example.choreboo_habittrackerfriend`
- Use Material3 APIs (not Material2)
- Use `Icons.Default.*` or `Icons.AutoMirrored.Filled.*` from material-icons-extended
- Prefer `Modifier.fillMaxWidth()` over hardcoded widths
- Always handle the `innerPadding` from `Scaffold`
- Room queries: use `Flow<>` for observable, `suspend` for one-shot
- When creating new screens, follow the existing pattern: ViewModel + Screen composable + components/ subfolder
- Use `AlertDialog` for confirmations (delete, level-up); `ModalBottomSheet` for selection lists (feed)
- For new features that need level-up detection, use the `XpResult` return from `ChorebooRepository.addXp()`
- When adding new habit icons, update `iconOptions` in AddEditHabitScreen (emoji-based `EmojiIcon` system)
- Write-through: any Room mutation should also call the corresponding Data Connect mutation

## Not Yet Implemented (Future)
- Glance widget (today's habits + pet mood)
- Sound effects (play on completion, feeding, level-up)
- Lottie animations (replace emoji placeholders)
- Multiple Choreboos
- IME keyboard padding (`imePadding()` not yet applied to Auth, Onboarding, AddEditHabit, Settings screens)

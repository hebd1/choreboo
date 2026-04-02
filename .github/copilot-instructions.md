# Choreboo Habit Tracker Friend - Copilot Instructions

## Project Overview
A Tamagotchi-style habit tracker Android app where users complete daily habits to earn XP, points, and food items for their digital pet called a "Choreboo." Choreboos have stats (hunger, happiness, energy), evolve through stages (Egg → Baby → Child → Teen → Adult → Legendary), and can be customized with accessories.

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
- **Database:** Room (local, offline-first, no backend)
- **Preferences:** DataStore
- **Animations:** Lottie Compose (placeholder emoji for now, swappable to Lottie later)
- **Images:** Coil
- **Background Work:** WorkManager (ReminderWorker for daily notifications)
- **Widget:** Glance (not yet implemented)
- **Navigation:** Navigation Compose with bottom tabs
- **Serialization:** Gson (for Room TypeConverters)
- **Desugaring:** Enabled (`isCoreLibraryDesugaringEnabled = true`) for `java.time` on minSdk 24
- **Min SDK:** 24 | **Target SDK:** 36 | **Compile SDK:** 36

## Architecture
- **Pattern:** MVVM (Screen → ViewModel → Repository → DAO → Room)
- **Package structure:**
  ```
  com.example.choreboo_habittrackerfriend/
  ├── MainActivity.kt              # @AndroidEntryPoint, hosts NavGraph + BottomNavBar, injects UserPreferences + ChorebooRepository
  ├── ChorebooApplication.kt         # @HiltAndroidApp, notification channels
  ├── navigation/                  # ChorebooNavGraph.kt, Screen sealed class
  ├── data/
  │   ├── local/                   # ChorebooDatabase, entities, DAOs, TypeConverters
  │   ├── datastore/               # UserPreferences (points, theme, reminders, onboarding, sound)
  │   └── repository/              # HabitRepository, ChorebooRepository (XpResult), ShopRepository (PurchaseResult), InventoryRepository
  ├── di/                          # AppModule (Hilt @Module providing DB, DAOs, DataStore)
  ├── domain/model/                # Habit (isScheduledForToday), ChorebooStats (mood, xpProgress), Item, enums
  ├── ui/
  │   ├── theme/                   # Color.kt (Choreboo palette), Theme.kt (themeMode param), Type.kt
  │   ├── components/              # BottomNavBar (petMood dynamic icon)
  │   ├── habits/                  # HabitListScreen (delete confirm, level-up dialog), AddEditHabitScreen, components/ (HabitCard, StreakBadge)
  │   ├── pet/                     # PetScreen (equipped accessories display, feed bottom sheet), PetViewModel (EquippedItemInfo), components/StatBar
  │   ├── shop/                    # ShopScreen (item descriptions), ShopItemCard
  │   ├── inventory/               # InventoryScreen (equip/unequip), InventoryItemCard (equipped state)
  │   ├── calendar/                # CalendarScreen (single LazyColumn, heatmap legend), CalendarViewModel
  │   ├── onboarding/              # OnboardingScreen (name your Choreboo, hatch egg)
  │   └── settings/                # SettingsScreen (theme, sound, reminders with permission request)
  └── worker/                      # ReminderWorker (daily notifications with random messages)
  ```

## Room Database Schema (v1, 6 entities)
- **habits** – id, title, description, iconName, frequency, customDays, targetCount, baseXp, createdAt, isArchived
- **habit_logs** – id, habitId (FK→habits CASCADE), completedAt, date (ISO string), xpEarned, streakAtCompletion
- **choreboos** – id, name, stage, level, xp, hunger, happiness, energy, lastInteractionAt, createdAt
- **items** – id, name, description, type (FOOD/HAT/CLOTHES/BACKGROUND), rarity, price, effectValue, effectStat, animationAsset (pre-seeded on DB create)
- **inventory_items** – id, itemId (FK→items), quantity, acquiredAt
- **equipped_items** – id, chorebooId (FK→choreboos), itemId (FK→items), slot

Using `fallbackToDestructiveMigration()` during development.

## Navigation
- 4 bottom tabs: Habits (`habits_list`), Choreboo (`pet`), Shop (`shop`), Calendar (`calendar`)
- Additional routes: `add_edit_habit?habitId={id}`, `inventory`, `settings`, `onboarding`
- Bottom bar hidden on: onboarding, add/edit habit, inventory, settings
- BottomNavBar shows dynamic pet mood icon for the Choreboo tab

## Key Conventions
- All ViewModels use `@HiltViewModel` with `@Inject constructor`
- Repositories are `@Singleton` and return `Flow<>` for reactive data
- UI state exposed as `StateFlow` via `.stateIn(viewModelScope, WhileSubscribed(5000), default)`
- One-shot events use `MutableSharedFlow` (snackbars, navigation, level-up celebrations)
- Domain models are separate from Room entities; mapping via extension functions (`toDomain()` / `toEntity()`)
- Enums stored as Strings in Room (no TypeConverter needed for enums)
- Dates stored as ISO-8601 strings ("2026-03-24") for easy Room queries
- Points/currency managed via DataStore, not Room
- Stat decay calculated on app open based on `lastInteractionAt` timestamp
- Core library desugaring enabled for `java.time` API support on minSdk 24

## Implemented Features
- **Habit completion** — XP earned (base + streak bonus), prevents over-completion via targetCount, 30% random food reward drop
- **Streak tracking** — StreakBadge component, streaks displayed per habit from HabitLogDao
- **Scheduling** — `Habit.isScheduledForToday()` disables completion button for CUSTOM habits on non-scheduled days
- **Delete confirmation** — AlertDialog before deleting a habit
- **Level-up celebration** — AlertDialog shown when XP causes level-up or stage evolution
- **Equip system** — Accessories (HAT/CLOTHES/BACKGROUND) equippable from inventory, shown on PetScreen
- **Theme wiring** — Settings theme picker (system/light/dark) propagated to Theme.kt via `themeMode` param
- **ReminderWorker** — Daily WorkManager job with randomized notification messages, POST_NOTIFICATIONS permission handled
- **Calendar heatmap** — Single LazyColumn with color-coded days + legend + detail logs with habit names

## Color Palette
- **Light:** Primary `#2E7D32` (forest green), Secondary `#00897B` (teal), Tertiary `#F57C00` (orange)
- **Dark:** Primary `#81C784`, Secondary `#4DB6AC`, Tertiary `#FFB74D`
- **Pet mood colors:** Happy=`#E8F5E9`, Hungry=`#FFF3E0`, Tired=`#E3F2FD`, Sad=`#ECEFF1`
- **Accent:** XpPurple=`#7C4DFF`, StreakFlame=`#FF6D00`, GoldGlow=`#FFD54F`
- Dynamic color is **disabled** so the custom Choreboo palette always shows

## Style Guidelines
- Modern, rounded UI: `RoundedCornerShape(16.dp)` for cards, `RoundedCornerShape(12.dp)` for inputs
- Use `MaterialTheme.typography.*` consistently (no hardcoded text styles)
- Touch targets ≥ 48dp
- All icons have `contentDescription` for accessibility
- Empty states show emoji + friendly message + call to action
- Use `AnimatedVisibility`, `animateColorAsState` for smooth transitions
- Destructive actions (delete) require confirmation dialog

## Available Habit Icons
CheckCircle, FitnessCenter, MenuBook, WaterDrop, SelfImprovement, MusicNote, LocalFireDepartment, DirectionsRun, Bedtime, Code, Restaurant, CleaningServices, School, Brush, Favorite

## Pet Animation Strategy
- Currently using placeholder emoji per stage (🥚🐣🐥🐤🐔🦅)
- Designed to be swapped with Lottie JSON files in `res/raw/` (choreboo_idle.json, choreboo_happy.json, etc.)
- `PetAnimationView` composable selects animation based on `ChorebooMood`
- Pet size scales by `ChorebooStage`

## Key Data Flow Patterns
- **`ChorebooRepository.addXp()`** returns `XpResult(levelsGained, newLevel, evolved, newStage)` for celebration UI
- **`HabitRepository.completeHabit()`** returns `CompletionResult(xpEarned, newStreak, alreadyComplete)` with targetCount enforcement
- **`ShopRepository.getRandomFoodItem()`** uses weighted rarity (70% COMMON, 25% RARE, 5% LEGENDARY)
- **`HabitRepository.getStreaksForToday()`** returns `Flow<Map<Long, Int>>` for StreakBadge display
- **EquippedItemInfo** — PetViewModel resolves equipped entity item IDs to display names reactively

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
- When adding new habit icons, update both `iconOptions` in AddEditHabitScreen and `getIconForName()` in HabitCard

## Not Yet Implemented (Future)
- Glance widget (today's habits + pet mood)
- Sound effects (play on completion, feeding, level-up)
- Lottie animations (replace emoji placeholders)
- Mystery eggs in shop
- Multiple Choreboos

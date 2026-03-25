# AGENTS.md

## Project

Tamagotchi-style habit tracker Android app (Kotlin, Jetpack Compose, Material3). Users complete habits → earn XP/points → feed and evolve a digital pet ("Weeboo"). Fully offline; Room + DataStore, no backend.

## Architecture

MVVM: `Screen composable → @HiltViewModel → @Singleton Repository → @Dao → Room`

- **Entities** live in `data/local/entity/` — Room-annotated data classes with String-typed enums and `Long` timestamps.
- **Domain models** live in `domain/model/` — mirror entities but use Kotlin enums and typed fields. `Habit` has `isScheduledForToday()` helper.
- **Mapping** uses private extension functions (`toDomain()` / `toEntity()`) defined at the bottom of each Repository file.
- **DI**: Single `AppModule` provides DB, all DAOs, DataStore, and `UserPreferences`. All wiring is constructor injection.
- **Desugaring**: `isCoreLibraryDesugaringEnabled = true` — `java.time` APIs work on minSdk 24.

## Data Flow Patterns

- **Reactive reads**: DAOs return `Flow<>` → Repositories expose `Flow<>` → ViewModels expose `StateFlow` via `.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), default)`.
- **One-shot writes**: DAO methods are `suspend`; called from `viewModelScope.launch {}` in ViewModels.
- **One-shot events** (snackbars, navigation, level-up celebrations): `MutableSharedFlow` exposed as `events` in ViewModels (see `HabitListEvent`, `ShopEvent`, `PetEvent`, `InventoryEvent`).
- **Points/currency**: Managed in `UserPreferences` (DataStore), NOT Room. Use `userPreferences.addPoints()` / `deductPoints()`.
- **Stat decay**: Calculated on PetScreen open via `weebooRepository.applyStatDecay()` based on `lastInteractionAt`.
- **XP/Level-up**: `WeebooRepository.addXp()` returns `XpResult(levelsGained, newLevel, evolved, newStage)` so callers can trigger celebration UI.
- **Habit completion**: `HabitRepository.completeHabit()` returns `CompletionResult(xpEarned, newStreak, alreadyComplete)` with targetCount enforcement.
- **Food rewards**: 30% chance on habit completion via `ShopRepository.getRandomFoodItem()` with weighted rarity.
- **Streaks**: `HabitRepository.getStreaksForToday()` returns `Flow<Map<Long, Int>>` for StreakBadge display.

## Key Conventions

- **Package**: `com.example.weeboo_habittrackerfriend`
- **Enums stored as `String` in Room** — parse with `try { EnumType.valueOf(field) } catch (_: Exception) { DEFAULT }` in `toDomain()`.
- **Dates**: ISO-8601 strings (`"2026-03-24"`) in `habit_logs.date`; timestamps (`Long`) for `createdAt`, `lastInteractionAt`.
- **`customDays`**: Comma-separated string in entity (`"Mon,Wed,Fri"`), `List<String>` in domain model.
- **DB seeding**: Shop items are inserted via raw SQL in `SeedDatabaseCallback.onCreate()` inside `WeebooDatabase.kt`. Uses `fallbackToDestructiveMigration()`.
- **Navigation**: `Screen` sealed class in `navigation/WeebooNavGraph.kt`. 4 bottom tabs (`habits_list`, `pet`, `shop`, `calendar`); bottom bar hidden on `onboarding`, `add_edit_habit`, `inventory`, `settings`. BottomNavBar shows dynamic pet mood icon.
- **Destructive actions** (delete habit) require `AlertDialog` confirmation.
- **Level-up celebrations** shown via `AlertDialog` in HabitListScreen when XP causes level/stage change.
- **Equipped accessories** displayed on PetScreen via `EquippedItemInfo` resolved in PetViewModel.
- **Notification permission**: Requested on Android 13+ via `ActivityResultContracts.RequestPermission()` in SettingsScreen.

## Adding a New Feature (Screen)

1. Create `ui/<feature>/` folder with `FeatureScreen.kt`, `FeatureViewModel.kt`, and optional `components/` subfolder.
2. ViewModel: `@HiltViewModel class FeatureViewModel @Inject constructor(...)`.
3. Add route to `Screen` sealed class and composable to `WeebooNavGraph`.
4. If it needs a bottom tab, add to `bottomNavRoutes` in `MainActivity.kt` and `BottomNavBar.kt`.
5. If it needs new data, add Entity → DAO → provide DAO in `AppModule` → create/update Repository.

## Available Habit Icons

CheckCircle, FitnessCenter, MenuBook, WaterDrop, SelfImprovement, MusicNote, LocalFireDepartment, DirectionsRun, Bedtime, Code, Restaurant, CleaningServices, School, Brush, Favorite — defined in `AddEditHabitScreen.iconOptions` and resolved in `HabitCard.getIconForName()`.

## Build & Run

- **Build**: `./gradlew assembleDebug` (uses AGP 9.0.1, Kotlin 2.0.21, KSP for Room/Hilt)
- **Version catalog**: All dependency versions in `gradle/libs.versions.toml`
- **Room schemas** exported to `app/schemas/`
- **Min SDK 24** — `java.time` APIs used with core library desugaring enabled
- **Desugaring**: `desugar_jdk_libs` 2.1.4 via `coreLibraryDesugaring` dependency

## UI Rules

- Material3 only — use `MaterialTheme.colorScheme.*` and `MaterialTheme.typography.*`.
- Dynamic color is **disabled** (`dynamicColor = false` in `Theme.kt`) so the custom Weeboo green/teal/orange palette is always applied.
- Theme.kt accepts `themeMode: String` ("system"/"light"/"dark") parameter from MainActivity.
- Card corners: `RoundedCornerShape(16.dp)`. Input corners: `RoundedCornerShape(12.dp)`.
- Always handle `innerPadding` from `Scaffold`. Use `Modifier.fillMaxWidth()` over hardcoded widths.
- Icons: `Icons.Default.*` or `Icons.AutoMirrored.Filled.*` from material-icons-extended, always with `contentDescription`.
- Pet animations: Currently emoji placeholders per `WeebooMood`; designed to swap to Lottie JSON in `res/raw/`.
- Use `AlertDialog` for confirmations (delete, level-up); `ModalBottomSheet` for selection lists (feed).
- CalendarScreen uses a single `LazyColumn` with `item {}` blocks to avoid nested scrolling.

## Not Yet Implemented

- Glance widget (today's habits + pet mood)
- Sound effects
- Lottie animations (replace emoji placeholders)
- Mystery eggs in shop
- Multiple Weeboos

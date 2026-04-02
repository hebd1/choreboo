# AGENTS.md

## Project

Tamagotchi-style habit tracker Android app (Kotlin, Jetpack Compose, Material3). Users complete habits → earn XP/points → feed and evolve a digital pet ("Choreboo"). Fully offline; Room + DataStore, no backend.

## Architecture

MVVM: `Screen composable → @HiltViewModel → @Singleton Repository → @Dao → Room`

- **Entities** live in `data/local/entity/` — Room-annotated data classes with String-typed enums and `Long` timestamps.
- **Domain models** live in `domain/model/` — mirror entities but use Kotlin enums and typed fields. `Habit` has `isScheduledForToday()` helper.
- **Mapping** uses extension functions (`toDomain()` / `toEntity()`) defined at the bottom of each Repository file (private except in `ShopRepository`).
- **DI**: Single `AppModule` provides DB, all DAOs, DataStore, and `UserPreferences`. All wiring is constructor injection.
- **Desugaring**: `isCoreLibraryDesugaringEnabled = true` — `java.time` APIs work on minSdk 24.
- **TypeConverters**: `Converters.kt` uses Gson for `List<String>` conversion. Enums are stored as plain Strings (no TypeConverter).

## Data Flow Patterns

- **Reactive reads**: DAOs return `Flow<>` → Repositories expose `Flow<>` → ViewModels expose `StateFlow` via `.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), default)`.
- **One-shot writes**: DAO methods are `suspend`; called from `viewModelScope.launch {}` in ViewModels.
- **One-shot events** (snackbars, navigation, level-up): `MutableSharedFlow` exposed as `events` in ViewModels (see `HabitListEvent`, `ShopEvent`, `PetEvent`, `InventoryEvent`).
- **Points/currency**: Managed in `UserPreferences` (DataStore), NOT Room. Use `userPreferences.addPoints()` / `deductPoints()`.
- **Stat decay**: Calculated on PetScreen open via `chorebooRepository.applyStatDecay()` based on `lastInteractionAt`.
- **XP/Level-up**: `ChorebooRepository.addXp()` returns `XpResult(levelsGained, newLevel, evolved, newStage)` so callers can trigger celebration UI.
- **Habit completion**: `HabitRepository.completeHabit()` returns `CompletionResult(xpEarned, newStreak, alreadyComplete)` with targetCount enforcement.
- **Food rewards**: 30% chance on habit completion via `ShopRepository.getRandomFoodItem()` with weighted rarity (70% COMMON, 25% RARE, 5% LEGENDARY).
- **Streaks**: `HabitRepository.getStreaksForToday()` returns `Flow<Map<Long, Int>>` for StreakBadge display.

## Build & Run

- **Gradle**: 9.3.1 wrapper, AGP 8.9.1, Kotlin 2.0.21, KSP 2.0.21-1.0.27
- **SDK**: minSdk 24, compileSdk 36, targetSdk 36
- **Version catalog**: All dependency versions in `gradle/libs.versions.toml`
- **Room schemas** exported to `app/schemas/`

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
- **Constants**: `SCREAMING_SNAKE_CASE` (e.g., `TOTAL_POINTS`, `REMINDER_CHANNEL_ID`).
- **Private mutable state**: Prefix with `_` (e.g., `_events`, `_formState`, `_selectedMonth`).
- **Result types**: `<Action>Result` (e.g., `CompletionResult`, `XpResult`, `PurchaseResult`).
- **Event sealed classes**: `<Feature>Event` (e.g., `HabitListEvent`, `ShopEvent`).

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
  - `PurchaseResult` (Success / InsufficientFunds / ItemNotFound)
  - `CompletionResult` (with `alreadyComplete` flag)
- **No try/catch in ViewModels or Screen composables** — coroutine failures propagate.

## File Organization

- **Result/sealed classes** for operation outcomes: defined at **top** of Repository file, before the class.
- **Event sealed classes**: defined at **bottom** of ViewModel file, after the class.
- **Helper data classes** (e.g., `HabitStreak`): at **bottom** of DAO files.
- **Mapping functions** (`toDomain()` / `toEntity()`): private extension fns at **bottom** of Repository files.
- **Private val constants** (e.g., `iconOptions`, `daysOfWeek`): at **top** of Screen files, before the composable.
- Some small composables live inline in their Screen file (e.g., `ShopItemCard`, `FeedBottomSheet`) rather than in the `components/` subfolder.

## Key Conventions

- **Package**: `com.example.choreboo_habittrackerfriend`
- **Enums stored as `String` in Room** — parse with `try/catch` in `toDomain()`.
- **Dates**: ISO-8601 strings (`"2026-03-24"`) in `habit_logs.date`; timestamps (`Long`) for `createdAt`, `lastInteractionAt`.
- **`customDays`**: Comma-separated string in entity (`"Mon,Wed,Fri"`), `List<String>` in domain model.
- **DB seeding**: Shop items inserted via raw SQL in `SeedDatabaseCallback.onCreate()` inside `ChorebooDatabase.kt`. Uses `fallbackToDestructiveMigration()`.
- **Navigation**: `Screen` sealed class in `navigation/ChorebooNavGraph.kt`. 4 bottom tabs (`habits_list`, `pet`, `shop`, `calendar`); bottom bar hidden on `onboarding`, `add_edit_habit`, `inventory`, `settings`.
- **Destructive actions** (delete habit) require `AlertDialog` confirmation.
- **Level-up celebrations** shown via `AlertDialog` when XP causes level/stage change.

## UI Rules

- Material3 only — use `MaterialTheme.colorScheme.*` and `MaterialTheme.typography.*`. No hardcoded text styles.
- Dynamic color is **disabled** (`dynamicColor = false`) — custom Choreboo green/teal/orange palette always applied.
- Card corners: `RoundedCornerShape(16.dp)`. Input corners: `RoundedCornerShape(12.dp)`.
- Always handle `innerPadding` from `Scaffold`. Use `Modifier.fillMaxWidth()` over hardcoded widths.
- Icons: `Icons.Default.*` or `Icons.AutoMirrored.Filled.*` from material-icons-extended, always with `contentDescription`.
- Touch targets ≥ 48dp. Empty states show emoji + friendly message + CTA.
- Use `AlertDialog` for confirmations; `ModalBottomSheet` for selection lists (feed).
- Pet animations: emoji placeholders per `ChorebooMood`; designed to swap to Lottie JSON in `res/raw/`.

## Adding a New Feature (Screen)

1. Create `ui/<feature>/` folder with `FeatureScreen.kt`, `FeatureViewModel.kt`, and optional `components/` subfolder.
2. ViewModel: `@HiltViewModel class FeatureViewModel @Inject constructor(...)`.
3. Add route to `Screen` sealed class and composable to `ChorebooNavGraph`.
4. If it needs a bottom tab, add to `bottomNavRoutes` in `MainActivity.kt` and `BottomNavBar.kt`.
5. If it needs new data: add Entity → DAO → provide DAO in `AppModule` → create/update Repository.

## Copilot Instructions

Additional context lives in `.github/copilot-instructions.md` (color palette hex codes, Room schema columns, pet animation strategy). Key code-gen rules from that file:
- Always use Material3 APIs (not Material2).
- Room queries: `Flow<>` for observable, `suspend` for one-shot.
- For level-up detection, use `XpResult` from `ChorebooRepository.addXp()`.
- When adding habit icons, update both `iconOptions` in `AddEditHabitScreen` and `getIconForName()` in `HabitCard`.

## Not Yet Implemented

- Glance widget (today's habits + pet mood)
- Sound effects
- Lottie animations (replace emoji placeholders)
- Mystery eggs in shop
- Multiple Choreboos

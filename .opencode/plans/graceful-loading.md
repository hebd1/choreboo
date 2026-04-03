# Graceful Loading Plan

## Problem 1: Habits screen flashes "No habits yet" before Room data loads
## Problem 2: Lottie animation blank for 1-2s on first Pet tab navigation

---

## Change 1: HabitListViewModel.kt

Add `isLoading` StateFlow and wire it to the habits flow.

### Add import
Add `onEach` to the existing imports:
```kotlin
import kotlinx.coroutines.flow.onEach
```

### Add isLoading state + modify habits flow
After line 31 (`} : ViewModel() {`), before the existing `habits` declaration:

```kotlin
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
```

Modify the `habits` declaration to include `.onEach`:
```kotlin
    val habits: StateFlow<List<Habit>> = habitRepository.getAllHabits()
        .onEach { _isLoading.value = false }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
```

---

## Change 2: HabitListScreen.kt

### Add import
```kotlin
import androidx.compose.material3.CircularProgressIndicator
```

### Collect isLoading
Around line 86 (where other states are collected), add:
```kotlin
val isLoading by viewModel.isLoading.collectAsState()
```

### Replace the empty state + list rendering logic (lines 225-286)

Replace the existing `AnimatedVisibility` empty state block and the `if (habits.isNotEmpty())` block with a three-way branch:

```kotlin
        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
            habits.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "\uD83D\uDCCB", style = MaterialTheme.typography.displayLarge)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No habits yet!",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tap + to create your first habit\nand start earning rewards for your Choreboo!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item { Spacer(modifier = Modifier.height(8.dp)) }
                    item {
                        WeeklyNarrativeCard(
                            completionPct = completionPct,
                            completionFraction = completionFraction,
                        )
                    }
                    items(habits, key = { it.id }) { habit ->
                        HabitCard(
                            habit = habit,
                            completedToday = todayCompletions[habit.id] ?: 0,
                            currentStreak = streaks[habit.id] ?: 0,
                            isScheduledToday = habit.isScheduledForToday(),
                            onComplete = { viewModel.completeHabit(habit.id) },
                            onEdit = { onEditHabit(habit.id) },
                            onDelete = { habitToDelete = habit },
                        )
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
```

This removes the `AnimatedVisibility` wrapper (which isn't needed since we now have a distinct loading state) and uses a clean `when` block.

---

## Change 3: MainActivity.kt

### Add import
```kotlin
import com.airbnb.lottie.LottieCompositionFactory
```

### Add preloading in onCreate()
After `enableEdgeToEdge()` and before `setContent {}`, add:

```kotlin
        // Preload fox Lottie animations into LottieCompositionCache so they're
        // instantly available when the user navigates to the Pet tab.
        listOf(
            "animations/fox/fox_happy_lottie.json",
            "animations/fox/fox_hungry_lottie.json",
            "animations/fox/fox_sad_lottie.json",
            "animations/fox/fox_idle_lottie.json",
            "animations/fox/fox_eating_lottie.json",
            "animations/fox/fox_interact_lottie.json",
        ).forEach { path ->
            LottieCompositionFactory.fromAsset(this, path)
        }
```

`LottieCompositionFactory.fromAsset()` returns a `LottieTask` that runs async and caches the parsed composition. When `rememberLottieComposition` later requests the same asset path, it gets an immediate cache hit.

---

## Files touched
1. `app/src/main/java/.../ui/habits/HabitListViewModel.kt` - add isLoading flow
2. `app/src/main/java/.../ui/habits/HabitListScreen.kt` - loading/empty/loaded branching
3. `app/src/main/java/.../MainActivity.kt` - Lottie preloading

## Verification
Run: `powershell.exe -File build.ps1 assembleDebug`

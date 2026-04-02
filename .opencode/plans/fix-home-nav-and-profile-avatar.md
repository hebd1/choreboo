# Plan: Fix Home Button Navigation + Replace Hatching Chick with Profile Avatar

## Problem
1. **Intermittent Home button bug**: After tapping the profile avatar in the top-right of HabitListScreen (which navigates to Settings via a plain `navController.navigate()`), the Home bottom tab stops working correctly and navigates to Settings instead of the habits screen. Root cause: the plain navigate creates a back stack entry that conflicts with the bottom nav's `saveState`/`restoreState` mechanism.
2. **Hatching chick icon**: The 🐣 emoji in the top-left of 4 screens should be replaced with the user's profile avatar.
3. **Profile photo in top-right**: Should be removed from HabitListScreen (this also fixes bug #1).

## Changes

### 1. HabitListScreen.kt
**File:** `app/src/main/java/com/example/choreboo_habittrackerfriend/ui/habits/HabitListScreen.kt`

- Remove `onNavigateToSettings: () -> Unit` from function signature (line 71)
- Remove unused import `Icons.Default.Settings` (line 25)
- Replace the 🐣 Box (lines 118-126) with `ProfileAvatar(profilePhotoUri, viewModel.googlePhotoUrl, size = 40.dp)`
- Remove the `IconButton(onClick = onNavigateToSettings) { ProfileAvatar(...) }` block from `actions` (lines 159-166)
- Keep the points pill in actions

### 2. ChorebooNavGraph.kt
**File:** `app/src/main/java/com/example/choreboo_habittrackerfriend/navigation/ChorebooNavGraph.kt`

- Remove `onNavigateToSettings = { navController.navigate(Screen.Settings.route) }` from the HabitListScreen call (line 67)

### 3. CalendarViewModel.kt
**File:** `app/src/main/java/com/example/choreboo_habittrackerfriend/ui/calendar/CalendarViewModel.kt`

- Add `AuthRepository` import and constructor injection parameter
- Add `profilePhotoUri` StateFlow (same pattern as HabitListViewModel lines 37-38):
  ```kotlin
  val profilePhotoUri: StateFlow<String?> = userPreferences.profilePhotoUri
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
  ```
- Add `googlePhotoUrl` computed property:
  ```kotlin
  val googlePhotoUrl: String?
      get() = authRepository.currentFirebaseUser?.photoUrl?.toString()
  ```

### 4. CalendarScreen.kt
**File:** `app/src/main/java/com/example/choreboo_habittrackerfriend/ui/calendar/CalendarScreen.kt`

- Add import for `ProfileAvatar`: `import com.example.choreboo_habittrackerfriend.ui.components.ProfileAvatar`
- Collect profile state from ViewModel:
  ```kotlin
  val profilePhotoUri by viewModel.profilePhotoUri.collectAsState()
  ```
- Replace 🐣 Box (lines 83-91) with:
  ```kotlin
  ProfileAvatar(
      profilePhotoUri = profilePhotoUri,
      googlePhotoUrl = viewModel.googlePhotoUrl,
      size = 40.dp,
  )
  ```

### 5. AddEditHabitViewModel.kt
**File:** `app/src/main/java/com/example/choreboo_habittrackerfriend/ui/habits/AddEditHabitViewModel.kt`

- Add imports:
  ```kotlin
  import com.example.choreboo_habittrackerfriend.data.datastore.UserPreferences
  import com.example.choreboo_habittrackerfriend.data.repository.AuthRepository
  import kotlinx.coroutines.flow.SharingStarted
  import kotlinx.coroutines.flow.StateFlow
  import kotlinx.coroutines.flow.stateIn
  ```
- Add `UserPreferences` and `AuthRepository` to constructor:
  ```kotlin
  class AddEditHabitViewModel @Inject constructor(
      savedStateHandle: SavedStateHandle,
      private val habitRepository: HabitRepository,
      private val userPreferences: UserPreferences,
      private val authRepository: AuthRepository,
      @ApplicationContext private val context: Context,
  ) : ViewModel() {
  ```
- Add profile fields after `_events`:
  ```kotlin
  val profilePhotoUri: StateFlow<String?> = userPreferences.profilePhotoUri
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

  val googlePhotoUrl: String?
      get() = authRepository.currentFirebaseUser?.photoUrl?.toString()
  ```

### 6. AddEditHabitScreen.kt
**File:** `app/src/main/java/com/example/choreboo_habittrackerfriend/ui/habits/AddEditHabitScreen.kt`

- Add import: `import com.example.choreboo_habittrackerfriend.ui.components.ProfileAvatar`
- Collect profile state from ViewModel in `AddEditHabitScreen` composable:
  ```kotlin
  val profilePhotoUri by viewModel.profilePhotoUri.collectAsState()
  ```
- Pass profile data to `ChorebooTopBar`:
  ```kotlin
  ChorebooTopBar(
      onNavigateBack = onNavigateBack,
      profilePhotoUri = profilePhotoUri,
      googlePhotoUrl = viewModel.googlePhotoUrl,
  )
  ```
- Update `ChorebooTopBar` signature:
  ```kotlin
  private fun ChorebooTopBar(
      onNavigateBack: () -> Unit,
      profilePhotoUri: String?,
      googlePhotoUrl: String?,
  )
  ```
- Replace 🐣 Box (lines 474-482) with:
  ```kotlin
  ProfileAvatar(
      profilePhotoUri = profilePhotoUri,
      googlePhotoUrl = googlePhotoUrl,
      size = 40.dp,
  )
  ```

### 7. SettingsScreen.kt
**File:** `app/src/main/java/com/example/choreboo_habittrackerfriend/ui/settings/SettingsScreen.kt`

- Replace 🐣 Box (lines 209-217) with:
  ```kotlin
  ProfileAvatar(
      profilePhotoUri = profilePhotoUri,
      googlePhotoUrl = viewModel.googlePhotoUrl,
      size = 40.dp,
  )
  ```
  (SettingsScreen already collects `profilePhotoUri` at line 87 and has access to `viewModel.googlePhotoUrl`)

## Build Verification
Run: `powershell.exe -File build.ps1 assembleDebug`

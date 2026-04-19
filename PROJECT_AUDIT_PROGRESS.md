# Project Audit Progress

Reference plan: `PROJECT_AUDIT_PLAN.md`

## Overall Status

- [x] Step 1. Startup, session restore, and onboarding gate
- [x] Step 2. User authentication
- [x] Step 3. Navigation and back stack
- [x] Step 4. Habit or chore lifecycle
- [x] Step 5. Habit completion, streaks, and XP
- [ ] Step 6. Household collaboration
- [ ] Step 7. Notifications, alarms, and background execution
- [ ] Step 8. Sync, offline behavior, and multi-device consistency
- [ ] Step 9. Pet state and progression
- [ ] Step 10. Stats, calendar, and reporting accuracy
- [ ] Step 11. Settings, sign-out, and dev reset
- [ ] Step 12. Billing, premium, and background economy
- [ ] Step 13. Data model, Room schema, and backend contract
- [ ] Step 14. Security and privacy
- [ ] Step 15. UI, UX clarity, and accessibility
- [ ] Step 16. Test coverage and audit closure

## Step 1 Findings

### Finding 1. Interrupted onboarding can leave the user in a partial state with a pet already created

- Severity: High
- Type: Logic gap / startup and onboarding state inconsistency

#### Why it matters

The onboarding flow creates the Choreboo before onboarding is marked complete. If the app is closed or killed on the paywall step, the user can return later with a local pet already created while `onboardingComplete` is still `false`.

Startup currently treats `onboardingComplete == false` as a fast-path directly back into onboarding and skips Room warmup and startup sync. That means the app does not reconcile the already-created pet before routing.

This creates a partial-onboarding state where the user is effectively treated as both:

- not onboarded for navigation purposes
- already onboarded enough to have persistent pet state

#### Reproduction

1. Start onboarding.
2. Hatch a pet successfully.
3. Reach the paywall step.
4. Close the app before tapping subscribe or skip.
5. Reopen the app.

Observed from code:

- `OnboardingViewModel.hatchChoreboo()` creates or activates the pet, then moves to `STEP_PAYWALL`.
- `userPreferences.setOnboardingComplete(true)` is only called later in `completeOnboarding()`.
- `MainViewModel.runStartupSequence()` takes the fast path when the user is authenticated but `onboardingComplete != true`, skipping Room warmup and sync.
- `MainActivity` then routes to `Screen.Onboarding`.

#### Risk

- The user can restart into onboarding with pet state already present.
- Repeating onboarding may reactivate an existing pet or create another pet of a different type before onboarding is ever completed.
- Product behavior is ambiguous: is pet creation supposed to be provisional until paywall completion, or should hatching itself count as onboarding completion?

#### File references

- `app/src/main/java/com/choreboo/app/ui/onboarding/OnboardingViewModel.kt`
- `app/src/main/java/com/choreboo/app/MainViewModel.kt`
- `app/src/main/java/com/choreboo/app/MainActivity.kt`
- `app/src/main/java/com/choreboo/app/data/repository/ChorebooRepository.kt`

#### Recommended fix direction

Choose one explicit product behavior and wire startup to match it:

1. Treat pet creation as onboarding completion immediately, then handle paywall separately.
2. Keep onboarding incomplete until paywall resolution, but persist and restore a resumable onboarding state so the user returns to the paywall instead of restarting onboarding.
3. Add startup reconciliation that checks for an existing local or synced pet before routing authenticated users back into onboarding.

### Missing test coverage for Step 1

- No test covers app restart after successful hatch but before onboarding completion.
- No test covers disagreement between `onboardingComplete == false` and existing local pet state.
- No test covers whether startup should reconcile existing pet state before routing authenticated users to onboarding.

Relevant current tests reviewed:

- `app/src/test/java/com/choreboo/app/MainViewModelTest.kt`
- `app/src/test/java/com/choreboo/app/ui/onboarding/OnboardingViewModelTest.kt`
- `app/src/test/java/com/choreboo/app/ui/auth/AuthViewModelTest.kt`

## Step 2 Findings

### Finding 2. Returning users can be misclassified as new users when post-auth sync fails

- Severity: High
- Type: Logic gap / auth-to-onboarding misrouting

#### Why it matters

The app decides whether auth succeeded for a returning user by checking whether a Choreboo exists locally after `syncAll(force = true)`. If sync fails on a fresh install or freshly cleared device, Room may still be empty even though the user already has a cloud account and cloud pet.

That means a real returning user can be routed into onboarding as if they were new.

#### Reproduction

1. Sign in with an account that already has a cloud Choreboo.
2. Ensure post-auth sync fails or times out before local Room is populated.
3. `AuthViewModel.handleResult()` checks `chorebooRepository.hasAnyChoreboo()`.
4. Because Room is still empty, `hasChoreboo` becomes `false`.
5. The UI receives `AuthSuccess(..., onboardingComplete = false)` and navigates to onboarding.

#### Risk

- Returning users can be incorrectly sent through onboarding.
- On a fresh install, this can look like account data was lost.
- Combined with the Step 1 onboarding-state issue, the user can end up in an inconsistent mixed state.

#### File references

- `app/src/main/java/com/choreboo/app/ui/auth/AuthViewModel.kt`
- `app/src/main/java/com/choreboo/app/navigation/ChorebooNavGraph.kt`
- `app/src/test/java/com/choreboo/app/ui/auth/AuthViewModelTest.kt`

#### Recommended fix direction

Do not derive returning-user status purely from local Room after a failed sync. Prefer one of:

1. Query cloud user state directly when sync fails.
2. Treat sync failure as an unresolved auth state and block navigation until the app can determine whether the user is new or returning.
3. Persist a more reliable onboarding or account bootstrap signal than "local pet exists right now".

### Finding 3. Cloud user bootstrap is best-effort, which can leave newly authenticated users without a cloud User row

- Severity: High
- Type: Logic gap / account bootstrap failure

#### Why it matters

The backend contract says the `User` row is created on first sign-in. But after auth success, `AuthViewModel` treats `userRepository.syncCurrentUserToCloud()` as non-blocking and silently ignores its failure.

This means a newly authenticated user can proceed into the app even if their cloud `User` row was never created.

That is especially risky because other cloud records reference `User` by foreign key:

- `Choreboo.owner`
- `Habit.owner`
- `Habit.assignedTo`
- `HabitLog.completedBy`
- `PurchasedBackground.owner`

If the `User` bootstrap upsert fails, later cloud writes can fail even though auth succeeded and the UI proceeds normally.

#### Reproduction

1. Register or sign in with a new account.
2. Allow Firebase Auth success.
3. Make `userRepository.syncCurrentUserToCloud()` fail or time out.
4. The exception is swallowed in `AuthViewModel.handleResult()`.
5. The app still emits `AuthSuccess` and proceeds.

#### Risk

- Newly authenticated users can enter onboarding and create local state that never gets a valid cloud owner row.
- Later write-through calls may fail for reasons the user never saw during login.
- There is no guaranteed retry path for the initial user bootstrap; current retries happen only if later profile-changing flows call `syncCurrentUserToCloud()` again.

#### File references

- `app/src/main/java/com/choreboo/app/ui/auth/AuthViewModel.kt`
- `app/src/main/java/com/choreboo/app/data/repository/UserRepository.kt`
- `dataconnect/schema/schema.gql`
- `dataconnect/choreboo-connector/mutations.gql`

#### Recommended fix direction

Treat initial cloud user bootstrap as a required part of successful auth completion, or introduce a dedicated retryable bootstrap state before the user can enter flows that depend on cloud FKs.

### Finding 4. Auth error mapping is brittle because it depends on exception message text instead of Firebase exception types

- Severity: Medium
- Type: Error handling gap

#### Why it matters

`AuthRepository.toAuthErrorType()` maps failures by checking substrings inside exception messages. That is fragile because message text can change, be null, vary across SDK versions, or differ from the structured Firebase exception types.

As a result, recognizable auth failures can degrade into `Unknown` even though Firebase exposes more precise exception classes.

#### Evidence

- `AuthRepository.toAuthErrorType()` uses string matching only.
- `AuthRepositoryTest.kt` imports Firebase exception classes but does not test typed exception mapping.

#### Risk

- The UI can show generic auth errors for well-known cases.
- Error behavior may change silently after Firebase SDK updates.

#### File references

- `app/src/main/java/com/choreboo/app/data/repository/AuthRepository.kt`
- `app/src/test/java/com/choreboo/app/data/repository/AuthRepositoryTest.kt`

#### Recommended fix direction

Map known Firebase auth exception types first, then use message parsing only as a fallback for untyped cases.

### Missing test coverage for Step 2

- No test covers returning-user login on a fresh install when post-auth sync fails and local Room is empty.
- No test covers failure of `syncCurrentUserToCloud()` during initial auth completion.
- No test verifies auth error mapping using typed Firebase exceptions instead of message strings.
- No test covers Google credential parsing failure beyond `GetCredentialException` handling.

Relevant current tests reviewed:

- `app/src/test/java/com/choreboo/app/ui/auth/AuthViewModelTest.kt`
- `app/src/test/java/com/choreboo/app/data/repository/AuthRepositoryTest.kt`
- `app/src/test/java/com/choreboo/app/data/repository/UserRepositorySyncTest.kt`

## Step 3 Findings

### Finding 5. Rapid taps can push duplicate `add_edit_habit` destinations onto the back stack

- Severity: Medium
- Type: Navigation / back-stack duplication

#### Why it matters

Navigation into `add_edit_habit` does not use `launchSingleTop`, destination guards, or tap debouncing. Both the floating add button and habit-card taps call `navController.navigate(...)` directly.

If the user taps quickly twice, the same destination can be pushed multiple times. That creates confusing back behavior, repeated save screens, and wrong assumptions about `previousBackStackEntry` when returning results.

#### Reproduction

1. Open `PetScreen`.
2. Rapidly tap the add-habit FAB twice, or rapidly tap a habit card twice.
3. Two `add_edit_habit` destinations can be added before the first transition settles.

Observed from code:

- FAB: `PetScreen` calls `onAddHabit` directly.
- Habit rows: `PetScreen` calls `onEditHabit(habit.id)` directly.
- `ChorebooNavGraph` navigates to `Screen.AddEditHabit.createRoute(...)` with no `launchSingleTop` or destination check.

#### Risk

- Duplicate add or edit screens on the stack.
- Back can appear to “do nothing” because it only returns to the duplicate instance.
- `previousBackStackEntry?.savedStateHandle?.set("habitCreated", true)` becomes less reliable if the user stacked duplicate edit screens.

#### File references

- `app/src/main/java/com/choreboo/app/ui/pet/PetScreen.kt`
- `app/src/main/java/com/choreboo/app/navigation/ChorebooNavGraph.kt`

#### Recommended fix direction

Add one of:

1. `launchSingleTop = true` when navigating to `add_edit_habit`.
2. A guard that skips navigation if the current route is already the target edit route.
3. UI-side debouncing for the FAB and habit-card navigation taps.

### Finding 6. Household -> Settings is treated like tab navigation, so in-app back loses the original context

- Severity: Medium
- Type: Ambiguous navigation behavior

#### Why it matters

From `HouseholdScreen`, the empty-state CTA navigates to `Settings` using tab-style navigation options:

- `popUpTo(Screen.Pet.route) { saveState = true }`
- `launchSingleTop = true`
- `restoreState = true`

That means the app is not really opening Settings as a pushed child screen. It is switching tabs. But `SettingsScreen` still accepts an `onNavigateBack` callback, which implies a pushed-screen back model.

In practice, `SettingsScreen` uses the shared top app bar with no explicit back button, so the only back behavior is the system back stack, which may not match the user's expectation of “return to Household after I fix this.”

#### Reproduction

1. Open `HouseholdScreen` with no household.
2. Tap the CTA that opens Settings to create or join a household.
3. Use system back after viewing Settings.

Observed from code:

- Household empty state routes to `onNavigateToSettings`.
- Nav graph navigates to `Settings` using tab-style state restoration, not a pushed child route.
- `SettingsScreen` has an `onNavigateBack` parameter but does not visibly use it in its top app bar.

#### Risk

- The path feels like a drill-in flow, but behavior is actually tab switching.
- User expectation about returning directly to the previous Household context is ambiguous.
- The code suggests two conflicting mental models for Settings: tab destination vs pushed destination.

#### File references

- `app/src/main/java/com/choreboo/app/ui/household/HouseholdScreen.kt`
- `app/src/main/java/com/choreboo/app/navigation/ChorebooNavGraph.kt`
- `app/src/main/java/com/choreboo/app/ui/settings/SettingsScreen.kt`
- `app/src/main/java/com/choreboo/app/ui/components/BottomNavBar.kt`

#### Recommended fix direction

Pick one model explicitly:

1. Settings is a pure tab destination: remove unused back-navigation affordances and make the Household CTA clearly a tab switch.
2. Settings is sometimes a pushed detail screen: add a dedicated child route or explicit back UI for that flow.

### Missing test coverage for Step 3

- No test covers duplicate navigation prevention for `add_edit_habit`.
- No test covers sign-out or reset navigation stack clearing at the nav-graph level.
- No test covers Household -> Settings -> back behavior.
- No test covers `habitCreated` saved-state behavior when duplicate destinations or unusual back-stack shapes exist.

Relevant current tests reviewed:

- No direct nav-graph or navigation-behavior tests found.
- Existing related tests are ViewModel-only, such as:
  - `app/src/test/java/com/choreboo/app/ui/settings/SettingsViewModelSignOutTest.kt`
  - `app/src/test/java/com/choreboo/app/ui/settings/SettingsViewModelHouseholdTest.kt`

## Step 4 Findings

### Finding 7. Assignee edits can change owner-only XP fields locally, then diverge from cloud state

- Severity: High
- Type: Logic gap / local-cloud permission mismatch

#### Why it matters

The backend intentionally forbids assignees from changing `difficulty` and `baseXp`. `UpdateAssignedHabit` only allows safe fields.

But the local save path for non-owners still writes assignee-edited `difficulty` and `baseXp` into Room before the cloud write-through runs. The subsequent cloud update will not persist those XP-affecting fields, so local state can temporarily diverge from cloud state until the next sync overwrites it.

That means assignees can locally see and use reward values they are not actually allowed to change.

#### Reproduction

1. Open a household habit assigned to the current user but owned by someone else.
2. Change the difficulty slider.
3. Save.

Observed from code:

- `AddEditHabitScreen` still shows the difficulty controls for assignees.
- `AddEditHabitViewModel.saveHabit()` preserves owner-only metadata like `householdId` and `assignedToUid`, but it still passes `state.difficulty` and `state.baseXp` into the `Habit` object for all users.
- `HabitRepository.upsertHabit()` writes that local object to Room immediately.
- For non-owners, write-through uses `updateAssignedHabit`, which does not send `difficulty` or `baseXp`.

#### Risk

- Assignees can locally alter XP values they do not control.
- The app can show one reward locally while the server retains another.
- Next sync can silently revert the visible values, which is confusing and potentially exploitable for local-only behavior until reconciliation.

#### File references

- `app/src/main/java/com/choreboo/app/ui/habits/AddEditHabitScreen.kt`
- `app/src/main/java/com/choreboo/app/ui/habits/AddEditHabitViewModel.kt`
- `app/src/main/java/com/choreboo/app/data/repository/HabitRepository.kt`
- `dataconnect/choreboo-connector/mutations.gql`

#### Recommended fix direction

For assignees, preserve existing `difficulty` and `baseXp` in `saveHabit()` and consider disabling or hiding the difficulty controls in the UI to match the backend permission model.

### Finding 8. Save action is not guarded by `isSaving`, so rapid taps can create duplicate habits

- Severity: High
- Type: Lifecycle / duplicate-write bug

#### Why it matters

`AddEditHabitViewModel` exposes `_isSaving`, but `AddEditHabitScreen` does not collect or use it to disable the save CTA. The save CTA is a plain `.clickable { viewModel.saveHabit() }` wrapper, so rapid taps can trigger multiple concurrent saves.

For new habits, each save path can insert a new local row and start a separate cloud create write-through, resulting in duplicate habits.

#### Reproduction

1. Open the add habit screen.
2. Enter valid data.
3. Tap the save CTA multiple times quickly.

Observed from code:

- `AddEditHabitViewModel.saveHabit()` sets `_isSaving = true`, but does not early-return if a save is already in progress.
- `AddEditHabitScreen` does not read `isSaving` and does not disable the clickable save surface.
- New-habit saves call `habitRepository.upsertHabit()` immediately, which writes to Room before any navigation event closes the screen.

#### Risk

- Duplicate local habits.
- Multiple cloud create mutations for the same intended habit.
- Confusing navigation timing if multiple `Saved` events are emitted close together.

#### File references

- `app/src/main/java/com/choreboo/app/ui/habits/AddEditHabitScreen.kt`
- `app/src/main/java/com/choreboo/app/ui/habits/AddEditHabitViewModel.kt`
- `app/src/main/java/com/choreboo/app/data/repository/HabitRepository.kt`

#### Recommended fix direction

Add both:

1. An early return in `saveHabit()` when `_isSaving.value` is already true.
2. UI disabling or a loading state on the save CTA tied to `isSaving`.

### Missing test coverage for Step 4

- No test covers assignee edits preserving owner-only `difficulty` and `baseXp` values.
- No test covers duplicate save prevention while `isSaving` is true.
- No test covers save-button UI disablement during active save.
- Local archive/unarchive reminder tests exist, but there is still no coverage for local-cloud permission mismatches in the edit flow.

Relevant current tests reviewed:

- `app/src/test/java/com/choreboo/app/ui/habits/AddEditHabitViewModelTest.kt`
- `app/src/test/java/com/choreboo/app/data/repository/HabitRepositoryTest.kt`
- `app/src/test/java/com/choreboo/app/data/repository/HabitRepositorySyncTest.kt`

## Step 5 Findings

### Finding 9. Household one-user-per-day enforcement is not actually guaranteed in the cloud model

- Severity: High
- Type: Logic gap / multi-device completion rule mismatch

#### Why it matters

The code and project docs say household habits use a one-user-per-day rule. `HabitRepository.completeHabit()` blocks locally if `GetLogsForHabitAndDate` returns any log for that habit/date pair.

But the cloud schema only enforces uniqueness on `HabitLog(habit, date, completedBy)`, which allows multiple different household members to create separate logs for the same habit on the same day.

That means the client-side pre-check is advisory, not authoritative. Under race conditions or multi-device concurrency, the backend can still accept multiple household completions for the same day by different users.

#### Evidence

- `completeHabit()` checks `GetLogsForHabitAndDate` and returns `alreadyComplete` if any log exists for that habit/date.
- Project docs explicitly say this is meant to enforce a one-user-per-day household rule.
- Cloud schema uniqueness is `@unique(fields: ["habit", "date", "completedBy"])`, which prevents duplicate completion by the same user, not by the household as a whole.

#### Risk

- Two household members can both complete the same household habit on the same date if they race or if one device is offline long enough to bypass the pre-check timing window.
- The app claims stronger correctness than the backend actually guarantees.
- Household status can diverge across devices until sync merges the duplicate logs back in.

#### File references

- `app/src/main/java/com/choreboo/app/data/repository/HabitRepository.kt`
- `dataconnect/choreboo-connector/queries.gql`
- `dataconnect/schema/schema.gql`
- `AGENTS.md`

#### Recommended fix direction

Move the one-user-per-day rule into an atomic server-side constraint or transaction. If the intended rule is truly one completion per habit per date for the household, the backend uniqueness and/or mutation check must enforce that directly.

### Finding 10. Habit completion is not atomic from the user perspective: the habit can complete successfully but still emit `CompletionError`

- Severity: High
- Type: Partial-success / misleading error handling

#### Why it matters

`PetViewModel.completeHabit()` wraps the whole chain in one `try/catch`:

1. `habitRepository.completeHabit()` writes the log and updates points.
2. `chorebooRepository.addXp()` updates the pet.
3. `autoFeedIfNeeded()` may also run.

If step 1 succeeds but step 2 throws, the code emits `PetEvent.CompletionError`. At that point the habit may already be completed and points may already be awarded, but the user is told the completion failed.

This is a classic partial-success bug.

#### Reproduction

1. Make `habitRepository.completeHabit()` succeed.
2. Make `chorebooRepository.addXp()` throw after the habit log and point updates are already written.
3. `PetViewModel.completeHabit()` catches the exception and emits `CompletionError`.

#### Risk

- User sees an error even though the habit may already be completed.
- Retrying can produce confusing "already completed" behavior right after an error toast.
- The pet and the habit log can be temporarily out of sync until the user notices or a later flow repairs it.

#### File references

- `app/src/main/java/com/choreboo/app/ui/pet/PetViewModel.kt`
- `app/src/main/java/com/choreboo/app/data/repository/HabitRepository.kt`
- `app/src/main/java/com/choreboo/app/data/repository/ChorebooRepository.kt`

#### Recommended fix direction

Split error handling so the app can distinguish:

1. completion failed before the log was written
2. habit completion succeeded but pet XP application failed afterward

At minimum, avoid reporting a full failure after the repository completion already committed.

### Missing test coverage for Step 5

- No test covers the mismatch between the documented one-user-per-day household rule and the weaker cloud uniqueness constraint.
- No test covers partial-success completion where `habitRepository.completeHabit()` succeeds but `chorebooRepository.addXp()` throws.
- No test covers notification completion when the underlying habit completes but later XP application fails.
- `PetViewModel` streak flow tests do not cover date-rollover interaction with `getStreaksForToday()`.

Relevant current tests reviewed:

- `app/src/test/java/com/choreboo/app/data/repository/HabitRepositoryTest.kt`
- `app/src/test/java/com/choreboo/app/ui/pet/PetViewModelTest.kt`
- `app/src/test/java/com/choreboo/app/domain/model/HabitTest.kt`

## Notes

- Step 1 completed as a code audit pass only. No production code changes made yet.
- Step 2 completed as a code audit pass only. No production code changes made yet.
- Step 3 completed as a code audit pass only. No production code changes made yet.
- Step 4 completed as a code audit pass only. No production code changes made yet.
- Step 5 completed as a code audit pass only. No production code changes made yet.
- Next target: Step 6, household collaboration.

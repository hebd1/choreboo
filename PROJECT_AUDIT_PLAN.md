# Project Audit Plan

## Goal

Audit the project subsystem by subsystem to identify:

- Bugs
- Gaps in logic
- Ambiguous behavior
- Missing test coverage

This is not a generic code review. The audit should focus on real behavior, edge cases, failure modes, and cross-device consistency.

## Audit Rules

Apply the same lenses to every step:

- Happy path behavior
- Invalid input and guardrails
- Offline, timeout, and retry behavior
- App background, resume, and process death behavior
- Multi-device and stale-cache behavior
- Date rollover and timezone-sensitive behavior

For every step, record:

- Confirmed behavior
- Bugs found
- Logic gaps
- Ambiguous product behavior
- Missing tests
- Severity
- Reproduction steps
- Key files reviewed

## Recommended Order

1. Startup, session restore, and onboarding gate
2. User authentication
3. Navigation and back stack
4. Habit or chore lifecycle
5. Habit completion, streaks, and XP
6. Household collaboration
7. Notifications, alarms, and background execution
8. Sync, offline behavior, and multi-device consistency
9. Pet state and progression
10. Stats, calendar, and reporting accuracy
11. Settings, sign-out, and dev reset
12. Billing, premium, and background economy
13. Data model, Room schema, and backend contract
14. Security and privacy
15. UI, UX clarity, and accessibility
16. Test coverage and audit closure

## Step 1. Startup, Session Restore, and Onboarding Gate

### Objective

Verify that app startup always routes the user into the correct state and that splash gating does not hide broken or inconsistent state.

### Audit Focus

- Splash screen gating
- Startup sequencing
- Auth restoration
- Onboarding flag behavior
- Local state vs synced cloud state
- Cold start vs warm resume differences

### Questions To Answer

- Can the app route to the wrong start destination?
- What happens if local onboarding state disagrees with synced pet state?
- Does failed startup sync leave the app in a misleading state?
- Are background startup tasks safe if they complete after the first screen renders?

### Primary Files

- `app/src/main/java/com/choreboo/app/MainActivity.kt`
- `app/src/main/java/com/choreboo/app/MainViewModel.kt`
- `app/src/main/java/com/choreboo/app/ui/onboarding/OnboardingViewModel.kt`
- `app/src/main/java/com/choreboo/app/ui/auth/AuthViewModel.kt`

## Step 2. User Authentication

### Objective

Audit login, registration, Google sign-in, password reset, auth error handling, and the immediate post-auth sync and setup flow.

### Audit Focus

- Email and password validation
- Google sign-in path
- Password reset behavior
- Auth error mapping
- Post-login sync behavior
- Profile photo persistence
- Returning user vs new user onboarding detection

### Questions To Answer

- Can auth succeed while app setup remains partially broken?
- Are all auth failures visible and understandable?
- Are duplicate-account or linked-account cases clear?
- Can onboarding status be derived incorrectly after login?

### Primary Files

- `app/src/main/java/com/choreboo/app/ui/auth/AuthScreen.kt`
- `app/src/main/java/com/choreboo/app/ui/auth/AuthViewModel.kt`
- `app/src/main/java/com/choreboo/app/data/repository/AuthRepository.kt`

## Step 3. Navigation and Back Stack

### Objective

Verify that navigation, tab switching, back behavior, sign-out resets, and saved-state flows are predictable and correct.

### Audit Focus

- Route definitions
- Start destination selection
- Bottom-nav behavior
- Back stack cleanup after auth and onboarding
- Add or edit habit round-trips
- SavedStateHandle usage

### Questions To Answer

- Does back always do the expected thing?
- Can users return to protected screens after sign-out?
- Are transient flags like `habitCreated` cleared reliably?
- Can recomposition or nav restore create stale navigation state?

### Primary Files

- `app/src/main/java/com/choreboo/app/navigation/ChorebooNavGraph.kt`
- `app/src/main/java/com/choreboo/app/MainActivity.kt`

## Step 4. Habit or Chore Lifecycle

### Objective

Audit creation, editing, assignment, archiving, unarchiving, and deletion of habits across personal and household use cases.

### Audit Focus

- Form validation
- Owner vs assignee editing behavior
- Household vs personal habit transitions
- Archive and unarchive behavior
- Delete sequencing and cleanup
- Reminder state changes when habits change

### Questions To Answer

- Are permissions enforced correctly for owners and assignees?
- Do reminder settings stay in sync after edits and archive state changes?
- Can delete or archive leave ghost data locally or in cloud?
- Are habit ownership fields ever missing or stale?

### Primary Files

- `app/src/main/java/com/choreboo/app/ui/habits/AddEditHabitScreen.kt`
- `app/src/main/java/com/choreboo/app/ui/habits/AddEditHabitViewModel.kt`
- `app/src/main/java/com/choreboo/app/data/repository/HabitRepository.kt`
- `app/src/main/java/com/choreboo/app/data/local/dao/HabitDao.kt`

## Step 5. Habit Completion, Streaks, and XP

### Objective

Verify completion rules, duplicate prevention, streak calculations, XP calculations, and schedule enforcement.

### Audit Focus

- Single-completion enforcement
- Household completion pre-checks
- Local DB duplicate prevention
- Schedule-aware completion rules
- XP formula correctness
- Streak continuity and resets
- Date handling for "today"

### Questions To Answer

- Can a habit be completed twice through racing paths?
- Are custom schedule streak rules correct?
- Do household habits resolve duplicates correctly across devices?
- Is "today" consistent across screens, repos, and receivers?

### Primary Files

- `app/src/main/java/com/choreboo/app/data/repository/HabitRepository.kt`
- `app/src/main/java/com/choreboo/app/ui/pet/PetViewModel.kt`
- `app/src/main/java/com/choreboo/app/domain/model/Habit.kt`

## Step 6. Household Collaboration

### Objective

Audit create, join, leave, member visibility, shared habit visibility, and household completion state.

### Audit Focus

- Invite code generation and collisions
- Household membership cache
- Member identity vs pet cache behavior
- Household habit status refresh behavior
- Leave flow cleanup
- Creator-specific edge cases

### Questions To Answer

- What happens when creators leave?
- Can stale household data remain after leave or sign-out?
- Are assigned habits handled correctly when membership changes?
- Are household member and pet caches reconciled correctly?

### Primary Files

- `app/src/main/java/com/choreboo/app/data/repository/HouseholdRepository.kt`
- `app/src/main/java/com/choreboo/app/ui/household/HouseholdViewModel.kt`
- `app/src/main/java/com/choreboo/app/ui/household/HouseholdScreen.kt`

## Step 7. Notifications, Alarms, and Background Execution

### Objective

Audit reminder scheduling, reminder delivery, notification actions, reboot and reinstall recovery, and permission-related behavior.

### Audit Focus

- Alarm scheduling logic
- Reminder receiver behavior
- Mark-complete notification action
- Reschedule-on-boot and app-update behavior
- Notification permission denied behavior
- Inexact timing and stale intent behavior

### Questions To Answer

- Can reminders stop firing silently?
- Can stale notifications cause invalid completions?
- Is the reminder chain preserved after archive, unarchive, reboot, and completion?
- What exactly happens when notification permission is denied?

### Primary Files

- `app/src/main/java/com/choreboo/app/worker/HabitReminderScheduler.kt`
- `app/src/main/java/com/choreboo/app/worker/HabitReminderReceiver.kt`
- `app/src/main/java/com/choreboo/app/worker/HabitCompleteReceiver.kt`
- `app/src/main/java/com/choreboo/app/worker/ReminderRescheduleWorker.kt`
- `app/src/main/java/com/choreboo/app/worker/BootReceiver.kt`

## Step 8. Sync, Offline Behavior, and Multi-Device Consistency

### Objective

Audit the entire sync model, including write-through, cloud-to-local reconciliation, retries, throttling, and conflict behavior.

### Audit Focus

- Sync triggers
- Cooldown behavior
- Forced sync behavior
- Pending sync flags
- Retry and backoff logic
- Deletion reconciliation
- Cloud-wins assumptions
- Best-effort sync branches

### Questions To Answer

- What is the source of truth for each data type?
- Can silent write-through failures hide real data loss?
- Are retries sufficient and consistent?
- Can concurrent sync and destructive flows conflict?
- Where is behavior intentional but ambiguous to the user?

### Primary Files

- `app/src/main/java/com/choreboo/app/data/repository/SyncManager.kt`
- `app/src/main/java/com/choreboo/app/data/repository/HabitRepository.kt`
- `app/src/main/java/com/choreboo/app/data/repository/ChorebooRepository.kt`
- `app/src/main/java/com/choreboo/app/data/repository/UserRepository.kt`
- `app/src/main/java/com/choreboo/app/di/AppLifecycleObserver.kt`

## Step 9. Pet State and Progression

### Objective

Audit pet stats, decay, feeding, sleeping, XP gain, level-up, evolution, active pet behavior, and background selection.

### Audit Focus

- Stat clamping
- Decay timing
- Feed and sleep behavior
- XP and level changes
- Evolution thresholds
- Active pet selection
- Background persistence and rendering

### Questions To Answer

- Can pet state become inconsistent across devices?
- Are level-up and evolution side effects reliable?
- Does auto-feed do exactly what the UI implies?
- Can background selection and purchase state diverge?

### Primary Files

- `app/src/main/java/com/choreboo/app/data/repository/ChorebooRepository.kt`
- `app/src/main/java/com/choreboo/app/ui/pet/PetScreen.kt`
- `app/src/main/java/com/choreboo/app/data/repository/BackgroundRepository.kt`

## Step 10. Stats, Calendar, and Reporting Accuracy

### Objective

Verify that all reported totals, heatmaps, logs, and streak displays are consistent with source data and rules.

### Audit Focus

- Calendar heatmap accuracy
- Stats aggregation
- Date filtering
- Log detail correctness
- Streak badge correctness
- Inclusion and exclusion rules

### Questions To Answer

- Do stats and calendar agree with each other?
- Are totals derived from the same logic as completion and XP?
- Are household-related logs included intentionally or accidentally?
- Are loading and refresh states accurate?

### Primary Files

- `app/src/main/java/com/choreboo/app/ui/calendar/CalendarViewModel.kt`
- `app/src/main/java/com/choreboo/app/ui/stats/StatsViewModel.kt`
- Related DAO query files

## Step 11. Settings, Sign-Out, and Dev Reset

### Objective

Audit all account cleanup and preference persistence flows, including sign-out, reset, and pending-write cancellation.

### Audit Focus

- Sign-out cleanup order
- Local cache cleanup
- Pending write cancellation
- Preference persistence across users
- Dev reset sequencing
- Error handling during reset

### Questions To Answer

- Does sign-out fully remove user-specific local state?
- Can pending writes fire after reset or sign-out?
- Which preferences should persist across account switches?
- Are destructive operations clearly communicated to the user?

### Primary Files

- `app/src/main/java/com/choreboo/app/ui/settings/SettingsViewModel.kt`
- `app/src/main/java/com/choreboo/app/ui/settings/SettingsScreen.kt`
- `app/src/main/java/com/choreboo/app/data/repository/ResetRepository.kt`
- `app/src/main/java/com/choreboo/app/data/datastore/UserPreferences.kt`

## Step 12. Billing, Premium, and Background Economy

### Objective

Audit billing connection behavior, premium verification, purchase restoration, product assumptions, and premium-adjacent UX.

### Audit Focus

- Billing connection lifecycle
- Retry and reconnect behavior
- Premium verification logic
- Product details assumptions
- Restore purchases behavior
- Premium state exposure to UI

### Questions To Answer

- Can premium state be wrong when billing is disconnected?
- Are subscription offer assumptions too brittle?
- Can product detail loading fail in ways the UI does not explain?
- Is premium state clearly separated from star-point purchases?

### Primary Files

- `app/src/main/java/com/choreboo/app/data/repository/BillingRepository.kt`
- `app/src/main/java/com/choreboo/app/data/repository/BackgroundRepository.kt`

## Step 13. Data Model, Room Schema, and Backend Contract

### Objective

Audit mappings, nullability, schema expectations, DB constraints, and cloud contract assumptions.

### Audit Focus

- Entity vs domain model mapping
- Enum string parsing behavior
- Nullability and fallback behavior
- Room constraints and indexes
- Migration assumptions
- Data Connect query and mutation shape assumptions

### Questions To Answer

- Do Room and cloud schemas express the same business rules?
- Are invalid cloud values silently hidden by defensive mapping?
- Are local constraints sufficient to prevent broken state?
- Are current destructive migration assumptions acceptable?

### Primary Files

- `app/src/main/java/com/choreboo/app/data/local/**/*`
- `dataconnect/schema/schema.gql`
- `dataconnect/choreboo-connector/queries.gql`
- `dataconnect/choreboo-connector/mutations.gql`

## Step 14. Security and Privacy

### Objective

Audit data access boundaries, auth scoping, local data exposure, backup behavior, and security-sensitive workflows.

### Audit Focus

- Query and mutation auth scoping
- Household data access boundaries
- Assignee permission boundaries
- Backup and data extraction exclusions
- Notification content sensitivity
- Dev reset and admin-like flows

### Questions To Answer

- Can one user access another user's data indirectly?
- Are household reads always scoped tightly enough?
- Is local user data cleared when it should be?
- Are any sensitive actions insufficiently protected or explained?

### Primary Files

- `dataconnect/choreboo-connector/queries.gql`
- `dataconnect/choreboo-connector/mutations.gql`
- `app/src/main/AndroidManifest.xml`
- Security XML and cleanup flows

## Step 15. UI, UX Clarity, and Accessibility

### Objective

Audit places where behavior may be technically correct but unclear, misleading, inaccessible, or inconsistent.

### Audit Focus

- Loading states
- Empty states
- Error messaging
- Disabled state clarity
- Snackbar and dialog timing
- Accessibility semantics and descriptions
- Product wording that hides technical behavior

### Questions To Answer

- Which flows are confusing even if code is correct?
- Which errors need product decisions rather than engineering fixes?
- Are edge states explained clearly to the user?
- Are important actions accessible and discoverable?

### Primary Files

- Screen composables under `app/src/main/java/com/choreboo/app/ui/`
- Shared components under `app/src/main/java/com/choreboo/app/ui/components/`

## Step 16. Test Coverage and Audit Closure

### Objective

Compare the audit findings against current automated coverage and identify high-value missing regression tests.

### Audit Focus

- Coverage of high-risk flows
- Missing integration-like unit tests
- Missing receiver and background execution tests
- Missing navigation and state restoration tests
- Missing conflict and multi-device behavior tests

### Questions To Answer

- Which findings already have tests that failed to catch them?
- Which critical flows still have no direct test coverage?
- Which bugs deserve regression tests before any future release?

### Priority Test Gaps To Check

- Auth plus sync integration behavior
- Navigation and back-stack cleanup
- Notification receiver flows
- Multi-device sync conflicts
- Date rollover behavior
- Billing disconnection and restoration edge cases

### Existing Test Areas To Cross-Reference

- `app/src/test/java/com/choreboo/app/MainViewModelTest.kt`
- `app/src/test/java/com/choreboo/app/ui/auth/AuthViewModelTest.kt`
- `app/src/test/java/com/choreboo/app/data/repository/HabitRepositoryTest.kt`
- `app/src/test/java/com/choreboo/app/data/repository/HabitRepositorySyncTest.kt`
- `app/src/test/java/com/choreboo/app/data/repository/SyncManagerTest.kt`
- `app/src/test/java/com/choreboo/app/ui/household/HouseholdViewModelTest.kt`
- `app/src/test/java/com/choreboo/app/worker/HabitReminderSchedulerTest.kt`

## Final Deliverable Format

When the audit is complete, group findings into:

1. Critical bugs
2. Logic gaps
3. Ambiguous product behavior
4. Missing tests

For each finding, include:

- Severity
- Short title
- Why it matters
- Reproduction or triggering condition
- File references
- Recommended fix direction

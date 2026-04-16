# Habit Tracking Enhancements - Implementation Summary

## Overview
Successfully implemented three major features for the Choreboo Habit Tracker app:
1. **Improved day-of-week scheduling** - Remove frequency enum, always show day selection
2. **Per-habit reminders with exact timing** - AlarmManager-based notifications at user-specified times
3. **Smart XP suggestion** - Local keyword heuristic analyzing habit title/description

---

## Feature 1: Day-of-Week Scheduling

### What Changed
- **Removed** the `HabitFrequency` enum (`DAILY`, `WEEKLY`, `CUSTOM`)
- **Simplified scheduling** to just day-of-week selection (MON-SUN)
- Default: all 7 days selected (daily habit)
- User deselects days to create custom schedules

### Files Modified
- `domain/model/Habit.kt` - Removed `frequency` field, made `customDays` non-nullable with all days as default
- `domain/model/HabitFrequency.kt` - **DELETED**
- `data/local/entity/HabitEntity.kt` - Removed `frequency` column, `customDays` now defaults to all 7 days
- `data/repository/HabitRepository.kt` - Updated mapping functions, removed frequency parsing
- `ui/habits/AddEditHabitScreen.kt` - Removed frequency dropdown, always show day chips
- `ui/habits/AddEditHabitViewModel.kt` - Removed frequency update function

### Database
- Version bumped from 1 → 2 (uses destructive migration)
- Removed `frequency` column from `habits` table
- `customDays` now always has value (never null)

### UI
**AddEditHabitScreen changes:**
- ❌ Removed: Frequency (DAILY/WEEKLY/CUSTOM) dropdown
- ✅ Added: Always-visible day-of-week chip selector
- Label: "Schedule"
- All 7 days selected by default for new habits

---

## Feature 2: Per-Habit Reminders with Exact Timing

### Architecture

#### New Classes Created

**1. `HabitReminderScheduler.kt`** (Utility object)
- Uses `AlarmManager.setExactAndAllowWhileIdle()` for exact-time alarms
- Calculates next trigger time based on scheduled days and reminder time
- Falls back to inexact alarm if `SCHEDULE_EXACT_ALARM` permission denied
- Methods:
  - `scheduleReminder(context, habitId, habitTitle, reminderTime, scheduledDays)`
  - `cancelReminder(context, habitId)`

**2. `HabitReminderReceiver.kt`** (BroadcastReceiver)
- Triggered by AlarmManager when alarm fires
- Posts notification with habit title
- Notification ID: `2000 + habitId` (avoids collision with global reminder's 1001)
- Opens MainActivity when notification tapped
- Uses existing `"choreboo_reminders"` notification channel

**3. `BootReceiver.kt`** (BroadcastReceiver)
- Listens for `ACTION_BOOT_COMPLETED`
- Triggers `ReminderRescheduleWorker` on device boot
- Ensures all habit reminders are re-scheduled after restart

**4. `ReminderRescheduleWorker.kt`** (@HiltWorker)
- Hilt-injected CoroutineWorker for dependency injection
- Queries all habits with `reminderEnabled = true`
- Re-schedules alarms for each habit
- Called by BootReceiver on device boot

### Data Changes

**HabitEntity** (Room entity)
- Added: `reminderEnabled: Boolean = false`
- Added: `reminderTime: String? = null` (ISO format "HH:mm")

**Habit** (Domain model)
- Added: `reminderEnabled: Boolean = false`
- Added: `reminderTime: LocalTime? = null`

**HabitRepository**
- Updated mappings to serialize/deserialize `LocalTime` from/to String
- Calls `HabitReminderScheduler.scheduleReminder()` on save when reminder is enabled

### ViewModel Changes

**AddEditHabitViewModel**
- Added form state fields: `reminderEnabled`, `reminderTime: LocalTime`
- New functions:
  - `updateReminderEnabled(enabled: Boolean)`
  - `updateReminderTime(time: LocalTime)`
- On save: schedules or cancels reminder via `HabitReminderScheduler`

### UI Changes

**AddEditHabitScreen**
- **Reminder toggle switch** ("Remind me") - `Switch` composable
- **Time picker button** (visible only when enabled)
  - Shows current time (e.g., "09:00 AM")
  - Tap opens Material3 `TimePicker` in `AlertDialog`
  - Defaults to 9:00 AM
- **Validation**: At least one scheduled day must be selected

### Manifest & Permissions

**AndroidManifest.xml**
```xml
<!-- Permissions -->
<uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />

<!-- Receivers -->
<receiver
    android:name=".worker.HabitReminderReceiver"
    android:exported="false">
    <intent-filter>
        <action android:name="com.choreboo.app.HABIT_REMINDER" />
    </intent-filter>
</receiver>

<receiver
    android:name=".worker.BootReceiver"
    android:exported="true">
    <intent-filter>
        <action android:name="android.intent.action.BOOT_COMPLETED" />
    </intent-filter>
</receiver>
```

### Dependencies

**gradle/libs.versions.toml**
- Added: `hiltWork = "1.2.0"` (for `@HiltWorker` support)

**app/build.gradle.kts**
- Added: `implementation(libs.hilt.work)`

---

## Feature 3: XP Suggestion Based on Habit Content

### Implementation
- **Local keyword heuristic** - no API calls, fully offline
- Analyzes habit title + description in real-time

### Keyword Categories & Suggested XP

| Category | XP | Keywords |
|----------|-----|----------|
| High Effort | 35 | exercise, workout, gym, run, jog, study, code, programming, clean, organize, train, practice, swim |
| Medium Effort | 20 | read, cook, meditate, stretch, journal, walk, yoga, hobby, project |
| Low Effort | 10 | water, drink, vitamin, pill, brush, teeth, skincare, floss |
| Default | 15 | (no keywords match) |

### ViewModel Implementation

**AddEditHabitViewModel**
- Function: `calculateSuggestedXp(title: String, description: String): Int`
- Debounced with `snapshotFlow(...).debounce(300)` to avoid lag
- Updates `suggestedXp` in form state as user types
- New function: `applySuggestedXp()` to accept suggestion

### Form State
- Added: `suggestedXp: Int?` (null when no suggestion differs from current)

### UI Changes

**AddEditHabitScreen**
- **`SuggestionChip`** next to XP slider label
- Shows suggested XP when it differs from current slider value
- Tapping chip calls `applySuggestedXp()` to update slider
- Only visible when suggestion differs from current XP

---

## Files Modified Summary

### Core Data Layer
| File | Changes |
|------|---------|
| `domain/model/Habit.kt` | Removed `frequency`, added `reminderEnabled`/`reminderTime`, simplified `isScheduledForToday()` |
| `domain/model/HabitFrequency.kt` | **DELETED** |
| `data/local/entity/HabitEntity.kt` | Removed `frequency` column, added reminder fields, `customDays` non-nullable |
| `data/local/ChorebooDatabase.kt` | Bumped version 1 → 2 |
| `data/repository/HabitRepository.kt` | Updated mappings for `LocalTime`, removed frequency parsing |

### UI Layer
| File | Changes |
|------|---------|
| `ui/habits/AddEditHabitViewModel.kt` | Added reminder fields, XP suggestion logic, debounced recalculation, reminder scheduling |
| `ui/habits/AddEditHabitScreen.kt` | Removed frequency dropdown, always show days, added reminder section, added time picker, added XP chip |

### Notification System (New)
| File | Purpose |
|------|---------|
| `worker/HabitReminderScheduler.kt` | AlarmManager scheduling utility |
| `worker/HabitReminderReceiver.kt` | Alarm trigger & notification posting |
| `worker/BootReceiver.kt` | Boot completion handler |
| `worker/ReminderRescheduleWorker.kt` | Hilt-injected worker for reschedule |

### Configuration
| File | Changes |
|------|---------|
| `app/AndroidManifest.xml` | Added permissions, registered receivers |
| `gradle/libs.versions.toml` | Added `hiltWork = "1.2.0"` |
| `app/build.gradle.kts` | Added `implementation(libs.hilt.work)` |

---

## Database Migration

Since destructive migration is enabled:
- Database version bumped from 1 → 2
- Old database is completely wiped on first run
- No migration logic needed

**Note**: In a production app, a proper Migration (1, 2) would be created to preserve user data.

---

## Testing Checklist

- [ ] Create a new habit with all 7 days selected (should show daily)
- [ ] Create a habit with only Mon/Wed/Fri selected (should show on those days only)
- [ ] Toggle reminder on → verify time picker appears
- [ ] Select custom time → verify time is saved
- [ ] Type "exercise" in title → verify XP suggestion appears (should suggest ~35)
- [ ] Type "water" in title → verify XP suggestion appears (should suggest ~10)
- [ ] Tap suggestion chip → verify XP slider updates
- [ ] Save habit with reminder enabled → verify alarm scheduled
- [ ] Edit habit → verify existing reminder settings load correctly
- [ ] Disable reminder → verify alarm is canceled
- [ ] Turn off device → restart device → verify alarms still fire at scheduled times

---

## Key Design Decisions

1. **AlarmManager for exact timing** - `setExactAndAllowWhileIdle()` ensures notifications fire at precise user-chosen time (with graceful fallback to inexact)

2. **Debounced XP suggestion** - 300ms debounce prevents UI lag while user types rapidly

3. **All-days-by-default** - New habits default to all 7 days (easily customizable), simpler UX than "select frequency then select days"

4. **Separate notification ID ranges** - Per-habit reminders (2000+) avoid collision with global reminder (1001)

5. **Destructive migration** - As requested, version bump wipes old data (simpler for now, proper migration in production)

6. **Fully offline** - All XP analysis is local keyword matching, no network calls or API keys required

---

## Dependencies Added
- `androidx.hilt:hilt-work:1.2.0` - For Hilt-injected WorkManager workers

## Permissions Added
- `android.permission.SCHEDULE_EXACT_ALARM` - For exact-time alarm scheduling
- `android.permission.RECEIVE_BOOT_COMPLETED` - To reschedule alarms on boot

---

## Known Limitations

1. **Android 12+ exact alarm permission** - May not be granted in system settings by user. Gracefully falls back to inexact alarm if denied.

2. **Destructive migration** - First run after update wipes all existing user data (habits, logs, pet, etc.). Use proper migration in production.

3. **No snooze/dismiss** - Notifications have standard snooze/dismiss but no custom snooze logic. Can be added later.

4. **Single notification per alarm** - Only posts one notification per habit. For multiple completions per day, multiple alarms would need unique requestCodes.

5. **No timezone handling** - Reminder time is device timezone only. Multi-timezone support would need ZoneId storage.

---

## Future Enhancements

- Add UI for notification sound selection (already has `sound_enabled` in UserPreferences)
- Add "nudge" reminders at configurable intervals
- Add do-not-disturb hours (quiet hours setting)
- Implement proper database migration to preserve user data on updates
- Add per-habit notification channel customization
- Add statistics showing reminder effectiveness
- Implement Lottie animations for habit completion celebrations

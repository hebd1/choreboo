package com.example.choreboo_habittrackerfriend.data.repository

import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import com.example.choreboo_habittrackerfriend.data.local.dao.HabitDao
import com.example.choreboo_habittrackerfriend.data.local.dao.HabitLogDao
import com.example.choreboo_habittrackerfriend.data.local.entity.HabitEntity
import com.example.choreboo_habittrackerfriend.data.local.dao.HabitLogWithName
import com.example.choreboo_habittrackerfriend.data.local.entity.HabitLogEntity
import com.example.choreboo_habittrackerfriend.data.datastore.UserPreferences
import com.example.choreboo_habittrackerfriend.dataconnect.ChorebooConnector
import com.example.choreboo_habittrackerfriend.dataconnect.execute
import com.example.choreboo_habittrackerfriend.dataconnect.instance
import com.example.choreboo_habittrackerfriend.domain.model.Habit
import com.example.choreboo_habittrackerfriend.worker.HabitReminderScheduler
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "HabitRepository"
private const val CLOUD_TIMEOUT_MS = 5000L

data class CompletionResult(
    val xpEarned: Int,
    val newStreak: Int,
    val alreadyComplete: Boolean = false,
)

@Singleton
class HabitRepository @Inject constructor(
    private val habitDao: HabitDao,
    private val habitLogDao: HabitLogDao,
    private val userPreferences: UserPreferences,
    private val userRepository: UserRepository,
    private val firebaseAuth: FirebaseAuth,
    @ApplicationContext private val context: android.content.Context,
) {
    private val connector by lazy { ChorebooConnector.instance }
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    /** Fire-and-forget scope for silent write-through calls. */
    private var writeScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Cancel all pending write-through coroutines and create a fresh scope.
     * Called on sign-out and account reset to prevent stale writes executing after
     * the user's data has been cleared.
     */
    fun cancelPendingWrites() {
        writeScope.coroutineContext[Job]?.cancel()
        writeScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }

    fun getAllHabits(): Flow<List<Habit>> = habitDao.getAllHabits().map { entities ->
        entities.map { it.toDomain() }
    }

    /**
     * Get habits for a specific user: personal habits they own, or household habits assigned to them.
     */
    fun getHabitsForUser(uid: String): Flow<List<Habit>> = habitDao.getHabitsForUser(uid).map { entities ->
        entities.map { it.toDomain() }
    }

    fun getHabitById(id: Long): Flow<Habit?> = habitDao.getHabitById(id).map { it?.toDomain() }

    suspend fun upsertHabit(habit: Habit): Long {
        require(habit.title.isNotBlank()) { "Habit title must not be blank" }
        require(habit.title.length <= 100) { "Habit title must be 100 characters or fewer, was ${habit.title.length}" }
        require(habit.baseXp > 0) { "Habit baseXp must be positive, was ${habit.baseXp}" }

        val localId = habitDao.upsertHabit(habit.toEntity())

        // Write-through to Data Connect (fire-and-forget)
        writeScope.launch {
            try {
                if (habit.remoteId != null) {
                    // Update existing remote habit
                    val remoteUuid = UUID.fromString(habit.remoteId)
                    connector.updateHabit.execute(
                        habitId = remoteUuid,
                        title = habit.title,
                        iconName = habit.iconName,
                        customDays = habit.customDays.joinToString(","),
                        difficulty = habit.difficulty,
                        baseXp = habit.baseXp,
                        reminderEnabled = habit.reminderEnabled,
                        isHouseholdHabit = habit.isHouseholdHabit,
                    ) {
                        description = habit.description
                        reminderTime = habit.reminderTime?.toString()
                        householdId = habit.householdId?.let { UUID.fromString(it) }
                        assignedToId = habit.assignedToUid
                    }
                    Log.d(TAG, "Updated habit in cloud: $remoteUuid")
                } else {
                    // Create new remote habit
                    val result = connector.createHabit.execute(
                        title = habit.title,
                        iconName = habit.iconName,
                        customDays = habit.customDays.joinToString(","),
                        difficulty = habit.difficulty,
                        baseXp = habit.baseXp,
                        reminderEnabled = habit.reminderEnabled,
                        isHouseholdHabit = habit.isHouseholdHabit,
                    ) {
                        description = habit.description
                        reminderTime = habit.reminderTime?.toString()
                        householdId = habit.householdId?.let { UUID.fromString(it) }
                        assignedToId = habit.assignedToUid
                    }
                    // Store the remote ID back in Room
                    val remoteId = result.data.habit_insert.id.toString()
                    habitDao.upsertHabit(habit.toEntity().copy(id = localId, remoteId = remoteId))
                    Log.d(TAG, "Created habit in cloud: $remoteId")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to sync habit to cloud", e)
            }
        }

        return localId
    }

    suspend fun deleteHabit(id: Long) {
        // Get the entity first to find the remote ID
        val entity = habitDao.getHabitByIdSync(id)
        habitDao.deleteHabitById(id)

        // Write-through: delete from Data Connect (fire-and-forget)
        entity?.remoteId?.let { remoteId ->
            writeScope.launch {
                try {
                    connector.deleteHabit.execute(
                        habitId = UUID.fromString(remoteId),
                    )
                    Log.d(TAG, "Deleted habit from cloud: $remoteId")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to delete habit from cloud", e)
                }
            }
        }
    }

    suspend fun archiveHabit(id: Long) {
        val entity = habitDao.getHabitByIdSync(id)
        habitDao.archiveHabit(id)

        // Write-through: archive in Data Connect (fire-and-forget)
        entity?.remoteId?.let { remoteId ->
            writeScope.launch {
                try {
                    connector.archiveHabit.execute(
                        habitId = UUID.fromString(remoteId),
                    )
                    Log.d(TAG, "Archived habit in cloud: $remoteId")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to archive habit in cloud", e)
                }
            }
        }
    }

    fun getLogsForDate(date: String): Flow<List<HabitLogEntity>> {
        return habitLogDao.getLogsForDate(date)
    }

    fun getLogsWithNamesForDate(date: String): Flow<List<HabitLogWithName>> {
        return habitLogDao.getLogsWithHabitNameForDate(date)
    }

    suspend fun getCompletionCountToday(habitId: Long): Int {
        val today = LocalDate.now().format(dateFormatter)
        return habitLogDao.getCompletionCountForDate(habitId, today)
    }

    suspend fun completeHabit(habitId: Long): CompletionResult {
        require(habitId > 0) { "habitId must be positive, was $habitId" }

        val today = LocalDate.now().format(dateFormatter)
        val habitEntity = habitDao.getHabitByIdSync(habitId)
            ?: return CompletionResult(xpEarned = 0, newStreak = 0, alreadyComplete = true)

        // Early return if already completed locally.
        val todayCount = habitLogDao.getCompletionCountForDate(habitId, today)
        if (todayCount >= 1) {
            return CompletionResult(xpEarned = 0, newStreak = 0, alreadyComplete = true)
        }

        // For household habits: check cloud asynchronously to see if anyone else already completed it today.
        // This is a best-effort pre-check (non-blocking). The local UNIQUE(habitId, date) constraint
        // is the primary duplicate guard. If the cloud check finds a completion, we sync it locally.
        if (habitEntity.isHouseholdHabit && habitEntity.remoteId != null) {
            writeScope.launch {
                try {
                    val cloudCheck = connector.getLogsForHabitAndDate.execute(
                        habitId = UUID.fromString(habitEntity.remoteId),
                        date = today,
                    )
                    val existingLogs = cloudCheck.data.habitLogs
                    if (existingLogs.isNotEmpty()) {
                        // Someone else already completed this habit today — sync their log locally
                        // so the UI can show "Completed by [name]" without a separate sync call.
                        val firstLog = existingLogs.first()
                        val remoteLogId = firstLog.id.toString()
                        if (habitLogDao.getLogByRemoteId(remoteLogId) == null) {
                            habitLogDao.insertLog(
                                HabitLogEntity(
                                    habitId = habitId,
                                    completedAt = firstLog.completedAt.toDate().time,
                                    date = today,
                                    xpEarned = firstLog.xpEarned,
                                    streakAtCompletion = firstLog.streakAtCompletion,
                                    completedByUid = firstLog.completedBy?.id,
                                    remoteId = remoteLogId,
                                ),
                            )
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Cloud pre-check for household habit failed — continuing with local completion", e)
                    // Fall through: let local completion proceed; UNIQUE constraint handles duplicates
                }
            }
        }

        val parsedCustomDays = habitEntity.customDays
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        val streak = calculateStreak(habitId, today, parsedCustomDays)
        val baseXp = habitEntity.baseXp
        val xpEarned = (baseXp + (streak * 2)).coerceAtMost(baseXp * 3)

        val currentUid = firebaseAuth.currentUser?.uid
        val log = HabitLogEntity(
            habitId = habitId,
            date = today,
            xpEarned = xpEarned,
            streakAtCompletion = streak + 1,
            completedByUid = currentUid,
        )
        // Atomic duplicate prevention: UNIQUE(habitId, date) + IGNORE returns -1 on conflict
        val localLogId = habitLogDao.insertLog(log)
        if (localLogId == -1L) {
            return CompletionResult(xpEarned = 0, newStreak = 0, alreadyComplete = true)
        }

        // Read current totals before updating so we can pass the new values to write-through
        val prevPoints = userPreferences.totalPoints.first()
        val prevLifetimeXp = userPreferences.totalLifetimeXp.first()
        userPreferences.addPoints(xpEarned)
        userPreferences.addLifetimeXp(xpEarned)
        val newPoints = prevPoints + xpEarned
        val newLifetimeXp = prevLifetimeXp + xpEarned

        // Write-through: sync new point totals to cloud (fire-and-forget, silent on failure)
        writeScope.launch {
            userRepository.syncPointsToCloud(newPoints, newLifetimeXp)
        }

        // Write-through: create log in Data Connect and save remoteId back (fire-and-forget)
        habitEntity.remoteId?.let { remoteHabitId ->
            writeScope.launch {
                try {
                    val result = connector.createHabitLog.execute(
                        habitId = UUID.fromString(remoteHabitId),
                        date = today,
                        xpEarned = xpEarned,
                        streakAtCompletion = streak + 1,
                    )
                    val remoteLogId = result.data.habitLog_insert.id.toString()
                    habitLogDao.updateLogRemoteId(localLogId, remoteLogId)
                    Log.d(TAG, "Created habit log in cloud for habit: $remoteHabitId (remoteLogId: $remoteLogId)")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to sync habit log to cloud", e)
                }
            }
        }

        return CompletionResult(
            xpEarned = xpEarned,
            newStreak = streak + 1,
        )
    }

    /**
     * Calculates the current streak for a habit, correctly skipping non-scheduled days.
     *
     * For CUSTOM habits the algorithm walks backward from yesterday, skipping any day that
     * is not in the habit's schedule (weekly day-codes like "MON" or monthly day-of-month
     * codes like "D15"). A streak is broken only when a *scheduled* day has no completion log.
     *
     * @param habitId      local Room ID of the habit
     * @param today        today's date in ISO-8601 format (yyyy-MM-dd)
     * @param customDays   parsed customDays list (e.g. ["MON","WED","FRI"] or ["D1","D15"])
     */
    private suspend fun calculateStreak(habitId: Long, today: String, customDays: List<String>): Int {
        val dates = habitLogDao.getCompletionDatesForHabit(habitId)
        if (dates.isEmpty()) return 0

        val dateSet = dates.toHashSet()

        // Separate weekly codes (e.g. "MON") from monthly codes (e.g. "D15")
        val weeklyDays = customDays
            .filter { it.length == 3 && it.all { c -> c.isLetter() } }
            .map { it.uppercase() }
            .toSet()
        val monthlyDays = customDays
            .filter { it.startsWith("D", ignoreCase = true) }
            .mapNotNull { it.substring(1).toIntOrNull() }
            .toSet()

        // If no recognisable schedule codes are present, treat as daily.
        val isDaily = weeklyDays.isEmpty() && monthlyDays.isEmpty()

        /**
         * Returns true if [date] is a scheduled day according to this habit's custom days.
         * When [isDaily] is true every day is scheduled.
         */
        fun isScheduled(date: LocalDate): Boolean {
            if (isDaily) return true
            if (weeklyDays.isNotEmpty()) {
                val dayCode = date.dayOfWeek.name.take(3).uppercase()
                if (dayCode in weeklyDays) return true
            }
            if (monthlyDays.isNotEmpty()) {
                val dom = date.dayOfMonth
                val lastDom = date.lengthOfMonth()
                // "D31" matches the last day of any month
                if (dom in monthlyDays) return true
                if (31 in monthlyDays && dom == lastDom) return true
            }
            return false
        }

        var streak = 0
        // Walk backward from yesterday (today is counted separately at the end)
        var checkDate = LocalDate.parse(today, dateFormatter).minusDays(1)
        val maxLookback = 365 // safety limit — no streak can span more than a year
        var daysWalked = 0
        while (daysWalked < maxLookback) {
            if (isScheduled(checkDate)) {
                val dateStr = checkDate.format(dateFormatter)
                if (dateStr in dateSet) {
                    streak++
                } else {
                    break // scheduled day was missed — streak ends here
                }
            }
            // Non-scheduled day: skip without breaking the streak
            checkDate = checkDate.minusDays(1)
            daysWalked++
        }

        // Count today if already completed
        if (today in dateSet) {
            streak++
        }
        return streak
    }

    fun getLogsForMonth(yearMonth: String): Flow<List<HabitLogEntity>> {
        return habitLogDao.getLogsForMonth("$yearMonth%")
    }

    /**
     * Returns the set of [DayOfWeek] values within the current week (Mon-Sun)
     * that have at least one habit completion.
     */
    fun getCompletionDaysForCurrentWeek(): Flow<Set<DayOfWeek>> {
        val today = LocalDate.now()
        val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val sunday = monday.plusDays(6)
        val startDate = monday.format(dateFormatter)
        val endDate = sunday.format(dateFormatter)
        return habitLogDao.getCompletionDatesInRange(startDate, endDate).map { dateStrings ->
            dateStrings.mapNotNull { dateStr ->
                try {
                    LocalDate.parse(dateStr, dateFormatter).dayOfWeek
                } catch (_: Exception) {
                    null
                }
            }.toSet()
        }
    }

    fun getStreaksForToday(): Flow<Map<Long, Int>> {
        val today = LocalDate.now().format(dateFormatter)
        return habitLogDao.getStreaksForDate(today).map { streaks ->
            streaks.associate { it.habitId to it.streak }
        }
    }

    /**
     * Clear all local habit and habit log data — used for sign-out cleanup.
     */
    suspend fun clearLocalData() {
        habitLogDao.deleteAllLogs()
        habitDao.deleteAllHabits()
    }

    /**
     * Cancel all pending AlarmManager reminder alarms. Called on sign-out to prevent stale
     * alarms from firing after the user switches accounts.
     */
    /**
     * Cancel all pending AlarmManager alarms for this device.
     * Called during sign-out to prevent stale notifications from firing after the user switches accounts.
     * This prevents a stale habit from a previous account being linked to the newly-signed-in user's pet.
     */
    suspend fun cancelAllReminders() {
        try {
            val allHabits = habitDao.getAllHabitsSync()
            for (habit in allHabits) {
                HabitReminderScheduler.cancelReminder(context, habit.id)
            }
            Log.d(TAG, "Cancelled ${allHabits.size} pending reminder alarms")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to cancel all reminders", e)
        }
    }

    /**
     * Called when the user leaves a household.
     * - Deletes other members' synced household habits from Room.
     * - Converts the user's own household habits to personal (clears isHouseholdHabit + householdId).
     * Write-through updates each converted habit in Data Connect (fire-and-forget, silent on failure).
     */
    suspend fun convertHouseholdHabitsToPersonal(uid: String) {
        // Remove other members' habits from Room
        habitDao.deleteNonOwnedHouseholdHabits(uid)

        // Convert own household habits to personal
        val allHabits = habitDao.getAllHabitsSync()
        val ownHouseholdHabits = allHabits.filter {
            it.ownerUid == uid && it.isHouseholdHabit
        }
        for (entity in ownHouseholdHabits) {
            val updated = entity.copy(isHouseholdHabit = false, householdId = null, assignedToUid = null, assignedToName = null)
            habitDao.upsertHabit(updated)

            // Write-through: update in Data Connect (fire-and-forget, silent on failure)
            entity.remoteId?.let { remoteId ->
                writeScope.launch {
                    try {
                        connector.updateHabit.execute(
                            habitId = UUID.fromString(remoteId),
                            title = entity.title,
                            iconName = entity.iconName,
                            customDays = entity.customDays,
                            difficulty = entity.difficulty,
                            baseXp = entity.baseXp,
                            reminderEnabled = entity.reminderEnabled,
                            isHouseholdHabit = false,
                        ) {
                            description = entity.description
                            reminderTime = entity.reminderTime
                            householdId = null
                            assignedToId = null
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to convert household habit to personal in cloud: $remoteId", e)
                    }
                }
            }
        }
        Log.d(TAG, "Converted ${ownHouseholdHabits.size} household habits to personal for uid=$uid")
    }

    /**
     * Pull habits from Data Connect and merge into Room (cloud wins).
     * Also syncs household habits owned by other members into Room so they appear
     * in the habit list. Called once after successful authentication and on every sync.
     */
    /**
     * Sync all habits from Firebase Data Connect to Room.
     * Two phases:
     * 1. Personal habits (owned by current user): Fetches from GetAllMyHabits, upserts to Room,
     *    and schedules reminders if enabled.
     * 2. Household habits from other members (if in a household): Fetches from GetMyHouseholdHabits,
     *    filters out own habits (already synced in phase 1), and only enables reminders on habits
     *    assigned to the current user. Also schedules reminders for assigned household habits.
     * Cloud-to-local conflict resolution: Cloud wins (overwrites).
     * G13 reconciliation: Deletes local habits with remoteId that aren't present in cloud response.
     */
    suspend fun syncHabitsFromCloud() {
        val uid = firebaseAuth.currentUser?.uid ?: return
        try {
            // ---- 1. Personal habits (owned by current user) ----
            val result = withTimeoutOrNull(CLOUD_TIMEOUT_MS) { connector.getAllMyHabits.execute() }
            if (result == null) {
                Log.w(TAG, "syncHabitsFromCloud: personal habits timed out")
                return
            }
            val cloudHabits = result.data.habits
            Log.d(TAG, "Fetched ${cloudHabits.size} personal habits from cloud")

            val cloudRemoteIds = mutableSetOf<String>()
            for (cloudHabit in cloudHabits) {
                val remoteId = cloudHabit.id.toString()
                cloudRemoteIds.add(remoteId)
                val existing = habitDao.getHabitByRemoteId(remoteId)

                val entity = HabitEntity(
                    id = existing?.id ?: 0,
                    title = cloudHabit.title,
                    description = cloudHabit.description,
                    iconName = cloudHabit.iconName,
                    customDays = cloudHabit.customDays,
                    difficulty = cloudHabit.difficulty,
                    baseXp = cloudHabit.baseXp,
                    reminderEnabled = cloudHabit.reminderEnabled,
                    reminderTime = cloudHabit.reminderTime,
                    createdAt = cloudHabit.createdAt.toDate().time,
                    isArchived = cloudHabit.isArchived,
                    isHouseholdHabit = cloudHabit.isHouseholdHabit,
                    ownerUid = uid,
                    householdId = cloudHabit.household?.id?.toString(),
                    assignedToUid = cloudHabit.assignedTo?.id,
                    assignedToName = cloudHabit.assignedTo?.displayName,
                    remoteId = remoteId,
                )
             val upsertedId = habitDao.upsertHabit(entity)
                 // For new habits (existing == null) entity.id is 0; use the auto-generated ID
                 // returned by upsertHabit instead so the PendingIntent request code is correct.
                 val habitLocalId = existing?.id ?: upsertedId

                 // Schedule reminder immediately after syncing if enabled (fixes B17: new devices signing in for first time).
                 // On new devices, synced personal habits have reminderEnabled=true but no AlarmManager alarms were registered.
                 if (cloudHabit.reminderEnabled && cloudHabit.reminderTime != null && !cloudHabit.isArchived) {
                    try {
                        val parsedTime = try {
                            LocalTime.parse(cloudHabit.reminderTime)
                        } catch (_: Exception) {
                            LocalTime.of(9, 0)
                        }
                        HabitReminderScheduler.scheduleReminder(
                            context,
                            habitLocalId,
                            cloudHabit.title,
                            parsedTime,
                            cloudHabit.customDays.split(",").map { it.trim() }.filter { it.isNotEmpty() },
                        )
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to schedule reminder for synced habit ${cloudHabit.title}", e)
                    }
                }
            }

            // G13: Deletion reconciliation for personal habits.
            val localSyncedHabits = habitDao.getHabitsWithRemoteId()
            val orphanIds = localSyncedHabits
                .filter { it.ownerUid == uid && it.remoteId !in cloudRemoteIds }
                .map { it.id }
            if (orphanIds.isNotEmpty()) {
                orphanIds.chunked(500).forEach { chunk -> habitDao.deleteByIds(chunk) }
                Log.d(TAG, "Deleted ${orphanIds.size} orphaned personal habits not present in cloud")
            }

            Log.d(TAG, "Synced ${cloudHabits.size} personal habits from cloud to Room")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync personal habits from cloud", e)
            throw e
        }

        // ---- 2. Household habits from other members (best-effort, silent on failure) ----
        // Syncs household habits visible to current user: habits assigned to them and unassigned habits (assignedToUid IS NULL).
        // Reminders are only enabled for assigned habits — unassigned household habits don't trigger notifications on any device.
        try {
            val uid = firebaseAuth.currentUser?.uid ?: return
            val hhResult = withTimeoutOrNull(CLOUD_TIMEOUT_MS) { connector.getMyHouseholdHabits.execute() }
            if (hhResult == null) {
                Log.w(TAG, "syncHabitsFromCloud: household habits timed out")
                return
            }
            val householdData = hhResult.data.user?.household
            if (householdData == null) {
                // User has no household — delete any stale other-member habits from Room
                habitDao.deleteNonOwnedHouseholdHabits(uid)
                return
            }
            val householdId = householdData.id.toString()
            val cloudHouseholdHabits = householdData.habits_on_household
                .filter { it.owner.id != uid } // skip own habits already synced above

            val cloudHouseholdRemoteIds = mutableSetOf<String>()
            for (cloudHabit in cloudHouseholdHabits) {
                val remoteId = cloudHabit.id.toString()
                cloudHouseholdRemoteIds.add(remoteId)
                val existing = habitDao.getHabitByRemoteId(remoteId)

                // Enable reminders only if this habit is assigned to the current user
                val isAssignedToCurrentUser = cloudHabit.assignedTo?.id == uid
                val reminderEnabled = isAssignedToCurrentUser && cloudHabit.reminderEnabled
                val reminderTime = if (isAssignedToCurrentUser) cloudHabit.reminderTime else null

                val entity = HabitEntity(
                    id = existing?.id ?: 0,
                    title = cloudHabit.title,
                    description = cloudHabit.description,
                    iconName = cloudHabit.iconName,
                    customDays = cloudHabit.customDays,
                    difficulty = cloudHabit.difficulty,
                    baseXp = cloudHabit.baseXp,
                    reminderEnabled = reminderEnabled,
                    reminderTime = reminderTime,
                    createdAt = cloudHabit.createdAt.toDate().time,
                    isArchived = cloudHabit.isArchived,
                    isHouseholdHabit = true,
                    ownerUid = cloudHabit.owner.id, // preserve original owner
                    householdId = householdId,
                    assignedToUid = cloudHabit.assignedTo?.id,
                    assignedToName = cloudHabit.assignedTo?.displayName,
                    remoteId = remoteId,
                )
                 val upsertedHhId = habitDao.upsertHabit(entity)
                 // For new habits entity.id is 0; use the auto-generated ID for alarm scheduling.
                 val hhHabitLocalId = existing?.id ?: upsertedHhId

                 // Schedule reminder for household habits assigned to current user (fixes B17: assignments on new devices).
                 // User A assigns a habit to User B → User B signs in on a new device → reminder should fire for User B.
                 if (isAssignedToCurrentUser && reminderEnabled && reminderTime != null && !cloudHabit.isArchived) {
                    try {
                        val parsedTime = try {
                            LocalTime.parse(reminderTime)
                        } catch (_: Exception) {
                            LocalTime.of(9, 0)
                        }
                        HabitReminderScheduler.scheduleReminder(
                            context,
                            hhHabitLocalId,
                            cloudHabit.title,
                            parsedTime,
                            cloudHabit.customDays.split(",").map { it.trim() }.filter { it.isNotEmpty() },
                        )
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to schedule reminder for assigned household habit ${cloudHabit.title}", e)
                    }
                }
            }

            // Reconciliation: delete local other-member habits no longer in cloud
            val localOtherHabits = habitDao.getOtherMembersHouseholdHabits(uid)
            val staleIds = localOtherHabits
                .filter { it.remoteId !in cloudHouseholdRemoteIds }
                .map { it.id }
            if (staleIds.isNotEmpty()) {
                staleIds.chunked(500).forEach { chunk -> habitDao.deleteByIds(chunk) }
                Log.d(TAG, "Deleted ${staleIds.size} stale other-member household habits")
            }

            Log.d(TAG, "Synced ${cloudHouseholdHabits.size} other-member household habits from cloud")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync household habits from cloud (non-fatal)", e)
            // Silent — household sync failure should not block the rest of the sync
        }
    }

    /**
     * Pull today's household habit logs for all household habits from Data Connect and merge
     * into Room. This ensures the UI can display "Completed by [member name]" even for habits
     * completed by another household member before the current user opens the app.
     * Silent on failure — this is best-effort enrichment data.
     */
    suspend fun syncHouseholdHabitLogsForToday() {
        val uid = firebaseAuth.currentUser?.uid ?: return
        // Skip the network call entirely if the user has no household habits locally —
        // they are not in a household or haven't synced one yet.
        if (!habitDao.hasHouseholdHabits()) return
        val today = LocalDate.now().format(dateFormatter)
        try {
            val result = withTimeoutOrNull(CLOUD_TIMEOUT_MS) {
                connector.getHouseholdHabitLogsForDate.execute(date = today)
            }
            if (result == null) {
                Log.w(TAG, "syncHouseholdHabitLogsForToday: timed out")
                return
            }
            val habitsWithLogs = result.data.user?.household?.habits_on_household
                ?: return

            // Build remote habit UUID → local habit ID map
            val allLocalHabits = habitDao.getAllHabitsSync()
            val remoteToLocalId = allLocalHabits
                .filter { it.remoteId != null }
                .associate { it.remoteId!! to it.id }

            for (habitEntry in habitsWithLogs) {
                val remoteHabitId = habitEntry.id.toString()
                val localHabitId = remoteToLocalId[remoteHabitId] ?: continue

                for (log in habitEntry.habitLogs_on_habit) {
                    val remoteLogId = log.id.toString()
                    // Skip if already stored locally
                    if (habitLogDao.getLogByRemoteId(remoteLogId) != null) continue

                    val logEntity = HabitLogEntity(
                        habitId = localHabitId,
                        completedAt = log.completedAt.toDate().time,
                        date = today,
                        xpEarned = log.xpEarned,
                        streakAtCompletion = log.streakAtCompletion,
                        completedByUid = log.completedBy.id,
                        remoteId = remoteLogId,
                    )
                    habitLogDao.insertLog(logEntity)
                }
            }
            Log.d(TAG, "Synced household habit logs for today")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync household habit logs for today (non-fatal)", e)
        }
    }

    /**
     * Pull habit logs from Data Connect (last 30 days) and merge into Room (cloud wins).
     * Requires habits to be synced first so remoteId → local ID mapping is available.
     */
    suspend fun syncHabitLogsFromCloud() {
        val uid = firebaseAuth.currentUser?.uid ?: return
        val endDate = LocalDate.now().format(dateFormatter)
        val startDate = LocalDate.now().minusDays(30).format(dateFormatter)

        try {
            val result = withTimeoutOrNull(CLOUD_TIMEOUT_MS) {
                connector.getMyLogsForDateRange.execute(
                    startDate = startDate,
                    endDate = endDate,
                )
            }
            if (result == null) {
                Log.w(TAG, "syncHabitLogsFromCloud: timed out")
                return
            }
            val cloudLogs = result.data.habitLogs
            Log.d(TAG, "Fetched ${cloudLogs.size} habit logs from cloud")

            // Build a map of remote habit UUID → local habit ID
            val allLocalHabits = habitDao.getAllHabitsSync()
            val remoteToLocalId = allLocalHabits
                .filter { it.remoteId != null }
                .associate { it.remoteId!! to it.id }

            val logsToInsert = cloudLogs.mapNotNull { cloudLog ->
                val remoteLogId = cloudLog.id.toString()
                val remoteHabitId = cloudLog.habit.id.toString()
                val localHabitId = remoteToLocalId[remoteHabitId] ?: return@mapNotNull null

                // Skip if we already have this log locally
                val existingLog = habitLogDao.getLogByRemoteId(remoteLogId)
                if (existingLog != null) return@mapNotNull null

                HabitLogEntity(
                    habitId = localHabitId,
                    completedAt = cloudLog.completedAt.toDate().time,
                    date = cloudLog.date,
                    xpEarned = cloudLog.xpEarned,
                    streakAtCompletion = cloudLog.streakAtCompletion,
                    completedByUid = uid,
                    remoteId = remoteLogId,
                )
            }

            if (logsToInsert.isNotEmpty()) {
                habitLogDao.insertLogs(logsToInsert)
            }
            Log.d(TAG, "Synced ${logsToInsert.size} new habit logs from cloud to Room")

            // G13: Deletion reconciliation — remove local logs within the synced date range
            // that no longer exist in cloud. Logs outside the 30-day window are untouched.
            val cloudLogRemoteIds = cloudLogs.map { it.id.toString() }.toSet()
            val localSyncedLogs = habitLogDao.getLogsWithRemoteIdInRange(startDate, endDate)
            val orphanLogIds = localSyncedLogs
                .filter { it.remoteId !in cloudLogRemoteIds }
                .map { it.id }
            if (orphanLogIds.isNotEmpty()) {
                orphanLogIds.chunked(500).forEach { chunk -> habitLogDao.deleteLogsByIds(chunk) }
                Log.d(TAG, "Deleted ${orphanLogIds.size} orphaned habit logs not present in cloud")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync habit logs from cloud", e)
            throw e
        }
    }
}

private fun HabitEntity.toDomain() = Habit(
    id = id,
    title = title,
    description = description,
    iconName = iconName,
    customDays = customDays.split(",").map { it.trim() }.filter { it.isNotEmpty() },
    difficulty = difficulty,
    baseXp = baseXp,
    reminderEnabled = reminderEnabled,
    reminderTime = reminderTime?.let { timeStr ->
        try {
            LocalTime.parse(timeStr)
        } catch (_: Exception) {
            null
        }
    },
    createdAt = createdAt,
    isArchived = isArchived,
    isHouseholdHabit = isHouseholdHabit,
    ownerUid = ownerUid,
    householdId = householdId,
    assignedToUid = assignedToUid,
    assignedToName = assignedToName,
    remoteId = remoteId,
)

private fun Habit.toEntity() = HabitEntity(
    id = id,
    title = title,
    description = description,
    iconName = iconName,
    customDays = customDays.joinToString(","),
    difficulty = difficulty,
    baseXp = baseXp,
    reminderEnabled = reminderEnabled,
    reminderTime = reminderTime?.toString(),
    createdAt = createdAt,
    isArchived = isArchived,
    isHouseholdHabit = isHouseholdHabit,
    ownerUid = ownerUid,
    householdId = householdId,
    assignedToUid = assignedToUid,
    assignedToName = assignedToName,
    remoteId = remoteId,
)

package com.choreboo.app.data.repository

import dagger.hilt.android.qualifiers.ApplicationContext
import com.choreboo.app.data.local.dao.HabitDao
import com.choreboo.app.data.local.dao.HabitLogDao
import com.choreboo.app.data.local.entity.HabitEntity
import com.choreboo.app.data.local.dao.HabitLogWithName
import com.choreboo.app.data.local.entity.HabitLogEntity
import com.choreboo.app.data.datastore.UserPreferences
import com.choreboo.app.dataconnect.ChorebooConnector
import com.choreboo.app.dataconnect.execute
import com.choreboo.app.dataconnect.instance
import com.choreboo.app.domain.model.Habit
import com.choreboo.app.domain.model.HabitLog
import com.choreboo.app.worker.HabitReminderScheduler
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private const val CLOUD_TIMEOUT_MS = 5000L

/**
 * D2: Retry delays for write-through failures. 3 attempts total:
 * 1st attempt immediate, 2nd after 1 s, 3rd after 3 s.
 */
private val WRITE_THROUGH_RETRY_DELAYS_MS = listOf(1_000L, 3_000L)

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
    internal constructor(
        habitDao: HabitDao,
        habitLogDao: HabitLogDao,
        userPreferences: UserPreferences,
        userRepository: UserRepository,
        firebaseAuth: FirebaseAuth,
        context: android.content.Context,
        reminderScheduler: HabitReminderHandler,
    ) : this(habitDao, habitLogDao, userPreferences, userRepository, firebaseAuth, context) {
        this.reminderScheduler = reminderScheduler
    }

    private val connector by lazy { ChorebooConnector.instance }
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    private var reminderScheduler: HabitReminderHandler = AlarmManagerHabitReminderHandler

    /** Fire-and-forget scope for silent write-through calls. */
    @Volatile
    private var writeScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Cancel all pending write-through coroutines and create a fresh scope.
     * Called on sign-out and account reset to prevent stale writes executing after
     * the user's data has been cleared.
     *
     * Thread-safety: must be called from the main thread (SettingsViewModel/ResetRepository
     * always invoke this from viewModelScope which is confined to Main). The cancel() and
     * scope reassignment are therefore sequentially ordered — no new coroutines can be
     * launched on the old scope after this returns because all callers hold a reference to
     * the *current* writeScope at launch time.
     */
    fun cancelPendingWrites() {
        writeScope.coroutineContext[Job]?.cancel()
        writeScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }

    /**
     * D2: Execute [block] with up to 3 attempts (1 immediate + 2 retries with backoff).
     * Returns true on success, false after all attempts fail.
     * Callers set pendingSync=true before calling, clear it after this returns.
     */
    private suspend fun retryWithBackoff(tag: String, block: suspend () -> Unit): Boolean {
        var attempt = 0
        while (true) {
            try {
                block()
                return true
            } catch (e: Exception) {
                if (attempt < WRITE_THROUGH_RETRY_DELAYS_MS.size) {
                    val delayMs = WRITE_THROUGH_RETRY_DELAYS_MS[attempt]
                    Timber.w(e, "Write-through [$tag] attempt ${attempt + 1} failed, retrying in ${delayMs}ms")
                    delay(delayMs)
                    attempt++
                } else {
                    Timber.e(e, "Write-through [$tag] exhausted all retries")
                    return false
                }
            }
        }
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

        // Defense-in-depth: ensure ownerUid is always populated before writing to Room.
        // The primary fix lives in AddEditHabitViewModel, but this guard catches any
        // future caller that forgets to set the owner.
        val effectiveHabit = if (habit.ownerUid.isNullOrBlank()) {
            val uid = firebaseAuth.currentUser?.uid
            if (uid != null) {
                Timber.w("upsertHabit called with blank ownerUid — filling from FirebaseAuth (habitTitle=${habit.title})")
                habit.copy(ownerUid = uid)
            } else {
                habit
            }
        } else {
            habit
        }

        val localId = habitDao.upsertHabit(effectiveHabit.toEntity())

        // Write-through to Data Connect.
        // New habits (no remoteId yet): mark pendingSync=true immediately, clear on success/exhaustion.
        // This prevents the cloud-to-local sync from overwriting local state with a stale cloud
        // response during the window between local insert and cloud round-trip (D2 fix).
        writeScope.launch {
            if (effectiveHabit.remoteId != null) {
                // Update existing remote habit — no pendingSync guard needed for updates
                try {
                    val remoteUuid = UUID.fromString(effectiveHabit.remoteId)
                    val currentUid = firebaseAuth.currentUser?.uid
                    if (currentUid != null && effectiveHabit.ownerUid == currentUid) {
                        // Owner — update all fields
                        // S7: Route to the correct mutation based on ownership.
                        connector.updateOwnHabit.execute(
                            habitId = remoteUuid,
                            title = effectiveHabit.title,
                            iconName = effectiveHabit.iconName,
                            customDays = effectiveHabit.customDays.joinToString(","),
                            difficulty = effectiveHabit.difficulty,
                            baseXp = effectiveHabit.baseXp,
                            reminderEnabled = effectiveHabit.reminderEnabled,
                            isHouseholdHabit = effectiveHabit.isHouseholdHabit,
                        ) {
                            description = effectiveHabit.description
                            reminderTime = effectiveHabit.reminderTime?.toString()
                            householdId = effectiveHabit.householdId?.let { UUID.fromString(it) }
                            assignedToId = effectiveHabit.assignedToUid
                        }
                        Timber.d("Updated own habit in cloud: $remoteUuid")
                    } else {
                        // Assignee — update safe fields only (no ownership/household changes)
                        connector.updateAssignedHabit.execute(
                            habitId = remoteUuid,
                            title = effectiveHabit.title,
                            iconName = effectiveHabit.iconName,
                            customDays = effectiveHabit.customDays.joinToString(","),
                            reminderEnabled = effectiveHabit.reminderEnabled,
                        ) {
                            description = effectiveHabit.description
                            reminderTime = effectiveHabit.reminderTime?.toString()
                        }
                        Timber.d("Updated assigned habit in cloud: $remoteUuid")
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Failed to sync habit update to cloud")
                }
            } else {
                // D2 + D4: New habit. Pre-generate UUID, stamp remoteId + pendingSync=true in
                // Room immediately, then retry the cloud insert up to 3 times. The pendingSync
                // flag prevents a concurrent sync from overwriting local state with a stale
                // cloud value before the cloud record actually exists.
                val preGeneratedId = UUID.randomUUID()
                val remoteId = preGeneratedId.toString()
                habitDao.upsertHabit(effectiveHabit.toEntity().copy(id = localId, remoteId = remoteId, pendingSync = true))
                val success = retryWithBackoff("createHabit:$localId") {
                    connector.createHabit.execute(
                        id = preGeneratedId,
                        title = effectiveHabit.title,
                        iconName = effectiveHabit.iconName,
                        customDays = effectiveHabit.customDays.joinToString(","),
                        difficulty = effectiveHabit.difficulty,
                        baseXp = effectiveHabit.baseXp,
                        reminderEnabled = effectiveHabit.reminderEnabled,
                        isHouseholdHabit = effectiveHabit.isHouseholdHabit,
                    ) {
                        description = effectiveHabit.description
                        reminderTime = effectiveHabit.reminderTime?.toString()
                        householdId = effectiveHabit.householdId?.let { UUID.fromString(it) }
                        assignedToId = effectiveHabit.assignedToUid
                    }
                }
                if (success) {
                    Timber.d("Created habit in cloud with pre-generated id: $remoteId")
                }
                // Always clear pendingSync whether success or exhausted — we don't retry
                // indefinitely; the next full sync will reconcile if still missing.
                habitDao.clearPendingSync(localId)
            }
        }

        return localId
    }

    suspend fun deleteHabit(id: Long) {
        // Get the entity first to find the remote ID
        val entity = habitDao.getHabitByIdSync(id)
        habitDao.deleteHabitById(id)

        // Write-through: delete logs then habit from Data Connect with retry.
        // Logs must be deleted first to avoid FK constraint violations (D5 fix).
        // Without retry, a transient failure leaves a ghost habit that reappears on next sync.
        entity?.remoteId?.let { remoteId ->
            writeScope.launch {
                retryWithBackoff("deleteHabit:$id") {
                    val habitUuid = UUID.fromString(remoteId)
                    connector.deleteLogsForHabit.execute(habitId = habitUuid)
                    connector.deleteHabit.execute(habitId = habitUuid)
                    Timber.d("Deleted habit + logs from cloud: $remoteId")
                }
            }
        }
    }

    suspend fun archiveHabit(id: Long) {
        val entity = habitDao.getHabitByIdSync(id)
        habitDao.archiveHabit(id)

        // A1: Cancel any pending reminder alarm so archived habits never fire notifications.
        reminderScheduler.cancel(context, id)
        Timber.d("Cancelled reminder for archived habit id=$id")

        // Write-through: archive in Data Connect with retry
        entity?.remoteId?.let { remoteId ->
            writeScope.launch {
                retryWithBackoff("archiveHabit:$id") {
                    connector.archiveHabit.execute(
                        habitId = UUID.fromString(remoteId),
                    )
                    Timber.d("Archived habit in cloud: $remoteId")
                }
            }
        }
    }

    /** Un-archive a habit locally and in Data Connect (mirrors archiveHabit). */
    suspend fun unarchiveHabit(id: Long) {
        val entity = habitDao.getHabitByIdSync(id)
        habitDao.unarchiveHabit(id)

        // A2: Re-schedule the reminder if it was enabled on this habit before archiving.
        if (entity != null && entity.reminderEnabled && entity.reminderTime != null) {
            try {
                val parsedTime = try {
                    LocalTime.parse(entity.reminderTime)
                } catch (e: Exception) {
                    Timber.w(e, "unarchiveHabit: failed to parse reminderTime ${entity.reminderTime}")
                    LocalTime.of(9, 0)
                }
                val customDays = entity.customDays
                    .split(",")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                reminderScheduler.schedule(context, id, entity.title, parsedTime, customDays)
                Timber.d("Re-scheduled reminder for unarchived habit id=$id")
            } catch (e: Exception) {
                Timber.w(e, "unarchiveHabit: failed to re-schedule reminder for habit id=$id")
            }
        }

        // Write-through: un-archive in Data Connect with retry
        entity?.remoteId?.let { remoteId ->
            writeScope.launch {
                retryWithBackoff("unarchiveHabit:$id") {
                    connector.unarchiveHabit.execute(
                        habitId = UUID.fromString(remoteId),
                    )
                    Timber.d("Unarchived habit in cloud: $remoteId")
                }
            }
        }
    }

    fun getLogsForDate(date: String): Flow<List<HabitLog>> {
        return habitLogDao.getLogsForDate(date).map { entities -> entities.map { it.toDomain() } }
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

        // For household habits: perform a blocking cloud pre-check with timeout to see if
        // another household member already completed this habit today.
        // C1: Changed from fire-and-forget async (writeScope.launch) to blocking so we can
        // return alreadyComplete = true immediately instead of racing the local UNIQUE constraint.
        // On timeout or network error, we fall through to local completion — the UNIQUE(habitId,date)
        // constraint remains the final duplicate guard.
        if (habitEntity.isHouseholdHabit && habitEntity.remoteId != null) {
            try {
                val cloudCheck = withTimeoutOrNull(CLOUD_TIMEOUT_MS) {
                    connector.getLogsForHabitAndDate.execute(
                        habitId = UUID.fromString(habitEntity.remoteId),
                        date = today,
                    )
                }
                if (cloudCheck != null) {
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
                        return CompletionResult(xpEarned = 0, newStreak = 0, alreadyComplete = true)
                    }
                } else {
                    Timber.w("Cloud pre-check for household habit timed out — continuing with local completion")
                }
            } catch (e: Exception) {
                Timber.w(e, "Cloud pre-check for household habit failed — continuing with local completion")
                // Fall through: let local completion proceed; UNIQUE constraint handles duplicates
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

        // addPoints/addLifetimeXp are atomic inside dataStore.edit and return the new value,
        // eliminating the read-then-compute race that existed with the old read-first pattern.
        val newPoints = userPreferences.addPoints(xpEarned)
        val newLifetimeXp = userPreferences.addLifetimeXp(xpEarned)

        // Write-through: sync new point totals to cloud (fire-and-forget, silent on failure)
        writeScope.launch {
            userRepository.syncPointsToCloud(newPoints, newLifetimeXp)
        }

        // Write-through: create log in Data Connect using a pre-generated UUID (D4 fix).
        // D2: Set pendingSync=true before the network call to prevent cloud-wins sync from
        // overwriting the freshly inserted log while the write-through is in flight.
        // The remoteId is stamped into Room immediately so if the app is killed before the
        // cloud response returns, the next sync can still reconcile the log correctly.
        habitEntity.remoteId?.let { remoteHabitId ->
            val preGeneratedLogId = UUID.randomUUID()
            val remoteLogId = preGeneratedLogId.toString()
            // Stamp remoteId + pendingSync before the network call
            habitLogDao.updateLogRemoteId(localLogId, remoteLogId)
            habitLogDao.markPendingSync(localLogId)
            writeScope.launch {
                val success = retryWithBackoff("createHabitLog:$localLogId") {
                    connector.createHabitLog.execute(
                        id = preGeneratedLogId,
                        habitId = UUID.fromString(remoteHabitId),
                        date = today,
                        xpEarned = xpEarned,
                        streakAtCompletion = streak + 1,
                    )
                }
                if (success) {
                    Timber.d("Created habit log in cloud for habit: $remoteHabitId (remoteLogId: $remoteLogId)")
                }
                // Always clear pendingSync whether success or exhausted.
                habitLogDao.clearPendingSync(localLogId)
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
        val since = LocalDate.parse(today, dateFormatter).minusDays(365).format(dateFormatter)
        val dates = habitLogDao.getCompletionDatesForHabit(habitId, since)
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
                // Exact match
                if (dom in monthlyDays) return true
                // If any scheduled day exceeds this month's length, it fires on the last day
                if (monthlyDays.any { it >= lastDom } && dom == lastDom) return true
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

    fun getLogsForMonth(yearMonth: String): Flow<List<HabitLog>> {
        return habitLogDao.getLogsForMonth("$yearMonth%").map { entities -> entities.map { it.toDomain() } }
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
                } catch (e: Exception) {
                    Timber.w(e, "Failed to parse date for day-of-week: $dateStr")
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
     * Cancel all pending AlarmManager alarms for this device.
     * Called during sign-out to prevent stale notifications from firing after the user switches accounts.
     * This prevents a stale habit from a previous account being linked to the newly-signed-in user's pet.
     */
    suspend fun cancelAllReminders() {
        try {
            val allHabits = habitDao.getAllHabitsSync()
            for (habit in allHabits) {
                reminderScheduler.cancel(context, habit.id)
            }
            Timber.d("Cancelled ${allHabits.size} pending reminder alarms")
        } catch (e: Exception) {
            Timber.w(e, "Failed to cancel all reminders")
        }
    }

    /**
     * Called when the user leaves a household.
     * - Deletes other members' synced household habits from Room.
     * - Converts the user's own household habits to personal (clears isHouseholdHabit + householdId).
     * - Reconciles reminder ownership now that assigned household work is no longer household-scoped.
     * Write-through updates each converted habit in Data Connect (fire-and-forget, silent on failure).
     */
    suspend fun convertHouseholdHabitsToPersonal(uid: String) {
        val allHabits = habitDao.getAllHabitsSync()
        val otherMembersHouseholdHabits = allHabits.filter {
            it.ownerUid != uid && it.isHouseholdHabit
        }
        val ownHouseholdHabits = allHabits.filter {
            it.ownerUid == uid && it.isHouseholdHabit
        }

        // Remove other members' habits from Room and cancel any reminder alarms they owned.
        otherMembersHouseholdHabits.forEach { habit ->
            reminderScheduler.cancel(context, habit.id)
        }
        habitDao.deleteNonOwnedHouseholdHabits(uid)

        // Convert own household habits to personal and re-evaluate reminder ownership.
        for (entity in ownHouseholdHabits) {
            val updated = entity.copy(isHouseholdHabit = false, householdId = null, assignedToUid = null, assignedToName = null)
            habitDao.upsertHabit(updated)
            syncReminderFromCloud(
                localHabitId = updated.id,
                title = updated.title,
                reminderEnabled = updated.reminderEnabled,
                reminderTime = updated.reminderTime,
                isArchived = updated.isArchived,
                customDays = updated.customDays,
                shouldOwnReminder = true,
                logTag = "convertHouseholdHabit:${entity.id}",
            )

            // Write-through: update in Data Connect as owner (fire-and-forget, with retry)
            entity.remoteId?.let { remoteId ->
                writeScope.launch {
                    retryWithBackoff("convertHouseholdHabit:${entity.id}") {
                        connector.updateOwnHabit.execute(
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
                    }
                }
            }
        }
        Timber.d("Converted ${ownHouseholdHabits.size} household habits to personal for uid=$uid")
    }

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
                Timber.w("syncHabitsFromCloud: personal habits timed out")
                return
            }
            val cloudHabits = result.data.habits
            Timber.d("Fetched ${cloudHabits.size} personal habits from cloud")

            val cloudRemoteIds = mutableSetOf<String>()
            for (cloudHabit in cloudHabits) {
                val remoteId = cloudHabit.id.toString()
                cloudRemoteIds.add(remoteId)
                val existing = habitDao.getHabitByRemoteId(remoteId)

                // D2: Skip overwriting rows that have a pending write-through in flight.
                // If pendingSync=true, the local state is ahead of the cloud; overwriting it
                // would clobber the user's change before the cloud has received it.
                if (existing?.pendingSync == true) {
                    Timber.d("syncHabitsFromCloud: skipping pendingSync habit remoteId=$remoteId")
                    val upsertedId = existing.id
                    syncReminderFromCloud(
                        localHabitId = upsertedId,
                        title = cloudHabit.title,
                        reminderEnabled = cloudHabit.reminderEnabled,
                        reminderTime = cloudHabit.reminderTime,
                        isArchived = cloudHabit.isArchived,
                        customDays = cloudHabit.customDays,
                        shouldOwnReminder = true,
                        logTag = "pendingSync habit ${cloudHabit.title}",
                    )
                    continue
                }

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
                syncReminderFromCloud(
                    localHabitId = habitLocalId,
                    title = cloudHabit.title,
                    reminderEnabled = cloudHabit.reminderEnabled,
                    reminderTime = cloudHabit.reminderTime,
                    isArchived = cloudHabit.isArchived,
                    customDays = cloudHabit.customDays,
                    shouldOwnReminder = true,
                    logTag = "synced habit ${cloudHabit.title}",
                )
            }

            // G13: Deletion reconciliation for personal habits.
            // D2: Exclude pendingSync=true rows — they have a write-through in flight and
            // their remoteId may not appear in the cloud response yet (newly created habit).
            val localSyncedHabits = habitDao.getHabitsWithRemoteId()
            val orphanIds = localSyncedHabits
                .filter { it.ownerUid == uid && it.remoteId !in cloudRemoteIds && !it.pendingSync }
                .map { it.id }
            if (orphanIds.isNotEmpty()) {
                orphanIds.forEach { reminderScheduler.cancel(context, it) }
                orphanIds.chunked(500).forEach { chunk -> habitDao.deleteByIds(chunk) }
                Timber.d("Deleted ${orphanIds.size} orphaned personal habits not present in cloud")
            }

            Timber.d("Synced ${cloudHabits.size} personal habits from cloud to Room")
        } catch (e: Exception) {
            Timber.e(e, "Failed to sync personal habits from cloud")
            throw e
        }

        // ---- 2. Household habits from other members (best-effort, silent on failure) ----
        // Syncs household habits visible to current user: habits assigned to them and unassigned habits (assignedToUid IS NULL).
        // Reminders are only enabled for assigned habits — unassigned household habits don't trigger notifications on any device.
        try {
            val uid = firebaseAuth.currentUser?.uid ?: return
            val hhResult = withTimeoutOrNull(CLOUD_TIMEOUT_MS) { connector.getMyHouseholdHabits.execute() }
            if (hhResult == null) {
                Timber.w("syncHabitsFromCloud: household habits timed out")
                return
            }
            val householdData = hhResult.data.user?.household
            if (householdData == null) {
                // User has no household — delete any stale other-member habits from Room
                habitDao.getOtherMembersHouseholdHabits(uid).forEach { reminderScheduler.cancel(context, it.id) }
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
                syncReminderFromCloud(
                    localHabitId = hhHabitLocalId,
                    title = cloudHabit.title,
                    reminderEnabled = reminderEnabled,
                    reminderTime = reminderTime,
                    isArchived = cloudHabit.isArchived,
                    customDays = cloudHabit.customDays,
                    shouldOwnReminder = isAssignedToCurrentUser,
                    logTag = "assigned household habit ${cloudHabit.title}",
                )
            }

            // Reconciliation: delete local other-member habits no longer in cloud
            val localOtherHabits = habitDao.getOtherMembersHouseholdHabits(uid)
            val staleIds = localOtherHabits
                .filter { it.remoteId !in cloudHouseholdRemoteIds }
                .map { it.id }
            if (staleIds.isNotEmpty()) {
                staleIds.forEach { reminderScheduler.cancel(context, it) }
                staleIds.chunked(500).forEach { chunk -> habitDao.deleteByIds(chunk) }
                Timber.d("Deleted ${staleIds.size} stale other-member household habits")
            }

            Timber.d("Synced ${cloudHouseholdHabits.size} other-member household habits from cloud")
        } catch (e: Exception) {
            Timber.e(e, "Failed to sync household habits from cloud (non-fatal)")
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
                Timber.w("syncHouseholdHabitLogsForToday: timed out")
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
            Timber.d("Synced household habit logs for today")
        } catch (e: Exception) {
            Timber.e(e, "Failed to sync household habit logs for today (non-fatal)")
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
            // D1 fix: Snapshot local remoteIds BEFORE the cloud fetch.
            // Any log whose remoteId is stamped by a concurrent write-through between the
            // cloud query and the local reconciliation query would otherwise appear as an
            // orphan and be wrongly deleted. By snapshotting first, those in-flight logs
            // are in neither the pre-snapshot set nor the cloud set → excluded from deletion.
            val preSnapshotLocalLogs = habitLogDao.getLogsWithRemoteIdInRange(startDate, endDate)
            val preSnapshotRemoteIds = preSnapshotLocalLogs.mapNotNull { it.remoteId }.toSet()

            val result = withTimeoutOrNull(CLOUD_TIMEOUT_MS) {
                connector.getMyLogsForDateRange.execute(
                    startDate = startDate,
                    endDate = endDate,
                )
            }
            if (result == null) {
                Timber.w("syncHabitLogsFromCloud: timed out")
                return
            }
            val cloudLogs = result.data.habitLogs
            Timber.d("Fetched ${cloudLogs.size} habit logs from cloud")

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
                    // C2 note: GetMyLogsForDateRange filters by auth.uid so completedByUid is
                    // always the current user. The query does not return a completedBy object.
                    completedByUid = uid,
                    remoteId = remoteLogId,
                )
            }

            if (logsToInsert.isNotEmpty()) {
                habitLogDao.insertLogs(logsToInsert)
            }
            Timber.d("Synced ${logsToInsert.size} new habit logs from cloud to Room")

            // G13: Deletion reconciliation — only consider logs that were already in the
            // pre-snapshot set (i.e. had a remoteId before the cloud fetch started).
            // D2: Also exclude pendingSync=true rows — they have a write-through in flight
            // and won't appear in the cloud set yet (log was just created).
            // Logs stamped by concurrent write-throughs during the fetch are excluded,
            // preventing the race where a freshly-completed log's remoteId is deleted.
            val cloudLogRemoteIds = cloudLogs.map { it.id.toString() }.toSet()
            val orphanLogIds = preSnapshotLocalLogs
                .filter { it.remoteId != null && it.remoteId in preSnapshotRemoteIds && it.remoteId !in cloudLogRemoteIds && !it.pendingSync }
                .map { it.id }
            if (orphanLogIds.isNotEmpty()) {
                orphanLogIds.chunked(500).forEach { chunk -> habitLogDao.deleteLogsByIds(chunk) }
                Timber.d("Deleted ${orphanLogIds.size} orphaned habit logs not present in cloud")
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to sync habit logs from cloud")
            throw e
        }
    }

    internal fun syncReminderFromCloud(
        localHabitId: Long,
        title: String,
        reminderEnabled: Boolean,
        reminderTime: String?,
        isArchived: Boolean,
        customDays: String,
        shouldOwnReminder: Boolean,
        logTag: String,
    ) {
        if (!shouldOwnReminder || !reminderEnabled || reminderTime == null || isArchived) {
            reminderScheduler.cancel(context, localHabitId)
            return
        }

        try {
            val parsedTime = try {
                LocalTime.parse(reminderTime)
            } catch (e: Exception) {
                Timber.w(e, "Failed to parse reminder time for $logTag: $reminderTime")
                LocalTime.of(9, 0)
            }
            reminderScheduler.schedule(
                context,
                localHabitId,
                title,
                parsedTime,
                customDays.split(",").map { it.trim() }.filter { it.isNotEmpty() },
            )
        } catch (e: Exception) {
            Timber.w(e, "Failed to sync reminder for $logTag")
        }
    }
}

internal interface HabitReminderHandler {
    fun schedule(
        context: android.content.Context,
        habitId: Long,
        habitTitle: String,
        reminderTime: LocalTime,
        scheduledDays: List<String>,
    )

    fun cancel(context: android.content.Context, habitId: Long)
}

private object AlarmManagerHabitReminderHandler : HabitReminderHandler {
    override fun schedule(
        context: android.content.Context,
        habitId: Long,
        habitTitle: String,
        reminderTime: LocalTime,
        scheduledDays: List<String>,
    ) {
        HabitReminderScheduler.scheduleReminder(context, habitId, habitTitle, reminderTime, scheduledDays)
    }

    override fun cancel(context: android.content.Context, habitId: Long) {
        HabitReminderScheduler.cancelReminder(context, habitId)
    }
}

private fun HabitLogEntity.toDomain() = HabitLog(
    id = id,
    habitId = habitId,
    completedAt = completedAt,
    date = date,
    xpEarned = xpEarned,
    streakAtCompletion = streakAtCompletion,
    completedByUid = completedByUid,
    remoteId = remoteId,
)

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
        } catch (e: Exception) {
            Timber.w(e, "Failed to parse reminderTime in toDomain(): $timeStr")
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
    ownerUid = ownerUid ?: "",
    householdId = householdId,
    assignedToUid = assignedToUid,
    assignedToName = assignedToName,
    remoteId = remoteId,
)

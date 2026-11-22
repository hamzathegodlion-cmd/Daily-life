package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alarm.AlarmScheduler
import com.example.alarm.AlarmSoundPlayer
import com.example.alarm.EnglishVoiceInfo
import com.example.alarm.VoiceAnnouncementManager
import com.example.data.model.ActivityCategory
import com.example.data.model.ActivityStatus
import com.example.data.model.AssistantSettings
import com.example.data.model.CompletionRecord
import com.example.data.model.DefaultTimetableFactory
import com.example.data.model.RoutineActivity
import com.example.data.model.WeekdayMask
import com.example.data.model.WorkoutExercise
import com.example.data.model.WorkoutGroupType
import com.example.data.model.WorkoutPlan
import com.example.data.repository.RoutineRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.TemporalAdjusters

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

data class DailySummaryStats(
    val date: LocalDate,
    val totalScheduled: Int,
    val completedCount: Int,
    val missedCount: Int,
    val incompleteCount: Int,
    val completionPercent: Int
)

data class WeeklySummaryStats(
    val weekStartDate: LocalDate,
    val totalStudyMinutes: Int,
    val totalWebMinutes: Int,
    val completedWorkouts: Int,
    val quranDaysCompleted: Int,
    val youtubeVideosUploaded: Int,
    val cyclingDistanceKm: Double,
    val dailyCheckCompletedDays: Int,
    val dailyCompletionPercentages: List<Pair<DayOfWeek, Int>>
)

data class DashboardUiState(
    val now: ZonedDateTime = ZonedDateTime.now(ZoneId.systemDefault()),
    val activities: List<RoutineActivity> = emptyList(),
    val todayActivities: List<RoutineActivity> = emptyList(),
    val currentActivity: RoutineActivity? = null,
    val nextActivity: RoutineActivity? = null,
    val countdownToNext: Duration = Duration.ZERO,
    val todayWorkoutGroup: WorkoutGroupType = WorkoutGroupType.REST_DAY,
    val workoutPlans: List<WorkoutPlan> = emptyList(),
    val completionsByKey: Map<String, CompletionRecord> = emptyMap(),
    val todayStats: DailySummaryStats = DailySummaryStats(LocalDate.now(), 0, 0, 0, 0, 0),
    val settings: AssistantSettings = AssistantSettings()
)

class RoutineViewModel(
    private val repository: RoutineRepository,
    private val alarmScheduler: AlarmScheduler,
    private val soundPlayer: AlarmSoundPlayer,
    private val voiceManager: VoiceAnnouncementManager,
    private val currentUserId: String
) : ViewModel() {

    private val _selectedCalendarDay = MutableStateFlow(LocalDate.now().dayOfWeek)
    val selectedCalendarDay: StateFlow<DayOfWeek> = _selectedCalendarDay.asStateFlow()

    private val _selectedWeekOffset = MutableStateFlow(0) // 0 = current week, -1 = previous week, etc.
    val selectedWeekOffset: StateFlow<Int> = _selectedWeekOffset.asStateFlow()

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    private val _availableVoices = MutableStateFlow<List<EnglishVoiceInfo>>(emptyList())
    val availableVoices: StateFlow<List<EnglishVoiceInfo>> = _availableVoices.asStateFlow()

    private val _syncError = MutableStateFlow<String?>(null)
    val syncError: StateFlow<String?> = _syncError.asStateFlow()

    private val clockFlow = flow {
        while (true) {
            emit(ZonedDateTime.now(ZoneId.systemDefault()))
            delay(1000L)
        }
    }

    val dashboardState: StateFlow<UiState<DashboardUiState>> = combine(
        clockFlow,
        repository.activitiesFlow,
        repository.completionsFlow,
        repository.workoutPlansFlow,
        repository.settingsFlow
    ) { now, activities, completions, plans, settings ->
        val today = now.toLocalDate()
        val todayKey = today.toString()
        val todayDayOfWeek = today.dayOfWeek
        val todayActs = activities
            .filter { WeekdayMask.includes(it.weekdaysMask, todayDayOfWeek) }
            .sortedBy { it.sortOrder }

        val compMap = completions.associateBy { it.id }
        val nowTime = now.toLocalTime()
        val currentAct = todayActs.firstOrNull { it.isActiveAt(nowTime) && it.startTotalMinutes != it.endTotalMinutes }
            ?: todayActs.firstOrNull { it.isActiveAt(nowTime) }

        val (nextAct, countdown) = findNextActivityAndCountdown(activities, now)
        val todayStats = computeDailyStats(today, todayActs, compMap, nowTime)

        UiState.Success(
            DashboardUiState(
                now = now,
                activities = activities,
                todayActivities = todayActs,
                currentActivity = currentAct,
                nextActivity = nextAct,
                countdownToNext = countdown,
                todayWorkoutGroup = DefaultTimetableFactory.workoutGroupForDay(todayDayOfWeek),
                workoutPlans = plans,
                completionsByKey = compMap,
                todayStats = todayStats,
                settings = settings
            )
        ) as UiState<DashboardUiState>
    }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Dashboard stream error", rootError)
            emit(UiState.Error(rootError.message ?: "Failed to load routine data"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    val weeklyStatsState: StateFlow<WeeklySummaryStats> = combine(
        _selectedWeekOffset,
        repository.activitiesFlow,
        repository.completionsFlow
    ) { offset, activities, completions ->
        computeWeeklyStats(offset, activities, completions)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = computeWeeklyStats(0, emptyList(), emptyList())
    )

    init {
        viewModelScope.launch {
            repository.ensureSeededLocally()
            alarmScheduler.rescheduleAll(repository.getActivitiesOnce())
            voiceManager.awaitReady()
            _availableVoices.value = voiceManager.getAvailableEnglishVoices()
        }

        // Start real-time Firestore synchronization scoped to authenticated UID
        viewModelScope.launch {
            repository.observeRemoteSync(currentUserId)
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val rootError = firestoreError ?: error
                    Log.w(TAG, "Remote sync error", rootError)
                    _syncError.value = rootError.localizedMessage ?: "Cloud sync unavailable (offline mode active)"
                }
                .collect {
                    _syncError.value = null
                }
        }
    }

    fun selectCalendarDay(dayOfWeek: DayOfWeek) {
        _selectedCalendarDay.value = dayOfWeek
    }

    fun shiftWeekOffset(delta: Int) {
        _selectedWeekOffset.value = (_selectedWeekOffset.value + delta).coerceAtMost(0)
    }

    fun clearBanner() {
        _statusBanner.value = null
    }

    fun toggleCompletion(activity: RoutineActivity, date: LocalDate = LocalDate.now()) {
        viewModelScope.launch {
            val key = "${date}__${activity.id}"
            val current = (dashboardState.value as? UiState.Success)?.data?.completionsByKey?.get(key)
            val nextStatus = if (current?.status == ActivityStatus.COMPLETED) {
                ActivityStatus.INCOMPLETE
            } else {
                ActivityStatus.COMPLETED
            }
            val result = repository.markActivityStatus(activity.id, date.toString(), nextStatus)
            if (result.isFailure) {
                _statusBanner.value = "Saved locally; cloud sync pending"
            }
        }
    }

    fun saveActivity(activity: RoutineActivity) {
        viewModelScope.launch {
            val result = repository.upsertActivity(activity)
            _statusBanner.value = if (result.isSuccess) {
                "Saved \"${activity.title}\" and updated alarms"
            } else {
                "Saved locally; cloud sync pending"
            }
        }
    }

    fun deleteActivity(activityId: String) {
        viewModelScope.launch {
            val result = repository.deleteActivity(activityId)
            _statusBanner.value = if (result.isSuccess) "Activity deleted" else "Deleted locally; cloud sync pending"
        }
    }

    fun restoreOriginalTimetable() {
        viewModelScope.launch {
            val result = repository.restoreOriginalTimetable()
            _statusBanner.value = if (result.isSuccess) {
                "Original timetable restored & alarms updated"
            } else {
                "Original timetable restored locally"
            }
        }
    }

    fun updateSettings(settings: AssistantSettings) {
        viewModelScope.launch {
            repository.updateSettings(settings)
        }
    }

    fun updateWorkoutPlan(plan: WorkoutPlan) {
        viewModelScope.launch {
            repository.upsertWorkoutPlan(plan)
            _statusBanner.value = "Updated ${plan.groupName} workout plan"
        }
    }

    fun updateExercise(plan: WorkoutPlan, updatedExercise: WorkoutExercise) {
        val newList = plan.exercises.map { if (it.id == updatedExercise.id) updatedExercise else it }
        updateWorkoutPlan(plan.copy(exercises = newList))
    }

    fun saveTrackerRecord(record: CompletionRecord, message: String? = null) {
        viewModelScope.launch {
            val res = repository.upsertCompletionRecord(record)
            if (message != null) {
                _statusBanner.value = if (res.isSuccess) message else "$message (sync pending)"
            }
        }
    }

    fun previewSound(soundKey: String, volume: Float) {
        soundPlayer.play(soundKey = soundKey, volume = volume, looping = false, vibrate = false, useAlarmStream = false)
    }

    fun stopSoundPreview() {
        soundPlayer.stop()
    }

    fun previewVoice(text: String, settings: AssistantSettings) {
        voiceManager.speakOnce(
            text = text,
            speed = settings.ttsSpeed,
            pitch = settings.ttsPitch,
            voiceName = settings.ttsVoiceName
        )
    }

    fun previewActivityAlarm(activity: RoutineActivity, settings: AssistantSettings) {
        viewModelScope.launch {
            val dynamicSpeech = DefaultTimetableFactory.buildDynamicAnnouncement(
                activity,
                LocalDate.now().dayOfWeek
            )
            if (settings.soundEnabled) {
                soundPlayer.play(activity.soundKey, settings.alarmVolume, looping = false, vibrate = false, useAlarmStream = false)
                delay(2200L)
                soundPlayer.stop()
            }
            if (settings.ttsEnabled) {
                voiceManager.speakOnce(dynamicSpeech, settings.ttsSpeed, settings.ttsPitch, settings.ttsVoiceName)
            }
        }
    }

    fun canScheduleExactAlarms(): Boolean = alarmScheduler.canScheduleExactAlarms()

    fun rescheduleAllAlarms() {
        viewModelScope.launch {
            alarmScheduler.rescheduleAll(repository.getActivitiesOnce())
            _statusBanner.value = "All scheduled alarms refreshed"
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearAllHistory()
            _statusBanner.value = "Activity history cleared"
        }
    }

    suspend fun exportBackupJson(): String = repository.exportBackupJson()

    fun importBackupJson(json: String) {
        viewModelScope.launch {
            val result = repository.importBackupJson(json)
            _statusBanner.value = if (result.isSuccess) {
                "Backup imported successfully"
            } else {
                "Import failed: ${result.exceptionOrNull()?.localizedMessage ?: "Invalid JSON"}"
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        soundPlayer.stop()
        voiceManager.stop()
    }

    companion object {
        private const val TAG = "RoutineVM"

        fun findNextActivityAndCountdown(
            activities: List<RoutineActivity>,
            now: ZonedDateTime
        ): Pair<RoutineActivity?, Duration> {
            val enabledOrAll = activities.ifEmpty { return null to Duration.ZERO }
            var bestActivity: RoutineActivity? = null
            var bestTrigger: ZonedDateTime? = null

            for (act in enabledOrAll) {
                for (day in DayOfWeek.entries) {
                    if (WeekdayMask.includes(act.weekdaysMask, day)) {
                        val trigger = AlarmScheduler.computeNextTriggerTime(
                            act.startHour,
                            act.startMinute,
                            day,
                            now
                        )
                        if (bestTrigger == null || trigger.isBefore(bestTrigger)) {
                            bestTrigger = trigger
                            bestActivity = act
                        }
                    }
                }
            }
            val duration = if (bestTrigger != null) Duration.between(now, bestTrigger) else Duration.ZERO
            return bestActivity to duration
        }

        fun computeDailyStats(
            date: LocalDate,
            scheduledForDay: List<RoutineActivity>,
            completionsByKey: Map<String, CompletionRecord>,
            nowTime: LocalTime
        ): DailySummaryStats {
            val dateKey = date.toString()
            var completed = 0
            var missed = 0
            var incomplete = 0
            val nowMin = nowTime.hour * 60 + nowTime.minute
            val isToday = date == LocalDate.now()
            val isPastDay = date.isBefore(LocalDate.now())

            scheduledForDay.forEach { act ->
                val rec = completionsByKey["${dateKey}__${act.id}"]
                when (rec?.status) {
                    ActivityStatus.COMPLETED -> completed++
                    ActivityStatus.MISSED -> missed++
                    else -> {
                        val hasEndedToday = !act.crossesMidnight && act.endTotalMinutes < nowMin
                        if (isPastDay || (isToday && hasEndedToday && act.category != ActivityCategory.SLEEP)) {
                            missed++
                        } else {
                            incomplete++
                        }
                    }
                }
            }
            val total = scheduledForDay.size
            val percent = if (total == 0) 0 else ((completed * 100.0) / total).toInt().coerceIn(0, 100)
            return DailySummaryStats(
                date = date,
                totalScheduled = total,
                completedCount = completed,
                missedCount = missed,
                incompleteCount = incomplete,
                completionPercent = percent
            )
        }

        fun computeWeeklyStats(
            weekOffset: Int,
            activities: List<RoutineActivity>,
            completions: List<CompletionRecord>
        ): WeeklySummaryStats {
            val today = LocalDate.now()
            val currentSunday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
            val weekStart = currentSunday.plusWeeks(weekOffset.toLong())
            val weekDates = (0L..6L).map { weekStart.plusDays(it) }
            val weekDateKeys = weekDates.map { it.toString() }.toSet()
            val compMap = completions.associateBy { it.id }
            val weekRecords = completions.filter { it.dateKey in weekDateKeys }

            val totalStudyMin = weekRecords.sumOf { it.studyMinutes } +
                weekRecords.filter { r ->
                    r.studyMinutes == 0 && r.status == ActivityStatus.COMPLETED &&
                        (r.activityId.startsWith("study"))
                }.sumOf { r ->
                    activities.firstOrNull { it.id == r.activityId }?.durationMinutes() ?: 0
                }

            val totalWebMin = weekRecords.sumOf { it.webMinutes } +
                weekRecords.filter { r ->
                    r.webMinutes == 0 && r.status == ActivityStatus.COMPLETED &&
                        (r.activityId.startsWith("web"))
                }.sumOf { r ->
                    activities.firstOrNull { it.id == r.activityId }?.durationMinutes() ?: 0
                }

            val completedWorkouts = weekRecords.count {
                it.activityId == "sport" && it.status == ActivityStatus.COMPLETED
            }
            val quranDays = weekRecords.count {
                it.activityId == "quran" && it.status == ActivityStatus.COMPLETED
            }
            val ytVideos = weekRecords.filter { it.activityId == "youtube" }.sumOf { rec ->
                var c = 0
                if (rec.v1Uploaded) c++
                if (rec.v2Uploaded) c++
                if (c == 0 && rec.status == ActivityStatus.COMPLETED) 2 else c
            }
            val cyclingKm = weekRecords.filter { it.activityId == "cycling" }.sumOf { it.cyclingDistanceKm }
            val dailyChecks = weekRecords.count {
                it.activityId == "daily_check" && it.status == ActivityStatus.COMPLETED
            }

            val dailyPercents = weekDates.map { date ->
                val dayActs = activities.filter { WeekdayMask.includes(it.weekdaysMask, date.dayOfWeek) }
                val stats = computeDailyStats(date, dayActs, compMap, LocalTime.now())
                date.dayOfWeek to stats.completionPercent
            }

            return WeeklySummaryStats(
                weekStartDate = weekStart,
                totalStudyMinutes = totalStudyMin,
                totalWebMinutes = totalWebMin,
                completedWorkouts = completedWorkouts,
                quranDaysCompleted = quranDays,
                youtubeVideosUploaded = ytVideos,
                cyclingDistanceKm = cyclingKm,
                dailyCheckCompletedDays = dailyChecks,
                dailyCompletionPercentages = dailyPercents
            )
        }
    }
}

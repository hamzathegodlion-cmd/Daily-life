package com.example.data.remote

import androidx.annotation.Keep
import com.example.data.model.ActivityCategory
import com.example.data.model.ActivityStatus
import com.example.data.model.AssistantSettings
import com.example.data.model.CompletionRecord
import com.example.data.model.RoutineActivity
import com.example.data.model.ThemeMode
import com.example.data.model.WorkoutExercise
import com.example.data.model.WorkoutPlan
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class FirestoreUserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val quranWirdJuz: Long = 1L,
    val quranMurajaahPages: Long = 5L,
    val cyclingTargetKm: Double = 22.0,
    val youtubeDailyVideos: Long = 2L,
    val pushupGoal: Long = 100L,
    val ttsEnabled: Boolean = true,
    val ttsSpeed: Double = 1.0,
    val ttsPitch: Double = 1.0,
    val ttsVoiceName: String = "",
    val ttsRepetitions: Long = 2L,
    val ttsDelaySeconds: Long = 3L,
    val alarmVolume: Double = 0.9,
    val vibrationEnabled: Boolean = true,
    val soundEnabled: Boolean = true,
    val soundDurationSeconds: Long = 20L,
    val snoozeMinutes: Long = 5L,
    val themeMode: String = ThemeMode.DARK.name,
    val use24HourFormat: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toSettings(): AssistantSettings = AssistantSettings(
        displayName = displayName,
        quranWirdJuz = quranWirdJuz.toInt().coerceIn(1, 30),
        quranMurajaahPages = quranMurajaahPages.toInt().coerceIn(1, 604),
        cyclingTargetKm = cyclingTargetKm.coerceIn(0.5, 300.0),
        youtubeDailyVideos = youtubeDailyVideos.toInt().coerceIn(1, 20),
        pushupGoal = pushupGoal.toInt().coerceIn(1, 500),
        ttsEnabled = ttsEnabled,
        ttsSpeed = ttsSpeed.toFloat().coerceIn(0.5f, 2.0f),
        ttsPitch = ttsPitch.toFloat().coerceIn(0.5f, 2.0f),
        ttsVoiceName = ttsVoiceName,
        ttsRepetitions = ttsRepetitions.toInt().coerceIn(1, 10),
        ttsDelaySeconds = ttsDelaySeconds.toInt().coerceIn(1, 30),
        alarmVolume = alarmVolume.toFloat().coerceIn(0.0f, 1.0f),
        vibrationEnabled = vibrationEnabled,
        soundEnabled = soundEnabled,
        soundDurationSeconds = soundDurationSeconds.toInt().coerceIn(3, 120),
        snoozeMinutes = snoozeMinutes.toInt().coerceIn(1, 60),
        themeMode = ThemeMode.safeFrom(themeMode),
        use24HourFormat = use24HourFormat
    )
}

@Keep
@IgnoreExtraProperties
data class FirestoreActivity(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val description: String = "",
    val category: String = ActivityCategory.CUSTOM.name,
    val startHour: Long = 0L,
    val startMinute: Long = 0L,
    val endHour: Long = 0L,
    val endMinute: Long = 0L,
    val weekdaysMask: Long = 127L,
    val repeatWeekly: Boolean = true,
    val alarmEnabled: Boolean = true,
    val soundKey: String = "sound_gentle_wakeup",
    val announcementText: String = "",
    val sortOrder: Long = 0L,
    val defaultKey: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toDomain(): RoutineActivity = RoutineActivity(
        id = id,
        title = title,
        description = description,
        category = ActivityCategory.safeFrom(category),
        startHour = startHour.toInt().coerceIn(0, 23),
        startMinute = startMinute.toInt().coerceIn(0, 59),
        endHour = endHour.toInt().coerceIn(0, 23),
        endMinute = endMinute.toInt().coerceIn(0, 59),
        weekdaysMask = weekdaysMask.toInt().coerceIn(0, 127),
        repeatWeekly = repeatWeekly,
        alarmEnabled = alarmEnabled,
        soundKey = soundKey.ifBlank { "sound_gentle_wakeup" },
        announcementText = announcementText,
        sortOrder = sortOrder.toInt().coerceIn(0, 1000),
        defaultKey = defaultKey
    )
}

@Keep
@IgnoreExtraProperties
data class FirestoreCompletion(
    @DocumentId val id: String = "",
    val userId: String = "",
    val dateKey: String = "",
    val activityId: String = "",
    val status: String = ActivityStatus.INCOMPLETE.name,
    val completedAtMillis: Long = 0L,
    val notes: String = "",
    val quranWirdDone: Long = 0L,
    val quranMurajaahDone: Long = 0L,
    val cyclingDistanceKm: Double = 0.0,
    val cyclingDurationMin: Long = 0L,
    val sleepDurationMin: Long = 0L,
    val v1Edited: Boolean = false,
    val v1Uploaded: Boolean = false,
    val v2Edited: Boolean = false,
    val v2Uploaded: Boolean = false,
    val studyMinutes: Long = 0L,
    val webMinutes: Long = 0L,
    val workoutCompletedCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toDomain(): CompletionRecord = CompletionRecord(
        id = id,
        dateKey = dateKey,
        activityId = activityId,
        status = ActivityStatus.safeFrom(status),
        completedAtMillis = completedAtMillis,
        notes = notes,
        quranWirdDone = quranWirdDone.toInt().coerceIn(0, 30),
        quranMurajaahDone = quranMurajaahDone.toInt().coerceIn(0, 604),
        cyclingDistanceKm = cyclingDistanceKm.coerceIn(0.0, 500.0),
        cyclingDurationMin = cyclingDurationMin.toInt().coerceIn(0, 1440),
        sleepDurationMin = sleepDurationMin.toInt().coerceIn(0, 1440),
        v1Edited = v1Edited,
        v1Uploaded = v1Uploaded,
        v2Edited = v2Edited,
        v2Uploaded = v2Uploaded,
        studyMinutes = studyMinutes.toInt().coerceIn(0, 1440),
        webMinutes = webMinutes.toInt().coerceIn(0, 1440),
        workoutCompletedCount = workoutCompletedCount.toInt().coerceIn(0, 20)
    )
}

@Keep
@IgnoreExtraProperties
data class FirestoreWorkoutPlan(
    @DocumentId val id: String = "",
    val userId: String = "",
    val groupName: String = "",
    val focusArea: String = "",
    val weekdaysMask: Long = 0L,
    val restDay: Boolean = false,
    val exerciseItems: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toDomain(): WorkoutPlan = WorkoutPlan(
        id = id,
        groupName = groupName,
        focusArea = focusArea,
        weekdaysMask = weekdaysMask.toInt().coerceIn(0, 127),
        restDay = restDay,
        exercises = exerciseItems.mapNotNull { WorkoutExercise.deserialize(it) }
    )
}

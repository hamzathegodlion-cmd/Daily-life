package com.example.data.model

import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class ActivityCategory(val label: String) {
    WAKE_UP("Wake Up"),
    MOSQUE("Mosque / Fajr"),
    QURAN("Quran"),
    SPORT("Sport / Workout"),
    LEARNING("Learning"),
    CYCLING("Cycling"),
    KITAB("Mosque / Kitab"),
    YOUTUBE("YouTube"),
    STUDY("Studying"),
    WEB_DEV("Web Development"),
    DAILY_CHECK("Daily Checking"),
    SLEEP("Sleep"),
    CUSTOM("Custom");

    companion object {
        fun safeFrom(raw: String?): ActivityCategory =
            entries.firstOrNull { it.name == raw } ?: CUSTOM
    }
}

enum class ActivityStatus(val label: String) {
    COMPLETED("Completed"),
    INCOMPLETE("Incomplete"),
    MISSED("Missed");

    companion object {
        fun safeFrom(raw: String?): ActivityStatus =
            entries.firstOrNull { it.name == raw } ?: INCOMPLETE
    }
}

enum class ThemeMode(val label: String) {
    DARK("Deep Navy (Dark)"),
    LIGHT("Daylight (Light)"),
    SYSTEM("Follow System");

    companion object {
        fun safeFrom(raw: String?): ThemeMode =
            entries.firstOrNull { it.name == raw } ?: DARK
    }
}

enum class WorkoutGroupType(val title: String, val focus: String) {
    GROUP_A("Group A", "Biceps and Back"),
    GROUP_B("Group B", "Triceps and Chest"),
    REST_DAY("Rest Day", "Recovery & Weekly Preparation")
}

enum class AlarmSoundOption(
    val key: String,
    val displayName: String,
    val description: String
) {
    GENTLE_WAKEUP("sound_gentle_wakeup", "Gentle Wake-up", "Warm ascending dawn chime"),
    DIGITAL_ALARM("sound_digital_alarm", "Digital Alarm", "Crisp modern pulse"),
    SOFT_BELL("sound_soft_bell", "Soft Bell", "Resonant calm bell"),
    CLASSIC_ALARM("sound_classic_alarm", "Classic Alarm", "Clear double-strike alert"),
    MOTIVATIONAL("sound_motivational", "Motivational Tone", "Uplifting fanfare chime"),
    QURAN_TONE("sound_quran_tone", "Quran Reminder Tone", "Serene harmonic fifths"),
    SPORT_TONE("sound_sport_tone", "Sport Reminder Tone", "Energetic workout cue"),
    LEARNING_TONE("sound_learning_tone", "Learning Reminder Tone", "Focused crystalline chime"),
    SLEEP_TONE("sound_sleep_tone", "Sleep Reminder Tone", "Soothing evening rest chord");

    companion object {
        fun fromKey(key: String?): AlarmSoundOption =
            entries.firstOrNull { it.key == key } ?: GENTLE_WAKEUP
    }
}

object WeekdayMask {
    const val SUNDAY = 1 shl 0     // 1
    const val MONDAY = 1 shl 1     // 2
    const val TUESDAY = 1 shl 2    // 4
    const val WEDNESDAY = 1 shl 3  // 8
    const val THURSDAY = 1 shl 4   // 16
    const val FRIDAY = 1 shl 5     // 32
    const val SATURDAY = 1 shl 6   // 64
    const val ALL_DAYS = 127       // 1..64

    const val STUDY_SUN_TUE = SUNDAY or TUESDAY
    const val STUDY_MON_WED_THU = MONDAY or WEDNESDAY or THURSDAY
    const val WEB_FRI_SAT = FRIDAY or SATURDAY
    const val GROUP_A_DAYS = MONDAY or WEDNESDAY or FRIDAY
    const val GROUP_B_DAYS = TUESDAY or THURSDAY or SATURDAY

    fun bitFor(dayOfWeek: DayOfWeek): Int = when (dayOfWeek) {
        DayOfWeek.SUNDAY -> SUNDAY
        DayOfWeek.MONDAY -> MONDAY
        DayOfWeek.TUESDAY -> TUESDAY
        DayOfWeek.WEDNESDAY -> WEDNESDAY
        DayOfWeek.THURSDAY -> THURSDAY
        DayOfWeek.FRIDAY -> FRIDAY
        DayOfWeek.SATURDAY -> SATURDAY
    }

    fun includes(mask: Int, dayOfWeek: DayOfWeek): Boolean =
        (mask and bitFor(dayOfWeek)) != 0

    fun formatDays(mask: Int): String {
        if (mask == ALL_DAYS) return "Every day"
        val ordered = listOf(
            DayOfWeek.SUNDAY to "Sun",
            DayOfWeek.MONDAY to "Mon",
            DayOfWeek.TUESDAY to "Tue",
            DayOfWeek.WEDNESDAY to "Wed",
            DayOfWeek.THURSDAY to "Thu",
            DayOfWeek.FRIDAY to "Fri",
            DayOfWeek.SATURDAY to "Sat"
        )
        val active = ordered.filter { includes(mask, it.first) }.map { it.second }
        return if (active.isEmpty()) "No days" else active.joinToString(", ")
    }
}

data class RoutineActivity(
    val id: String,
    val title: String,
    val description: String,
    val category: ActivityCategory,
    val startHour: Int,
    val startMinute: Int,
    val endHour: Int,
    val endMinute: Int,
    val weekdaysMask: Int,
    val repeatWeekly: Boolean = true,
    val alarmEnabled: Boolean = true,
    val soundKey: String = AlarmSoundOption.GENTLE_WAKEUP.key,
    val announcementText: String,
    val sortOrder: Int = 0,
    val defaultKey: String = ""
) {
    val startTotalMinutes: Int get() = startHour * 60 + startMinute
    val endTotalMinutes: Int get() = endHour * 60 + endMinute
    val crossesMidnight: Boolean get() = endTotalMinutes <= startTotalMinutes

    fun durationMinutes(): Int {
        val diff = endTotalMinutes - startTotalMinutes
        return if (diff > 0) diff else (24 * 60 + diff)
    }

    fun formatTimeRange(use24Hour: Boolean = false): String {
        val start = formatTime(startHour, startMinute, use24Hour)
        val end = formatTime(endHour, endMinute, use24Hour)
        return if (start == end) start else "$start – $end"
    }

    fun formatStartTime(use24Hour: Boolean = false): String =
        formatTime(startHour, startMinute, use24Hour)

    fun isActiveAt(now: LocalTime): Boolean {
        val nowMin = now.hour * 60 + now.minute
        return if (!crossesMidnight) {
            nowMin in startTotalMinutes until endTotalMinutes
        } else {
            nowMin >= startTotalMinutes || nowMin < endTotalMinutes
        }
    }

    companion object {
        private val fmt12 = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)
        private val fmt24 = DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH)

        fun formatTime(hour: Int, minute: Int, use24Hour: Boolean): String {
            val time = LocalTime.of(hour.coerceIn(0, 23), minute.coerceIn(0, 59))
            return time.format(if (use24Hour) fmt24 else fmt12)
        }
    }
}

data class WorkoutExercise(
    val id: String,
    val name: String,
    val phase: String, // "FIRST_20" or "NEXT_20"
    val targetReps: Int,
    val targetSets: Int,
    val durationMinutes: Int,
    val description: String
) {
    fun serialize(): String =
        listOf(id, name, phase, targetReps.toString(), targetSets.toString(), durationMinutes.toString(), description.replace("|", "/"))
            .joinToString("|")

    companion object {
        fun deserialize(raw: String): WorkoutExercise? {
            val parts = raw.split("|")
            if (parts.size < 7) return null
            return WorkoutExercise(
                id = parts[0],
                name = parts[1],
                phase = parts[2],
                targetReps = parts[3].toIntOrNull() ?: 10,
                targetSets = parts[4].toIntOrNull() ?: 3,
                durationMinutes = parts[5].toIntOrNull() ?: 5,
                description = parts.drop(6).joinToString("|")
            )
        }
    }
}

data class WorkoutPlan(
    val id: String,
    val groupName: String,
    val focusArea: String,
    val weekdaysMask: Int,
    val restDay: Boolean,
    val exercises: List<WorkoutExercise>
)

data class CompletionRecord(
    val id: String, // yyyy-MM-dd__activityId
    val dateKey: String,
    val activityId: String,
    val status: ActivityStatus = ActivityStatus.INCOMPLETE,
    val completedAtMillis: Long = 0L,
    val notes: String = "",
    val quranWirdDone: Int = 0,
    val quranMurajaahDone: Int = 0,
    val cyclingDistanceKm: Double = 0.0,
    val cyclingDurationMin: Int = 0,
    val sleepDurationMin: Int = 0,
    val v1Edited: Boolean = false,
    val v1Uploaded: Boolean = false,
    val v2Edited: Boolean = false,
    val v2Uploaded: Boolean = false,
    val studyMinutes: Int = 0,
    val webMinutes: Int = 0,
    val workoutCompletedCount: Int = 0
)

data class AssistantSettings(
    val displayName: String = "",
    val quranWirdJuz: Int = 1,
    val quranMurajaahPages: Int = 5,
    val cyclingTargetKm: Double = 22.0,
    val youtubeDailyVideos: Int = 2,
    val pushupGoal: Int = 100,
    val ttsEnabled: Boolean = true,
    val ttsSpeed: Float = 1.0f,
    val ttsPitch: Float = 1.0f,
    val ttsVoiceName: String = "",
    val ttsRepetitions: Int = 2,
    val ttsDelaySeconds: Int = 3,
    val alarmVolume: Float = 0.9f,
    val vibrationEnabled: Boolean = true,
    val soundEnabled: Boolean = true,
    val soundDurationSeconds: Int = 20,
    val snoozeMinutes: Int = 5,
    val themeMode: ThemeMode = ThemeMode.DARK,
    val use24HourFormat: Boolean = false
)

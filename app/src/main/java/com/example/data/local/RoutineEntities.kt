package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.ActivityCategory
import com.example.data.model.ActivityStatus
import com.example.data.model.CompletionRecord
import com.example.data.model.RoutineActivity
import com.example.data.model.WorkoutExercise
import com.example.data.model.WorkoutPlan

@Entity(tableName = "routine_activities")
data class RoutineActivityEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val category: String,
    val startHour: Int,
    val startMinute: Int,
    val endHour: Int,
    val endMinute: Int,
    val weekdaysMask: Int,
    val repeatWeekly: Boolean,
    val alarmEnabled: Boolean,
    val soundKey: String,
    val announcementText: String,
    val sortOrder: Int,
    val defaultKey: String
) {
    fun toDomain(): RoutineActivity = RoutineActivity(
        id = id,
        title = title,
        description = description,
        category = ActivityCategory.safeFrom(category),
        startHour = startHour,
        startMinute = startMinute,
        endHour = endHour,
        endMinute = endMinute,
        weekdaysMask = weekdaysMask,
        repeatWeekly = repeatWeekly,
        alarmEnabled = alarmEnabled,
        soundKey = soundKey,
        announcementText = announcementText,
        sortOrder = sortOrder,
        defaultKey = defaultKey
    )

    companion object {
        fun fromDomain(model: RoutineActivity): RoutineActivityEntity = RoutineActivityEntity(
            id = model.id,
            title = model.title,
            description = model.description,
            category = model.category.name,
            startHour = model.startHour,
            startMinute = model.startMinute,
            endHour = model.endHour,
            endMinute = model.endMinute,
            weekdaysMask = model.weekdaysMask,
            repeatWeekly = model.repeatWeekly,
            alarmEnabled = model.alarmEnabled,
            soundKey = model.soundKey,
            announcementText = model.announcementText,
            sortOrder = model.sortOrder,
            defaultKey = model.defaultKey
        )
    }
}

@Entity(tableName = "completion_records")
data class CompletionRecordEntity(
    @PrimaryKey val id: String,
    val dateKey: String,
    val activityId: String,
    val status: String,
    val completedAtMillis: Long,
    val notes: String,
    val quranWirdDone: Int,
    val quranMurajaahDone: Int,
    val cyclingDistanceKm: Double,
    val cyclingDurationMin: Int,
    val sleepDurationMin: Int,
    val v1Edited: Boolean,
    val v1Uploaded: Boolean,
    val v2Edited: Boolean,
    val v2Uploaded: Boolean,
    val studyMinutes: Int,
    val webMinutes: Int,
    val workoutCompletedCount: Int
) {
    fun toDomain(): CompletionRecord = CompletionRecord(
        id = id,
        dateKey = dateKey,
        activityId = activityId,
        status = ActivityStatus.safeFrom(status),
        completedAtMillis = completedAtMillis,
        notes = notes,
        quranWirdDone = quranWirdDone,
        quranMurajaahDone = quranMurajaahDone,
        cyclingDistanceKm = cyclingDistanceKm,
        cyclingDurationMin = cyclingDurationMin,
        sleepDurationMin = sleepDurationMin,
        v1Edited = v1Edited,
        v1Uploaded = v1Uploaded,
        v2Edited = v2Edited,
        v2Uploaded = v2Uploaded,
        studyMinutes = studyMinutes,
        webMinutes = webMinutes,
        workoutCompletedCount = workoutCompletedCount
    )

    companion object {
        fun fromDomain(model: CompletionRecord): CompletionRecordEntity = CompletionRecordEntity(
            id = model.id,
            dateKey = model.dateKey,
            activityId = model.activityId,
            status = model.status.name,
            completedAtMillis = model.completedAtMillis,
            notes = model.notes,
            quranWirdDone = model.quranWirdDone,
            quranMurajaahDone = model.quranMurajaahDone,
            cyclingDistanceKm = model.cyclingDistanceKm,
            cyclingDurationMin = model.cyclingDurationMin,
            sleepDurationMin = model.sleepDurationMin,
            v1Edited = model.v1Edited,
            v1Uploaded = model.v1Uploaded,
            v2Edited = model.v2Edited,
            v2Uploaded = model.v2Uploaded,
            studyMinutes = model.studyMinutes,
            webMinutes = model.webMinutes,
            workoutCompletedCount = model.workoutCompletedCount
        )
    }
}

@Entity(tableName = "workout_plans")
data class WorkoutPlanEntity(
    @PrimaryKey val id: String,
    val groupName: String,
    val focusArea: String,
    val weekdaysMask: Int,
    val restDay: Boolean,
    val exercisesSerialized: String
) {
    fun toDomain(): WorkoutPlan {
        val list = if (exercisesSerialized.isBlank()) {
            emptyList()
        } else {
            exercisesSerialized.split("\n").mapNotNull { WorkoutExercise.deserialize(it) }
        }
        return WorkoutPlan(
            id = id,
            groupName = groupName,
            focusArea = focusArea,
            weekdaysMask = weekdaysMask,
            restDay = restDay,
            exercises = list
        )
    }

    companion object {
        fun fromDomain(model: WorkoutPlan): WorkoutPlanEntity = WorkoutPlanEntity(
            id = model.id,
            groupName = model.groupName,
            focusArea = model.focusArea,
            weekdaysMask = model.weekdaysMask,
            restDay = model.restDay,
            exercisesSerialized = model.exercises.joinToString("\n") { it.serialize() }
        )
    }
}

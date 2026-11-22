package com.example.data.repository

import com.example.alarm.AlarmScheduler
import com.example.data.local.CompletionRecordEntity
import com.example.data.local.RoutineActivityEntity
import com.example.data.local.RoutineDatabase
import com.example.data.local.SettingsDataStore
import com.example.data.local.WorkoutPlanEntity
import com.example.data.model.ActivityCategory
import com.example.data.model.ActivityStatus
import com.example.data.model.AssistantSettings
import com.example.data.model.CompletionRecord
import com.example.data.model.DefaultTimetableFactory
import com.example.data.model.RoutineActivity
import com.example.data.model.ThemeMode
import com.example.data.model.WorkoutPlan
import com.example.data.remote.FirestoreSyncRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.util.UUID

class RoutineRepository(
    private val database: RoutineDatabase,
    private val settingsDataStore: SettingsDataStore,
    private val firestoreSync: FirestoreSyncRepository,
    private val alarmScheduler: AlarmScheduler,
    private val auth: FirebaseAuth
) {
    private val activityDao = database.activityDao()
    private val completionDao = database.completionDao()
    private val workoutDao = database.workoutPlanDao()

    val activitiesFlow: Flow<List<RoutineActivity>> =
        activityDao.observeAllActivities().map { list -> list.map { it.toDomain() } }

    val completionsFlow: Flow<List<CompletionRecord>> =
        completionDao.observeAllRecords().map { list -> list.map { it.toDomain() } }

    val workoutPlansFlow: Flow<List<WorkoutPlan>> =
        workoutDao.observeAllPlans().map { list -> list.map { it.toDomain() } }

    val settingsFlow: Flow<AssistantSettings> = settingsDataStore.settingsFlow

    private fun currentUid(): String? = auth.currentUser?.uid?.takeIf { it.isNotBlank() }

    suspend fun ensureSeededLocally() {
        val currentActs = activityDao.getAllActivitiesOnce()
        if (currentActs.isEmpty()) {
            val defaults = DefaultTimetableFactory.defaultActivities()
            activityDao.upsertAll(defaults.map { RoutineActivityEntity.fromDomain(it) })
            alarmScheduler.rescheduleAll(defaults)
        }
        val currentPlans = workoutDao.getAllPlansOnce()
        if (currentPlans.isEmpty()) {
            val settings = settingsDataStore.getSettingsOnce()
            val plans = DefaultTimetableFactory.defaultWorkoutPlans(settings.pushupGoal)
            workoutDao.upsertAll(plans.map { WorkoutPlanEntity.fromDomain(it) })
        }
    }

    suspend fun getActivitiesOnce(): List<RoutineActivity> =
        activityDao.getAllActivitiesOnce().map { it.toDomain() }

    suspend fun getActivityById(id: String): RoutineActivity? =
        activityDao.getActivityById(id)?.toDomain()

    suspend fun upsertActivity(activity: RoutineActivity): Result<Unit> {
        val cleanId = activity.id.ifBlank { "act_${UUID.randomUUID().toString().replace("-", "").take(16)}" }
        val normalized = activity.copy(id = cleanId)
        activityDao.upsert(RoutineActivityEntity.fromDomain(normalized))
        alarmScheduler.rescheduleAll(getActivitiesOnce())
        val uid = currentUid()
        return if (uid != null) {
            firestoreSync.upsertActivity(uid, normalized)
        } else {
            Result.success(Unit)
        }
    }

    suspend fun deleteActivity(activityId: String): Result<Unit> {
        alarmScheduler.cancelActivityAlarms(activityId)
        activityDao.deleteById(activityId)
        val uid = currentUid()
        return if (uid != null) {
            firestoreSync.deleteActivity(uid, activityId)
        } else {
            Result.success(Unit)
        }
    }

    suspend fun restoreOriginalTimetable(): Result<Unit> {
        val settings = settingsDataStore.getSettingsOnce()
        val defaultActs = DefaultTimetableFactory.defaultActivities()
        val defaultPlans = DefaultTimetableFactory.defaultWorkoutPlans(settings.pushupGoal)

        activityDao.clearAll()
        activityDao.upsertAll(defaultActs.map { RoutineActivityEntity.fromDomain(it) })
        workoutDao.clearAll()
        workoutDao.upsertAll(defaultPlans.map { WorkoutPlanEntity.fromDomain(it) })
        alarmScheduler.rescheduleAll(defaultActs)

        val uid = currentUid() ?: return Result.success(Unit)
        val actResult = firestoreSync.replaceAllActivities(uid, defaultActs)
        if (actResult.isFailure) return actResult
        for (plan in defaultPlans) {
            val r = firestoreSync.upsertWorkoutPlan(uid, plan)
            if (r.isFailure) return r
        }
        return Result.success(Unit)
    }

    suspend fun markActivityStatus(
        activityId: String,
        dateKey: String = LocalDate.now().toString(),
        status: ActivityStatus,
        notes: String? = null
    ): Result<Unit> {
        val recordId = "${dateKey}__${activityId}"
        val existing = completionDao.getRecordById(recordId)?.toDomain()
        val updated = (existing ?: CompletionRecord(
            id = recordId,
            dateKey = dateKey,
            activityId = activityId
        )).copy(
            status = status,
            completedAtMillis = if (status == ActivityStatus.COMPLETED) System.currentTimeMillis() else 0L,
            notes = notes ?: existing?.notes ?: ""
        )
        return upsertCompletionRecord(updated)
    }

    suspend fun upsertCompletionRecord(record: CompletionRecord): Result<Unit> {
        completionDao.upsert(CompletionRecordEntity.fromDomain(record))
        val uid = currentUid()
        return if (uid != null) {
            firestoreSync.upsertCompletion(uid, record)
        } else {
            Result.success(Unit)
        }
    }

    suspend fun upsertWorkoutPlan(plan: WorkoutPlan): Result<Unit> {
        workoutDao.upsert(WorkoutPlanEntity.fromDomain(plan))
        val uid = currentUid()
        return if (uid != null) {
            firestoreSync.upsertWorkoutPlan(uid, plan)
        } else {
            Result.success(Unit)
        }
    }

    suspend fun updateSettings(settings: AssistantSettings): Result<Unit> {
        settingsDataStore.updateSettings(settings)
        val uid = currentUid()
        return if (uid != null) {
            firestoreSync.upsertUserProfile(uid, settings)
        } else {
            Result.success(Unit)
        }
    }

    suspend fun clearAllHistory(): Result<Unit> {
        completionDao.clearAll()
        val uid = currentUid()
        return if (uid != null) {
            firestoreSync.clearCompletions(uid)
        } else {
            Result.success(Unit)
        }
    }

    /**
     * Real-time Firestore listener stream for the signed-in user.
     * Merges remote updates into Room/DataStore and seeds Firestore if remote account is empty.
     */
    fun observeRemoteSync(userId: String): Flow<Unit> {
        return combine(
            firestoreSync.observeUserProfile(userId),
            firestoreSync.observeActivities(userId),
            firestoreSync.observeCompletions(userId),
            firestoreSync.observeWorkoutPlans(userId)
        ) { remoteSettings, remoteActivities, remoteCompletions, remotePlans ->
            if (remoteSettings == null) {
                val localSettings = settingsDataStore.getSettingsOnce()
                firestoreSync.upsertUserProfile(userId, localSettings)
            } else {
                settingsDataStore.updateSettings(remoteSettings)
            }

            if (remoteActivities.isEmpty()) {
                ensureSeededLocally()
                val localActs = activityDao.getAllActivitiesOnce().map { it.toDomain() }
                for (act in localActs) {
                    firestoreSync.upsertActivity(userId, act)
                }
            } else {
                activityDao.clearAll()
                activityDao.upsertAll(remoteActivities.map { RoutineActivityEntity.fromDomain(it) })
                alarmScheduler.rescheduleAll(remoteActivities)
            }

            if (remoteCompletions.isNotEmpty()) {
                completionDao.upsertAll(remoteCompletions.map { CompletionRecordEntity.fromDomain(it) })
            }

            if (remotePlans.isEmpty()) {
                ensureSeededLocally()
                val localPlans = workoutDao.getAllPlansOnce().map { it.toDomain() }
                for (plan in localPlans) {
                    firestoreSync.upsertWorkoutPlan(userId, plan)
                }
            } else {
                workoutDao.upsertAll(remotePlans.map { WorkoutPlanEntity.fromDomain(it) })
            }
        }
    }

    suspend fun exportBackupJson(): String {
        val settings = settingsDataStore.getSettingsOnce()
        val activities = activityDao.getAllActivitiesOnce().map { it.toDomain() }
        val completions = completionDao.getAllRecordsOnce().map { it.toDomain() }
        val plans = workoutDao.getAllPlansOnce().map { it.toDomain() }

        val root = JSONObject()
        root.put("version", 1)
        root.put("exportedAtMillis", System.currentTimeMillis())

        val settingsObj = JSONObject().apply {
            put("displayName", settings.displayName)
            put("quranWirdJuz", settings.quranWirdJuz)
            put("quranMurajaahPages", settings.quranMurajaahPages)
            put("cyclingTargetKm", settings.cyclingTargetKm)
            put("youtubeDailyVideos", settings.youtubeDailyVideos)
            put("pushupGoal", settings.pushupGoal)
            put("ttsEnabled", settings.ttsEnabled)
            put("ttsSpeed", settings.ttsSpeed.toDouble())
            put("ttsPitch", settings.ttsPitch.toDouble())
            put("ttsVoiceName", settings.ttsVoiceName)
            put("ttsRepetitions", settings.ttsRepetitions)
            put("ttsDelaySeconds", settings.ttsDelaySeconds)
            put("alarmVolume", settings.alarmVolume.toDouble())
            put("vibrationEnabled", settings.vibrationEnabled)
            put("soundEnabled", settings.soundEnabled)
            put("soundDurationSeconds", settings.soundDurationSeconds)
            put("snoozeMinutes", settings.snoozeMinutes)
            put("themeMode", settings.themeMode.name)
            put("use24HourFormat", settings.use24HourFormat)
        }
        root.put("settings", settingsObj)

        val actsArr = JSONArray()
        activities.forEach { a ->
            actsArr.put(JSONObject().apply {
                put("id", a.id)
                put("title", a.title)
                put("description", a.description)
                put("category", a.category.name)
                put("startHour", a.startHour)
                put("startMinute", a.startMinute)
                put("endHour", a.endHour)
                put("endMinute", a.endMinute)
                put("weekdaysMask", a.weekdaysMask)
                put("repeatWeekly", a.repeatWeekly)
                put("alarmEnabled", a.alarmEnabled)
                put("soundKey", a.soundKey)
                put("announcementText", a.announcementText)
                put("sortOrder", a.sortOrder)
                put("defaultKey", a.defaultKey)
            })
        }
        root.put("activities", actsArr)

        val compArr = JSONArray()
        completions.forEach { c ->
            compArr.put(JSONObject().apply {
                put("id", c.id)
                put("dateKey", c.dateKey)
                put("activityId", c.activityId)
                put("status", c.status.name)
                put("completedAtMillis", c.completedAtMillis)
                put("notes", c.notes)
                put("quranWirdDone", c.quranWirdDone)
                put("quranMurajaahDone", c.quranMurajaahDone)
                put("cyclingDistanceKm", c.cyclingDistanceKm)
                put("cyclingDurationMin", c.cyclingDurationMin)
                put("sleepDurationMin", c.sleepDurationMin)
                put("v1Edited", c.v1Edited)
                put("v1Uploaded", c.v1Uploaded)
                put("v2Edited", c.v2Edited)
                put("v2Uploaded", c.v2Uploaded)
                put("studyMinutes", c.studyMinutes)
                put("webMinutes", c.webMinutes)
                put("workoutCompletedCount", c.workoutCompletedCount)
            })
        }
        root.put("completions", compArr)

        val plansArr = JSONArray()
        plans.forEach { p ->
            plansArr.put(JSONObject().apply {
                put("id", p.id)
                put("groupName", p.groupName)
                put("focusArea", p.focusArea)
                put("weekdaysMask", p.weekdaysMask)
                put("restDay", p.restDay)
                put("exercises", JSONArray(p.exercises.map { it.serialize() }))
            })
        }
        root.put("workoutPlans", plansArr)

        return root.toString(2)
    }

    suspend fun importBackupJson(rawJson: String): Result<Unit> {
        return try {
            val root = JSONObject(rawJson)
            root.optJSONObject("settings")?.let { s ->
                val settings = AssistantSettings(
                    displayName = s.optString("displayName", ""),
                    quranWirdJuz = s.optInt("quranWirdJuz", 1),
                    quranMurajaahPages = s.optInt("quranMurajaahPages", 5),
                    cyclingTargetKm = s.optDouble("cyclingTargetKm", 22.0),
                    youtubeDailyVideos = s.optInt("youtubeDailyVideos", 2),
                    pushupGoal = s.optInt("pushupGoal", 100),
                    ttsEnabled = s.optBoolean("ttsEnabled", true),
                    ttsSpeed = s.optDouble("ttsSpeed", 1.0).toFloat(),
                    ttsPitch = s.optDouble("ttsPitch", 1.0).toFloat(),
                    ttsVoiceName = s.optString("ttsVoiceName", ""),
                    ttsRepetitions = s.optInt("ttsRepetitions", 2),
                    ttsDelaySeconds = s.optInt("ttsDelaySeconds", 3),
                    alarmVolume = s.optDouble("alarmVolume", 0.9).toFloat(),
                    vibrationEnabled = s.optBoolean("vibrationEnabled", true),
                    soundEnabled = s.optBoolean("soundEnabled", true),
                    soundDurationSeconds = s.optInt("soundDurationSeconds", 20),
                    snoozeMinutes = s.optInt("snoozeMinutes", 5),
                    themeMode = ThemeMode.safeFrom(s.optString("themeMode")),
                    use24HourFormat = s.optBoolean("use24HourFormat", false)
                )
                updateSettings(settings)
            }

            val actsArr = root.optJSONArray("activities")
            if (actsArr != null) {
                val importedActs = mutableListOf<RoutineActivity>()
                for (i in 0 until actsArr.length()) {
                    val o = actsArr.getJSONObject(i)
                    importedActs.add(
                        RoutineActivity(
                            id = o.optString("id").ifBlank { "act_$i" },
                            title = o.optString("title", "Activity"),
                            description = o.optString("description", ""),
                            category = ActivityCategory.safeFrom(o.optString("category")),
                            startHour = o.optInt("startHour", 8),
                            startMinute = o.optInt("startMinute", 0),
                            endHour = o.optInt("endHour", 9),
                            endMinute = o.optInt("endMinute", 0),
                            weekdaysMask = o.optInt("weekdaysMask", 127),
                            repeatWeekly = o.optBoolean("repeatWeekly", true),
                            alarmEnabled = o.optBoolean("alarmEnabled", true),
                            soundKey = o.optString("soundKey", "sound_gentle_wakeup"),
                            announcementText = o.optString("announcementText", ""),
                            sortOrder = o.optInt("sortOrder", i),
                            defaultKey = o.optString("defaultKey", "")
                        )
                    )
                }
                if (importedActs.isNotEmpty()) {
                    activityDao.clearAll()
                    activityDao.upsertAll(importedActs.map { RoutineActivityEntity.fromDomain(it) })
                    alarmScheduler.rescheduleAll(importedActs)
                    currentUid()?.let { firestoreSync.replaceAllActivities(it, importedActs) }
                }
            }

            val compArr = root.optJSONArray("completions")
            if (compArr != null) {
                val importedComps = mutableListOf<CompletionRecord>()
                for (i in 0 until compArr.length()) {
                    val o = compArr.getJSONObject(i)
                    val rec = CompletionRecord(
                        id = o.optString("id"),
                        dateKey = o.optString("dateKey"),
                        activityId = o.optString("activityId"),
                        status = ActivityStatus.safeFrom(o.optString("status")),
                        completedAtMillis = o.optLong("completedAtMillis", 0L),
                        notes = o.optString("notes", ""),
                        quranWirdDone = o.optInt("quranWirdDone", 0),
                        quranMurajaahDone = o.optInt("quranMurajaahDone", 0),
                        cyclingDistanceKm = o.optDouble("cyclingDistanceKm", 0.0),
                        cyclingDurationMin = o.optInt("cyclingDurationMin", 0),
                        sleepDurationMin = o.optInt("sleepDurationMin", 0),
                        v1Edited = o.optBoolean("v1Edited", false),
                        v1Uploaded = o.optBoolean("v1Uploaded", false),
                        v2Edited = o.optBoolean("v2Edited", false),
                        v2Uploaded = o.optBoolean("v2Uploaded", false),
                        studyMinutes = o.optInt("studyMinutes", 0),
                        webMinutes = o.optInt("webMinutes", 0),
                        workoutCompletedCount = o.optInt("workoutCompletedCount", 0)
                    )
                    if (rec.id.isNotBlank() && rec.dateKey.length == 10) {
                        importedComps.add(rec)
                        currentUid()?.let { firestoreSync.upsertCompletion(it, rec) }
                    }
                }
                if (importedComps.isNotEmpty()) {
                    completionDao.upsertAll(importedComps.map { CompletionRecordEntity.fromDomain(it) })
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

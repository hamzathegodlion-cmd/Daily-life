package com.example.data.remote

import android.content.Context
import com.example.R
import com.example.data.model.AssistantSettings
import com.example.data.model.CompletionRecord
import com.example.data.model.RoutineActivity
import com.example.data.model.WorkoutPlan
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class FirestoreSyncRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun isValidDocId(id: String): Boolean =
        id.isNotBlank() && id.length <= 128 && id.matches(Regex("^[a-zA-Z0-9_\\-]+$"))

    fun observeUserProfile(userId: String): Flow<AssistantSettings?> = flow {
        require(userId.isNotBlank()) { "User ID must not be blank" }
        val path = "users/$userId"
        emitAll(
            db.collection("users").document(userId)
                .snapshots()
                .map { snapshot ->
                    if (!snapshot.exists()) null
                    else snapshot.toObject(
                        FirestoreUserProfile::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )?.toSettings()
                }
                .catch { error ->
                    handleFlowError(error, OperationType.GET, path, userId)
                    throw error
                }
        )
    }

    fun observeActivities(userId: String): Flow<List<RoutineActivity>> = flow {
        require(userId.isNotBlank()) { "User ID must not be blank" }
        val path = "users/$userId/activities"
        emitAll(
            db.collection("users").document(userId).collection("activities")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        FirestoreActivity::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).map { it.toDomain() }
                        .sortedWith(compareBy({ it.sortOrder }, { it.startHour }, { it.startMinute }))
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, path, userId)
                    throw error
                }
        )
    }

    fun observeCompletions(userId: String): Flow<List<CompletionRecord>> = flow {
        require(userId.isNotBlank()) { "User ID must not be blank" }
        val path = "users/$userId/completions"
        emitAll(
            db.collection("users").document(userId).collection("completions")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        FirestoreCompletion::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).map { it.toDomain() }
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, path, userId)
                    throw error
                }
        )
    }

    fun observeWorkoutPlans(userId: String): Flow<List<WorkoutPlan>> = flow {
        require(userId.isNotBlank()) { "User ID must not be blank" }
        val path = "users/$userId/workoutPlans"
        emitAll(
            db.collection("users").document(userId).collection("workoutPlans")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        FirestoreWorkoutPlan::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).map { it.toDomain() }
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, path, userId)
                    throw error
                }
        )
    }

    private fun handleFlowError(error: Throwable, op: OperationType, path: String, expectedUid: String) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == expectedUid) {
            handleFirestoreError(exceptionToLog, op, path)
        }
    }

    suspend fun upsertUserProfile(userId: String, settings: AssistantSettings): Result<Unit> {
        var path = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user changed; refusing cross-user write." }
            require(isValidDocId(uid)) { "Invalid user ID" }
            val docRef = db.collection("users").document(uid)
            path = docRef.path
            val exists = docRef.get().await().exists()
            val mutableFields = mapOf(
                "displayName" to settings.displayName.take(100),
                "quranWirdJuz" to settings.quranWirdJuz.coerceIn(1, 30).toLong(),
                "quranMurajaahPages" to settings.quranMurajaahPages.coerceIn(1, 604).toLong(),
                "cyclingTargetKm" to settings.cyclingTargetKm.coerceIn(0.5, 300.0),
                "youtubeDailyVideos" to settings.youtubeDailyVideos.coerceIn(1, 20).toLong(),
                "pushupGoal" to settings.pushupGoal.coerceIn(1, 500).toLong(),
                "ttsEnabled" to settings.ttsEnabled,
                "ttsSpeed" to settings.ttsSpeed.toDouble().coerceIn(0.5, 2.0),
                "ttsPitch" to settings.ttsPitch.toDouble().coerceIn(0.5, 2.0),
                "ttsVoiceName" to settings.ttsVoiceName.take(120),
                "ttsRepetitions" to settings.ttsRepetitions.coerceIn(1, 10).toLong(),
                "ttsDelaySeconds" to settings.ttsDelaySeconds.coerceIn(1, 30).toLong(),
                "alarmVolume" to settings.alarmVolume.toDouble().coerceIn(0.0, 1.0),
                "vibrationEnabled" to settings.vibrationEnabled,
                "soundEnabled" to settings.soundEnabled,
                "soundDurationSeconds" to settings.soundDurationSeconds.coerceIn(3, 120).toLong(),
                "snoozeMinutes" to settings.snoozeMinutes.coerceIn(1, 60).toLong(),
                "themeMode" to settings.themeMode.name,
                "use24HourFormat" to settings.use24HourFormat,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (exists) {
                docRef.update(mutableFields).await()
            } else {
                val createPayload = mutableFields + mapOf(
                    "userId" to uid,
                    "createdAt" to FieldValue.serverTimestamp()
                )
                docRef.set(createPayload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun upsertActivity(userId: String, activity: RoutineActivity): Result<Unit> {
        var path = "users/$userId/activities/${activity.id}"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user changed; refusing cross-user write." }
            require(isValidDocId(activity.id)) { "Activity ID contains invalid characters" }
            val docRef = db.collection("users").document(uid).collection("activities").document(activity.id)
            path = docRef.path
            val exists = docRef.get().await().exists()
            val mutableFields = mapOf(
                "title" to activity.title.trim().ifBlank { "Routine Activity" }.take(120),
                "description" to activity.description.take(500),
                "category" to activity.category.name,
                "startHour" to activity.startHour.coerceIn(0, 23).toLong(),
                "startMinute" to activity.startMinute.coerceIn(0, 59).toLong(),
                "endHour" to activity.endHour.coerceIn(0, 23).toLong(),
                "endMinute" to activity.endMinute.coerceIn(0, 59).toLong(),
                "weekdaysMask" to activity.weekdaysMask.coerceIn(0, 127).toLong(),
                "repeatWeekly" to activity.repeatWeekly,
                "alarmEnabled" to activity.alarmEnabled,
                "soundKey" to activity.soundKey.ifBlank { "sound_gentle_wakeup" }.take(60),
                "announcementText" to activity.announcementText.take(600),
                "sortOrder" to activity.sortOrder.coerceIn(0, 1000).toLong(),
                "defaultKey" to activity.defaultKey.take(80),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (exists) {
                docRef.update(mutableFields).await()
            } else {
                val createPayload = mutableFields + mapOf(
                    "userId" to uid,
                    "createdAt" to FieldValue.serverTimestamp()
                )
                docRef.set(createPayload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteActivity(userId: String, activityId: String): Result<Unit> {
        val path = "users/$userId/activities/$activityId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user changed; refusing cross-user delete." }
            require(isValidDocId(activityId)) { "Invalid activity ID" }
            db.collection("users").document(uid).collection("activities").document(activityId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun upsertCompletion(userId: String, record: CompletionRecord): Result<Unit> {
        var path = "users/$userId/completions/${record.id}"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user changed; refusing cross-user write." }
            require(isValidDocId(record.id)) { "Record ID contains invalid characters" }
            require(record.dateKey.length == 10) { "dateKey must be yyyy-MM-dd" }
            val docRef = db.collection("users").document(uid).collection("completions").document(record.id)
            path = docRef.path
            val exists = docRef.get().await().exists()
            val mutableFields = mapOf(
                "dateKey" to record.dateKey,
                "activityId" to record.activityId.take(128),
                "status" to record.status.name,
                "completedAtMillis" to record.completedAtMillis.coerceAtLeast(0L),
                "notes" to record.notes.take(1000),
                "quranWirdDone" to record.quranWirdDone.coerceIn(0, 30).toLong(),
                "quranMurajaahDone" to record.quranMurajaahDone.coerceIn(0, 604).toLong(),
                "cyclingDistanceKm" to record.cyclingDistanceKm.coerceIn(0.0, 500.0),
                "cyclingDurationMin" to record.cyclingDurationMin.coerceIn(0, 1440).toLong(),
                "sleepDurationMin" to record.sleepDurationMin.coerceIn(0, 1440).toLong(),
                "v1Edited" to record.v1Edited,
                "v1Uploaded" to record.v1Uploaded,
                "v2Edited" to record.v2Edited,
                "v2Uploaded" to record.v2Uploaded,
                "studyMinutes" to record.studyMinutes.coerceIn(0, 1440).toLong(),
                "webMinutes" to record.webMinutes.coerceIn(0, 1440).toLong(),
                "workoutCompletedCount" to record.workoutCompletedCount.coerceIn(0, 20).toLong(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (exists) {
                docRef.update(mutableFields).await()
            } else {
                val createPayload = mutableFields + mapOf(
                    "userId" to uid,
                    "createdAt" to FieldValue.serverTimestamp()
                )
                docRef.set(createPayload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun upsertWorkoutPlan(userId: String, plan: WorkoutPlan): Result<Unit> {
        var path = "users/$userId/workoutPlans/${plan.id}"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user changed; refusing cross-user write." }
            require(isValidDocId(plan.id)) { "Workout plan ID contains invalid characters" }
            val docRef = db.collection("users").document(uid).collection("workoutPlans").document(plan.id)
            path = docRef.path
            val exists = docRef.get().await().exists()
            val serializedList = plan.exercises.take(20).map { it.serialize() }
            val mutableFields = mapOf(
                "groupName" to plan.groupName.trim().ifBlank { "Workout" }.take(60),
                "focusArea" to plan.focusArea.trim().ifBlank { "General" }.take(120),
                "weekdaysMask" to plan.weekdaysMask.coerceIn(0, 127).toLong(),
                "restDay" to plan.restDay,
                "exerciseItems" to serializedList,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (exists) {
                docRef.update(mutableFields).await()
            } else {
                val createPayload = mutableFields + mapOf(
                    "userId" to uid,
                    "createdAt" to FieldValue.serverTimestamp()
                )
                docRef.set(createPayload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun clearCompletions(userId: String): Result<Unit> {
        val path = "users/$userId/completions"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user changed; refusing cross-user delete." }
            val snap = db.collection("users").document(uid).collection("completions").get().await()
            for (doc in snap.documents) {
                doc.reference.delete().await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun replaceAllActivities(userId: String, activities: List<RoutineActivity>): Result<Unit> {
        val path = "users/$userId/activities"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user changed; refusing cross-user sync." }
            val col = db.collection("users").document(uid).collection("activities")
            val existingDocs = col.get().await().documents
            val targetIds = activities.map { it.id }.toSet()
            for (doc in existingDocs) {
                if (doc.id !in targetIds) {
                    doc.reference.delete().await()
                }
            }
            for (act in activities) {
                upsertActivity(uid, act).getOrThrow()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }
}

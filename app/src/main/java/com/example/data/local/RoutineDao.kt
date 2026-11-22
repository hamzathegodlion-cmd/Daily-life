package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RoutineActivityDao {
    @Query("SELECT * FROM routine_activities ORDER BY sortOrder ASC, startHour ASC, startMinute ASC")
    fun observeAllActivities(): Flow<List<RoutineActivityEntity>>

    @Query("SELECT * FROM routine_activities ORDER BY sortOrder ASC, startHour ASC, startMinute ASC")
    suspend fun getAllActivitiesOnce(): List<RoutineActivityEntity>

    @Query("SELECT * FROM routine_activities WHERE id = :id LIMIT 1")
    suspend fun getActivityById(id: String): RoutineActivityEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(activities: List<RoutineActivityEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(activity: RoutineActivityEntity)

    @Query("DELETE FROM routine_activities WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM routine_activities")
    suspend fun clearAll()
}

@Dao
interface CompletionRecordDao {
    @Query("SELECT * FROM completion_records ORDER BY dateKey DESC")
    fun observeAllRecords(): Flow<List<CompletionRecordEntity>>

    @Query("SELECT * FROM completion_records WHERE dateKey = :dateKey")
    fun observeRecordsForDate(dateKey: String): Flow<List<CompletionRecordEntity>>

    @Query("SELECT * FROM completion_records WHERE id = :id LIMIT 1")
    suspend fun getRecordById(id: String): CompletionRecordEntity?

    @Query("SELECT * FROM completion_records ORDER BY dateKey DESC")
    suspend fun getAllRecordsOnce(): List<CompletionRecordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(record: CompletionRecordEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(records: List<CompletionRecordEntity>)

    @Query("DELETE FROM completion_records")
    suspend fun clearAll()
}

@Dao
interface WorkoutPlanDao {
    @Query("SELECT * FROM workout_plans ORDER BY id ASC")
    fun observeAllPlans(): Flow<List<WorkoutPlanEntity>>

    @Query("SELECT * FROM workout_plans ORDER BY id ASC")
    suspend fun getAllPlansOnce(): List<WorkoutPlanEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(plan: WorkoutPlanEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(plans: List<WorkoutPlanEntity>)

    @Query("DELETE FROM workout_plans")
    suspend fun clearAll()
}

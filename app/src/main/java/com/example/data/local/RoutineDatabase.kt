package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        RoutineActivityEntity::class,
        CompletionRecordEntity::class,
        WorkoutPlanEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class RoutineDatabase : RoomDatabase() {
    abstract fun activityDao(): RoutineActivityDao
    abstract fun completionDao(): CompletionRecordDao
    abstract fun workoutPlanDao(): WorkoutPlanDao

    companion object {
        @Volatile
        private var INSTANCE: RoutineDatabase? = null

        fun getInstance(context: Context): RoutineDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    RoutineDatabase::class.java,
                    "my_daily_life_assistant.db"
                ).fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}

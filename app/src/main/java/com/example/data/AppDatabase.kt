package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.WorkoutDao
import com.example.data.model.PlanExerciseEntity
import com.example.data.model.SetLogEntity
import com.example.data.model.WorkoutSessionEntity

@Database(
    entities = [
        PlanExerciseEntity::class,
        WorkoutSessionEntity::class,
        SetLogEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun workoutDao(): WorkoutDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ppl_workout_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

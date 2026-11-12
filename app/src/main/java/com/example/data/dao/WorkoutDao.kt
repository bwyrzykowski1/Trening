package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.PlanExerciseEntity
import com.example.data.model.SetLogEntity
import com.example.data.model.WorkoutSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {

    // Plan Exercises
    @Query("SELECT * FROM plan_exercises WHERE dayType = :dayType ORDER BY orderIndex ASC")
    fun getExercisesForDay(dayType: String): Flow<List<PlanExerciseEntity>>

    @Query("SELECT * FROM plan_exercises WHERE dayType = :dayType ORDER BY orderIndex ASC")
    suspend fun getExercisesForDaySync(dayType: String): List<PlanExerciseEntity>

    @Query("SELECT * FROM plan_exercises ORDER BY dayType ASC, orderIndex ASC")
    fun getAllExercises(): Flow<List<PlanExerciseEntity>>

    @Query("SELECT COUNT(*) FROM plan_exercises")
    suspend fun getExerciseCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercise(exercise: PlanExerciseEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercises(exercises: List<PlanExerciseEntity>)

    @Update
    suspend fun updateExercise(exercise: PlanExerciseEntity)

    @Delete
    suspend fun deleteExercise(exercise: PlanExerciseEntity)

    @Query("DELETE FROM plan_exercises WHERE dayType = :dayType")
    suspend fun deleteExercisesForDay(dayType: String)

    @Query("DELETE FROM plan_exercises")
    suspend fun deleteAllExercises()

    // Workout Sessions
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: WorkoutSessionEntity): Long

    @Query("SELECT * FROM workout_sessions ORDER BY dateMillis DESC")
    fun getAllSessions(): Flow<List<WorkoutSessionEntity>>

    @Query("SELECT * FROM workout_sessions WHERE id = :sessionId LIMIT 1")
    suspend fun getSessionById(sessionId: Long): WorkoutSessionEntity?

    @Query("SELECT * FROM workout_sessions ORDER BY dateMillis DESC LIMIT 1")
    suspend fun getLatestSession(): WorkoutSessionEntity?

    // Set Logs
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSetLog(setLog: SetLogEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSetLogs(setLogs: List<SetLogEntity>)

    @Query("SELECT * FROM set_logs WHERE sessionId = :sessionId ORDER BY id ASC")
    suspend fun getSetLogsForSession(sessionId: Long): List<SetLogEntity>

    @Query("SELECT * FROM set_logs WHERE sessionId = :sessionId ORDER BY id ASC")
    fun getSetLogsForSessionFlow(sessionId: Long): Flow<List<SetLogEntity>>

    // Query previous performance for an exercise
    @Query("SELECT * FROM set_logs WHERE exerciseName = :exerciseName ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestSetForExercise(exerciseName: String): SetLogEntity?

    @Query("SELECT * FROM set_logs WHERE exerciseName = :exerciseName ORDER BY timestamp ASC")
    suspend fun getSetLogsForExercise(exerciseName: String): List<SetLogEntity>

    @Query("SELECT DISTINCT exerciseName FROM set_logs ORDER BY exerciseName ASC")
    fun getAllLoggedExerciseNames(): Flow<List<String>>

    @Query("SELECT * FROM set_logs ORDER BY timestamp ASC")
    fun getAllSetLogs(): Flow<List<SetLogEntity>>

    @Query("SELECT * FROM workout_sessions ORDER BY dateMillis ASC")
    suspend fun getAllSessionsSync(): List<WorkoutSessionEntity>

    @Query("SELECT * FROM set_logs ORDER BY timestamp ASC")
    suspend fun getAllSetLogsSync(): List<SetLogEntity>

    @Query("SELECT COUNT(*) FROM workout_sessions WHERE dateMillis = :dateMillis AND dayType = :dayType")
    suspend fun countSessionsWithDateAndDay(dateMillis: Long, dayType: String): Int
}

package com.example.data

import com.example.data.dao.WorkoutDao
import com.example.data.model.PlanExerciseEntity
import com.example.data.model.SetLogEntity
import com.example.data.model.WorkoutSessionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class WorkoutRepository(private val dao: WorkoutDao) {

    val allSessions: Flow<List<WorkoutSessionEntity>> = dao.getAllSessions()
    val allLoggedExercises: Flow<List<String>> = dao.getAllLoggedExerciseNames()

    fun getExercisesForDay(dayType: String): Flow<List<PlanExerciseEntity>> {
        return dao.getExercisesForDay(dayType)
    }

    suspend fun preloadDefaultsIfEmpty() = withContext(Dispatchers.IO) {
        val count = dao.getExerciseCount()
        if (count == 0) {
            val defaults = DefaultWorkoutData.getDefaultExercises()
            dao.insertExercises(defaults)
        }
    }

    suspend fun resetToDefaults() = withContext(Dispatchers.IO) {
        dao.deleteAllExercises()
        val defaults = DefaultWorkoutData.getDefaultExercises()
        dao.insertExercises(defaults)
    }

    suspend fun addExercise(exercise: PlanExerciseEntity) = withContext(Dispatchers.IO) {
        dao.insertExercise(exercise)
    }

    suspend fun updateExercise(exercise: PlanExerciseEntity) = withContext(Dispatchers.IO) {
        dao.updateExercise(exercise)
    }

    suspend fun deleteExercise(exercise: PlanExerciseEntity) = withContext(Dispatchers.IO) {
        dao.deleteExercise(exercise)
    }

    suspend fun reorderExercises(exercises: List<PlanExerciseEntity>) = withContext(Dispatchers.IO) {
        exercises.forEachIndexed { index, ex ->
            dao.updateExercise(ex.copy(orderIndex = index))
        }
    }

    suspend fun getLatestSetForExercise(exerciseName: String): SetLogEntity? = withContext(Dispatchers.IO) {
        dao.getLatestSetForExercise(exerciseName)
    }

    suspend fun saveCompletedSession(
        dayType: String,
        durationSeconds: Int,
        setLogs: List<SetLogEntity>
    ): Long = withContext(Dispatchers.IO) {
        val totalTonnage = setLogs.sumOf { it.weightKg * it.reps }
        val session = WorkoutSessionEntity(
            dayType = dayType,
            durationSeconds = durationSeconds,
            totalTonnageKg = totalTonnage,
            completedSetsCount = setLogs.size
        )
        val sessionId = dao.insertSession(session)

        val updatedLogs = setLogs.map { it.copy(sessionId = sessionId) }
        dao.insertSetLogs(updatedLogs)
        sessionId
    }

    suspend fun deleteWorkoutSession(sessionId: Long) = withContext(Dispatchers.IO) {
        dao.deleteSetLogsForSession(sessionId)
        dao.deleteSessionById(sessionId)
    }

    suspend fun getSetLogsForSession(sessionId: Long): List<SetLogEntity> = withContext(Dispatchers.IO) {
        dao.getSetLogsForSession(sessionId)
    }

    suspend fun getSessionDetails(sessionId: Long): Pair<WorkoutSessionEntity?, List<SetLogEntity>> = withContext(Dispatchers.IO) {
        val session = dao.getSessionById(sessionId)
        val logs = dao.getSetLogsForSession(sessionId)
        Pair(session, logs)
    }

    suspend fun getProgressionForExercise(exerciseName: String): List<SetLogEntity> = withContext(Dispatchers.IO) {
        dao.getSetLogsForExercise(exerciseName)
    }

    fun getAllSetLogs(): Flow<List<SetLogEntity>> {
        return dao.getAllSetLogs()
    }

    suspend fun exportAllDataToCsv(): String = withContext(Dispatchers.IO) {
        val sessions = dao.getAllSessionsSync()
        val logs = dao.getAllSetLogsSync()
        com.example.data.csv.CsvManager.generateCsv(sessions, logs)
    }

    suspend fun importCsvData(csvText: String): com.example.data.csv.ImportSummary = withContext(Dispatchers.IO) {
        val parsed = com.example.data.csv.CsvManager.parseCsv(csvText)
        if (parsed.isEmpty()) {
            return@withContext com.example.data.csv.ImportSummary(
                sessionsImported = 0,
                setsImported = 0,
                sessionsSkipped = 0,
                message = "Nie znaleziono poprawnych danych treningowych w wybranym pliku CSV."
            )
        }

        var importedSessions = 0
        var importedSets = 0
        var skippedSessions = 0

        for (item in parsed) {
            val exists = dao.countSessionsWithDateAndDay(item.session.dateMillis, item.session.dayType)
            if (exists > 0) {
                skippedSessions++
            } else {
                val newSessionId = dao.insertSession(item.session)
                if (item.sets.isNotEmpty()) {
                    val logsToInsert = item.sets.map { it.copy(sessionId = newSessionId) }
                    dao.insertSetLogs(logsToInsert)
                    importedSets += logsToInsert.size
                }
                importedSessions++
            }
        }

        val message = if (importedSessions > 0) {
            "Zaimportowano $importedSessions treningów ($importedSets serii)" +
                if (skippedSessions > 0) " (Pominięto $skippedSessions już istniejących)" else ""
        } else {
            "Wszystkie sesje z pliku ($skippedSessions) znajdują się już w bazie."
        }

        com.example.data.csv.ImportSummary(
            sessionsImported = importedSessions,
            setsImported = importedSets,
            sessionsSkipped = skippedSessions,
            message = message
        )
    }
}

package com.example.ui.viewmodel

import com.example.data.model.SetLogEntity
import com.example.ui.components.IntervalPhase
import java.util.Locale

data class StepTarget(
    val exerciseIndex: Int,
    val setNumber: Int
)

object WorkoutSessionHelper {

    fun formatWeight(weightKg: Double): String {
        return String.format(Locale.US, "%.1f", weightKg).removeSuffix(".0")
    }

    /**
     * Inserts a new set log or updates an existing log for the same (exerciseName, setNumber)
     * so returning to an earlier set/exercise to adjust weight or reps never creates duplicates.
     */
    fun upsertSetLog(
        logs: List<SetLogEntity>,
        newLog: SetLogEntity
    ): List<SetLogEntity> {
        val existingIndex = logs.indexOfLast {
            it.exerciseName.equals(newLog.exerciseName, ignoreCase = true) &&
                it.setNumber == newLog.setNumber
        }
        return if (existingIndex >= 0) {
            logs.toMutableList().apply {
                this[existingIndex] = newLog.copy(id = logs[existingIndex].id)
            }
        } else {
            logs + newLog
        }
    }

    fun updateSetLogAt(
        logs: List<SetLogEntity>,
        index: Int,
        weightKg: Double,
        reps: Int
    ): List<SetLogEntity> {
        if (index !in logs.indices) return logs
        return logs.toMutableList().apply {
            this[index] = this[index].copy(
                weightKg = weightKg.coerceAtLeast(0.0),
                reps = reps.coerceAtLeast(0)
            )
        }
    }

    fun removeSetLogAt(
        logs: List<SetLogEntity>,
        index: Int
    ): List<SetLogEntity> {
        if (index !in logs.indices) return logs
        return logs.toMutableList().apply { removeAt(index) }
    }

    /**
     * Resolves where stepping back one unit should navigate:
     * - From REST -> back to EXERCISE for the exact same (exerciseIndex, setNumber)
     * - From EXERCISE set > 1 -> previous set of the same exercise
     * - From EXERCISE set == 1 and exerciseIndex > 0 -> last set of the previous exercise
     */
    fun resolvePreviousStep(state: ActiveWorkoutState): StepTarget? {
        if (state.exercises.isEmpty()) return null
        if (state.intervalPhase == IntervalPhase.REST) {
            return StepTarget(state.currentExerciseIndex, state.currentSetNumber)
        }
        if (state.currentSetNumber > 1) {
            return StepTarget(state.currentExerciseIndex, state.currentSetNumber - 1)
        }
        if (state.currentExerciseIndex > 0) {
            val prevIndex = state.currentExerciseIndex - 1
            val prevExercise = state.exercises[prevIndex]
            return StepTarget(prevIndex, prevExercise.targetSets.coerceAtLeast(1))
        }
        return null
    }

    /**
     * Resolves where "Poprzednie ćwiczenie" should navigate:
     * Jumps to exerciseIndex - 1 on its last recorded set, or set 1 if nothing was recorded yet.
     */
    fun resolvePreviousExercise(state: ActiveWorkoutState): StepTarget? {
        if (state.currentExerciseIndex <= 0 || state.exercises.isEmpty()) return null
        val prevIndex = state.currentExerciseIndex - 1
        val prevExercise = state.exercises[prevIndex]
        val maxSets = prevExercise.targetSets.coerceAtLeast(1)
        val recordedForPrev = state.recordedLogs.filter {
            it.exerciseName.equals(prevExercise.name, ignoreCase = true)
        }
        val targetSet = recordedForPrev.maxOfOrNull { it.setNumber }
            ?.coerceIn(1, maxSets)
            ?: 1
        return StepTarget(prevIndex, targetSet)
    }

    fun findRecordedLog(
        logs: List<SetLogEntity>,
        exerciseName: String?,
        setNumber: Int
    ): SetLogEntity? {
        if (exerciseName.isNullOrBlank()) return null
        return logs.lastOrNull {
            it.exerciseName.equals(exerciseName, ignoreCase = true) && it.setNumber == setNumber
        }
    }
}

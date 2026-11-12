package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "plan_exercises")
data class PlanExerciseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val dayType: String, // PUSH, PULL, LEGS
    val name: String,
    val targetSets: Int,
    val minReps: Int,
    val maxReps: Int,
    val orderIndex: Int,
    val muscleGroup: String,
    val isCustom: Boolean = false
) {
    val repsRangeLabel: String
        get() = if (minReps == maxReps) "$minReps" else "$minReps–$maxReps"
}

package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "set_logs")
data class SetLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionId: Long,
    val exerciseName: String,
    val setNumber: Int,
    val weightKg: Double,
    val reps: Int,
    val timestamp: Long = System.currentTimeMillis()
) {
    // Brzycki formula for estimated 1 Rep Max
    val estimated1RM: Double
        get() = if (reps > 0) weightKg * (36.0 / (37.0 - reps.coerceAtMost(36))) else weightKg

    val volume: Double
        get() = weightKg * reps
}

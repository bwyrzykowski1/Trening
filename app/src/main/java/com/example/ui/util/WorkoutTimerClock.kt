package com.example.ui.util

import android.os.SystemClock

/**
 * Provides monotonic elapsed milliseconds that continue advancing even while the device
 * is in deep sleep or the app is frozen in the background.
 */
interface ElapsedClock {
    fun elapsedRealtime(): Long
}

object SystemElapsedClock : ElapsedClock {
    override fun elapsedRealtime(): Long = SystemClock.elapsedRealtime()
}

data class TimerTickResult(
    val remainingMs: Long,
    val remainingSeconds: Int,
    val isExpired: Boolean,
    val overshootMs: Long
)

object WorkoutTimerMath {
    const val DEFAULT_INTERVAL_SECONDS = 120
    const val DEFAULT_INTERVAL_MS = DEFAULT_INTERVAL_SECONDS * 1000L

    /**
     * Converts exact remaining milliseconds to display seconds using ceiling rounding so
     * 120_000ms -> 120s, 119_001ms -> 120s, 119_000ms -> 119s, 1ms -> 1s, 0ms -> 0s.
     */
    fun millisToDisplaySeconds(remainingMs: Long): Int {
        if (remainingMs <= 0L) return 0
        return ((remainingMs + 999L) / 1000L).toInt()
    }

    /**
     * Computes remaining countdown state against real monotonic elapsed time.
     */
    fun evaluateTick(intervalEndElapsedMs: Long, nowElapsedMs: Long): TimerTickResult {
        val diff = intervalEndElapsedMs - nowElapsedMs
        return if (diff > 0L) {
            TimerTickResult(
                remainingMs = diff,
                remainingSeconds = millisToDisplaySeconds(diff),
                isExpired = false,
                overshootMs = 0L
            )
        } else {
            TimerTickResult(
                remainingMs = 0L,
                remainingSeconds = 0,
                isExpired = true,
                overshootMs = -diff
            )
        }
    }

    /**
     * Computes the starting remaining duration of the next interval after an expiration,
     * preserving accuracy if the app was briefly suspended in the background (< one full interval),
     * while avoiding skipping multiple unlogged exercises if the phone was idle for a long time.
     */
    fun nextIntervalRemainingMs(
        overshootMs: Long,
        nextIntervalDurationMs: Long = DEFAULT_INTERVAL_MS
    ): Long {
        return if (overshootMs in 1L until nextIntervalDurationMs) {
            nextIntervalDurationMs - overshootMs
        } else {
            nextIntervalDurationMs
        }
    }
}

package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import java.util.Locale

/**
 * Foreground Service keeping the workout interval timer process alive while the user
 * switches apps or locks the screen, displaying a live native countdown chronometer
 * in the status bar and on the lock screen.
 */
class WorkoutTimerService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_START_OR_UPDATE -> {
                val phaseLabel = intent.getStringExtra(EXTRA_PHASE_LABEL) ?: "TRENING"
                val exerciseInfo = intent.getStringExtra(EXTRA_EXERCISE_INFO) ?: "Aktywna sesja PPL"
                val isRunning = intent.getBooleanExtra(EXTRA_IS_RUNNING, true)
                val remainingMs = intent.getLongExtra(EXTRA_REMAINING_MS, 0L).coerceAtLeast(0L)

                val notification = buildNotification(
                    phaseLabel = phaseLabel,
                    exerciseInfo = exerciseInfo,
                    isRunning = isRunning,
                    remainingMs = remainingMs
                )

                val fgsType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                } else {
                    0
                }

                try {
                    ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, fgsType)
                } catch (_: Exception) {
                    // Gracefully degrade if OEM/system restricts foreground start
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Stoper treningowy",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Wyświetla aktywne odliczanie interwałów ćwiczeń i przerw w tle"
                setShowBadge(false)
                enableVibration(false)
                setSound(null, null)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(
        phaseLabel: String,
        exerciseInfo: String,
        isRunning: Boolean,
        remainingMs: Long
    ): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_timer)
            .setContentText(exerciseInfo)
            .setContentIntent(contentPendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_WORKOUT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        if (isRunning && remainingMs > 0L) {
            val targetWallClockMs = System.currentTimeMillis() + remainingMs
            builder
                .setContentTitle("$phaseLabel • Aktywny stoper")
                .setShowWhen(true)
                .setUsesChronometer(true)
                .setChronometerCountDown(true)
                .setWhen(targetWallClockMs)
        } else {
            val secs = ((remainingMs + 999L) / 1000L).toInt().coerceAtLeast(0)
            val mins = secs / 60
            val remSecs = secs % 60
            val formatted = String.format(Locale.getDefault(), "%02d:%02d", mins, remSecs)
            builder
                .setContentTitle("$phaseLabel • Wstrzymano ($formatted)")
                .setShowWhen(false)
                .setUsesChronometer(false)
        }

        return builder.build()
    }

    companion object {
        private const val CHANNEL_ID = "ppl_workout_timer_channel"
        private const val NOTIFICATION_ID = 1042

        const val ACTION_START_OR_UPDATE = "com.example.service.ACTION_START_OR_UPDATE"
        const val ACTION_STOP = "com.example.service.ACTION_STOP"

        const val EXTRA_PHASE_LABEL = "extra_phase_label"
        const val EXTRA_EXERCISE_INFO = "extra_exercise_info"
        const val EXTRA_IS_RUNNING = "extra_is_running"
        const val EXTRA_REMAINING_MS = "extra_remaining_ms"

        fun startOrUpdate(
            context: Context,
            phaseLabel: String,
            exerciseInfo: String,
            isRunning: Boolean,
            remainingMs: Long
        ) {
            val intent = Intent(context, WorkoutTimerService::class.java).apply {
                action = ACTION_START_OR_UPDATE
                putExtra(EXTRA_PHASE_LABEL, phaseLabel)
                putExtra(EXTRA_EXERCISE_INFO, exerciseInfo)
                putExtra(EXTRA_IS_RUNNING, isRunning)
                putExtra(EXTRA_REMAINING_MS, remainingMs)
            }
            try {
                ContextCompat.startForegroundService(context, intent)
            } catch (_: Exception) {
                // Fallback safely if background FGS launch is restricted; monotonic clock still keeps exact time
            }
        }

        fun stop(context: Context) {
            try {
                val intent = Intent(context, WorkoutTimerService::class.java).apply {
                    action = ACTION_STOP
                }
                context.startService(intent)
            } catch (_: Exception) {
                // Service wasn't running
            }
        }
    }
}

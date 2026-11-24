package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.RestTimerColor
import com.example.ui.theme.WorkTimerColor
import kotlin.math.roundToInt

enum class IntervalPhase(val label: String, val description: String) {
    EXERCISE("ĆWICZENIE / SERIA", "Wykonuj ćwiczenie z pełnym skupieniem"),
    REST("PRZERWA / ODPOCZYNEK", "Uspokój oddech i zregeneruj siły do kolejnej serii")
}

@Composable
fun IntervalTimerCircle(
    phase: IntervalPhase,
    secondsRemaining: Int,
    totalSeconds: Int,
    isRunning: Boolean,
    isSoundEnabled: Boolean = true,
    soundVolume: Float = 1.0f,
    isLoudModeEnabled: Boolean = false,
    onToggleSound: () -> Unit = {},
    onSoundVolumeChange: (Float) -> Unit = {},
    onSoundVolumeChangeFinished: () -> Unit = {},
    onAdjustSoundVolume: (Float) -> Unit = {},
    onToggleLoudMode: () -> Unit = {},
    onTestSound: () -> Unit = {},
    onTogglePlayPause: () -> Unit,
    onSkipInterval: () -> Unit,
    onAdd30Seconds: () -> Unit,
    modifier: Modifier = Modifier
) {
    val phaseColor by animateColorAsState(
        targetValue = if (phase == IntervalPhase.EXERCISE) WorkTimerColor else RestTimerColor,
        animationSpec = tween(400),
        label = "phaseColor"
    )

    val progressFraction = (secondsRemaining.toFloat() / totalSeconds.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = progressFraction,
        animationSpec = tween(300),
        label = "animatedProgress"
    )

    val minutes = secondsRemaining / 60
    val seconds = secondsRemaining % 60
    val timeFormatted = String.format("%02d:%02d", minutes, seconds)

    val effectiveVolume = if (isSoundEnabled) soundVolume.coerceIn(0f, 1f) else 0f
    val volumePercent = (effectiveVolume * 100f).roundToInt()
    val volumeAccent = if (isLoudModeEnabled && isSoundEnabled) AmberAccent else phaseColor

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        tonalElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header tag with sound cue toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = phaseColor.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = phase.label,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.2.sp
                        ),
                        color = phaseColor,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }

                IconButton(
                    onClick = onToggleSound,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("timer_sound_toggle")
                ) {
                    Icon(
                        imageVector = if (isSoundEnabled && effectiveVolume > 0f) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                        contentDescription = if (isSoundEnabled) "Wycisz dźwięki" else "Włącz dźwięki",
                        tint = if (isSoundEnabled && effectiveVolume > 0f) volumeAccent else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Circular Timer
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(190.dp)
            ) {
                Canvas(modifier = Modifier.size(180.dp)) {
                    val strokeWidth = 12.dp.toPx()
                    val diameter = size.minDimension - strokeWidth
                    val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)
                    val arcSize = Size(diameter, diameter)

                    // Track background
                    drawArc(
                        color = Color.White.copy(alpha = 0.08f),
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth)
                    )

                    // Active Countdown Arc
                    drawArc(
                        brush = Brush.sweepGradient(
                            listOf(phaseColor.copy(alpha = 0.6f), phaseColor)
                        ),
                        startAngle = -90f,
                        sweepAngle = 360f * animatedProgress,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = timeFormatted,
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (isRunning) "W toku" else "Pauza",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isRunning) phaseColor else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = phase.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 10.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // +30 seconds
                FilledTonalButton(
                    onClick = onAdd30Seconds,
                    modifier = Modifier.testTag("timer_add_30s")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Dodaj 30 sekund",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "30s", style = MaterialTheme.typography.labelMedium)
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Play / Pause
                Button(
                    onClick = onTogglePlayPause,
                    colors = ButtonDefaults.buttonColors(containerColor = phaseColor),
                    shape = CircleShape,
                    modifier = Modifier
                        .size(52.dp)
                        .testTag("timer_play_pause")
                ) {
                    Icon(
                        imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isRunning) "Wstrzymaj" else "Wznów",
                        tint = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Skip interval
                OutlinedButton(
                    onClick = onSkipInterval,
                    modifier = Modifier.testTag("timer_skip_interval")
                ) {
                    Text(
                        text = "Pomiń",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.FastForward,
                        contentDescription = "Pomiń interwał",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // In-App Sound Volume & Loud Mode Controls
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (isLoudModeEnabled && isSoundEnabled) {
                        AmberAccent.copy(alpha = 0.45f)
                    } else {
                        Color.White.copy(alpha = 0.08f)
                    }
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Głośność sygnałów: $volumePercent%",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (volumePercent > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (isLoudModeEnabled && isSoundEnabled) {
                                Text(
                                    text = "Kanał alarmu • maksymalna moc",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AmberAccent
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            FilterChip(
                                selected = isLoudModeEnabled,
                                onClick = onToggleLoudMode,
                                label = {
                                    Text(
                                        text = "Głośny",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.NotificationsActive,
                                        contentDescription = "Tryb głośny (kanał alarmu)",
                                        modifier = Modifier.size(15.dp)
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AmberAccent.copy(alpha = 0.22f),
                                    selectedLabelColor = AmberAccent,
                                    selectedLeadingIconColor = AmberAccent
                                ),
                                modifier = Modifier.testTag("timer_loud_mode_toggle")
                            )

                            TextButton(
                                onClick = onTestSound,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.testTag("timer_sound_test")
                            ) {
                                Text(
                                    text = "Test",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = volumeAccent
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { onAdjustSoundVolume(-0.1f) },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("timer_volume_down")
                        ) {
                            Icon(
                                imageVector = Icons.Default.VolumeDown,
                                contentDescription = "Zmniejsz głośność o 10%",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Slider(
                            value = effectiveVolume,
                            onValueChange = onSoundVolumeChange,
                            onValueChangeFinished = onSoundVolumeChangeFinished,
                            valueRange = 0f..1f,
                            colors = SliderDefaults.colors(
                                thumbColor = volumeAccent,
                                activeTrackColor = volumeAccent,
                                inactiveTrackColor = Color.White.copy(alpha = 0.14f)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("timer_volume_slider")
                        )

                        IconButton(
                            onClick = { onAdjustSoundVolume(0.1f) },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("timer_volume_up")
                        ) {
                            Icon(
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = "Zwiększ głośność o 10%",
                                tint = volumeAccent
                            )
                        }
                    }
                }
            }
        }
    }
}

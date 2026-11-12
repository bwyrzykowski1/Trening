package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.RestTimerColor
import com.example.ui.theme.WorkTimerColor
import com.example.ui.viewmodel.ActiveWorkoutState

@Composable
fun ActiveWorkoutMiniBar(
    state: ActiveWorkoutState,
    onExpand: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSkipInterval: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = state.isActive,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it }),
        modifier = modifier
    ) {
        val currentEx = state.currentExercise
        val exName = currentEx?.name ?: "Trening ${state.dayType.code}"
        val setInfo = currentEx?.let { "Seria ${state.currentSetNumber} z ${it.targetSets}" } ?: ""

        val isWork = state.intervalPhase == IntervalPhase.EXERCISE
        val phaseColor = if (isWork) WorkTimerColor else RestTimerColor
        val phaseLabel = if (isWork) "SERIA" else "PRZERWA"

        val minutes = state.timerSecondsRemaining / 60
        val seconds = state.timerSecondsRemaining % 60
        val timeFormatted = String.format("%02d:%02d", minutes, seconds)
        val progress = (state.timerSecondsRemaining.toFloat() / state.timerTotalSeconds.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .navigationBarsPadding(),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 10.dp,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onExpand)
                    .testTag("workout_mini_player_bar")
            ) {
                // Subtle progress indicator on top edge
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp),
                    color = phaseColor,
                    trackColor = Color.White.copy(alpha = 0.08f)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Time badge with phase color glow
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = phaseColor.copy(alpha = 0.2f),
                        modifier = Modifier.clip(RoundedCornerShape(10.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = timeFormatted,
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 0.5.sp
                                ),
                                color = phaseColor
                            )
                            Text(
                                text = phaseLabel,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 9.sp
                                ),
                                color = phaseColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Exercise Title & Set Info
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = exName,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (setInfo.isNotEmpty()) {
                            Text(
                                text = setInfo,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Controls
                    IconButton(
                        onClick = onTogglePlayPause,
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("mini_player_play_pause")
                    ) {
                        Icon(
                            imageVector = if (state.isTimerRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (state.isTimerRunning) "Wstrzymaj" else "Wznów",
                            tint = phaseColor
                        )
                    }

                    IconButton(
                        onClick = onSkipInterval,
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("mini_player_skip")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FastForward,
                            contentDescription = "Pomiń interwał",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = onExpand,
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("mini_player_expand")
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowUp,
                            contentDescription = "Rozwiń trening",
                            tint = CyanAccent
                        )
                    }
                }
            }
        }
    }
}

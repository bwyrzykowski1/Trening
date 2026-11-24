package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SetLogEntity
import com.example.ui.components.EditRecordedSetDialog
import com.example.ui.components.IntervalPhase
import com.example.ui.components.IntervalTimerCircle
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.LimeAccent
import com.example.ui.viewmodel.ActiveWorkoutState
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveWorkoutScreen(
    state: ActiveWorkoutState,
    onMinimizeWorkout: () -> Unit,
    onToggleSound: () -> Unit,
    onSoundVolumeChange: (Float) -> Unit,
    onSoundVolumeChangeFinished: () -> Unit,
    onAdjustSoundVolume: (Float) -> Unit,
    onToggleLoudMode: () -> Unit,
    onTestSound: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSkipInterval: () -> Unit,
    onAdd30Seconds: () -> Unit,
    onWeightChanged: (String) -> Unit,
    onRepsChanged: (String) -> Unit,
    onAdjustWeight: (Double) -> Unit,
    onAdjustReps: (Int) -> Unit,
    onApplySuggestion: () -> Unit,
    onLogSetAndRest: () -> Unit,
    onFinishRestAndStartNext: () -> Unit,
    onPreviousStep: () -> Unit,
    onPreviousExercise: () -> Unit,
    onSelectSetNumber: (Int) -> Unit,
    onUpdateRecordedSet: (index: Int, weightKg: Double, reps: Int) -> Unit,
    onDeleteRecordedSet: (index: Int) -> Unit,
    onNextExercise: () -> Unit,
    onFinishWorkout: () -> Unit,
    onCancelWorkout: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showCancelDialog by remember { mutableStateOf(false) }
    var editingLogIndex by remember { mutableStateOf<Int?>(null) }

    editingLogIndex?.let { idx ->
        val logToEdit = state.recordedLogs.getOrNull(idx)
        if (logToEdit != null) {
            EditRecordedSetDialog(
                log = logToEdit,
                onSave = { weightKg, reps ->
                    onUpdateRecordedSet(idx, weightKg, reps)
                    editingLogIndex = null
                },
                onDelete = {
                    onDeleteRecordedSet(idx)
                    editingLogIndex = null
                },
                onDismiss = { editingLogIndex = null }
            )
        } else {
            editingLogIndex = null
        }
    }

    // System back gesture minimizes the workout to the bottom mini-player bar
    BackHandler {
        onMinimizeWorkout()
    }

    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text("Opuścić trening?") },
            text = {
                Text("Możesz zminimalizować trening do paska na dole i wrócić do menu bez przerywania ćwiczeń. Czy na pewno chcesz całkowicie anulować tę sesję?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showCancelDialog = false
                        onCancelWorkout()
                    }
                ) {
                    Text("Anuluj sesję", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            showCancelDialog = false
                            onMinimizeWorkout()
                        }
                    ) {
                        Text("Zminimalizuj", color = CyanAccent)
                    }
                    TextButton(onClick = { showCancelDialog = false }) {
                        Text("Kontynuuj")
                    }
                }
            }
        )
    }

    val currentEx = state.currentExercise
    val totalExercises = state.exercises.size
    val currentExIndex = state.currentExerciseIndex + 1
    val overallProgress = if (totalExercises > 0) currentExIndex.toFloat() / totalExercises.toFloat() else 0f

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "Sesja: ${state.dayType.code}",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "Ćwiczenie $currentExIndex z $totalExercises",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onMinimizeWorkout,
                            modifier = Modifier.testTag("action_minimize_workout")
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Zminimalizuj do menu",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { showCancelDialog = true },
                            modifier = Modifier.testTag("action_cancel_workout_dialog")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Opuść trening",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TextButton(
                            onClick = onFinishWorkout,
                            modifier = Modifier.testTag("action_finish_workout")
                        ) {
                            Text(
                                text = "Zakończ",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = LimeAccent
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
                LinearProgressIndicator(
                    progress = { overallProgress },
                    modifier = Modifier.fillMaxWidth(),
                    color = CyanAccent,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Live 2m / 2m Interval Countdown Timer
            item {
                IntervalTimerCircle(
                    phase = state.intervalPhase,
                    secondsRemaining = state.timerSecondsRemaining,
                    totalSeconds = state.timerTotalSeconds,
                    isRunning = state.isTimerRunning,
                    isSoundEnabled = state.isSoundEnabled,
                    soundVolume = state.soundVolume,
                    isLoudModeEnabled = state.isLoudModeEnabled,
                    onToggleSound = onToggleSound,
                    onSoundVolumeChange = onSoundVolumeChange,
                    onSoundVolumeChangeFinished = onSoundVolumeChangeFinished,
                    onAdjustSoundVolume = onAdjustSoundVolume,
                    onToggleLoudMode = onToggleLoudMode,
                    onTestSound = onTestSound,
                    onTogglePlayPause = onTogglePlayPause,
                    onSkipInterval = onSkipInterval,
                    onAdd30Seconds = onAdd30Seconds
                )
            }

            // Current Exercise Details Card & Logging (during EXERCISE) OR Dedicated Rest Phase Card (during REST)
            if (currentEx != null) {
                if (state.intervalPhase == IntervalPhase.EXERCISE) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = CyanAccent.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = currentEx.muscleGroup,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = CyanAccent,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(
                                                onClick = {
                                                    if (state.currentSetNumber > 1) {
                                                        onSelectSetNumber(state.currentSetNumber - 1)
                                                    } else if (state.canGoToPreviousExercise) {
                                                        onPreviousStep()
                                                    }
                                                },
                                                enabled = state.currentSetNumber > 1 || state.canGoToPreviousExercise,
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .testTag("previous_set_chevron")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.ChevronLeft,
                                                    contentDescription = "Poprzednia seria",
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            }
                                            Text(
                                                text = "Seria ${state.currentSetNumber}/${currentEx.targetSets}",
                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            IconButton(
                                                onClick = { onSelectSetNumber(state.currentSetNumber + 1) },
                                                enabled = state.currentSetNumber < currentEx.targetSets,
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .testTag("next_set_chevron")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.ChevronRight,
                                                    contentDescription = "Następna seria",
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Text(
                                    text = currentEx.name,
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Docelowo: ${currentEx.repsRangeLabel} powtórzeń na serię",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (state.currentSetRecordedLog != null) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = LimeAccent.copy(alpha = 0.16f)
                                        ) {
                                            Text(
                                                text = "Zapisana",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = LimeAccent,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Previous Performance Suggestion Badge
                                val suggestion = state.suggestedLastSet
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (suggestion != null) LimeAccent.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.History,
                                                contentDescription = null,
                                                tint = if (suggestion != null) LimeAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(
                                                    text = if (suggestion != null) "Poprzedni trening:" else "Brak poprzednich wpisów",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    text = if (suggestion != null) {
                                                        "${String.format(Locale.getDefault(), "%.1f", suggestion.weightKg)} kg × ${suggestion.reps} powt."
                                                    } else "Pierwsze wykonanie tego ćwiczenia",
                                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                    color = if (suggestion != null) LimeAccent else MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }

                                        if (suggestion != null) {
                                            FilledTonalButton(
                                                onClick = onApplySuggestion,
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.testTag("apply_suggestion_button")
                                            ) {
                                                Text("Wstaw", style = MaterialTheme.typography.labelMedium)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Weight & Reps Logging Inputs
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Text(
                                    text = "Zarejestruj serię ${state.currentSetNumber}:",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                // Weight Row
                                Text(
                                    text = "Ciężar (kg)",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = state.inputWeight,
                                        onValueChange = onWeightChanged,
                                        placeholder = { Text("np. 60.0") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        singleLine = true,
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("input_weight")
                                    )

                                    Spacer(modifier = Modifier.width(8.dp))

                                    FilledTonalIconButton(
                                        onClick = { onAdjustWeight(-2.5) },
                                        modifier = Modifier.size(44.dp)
                                    ) {
                                        Text("-2.5", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                                    }

                                    Spacer(modifier = Modifier.width(4.dp))

                                    FilledTonalIconButton(
                                        onClick = { onAdjustWeight(2.5) },
                                        modifier = Modifier.size(44.dp)
                                    ) {
                                        Text("+2.5", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                                    }

                                    Spacer(modifier = Modifier.width(4.dp))

                                    FilledTonalIconButton(
                                        onClick = { onAdjustWeight(5.0) },
                                        modifier = Modifier.size(44.dp)
                                    ) {
                                        Text("+5", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // Reps Row
                                Text(
                                    text = "Powtórzenia (reps)",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = state.inputReps,
                                        onValueChange = onRepsChanged,
                                        placeholder = { Text("np. 8") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("input_reps")
                                    )

                                    Spacer(modifier = Modifier.width(8.dp))

                                    FilledTonalIconButton(
                                        onClick = { onAdjustReps(-1) },
                                        modifier = Modifier.size(44.dp)
                                    ) {
                                        Icon(Icons.Default.Remove, contentDescription = "-1 powtórzenie")
                                    }

                                    Spacer(modifier = Modifier.width(4.dp))

                                    FilledTonalIconButton(
                                        onClick = { onAdjustReps(1) },
                                        modifier = Modifier.size(44.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "+1 powtórzenie")
                                    }
                                }

                                Spacer(modifier = Modifier.height(18.dp))

                                // Primary Log Action Button
                                val isEditingExistingSet = state.currentSetRecordedLog != null
                                Button(
                                    onClick = onLogSetAndRest,
                                    colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                        .testTag("log_set_and_rest_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color(0xFF003544)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isEditingExistingSet) {
                                            "Zapisz zmiany i przejdź do odpoczynku"
                                        } else {
                                            "Zapisz serię i przejdź do odpoczynku"
                                        },
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = Color(0xFF003544)
                                    )
                                }

                                if (state.canGoToPreviousExercise) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    OutlinedButton(
                                        onClick = onPreviousExercise,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp)
                                            .testTag("card_previous_exercise_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Wróć do: ${state.previousExercise?.name ?: "poprzedniego ćwiczenia"}",
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            style = MaterialTheme.typography.labelLarge
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // IntervalPhase.REST - Dedicated Rest & Transition Card
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            border = BorderStroke(1.5.dp, CyanAccent.copy(alpha = 0.5f)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(18.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = CyanAccent.copy(alpha = 0.18f)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.HourglassTop,
                                                contentDescription = null,
                                                tint = CyanAccent,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "ODPOCZYNEK",
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                color = CyanAccent
                                            )
                                        }
                                    }

                                    Text(
                                        text = "Pozostało: ${state.timerSecondsRemaining}s",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = CyanAccent
                                    )
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // Set Just Completed Info OR Missing Set Prompt (when timer expired before logging)
                                val currentSetLog = state.currentSetRecordedLog
                                if (currentSetLog != null) {
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                modifier = Modifier.weight(1f),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.CheckCircle,
                                                    contentDescription = null,
                                                    tint = LimeAccent,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column {
                                                    Text(
                                                        text = "Zapisano serię #${currentSetLog.setNumber}: ${currentSetLog.exerciseName}",
                                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Text(
                                                        text = "${currentSetLog.weightKg} kg × ${currentSetLog.reps} powt. (tonaż: ${(currentSetLog.weightKg * currentSetLog.reps).toInt()} kg)",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = LimeAccent
                                                    )
                                                }
                                            }
                                            TextButton(
                                                onClick = onPreviousStep,
                                                modifier = Modifier.testTag("rest_edit_current_set_button")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Edit,
                                                    contentDescription = "Zmień ciężar i powtórzenia",
                                                    tint = CyanAccent,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "Zmień",
                                                    color = CyanAccent,
                                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                } else {
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        color = AmberAccent.copy(alpha = 0.14f),
                                        border = BorderStroke(1.dp, AmberAccent.copy(alpha = 0.45f))
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.WarningAmber,
                                                    contentDescription = null,
                                                    tint = AmberAccent,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column {
                                                    Text(
                                                        text = "Czas minął — seria #${state.currentSetNumber} nie jest zapisana",
                                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Text(
                                                        text = currentEx.name,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(10.dp))
                                            FilledTonalButton(
                                                onClick = onPreviousStep,
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(48.dp)
                                                    .testTag("rest_return_to_log_set_button")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "Wróć i wpisz ciężar oraz powtórzenia",
                                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                }

                                // What comes next & Primary Next Step Button
                                if (!state.isLastSetOfCurrentExercise) {
                                    val nextSetNum = state.currentSetNumber + 1
                                    Text(
                                        text = "Kolejne zadanie:",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Seria $nextSetNum z ${currentEx.targetSets} • ${currentEx.name}",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Docelowo: ${currentEx.repsRangeLabel} powtórzeń",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    Spacer(modifier = Modifier.height(18.dp))

                                    Button(
                                        onClick = onFinishRestAndStartNext,
                                        colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(52.dp)
                                            .testTag("finish_rest_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                            contentDescription = null,
                                            tint = Color(0xFF003544)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Przejdź do kolejnej serii ($nextSetNum/${currentEx.targetSets})",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = Color(0xFF003544)
                                        )
                                    }
                                } else if (state.nextExercise != null) {
                                    val nextEx = state.nextExercise!!
                                    Text(
                                        text = "Ćwiczenie ukończone! Następne w planie:",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = LimeAccent
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = nextEx.name,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Partia: ${nextEx.muscleGroup} • ${nextEx.targetSets} serie po ${nextEx.repsRangeLabel} powt.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    Spacer(modifier = Modifier.height(18.dp))

                                    Button(
                                        onClick = onFinishRestAndStartNext,
                                        colors = ButtonDefaults.buttonColors(containerColor = LimeAccent),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(52.dp)
                                            .testTag("finish_rest_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.SkipNext,
                                            contentDescription = null,
                                            tint = Color.Black
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Przejdź do kolejnego ćwiczenia",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = Color.Black
                                        )
                                    }
                                } else {
                                    Text(
                                        text = "Ukończono wszystkie ćwiczenia!",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = LimeAccent
                                    )
                                    Text(
                                        text = "Świetna robota! Twój trening na dzisiaj dobiegł końca.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    Spacer(modifier = Modifier.height(18.dp))

                                    Button(
                                        onClick = onFinishWorkout,
                                        colors = ButtonDefaults.buttonColors(containerColor = LimeAccent),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(52.dp)
                                            .testTag("finish_workout_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = Color.Black
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Zakończ trening i zobacz podsumowanie",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = Color.Black
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Completed Sets List in this Session (tap any entry to edit weight & reps)
            if (state.recordedLogs.isNotEmpty()) {
                item {
                    Column {
                        Text(
                            text = "Ukończone serie w tym treningu (${state.recordedLogs.size})",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Kliknij dowolną serię, aby zmienić ciężar lub powtórzenia",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                val reversedIndexedLogs = state.recordedLogs.mapIndexed { originalIndex, log ->
                    originalIndex to log
                }.reversed()

                itemsIndexed(reversedIndexedLogs) { _, (originalIndex, log) ->
                    SetRecordItem(
                        log = log,
                        onClick = { editingLogIndex = originalIndex }
                    )
                }
            }

            // Bottom Navigation Helper Actions
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onPreviousExercise,
                            enabled = state.canGoToPreviousExercise,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("previous_exercise_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipPrevious,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Poprzednie")
                        }

                        OutlinedButton(
                            onClick = onNextExercise,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("next_exercise_button")
                        ) {
                            Text("Kolejne")
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.SkipNext,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Button(
                        onClick = onFinishWorkout,
                        colors = ButtonDefaults.buttonColors(containerColor = LimeAccent),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("bottom_finish_workout_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Zakończ sesję", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun SetRecordItem(
    log: SetLogEntity,
    onClick: () -> Unit = {}
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .testTag("recorded_set_item_${log.setNumber}"),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = log.exerciseName,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Seria #${log.setNumber} • ${(log.weightKg * log.reps).toInt()} kg tonażu",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = CyanAccent.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "${log.weightKg} kg × ${log.reps}",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = CyanAccent,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edytuj serię",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

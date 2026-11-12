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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.unit.dp
import com.example.data.model.DayType
import com.example.data.model.PlanExerciseEntity
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.LimeAccent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPlanScreen(
    selectedDay: DayType,
    exercises: List<PlanExerciseEntity>,
    onSelectDay: (DayType) -> Unit,
    onAddExercise: (DayType, String, Int, Int, Int, String) -> Unit,
    onUpdateExercise: (PlanExerciseEntity) -> Unit,
    onDeleteExercise: (PlanExerciseEntity) -> Unit,
    onMoveExercise: (PlanExerciseEntity, Boolean) -> Unit,
    onResetToDefaults: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler {
        onBack()
    }

    var showAddDialog by remember { mutableStateOf(false) }
    var exerciseToEdit by remember { mutableStateOf<PlanExerciseEntity?>(null) }
    var exerciseToDelete by remember { mutableStateOf<PlanExerciseEntity?>(null) }
    var showResetDialog by remember { mutableStateOf(false) }

    // Add / Edit Dialog state
    var inputName by remember { mutableStateOf("") }
    var inputSets by remember { mutableStateOf("3") }
    var inputMinReps by remember { mutableStateOf("8") }
    var inputMaxReps by remember { mutableStateOf("12") }
    var inputMuscle by remember { mutableStateOf("") }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Dodaj ćwiczenie do ${selectedDay.code}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = inputName,
                        onValueChange = { inputName = it },
                        label = { Text("Nazwa ćwiczenia") },
                        placeholder = { Text("np. Wyciskanie sztangielek") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = inputMuscle,
                        onValueChange = { inputMuscle = it },
                        label = { Text("Partia mięśniowa") },
                        placeholder = { Text("np. Klatka piersiowa / Barki") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = inputSets,
                            onValueChange = { inputSets = it },
                            label = { Text("Serie") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = inputMinReps,
                            onValueChange = { inputMinReps = it },
                            label = { Text("Min powt.") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = inputMaxReps,
                            onValueChange = { inputMaxReps = it },
                            label = { Text("Max powt.") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val name = inputName.trim()
                        val sets = inputSets.toIntOrNull() ?: 3
                        val minR = inputMinReps.toIntOrNull() ?: 8
                        val maxR = inputMaxReps.toIntOrNull() ?: 12
                        val muscle = inputMuscle.ifBlank { "Ogólne" }

                        if (name.isNotEmpty()) {
                            onAddExercise(selectedDay, name, sets, minR, maxR, muscle)
                            showAddDialog = false
                            inputName = ""
                            inputMuscle = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanAccent)
                ) {
                    Text("Dodaj", color = Color(0xFF003544))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Anuluj")
                }
            }
        )
    }

    // Edit Exercise Dialog
    exerciseToEdit?.let { ex ->
        AlertDialog(
            onDismissRequest = { exerciseToEdit = null },
            title = { Text("Edytuj ćwiczenie") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = inputName,
                        onValueChange = { inputName = it },
                        label = { Text("Nazwa ćwiczenia") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = inputMuscle,
                        onValueChange = { inputMuscle = it },
                        label = { Text("Partia mięśniowa") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = inputSets,
                            onValueChange = { inputSets = it },
                            label = { Text("Serie") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = inputMinReps,
                            onValueChange = { inputMinReps = it },
                            label = { Text("Min powt.") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = inputMaxReps,
                            onValueChange = { inputMaxReps = it },
                            label = { Text("Max powt.") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val name = inputName.trim()
                        val sets = inputSets.toIntOrNull() ?: ex.targetSets
                        val minR = inputMinReps.toIntOrNull() ?: ex.minReps
                        val maxR = inputMaxReps.toIntOrNull() ?: ex.maxReps
                        val muscle = inputMuscle.ifBlank { ex.muscleGroup }

                        if (name.isNotEmpty()) {
                            onUpdateExercise(
                                ex.copy(
                                    name = name,
                                    targetSets = sets,
                                    minReps = minR,
                                    maxReps = maxR,
                                    muscleGroup = muscle
                                )
                            )
                            exerciseToEdit = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanAccent)
                ) {
                    Text("Zapisz", color = Color(0xFF003544))
                }
            },
            dismissButton = {
                TextButton(onClick = { exerciseToEdit = null }) {
                    Text("Anuluj")
                }
            }
        )
    }

    // Delete Confirmation Dialog
    exerciseToDelete?.let { ex ->
        AlertDialog(
            onDismissRequest = { exerciseToDelete = null },
            title = { Text("Usunąć ćwiczenie?") },
            text = { Text("Czy na pewno chcesz usunąć ćwiczenie \"${ex.name}\" z planu ${selectedDay.code}?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteExercise(ex)
                        exerciseToDelete = null
                    }
                ) {
                    Text("Usuń", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { exerciseToDelete = null }) {
                    Text("Anuluj")
                }
            }
        )
    }

    // Reset Defaults Confirmation Dialog
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Przywrócić oryginalny plan z PDF?") },
            text = { Text("Spowoduje to przywrócenie domyślnych ćwiczeń dla PUSH, PULL i LEGS z załączonego dokumentu. Twoja historia treningów i serie nie zostaną usunięte.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showResetDialog = false
                        onResetToDefaults()
                    }
                ) {
                    Text("Przywróć domyślne", color = CyanAccent)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Anuluj")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Edycja Planu Treningowego",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Dostosuj ćwiczenia, serie i powtórzenia",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Wróć",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showResetDialog = true },
                        modifier = Modifier.testTag("action_reset_defaults")
                    ) {
                        Icon(
                            imageVector = Icons.Default.RestartAlt,
                            contentDescription = "Przywróć domyślne z PDF",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            inputName = ""
                            inputSets = "3"
                            inputMinReps = "8"
                            inputMaxReps = "12"
                            inputMuscle = ""
                            showAddDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("add_exercise_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFF003544))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Dodaj nowe ćwiczenie do ${selectedDay.code}",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF003544)
                        )
                    }
                }
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Day selection filter chips
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DayType.entries.forEach { day ->
                        val isSelected = day == selectedDay
                        FilterChip(
                            selected = isSelected,
                            onClick = { onSelectDay(day) },
                            label = { Text(day.code, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CyanAccent.copy(alpha = 0.2f),
                                selectedLabelColor = CyanAccent
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Ćwiczenia (${exercises.size})",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    TextButton(onClick = { showResetDialog = true }) {
                        Text("Reset do PDF", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // Exercise items with edit, reorder and delete actions
            itemsIndexed(exercises, key = { _, item -> item.id }) { index, exercise ->
                EditableExerciseCard(
                    index = index + 1,
                    exercise = exercise,
                    canMoveUp = index > 0,
                    canMoveDown = index < exercises.size - 1,
                    onMoveUp = { onMoveExercise(exercise, true) },
                    onMoveDown = { onMoveExercise(exercise, false) },
                    onEdit = {
                        inputName = exercise.name
                        inputSets = exercise.targetSets.toString()
                        inputMinReps = exercise.minReps.toString()
                        inputMaxReps = exercise.maxReps.toString()
                        inputMuscle = exercise.muscleGroup
                        exerciseToEdit = exercise
                    },
                    onDelete = { exerciseToDelete = exercise }
                )
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
fun EditableExerciseCard(
    index: Int,
    exercise: PlanExerciseEntity,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Index bubble
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$index",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = CyanAccent
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = exercise.name,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${exercise.targetSets} serie • ${exercise.repsRangeLabel} powt. • ${exercise.muscleGroup}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Move Up / Down
            Column {
                IconButton(
                    onClick = onMoveUp,
                    enabled = canMoveUp,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowUpward,
                        contentDescription = "W górę",
                        tint = if (canMoveUp) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                        modifier = Modifier.size(16.dp)
                    )
                }
                IconButton(
                    onClick = onMoveDown,
                    enabled = canMoveDown,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowDownward,
                        contentDescription = "W dół",
                        tint = if (canMoveDown) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Edit
            IconButton(
                onClick = onEdit,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edytuj",
                    tint = CyanAccent,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Delete
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Usuń",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

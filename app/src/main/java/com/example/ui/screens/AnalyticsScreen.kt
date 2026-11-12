package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.WorkoutSessionEntity
import com.example.ui.components.ChartDataPoint
import com.example.ui.components.ProgressionChart
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.LimeAccent
import com.example.ui.viewmodel.AnalyticsMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.ui.theme.getDayTypeColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    sessions: List<WorkoutSessionEntity>,
    distinctExercises: List<String>,
    selectedExercise: String?,
    analyticsMode: AnalyticsMode,
    chartPoints: List<ChartDataPoint>,
    exportImportMessage: String?,
    onDismissMessage: () -> Unit,
    onSelectMode: (AnalyticsMode) -> Unit,
    onSelectExercise: (String?) -> Unit,
    onOpenExerciseDetail: (String) -> Unit,
    onDeleteSession: (Long) -> Unit,
    onOpenCalendar: () -> Unit,
    onExportCsvToUri: (Uri) -> Unit,
    onShareCsv: () -> Unit,
    onImportCsvFromUri: (Uri) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler {
        onBack()
    }

    var sessionToDelete by remember { mutableStateOf<WorkoutSessionEntity?>(null) }

    if (sessionToDelete != null) {
        val s = sessionToDelete!!
        val dateText = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(Date(s.dateMillis))
        AlertDialog(
            onDismissRequest = { sessionToDelete = null },
            title = { Text("Usunąć ten trening?") },
            text = {
                Text(
                    "Czy na pewno chcesz usunąć sesję treningową ${s.dayType} z dnia $dateText? Zostaną usunięte wszystkie zapisane w niej serie i tonaż. Tej operacji nie można cofnąć."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val id = s.id
                        sessionToDelete = null
                        onDeleteSession(id)
                    }
                ) {
                    Text("Usuń trening", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { sessionToDelete = null }) {
                    Text("Anuluj")
                }
            }
        )
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            onExportCsvToUri(uri)
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            onImportCsvFromUri(uri)
        }
    }

    exportImportMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = onDismissMessage,
            title = { Text("Kopia zapasowa CSV") },
            text = { Text(msg) },
            confirmButton = {
                TextButton(onClick = onDismissMessage) {
                    Text("OK", fontWeight = FontWeight.Bold, color = CyanAccent)
                }
            }
        )
    }

    val totalTonnage = sessions.sumOf { it.totalTonnageKg }
    val avgDurationMin = if (sessions.isNotEmpty()) {
        (sessions.sumOf { it.durationSeconds } / sessions.size) / 60
    } else 0

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Progresja & Historia",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Wykresy siły, tonażu i archiwalne treningi",
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
                        onClick = onOpenCalendar,
                        modifier = Modifier.testTag("action_open_calendar")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "Kalendarz treningów",
                            tint = CyanAccent
                        )
                    }
                    IconButton(
                        onClick = {
                            val timestamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
                            exportLauncher.launch("PPL_trening_backup_$timestamp.csv")
                        },
                        modifier = Modifier.testTag("action_export_csv")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = "Eksportuj do CSV",
                            tint = CyanAccent
                        )
                    }
                    IconButton(
                        onClick = {
                            importLauncher.launch(arrayOf("text/*", "text/csv", "text/comma-separated-values", "*/*"))
                        },
                        modifier = Modifier.testTag("action_import_csv")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileUpload,
                            contentDescription = "Importuj z CSV",
                            tint = LimeAccent
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
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
            // Lifetime stats overview cards
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatMetricCard(
                        title = "Treningi",
                        value = "${sessions.size}",
                        icon = Icons.Default.CheckCircle,
                        tint = CyanAccent,
                        modifier = Modifier.weight(1f)
                    )
                    StatMetricCard(
                        title = "Tonaż łącznie",
                        value = "${(totalTonnage / 1000).toInt()} t",
                        icon = Icons.Default.FitnessCenter,
                        tint = LimeAccent,
                        modifier = Modifier.weight(1f)
                    )
                    StatMetricCard(
                        title = "Średni czas",
                        value = "$avgDurationMin min",
                        icon = Icons.Default.Timer,
                        tint = CyanAccent,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Backup & CSV Export / Import Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(CyanAccent.copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Storage,
                                    contentDescription = null,
                                    tint = CyanAccent,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Kopia zapasowa i CSV",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Eksportuj lub importuj historię treningów",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Eksportuj wszystkie zapisane treningi i wykonane serie do pliku CSV zgodnego z programami Excel i Arkusze Google, lub przywróć historię z kopii zapasowej.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    val timestamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
                                    exportLauncher.launch("PPL_trening_backup_$timestamp.csv")
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("btn_export_csv")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FileDownload,
                                    contentDescription = null,
                                    tint = Color(0xFF003544),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Eksportuj", color = Color(0xFF003544), fontWeight = FontWeight.Bold)
                            }

                            FilledTonalButton(
                                onClick = onShareCsv,
                                modifier = Modifier.testTag("btn_share_csv")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Udostępnij CSV",
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    importLauncher.launch(arrayOf("text/*", "text/csv", "text/comma-separated-values", "*/*"))
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("btn_import_csv")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FileUpload,
                                    contentDescription = null,
                                    tint = LimeAccent,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Importuj", color = LimeAccent, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Mode switcher chips
            item {
                Column {
                    Text(
                        text = "Wskaźnik progresji:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AnalyticsMode.entries.forEach { mode ->
                            FilterChip(
                                selected = analyticsMode == mode,
                                onClick = { onSelectMode(mode) },
                                label = { Text(mode.label, style = MaterialTheme.typography.labelSmall) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CyanAccent.copy(alpha = 0.2f),
                                    selectedLabelColor = CyanAccent
                                ),
                                modifier = Modifier.testTag("mode_chip_${mode.name}")
                            )
                        }
                    }
                }
            }

            // Exercise chips if in MAX_WEIGHT or ESTIMATED_1RM mode
            if (analyticsMode != AnalyticsMode.TONNAGE && distinctExercises.isNotEmpty()) {
                item {
                    Column {
                        Text(
                            text = "Wybierz ćwiczenie do analizy:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(distinctExercises) { exName ->
                                val isSelected = exName == (selectedExercise ?: distinctExercises.firstOrNull())
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { onSelectExercise(exName) },
                                    label = { Text(exName, style = MaterialTheme.typography.labelSmall) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = LimeAccent.copy(alpha = 0.2f),
                                        selectedLabelColor = LimeAccent
                                    )
                                )
                            }
                        }

                        val currentEx = selectedExercise ?: distinctExercises.firstOrNull()
                        if (currentEx != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            TextButton(
                                onClick = { onOpenExerciseDetail(currentEx) },
                                modifier = Modifier.testTag("btn_inspect_exercise_progression")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TrendingUp,
                                    contentDescription = null,
                                    tint = CyanAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Zobacz historię powtórzeń i ciężaru dla „$currentEx” ↗",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = CyanAccent
                                )
                            }
                        }
                    }
                }
            }

            // Progression Chart
            item {
                val chartTitle = when (analyticsMode) {
                    AnalyticsMode.TONNAGE -> "Tonaż wszystkich treningów"
                    AnalyticsMode.MAX_WEIGHT -> "Maksymalny ciężar: ${selectedExercise ?: distinctExercises.firstOrNull() ?: ""}"
                    AnalyticsMode.ESTIMATED_1RM -> "Szacowane 1RM: ${selectedExercise ?: distinctExercises.firstOrNull() ?: ""}"
                }

                ProgressionChart(
                    title = chartTitle,
                    unit = "kg",
                    points = chartPoints,
                    accentColor = if (analyticsMode == AnalyticsMode.TONNAGE) LimeAccent else CyanAccent
                )
            }

            // History list header
            item {
                Text(
                    text = "Historia sesji treningowych (${sessions.size})",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (sessions.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = "Brak ukończonych sesji treningowych. Twój pierwszy trening pojawi się tutaj!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            } else {
                items(sessions) { session ->
                    SessionHistoryCard(
                        session = session,
                        onDelete = { sessionToDelete = session }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
fun StatMetricCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(tint.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = value, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
            Text(text = title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun SessionHistoryCard(
    session: WorkoutSessionEntity,
    onDelete: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale.getDefault())
    val durationMin = session.durationSeconds / 60
    val sessionColor = getDayTypeColor(session.dayType)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = sessionColor.copy(alpha = 0.18f)
                    ) {
                        Text(
                            text = session.dayType,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = sessionColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "$durationMin min",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = dateFormat.format(Date(session.dateMillis)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${String.format(Locale.getDefault(), "%.0f", session.totalTonnageKg)} kg",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = LimeAccent
                    )
                    Text(
                        text = "${session.completedSetsCount} serii",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.testTag("delete_session_history_${session.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Usuń trening",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.75f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

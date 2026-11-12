package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DayType
import com.example.data.model.SetLogEntity
import com.example.data.model.WorkoutSessionEntity
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.LegsColor
import com.example.ui.theme.LimeAccent
import com.example.ui.theme.PullColor
import com.example.ui.theme.PushColor
import com.example.ui.theme.getDayTypeColor
import java.text.SimpleDateFormat
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    sessions: List<WorkoutSessionEntity>,
    allSetLogs: List<SetLogEntity>,
    latestSession: WorkoutSessionEntity?,
    selectedDate: LocalDate,
    displayedMonth: YearMonth,
    onSelectDate: (LocalDate) -> Unit,
    onNextMonth: () -> Unit,
    onPreviousMonth: () -> Unit,
    onResetToToday: () -> Unit,
    onDeleteSession: (Long) -> Unit,
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

    // Map sessions by LocalDate
    val sessionsByDate = remember(sessions) {
        sessions.groupBy { session ->
            Instant.ofEpochMilli(session.dateMillis)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
        }
    }

    // Sessions for selected date
    val sessionsOnSelectedDate = sessionsByDate[selectedDate].orEmpty()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Kalendarz Treningów",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Harmonogram sesji Push / Pull / Legs",
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
                        onClick = onResetToToday,
                        modifier = Modifier.testTag("action_reset_today")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = "Idź do dzisiaj",
                            tint = CyanAccent
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
            // "Kiedy ostatnio byłem na treningu" Banner Card
            item {
                LastWorkoutBannerCard(latestSession = latestSession)
            }

            // Interactive Calendar Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Month Header Navigation
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = onPreviousMonth) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Poprzedni miesiąc",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            val polishMonthYear = remember(displayedMonth) {
                                val polishMonths = listOf(
                                    "Styczeń", "Luty", "Marzec", "Kwiecień", "Maj", "Czerwiec",
                                    "Lipiec", "Sierpień", "Wrzesień", "Październik", "Listopad", "Grudzień"
                                )
                                "${polishMonths[displayedMonth.monthValue - 1]} ${displayedMonth.year}"
                            }

                            Text(
                                text = polishMonthYear,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            IconButton(onClick = onNextMonth) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "Następny miesiąc",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Weekday Labels
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val daysOfWeek = listOf("Pn", "Wt", "Śr", "Cz", "Pt", "Sb", "Nd")
                            daysOfWeek.forEach { dayName ->
                                Text(
                                    text = dayName,
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Days Grid
                        val firstDayOfMonth = displayedMonth.atDay(1)
                        val daysInMonth = displayedMonth.lengthOfMonth()
                        // Day of week 1 (Monday) to 7 (Sunday)
                        val firstDayOfWeekIndex = firstDayOfMonth.dayOfWeek.value - 1

                        val totalCells = ((firstDayOfWeekIndex + daysInMonth + 6) / 7) * 7
                        val rows = totalCells / 7

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            for (row in 0 until rows) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    for (col in 0..6) {
                                        val cellIndex = row * 7 + col
                                        val dayNumber = cellIndex - firstDayOfWeekIndex + 1

                                        if (dayNumber in 1..daysInMonth) {
                                            val cellDate = displayedMonth.atDay(dayNumber)
                                            val isSelected = cellDate == selectedDate
                                            val isToday = cellDate == LocalDate.now()
                                            val workoutsOnDay = sessionsByDate[cellDate].orEmpty()

                                            CalendarDayCell(
                                                dayNumber = dayNumber,
                                                isSelected = isSelected,
                                                isToday = isToday,
                                                workouts = workoutsOnDay,
                                                onClick = { onSelectDate(cellDate) },
                                                modifier = Modifier.weight(1f)
                                            )
                                        } else {
                                            // Empty cell for alignment
                                            Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Color Legend for PUSH / PULL / LEGS
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    RoundedCornerShape(10.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LegendItem(label = "PUSH", color = PushColor)
                            LegendItem(label = "PULL", color = PullColor)
                            LegendItem(label = "LEGS", color = LegsColor)
                        }
                    }
                }
            }

            // Day inspection section
            item {
                val polishDateTitle = remember(selectedDate) {
                    val polishMonthsGenitive = listOf(
                        "stycznia", "lutego", "marca", "kwietnia", "maja", "czerwca",
                        "lipca", "sierpnia", "wrzesień", "października", "listopada", "grudnia"
                    )
                    val polishDays = listOf("Poniedziałek", "Wtorek", "Środa", "Czwartek", "Piątek", "Sobota", "Niedziela")
                    val dayName = polishDays[selectedDate.dayOfWeek.value - 1]
                    "$dayName, ${selectedDate.dayOfMonth} ${polishMonthsGenitive[selectedDate.monthValue - 1]} ${selectedDate.year}"
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Dzień: $polishDateTitle",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (sessionsOnSelectedDate.isNotEmpty()) {
                                "Zarejestrowano ${sessionsOnSelectedDate.size} sesję treningową"
                            } else {
                                "Dzień odpoczynku (brak zarejestrowanego treningu)"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (sessionsOnSelectedDate.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "W tym dniu nie zarejestrowano żadnego treningu.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(sessionsOnSelectedDate) { session ->
                    val sessionLogs = remember(session.id, allSetLogs) {
                        allSetLogs.filter { it.sessionId == session.id }
                    }
                    CalendarSessionItemCard(
                        session = session,
                        setLogs = sessionLogs,
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
fun CalendarDayCell(
    dayNumber: Int,
    isSelected: Boolean,
    isToday: Boolean,
    workouts: List<WorkoutSessionEntity>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when {
        isSelected -> CyanAccent.copy(alpha = 0.2f)
        workouts.isNotEmpty() -> MaterialTheme.colorScheme.surfaceVariant
        else -> Color.Transparent
    }

    val borderColor = when {
        isSelected -> CyanAccent
        isToday -> LimeAccent
        else -> Color.Transparent
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(backgroundColor)
            .border(if (isSelected || isToday) 1.5.dp else 0.dp, borderColor, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(3.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "$dayNumber",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = if (isSelected || isToday || workouts.isNotEmpty()) FontWeight.Bold else FontWeight.Normal
                ),
                color = when {
                    isSelected -> CyanAccent
                    isToday -> LimeAccent
                    workouts.isNotEmpty() -> MaterialTheme.colorScheme.onSurface
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )

            // Coloured Dots for workouts on this day
            if (workouts.isNotEmpty()) {
                Spacer(modifier = Modifier.height(3.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    workouts.take(3).forEach { w ->
                        val dotColor = getDayTypeColor(w.dayType)
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(dotColor, CircleShape)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun LastWorkoutBannerCard(latestSession: WorkoutSessionEntity?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(CyanAccent.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = CyanAccent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Ostatni trening",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (latestSession != null) {
                    val sessionColor = getDayTypeColor(latestSession.dayType)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = sessionColor.copy(alpha = 0.18f)
                    ) {
                        Text(
                            text = latestSession.dayType,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = sessionColor,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (latestSession == null) {
                Text(
                    text = "Brak zarejestrowanych sesji. Rozpocznij swój pierwszy trening z planu PPL!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                val sessionDate = Instant.ofEpochMilli(latestSession.dateMillis)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                val today = LocalDate.now()
                val daysAgo = ChronoUnit.DAYS.between(sessionDate, today)

                val relativeText = when (daysAgo) {
                    0L -> "Dzisiaj"
                    1L -> "Wczoraj"
                    else -> "$daysAgo dni temu"
                }

                val fullDateFormat = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale.getDefault())
                val formattedDate = fullDateFormat.format(Date(latestSession.dateMillis))

                Text(
                    text = "$relativeText ($formattedDate)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.FitnessCenter,
                            contentDescription = null,
                            tint = LimeAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${String.format(Locale.getDefault(), "%.0f", latestSession.totalTonnageKg)} kg tonażu",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = CyanAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${latestSession.completedSetsCount} serii",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = Color(0xFFFF6B4A),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${latestSession.durationSeconds / 60} min",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CalendarSessionItemCard(
    session: WorkoutSessionEntity,
    setLogs: List<SetLogEntity>,
    onDelete: () -> Unit
) {
    val sessionColor = getDayTypeColor(session.dayType)
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header with dayType badge, time, and delete button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = sessionColor.copy(alpha = 0.18f)
                    ) {
                        Text(
                            text = session.dayType,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = sessionColor,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Text(
                        text = "Godz. ${timeFormat.format(Date(session.dateMillis))} • ${session.durationSeconds / 60} min",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.testTag("delete_session_${session.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Usuń trening",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Metrics row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text("Tonaż", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = "${String.format(Locale.getDefault(), "%.0f", session.totalTonnageKg)} kg",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = LimeAccent
                        )
                    }
                }

                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text("Ukończone serie", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = "${session.completedSetsCount} serii",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Grouped Exercise Logs
            if (setLogs.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Wykonane ćwiczenia:",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))

                val grouped = setLogs.groupBy { it.exerciseName }
                grouped.forEach { (exerciseName, sets) ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = exerciseName,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            val setsSummary = sets.joinToString(separator = ", ") { s ->
                                "${s.weightKg} kg × ${s.reps}"
                            }
                            Text(
                                text = setsSummary,
                                style = MaterialTheme.typography.labelSmall,
                                color = CyanAccent
                            )
                        }
                    }
                }
            }
        }
    }
}

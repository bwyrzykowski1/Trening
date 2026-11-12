package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.platform.LocalContext
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.ActiveWorkoutMiniBar
import com.example.ui.components.ExerciseDetailDialog
import com.example.ui.screens.ActiveWorkoutScreen
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.CalendarScreen
import com.example.ui.screens.EditPlanScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.SummaryScreen
import com.example.ui.theme.PPLWorkoutTheme
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.WorkoutViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PPLWorkoutTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    WorkoutApp()
                }
            }
        }
    }
}

@Composable
fun WorkoutApp(viewModel: WorkoutViewModel = viewModel()) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val selectedDay by viewModel.selectedDay.collectAsStateWithLifecycle()
    val exercises by viewModel.exercisesForSelectedDay.collectAsStateWithLifecycle()
    val activeWorkoutState by viewModel.activeWorkout.collectAsStateWithLifecycle()
    val summarySession by viewModel.summarySession.collectAsStateWithLifecycle()
    val summarySetLogs by viewModel.summarySetLogs.collectAsStateWithLifecycle()
    val allSessions by viewModel.allSessions.collectAsStateWithLifecycle()
    val allLoggedExercises by viewModel.allLoggedExercises.collectAsStateWithLifecycle()
    val analyticsMode by viewModel.analyticsMode.collectAsStateWithLifecycle()
    val selectedAnalyticsExercise by viewModel.selectedAnalyticsExercise.collectAsStateWithLifecycle()
    val chartPoints by viewModel.chartPoints.collectAsStateWithLifecycle()
    val exportImportMessage by viewModel.exportImportMessage.collectAsStateWithLifecycle()

    val latestSession by viewModel.latestSession.collectAsStateWithLifecycle()
    val allSetLogs by viewModel.allSetLogs.collectAsStateWithLifecycle()
    val detailExerciseName by viewModel.detailExerciseName.collectAsStateWithLifecycle()
    val detailMetric by viewModel.detailMetric.collectAsStateWithLifecycle()
    val detailExerciseLogs by viewModel.detailExerciseLogs.collectAsStateWithLifecycle()
    val detailExerciseChartPoints by viewModel.detailExerciseChartPoints.collectAsStateWithLifecycle()
    val calendarSelectedDate by viewModel.calendarSelectedDate.collectAsStateWithLifecycle()
    val calendarMonth by viewModel.calendarMonth.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = currentScreen,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "screen_transition",
            modifier = Modifier.fillMaxSize()
        ) { screen ->
            when (screen) {
                AppScreen.HOME -> {
                    HomeScreen(
                        selectedDay = selectedDay,
                        exercises = exercises,
                        latestSession = latestSession,
                        isWorkoutActive = activeWorkoutState.isActive,
                        onResumeWorkout = { viewModel.navigateTo(AppScreen.ACTIVE_WORKOUT) },
                        onSelectDay = { viewModel.selectDay(it) },
                        onStartWorkout = { viewModel.startWorkout() },
                        onOpenEditPlan = { viewModel.navigateTo(AppScreen.EDIT_PLAN) },
                        onOpenAnalytics = { viewModel.navigateTo(AppScreen.ANALYTICS) },
                        onOpenCalendar = { viewModel.navigateTo(AppScreen.CALENDAR) },
                        onOpenExerciseDetail = { viewModel.openExerciseDetails(it) }
                    )
                }

                AppScreen.ACTIVE_WORKOUT -> {
                    ActiveWorkoutScreen(
                        state = activeWorkoutState,
                        onMinimizeWorkout = { viewModel.navigateTo(AppScreen.HOME) },
                        onToggleSound = { viewModel.toggleSound() },
                        onTogglePlayPause = { viewModel.togglePlayPause() },
                        onSkipInterval = { viewModel.skipInterval() },
                        onAdd30Seconds = { viewModel.add30Seconds() },
                        onWeightChanged = { viewModel.updateInputWeight(it) },
                        onRepsChanged = { viewModel.updateInputReps(it) },
                        onAdjustWeight = { viewModel.adjustWeight(it) },
                        onAdjustReps = { viewModel.adjustReps(it) },
                        onApplySuggestion = { viewModel.applySuggestion() },
                        onLogSetAndRest = { viewModel.logSetAndRest() },
                        onFinishRestAndStartNext = { viewModel.finishRestAndStartNext() },
                        onNextExercise = { viewModel.nextExercise() },
                        onFinishWorkout = { viewModel.finishWorkout() },
                        onCancelWorkout = { viewModel.cancelWorkout() }
                    )
                }

                AppScreen.SUMMARY -> {
                    SummaryScreen(
                        session = summarySession,
                        setLogs = summarySetLogs,
                        chartPoints = chartPoints,
                        analyticsMode = analyticsMode,
                        onSetAnalyticsMode = { viewModel.setAnalyticsMode(it) },
                        onBackToHome = { viewModel.navigateTo(AppScreen.HOME) },
                        onOpenFullAnalytics = { viewModel.navigateTo(AppScreen.ANALYTICS) }
                    )
                }

                AppScreen.EDIT_PLAN -> {
                    EditPlanScreen(
                        selectedDay = selectedDay,
                        exercises = exercises,
                        onSelectDay = { viewModel.selectDay(it) },
                        onAddExercise = { day, name, sets, minReps, maxReps, muscle ->
                            viewModel.addExerciseToPlan(day, name, sets, minReps, maxReps, muscle)
                        },
                        onUpdateExercise = { viewModel.updateExerciseInPlan(it) },
                        onDeleteExercise = { viewModel.deleteExerciseFromPlan(it) },
                        onMoveExercise = { ex, up -> viewModel.moveExercise(ex, up) },
                        onResetToDefaults = { viewModel.resetPlanToDefaults() },
                        onBack = { viewModel.navigateTo(AppScreen.HOME) }
                    )
                }

                AppScreen.ANALYTICS -> {
                    val context = LocalContext.current
                    AnalyticsScreen(
                        sessions = allSessions,
                        distinctExercises = allLoggedExercises,
                        selectedExercise = selectedAnalyticsExercise,
                        analyticsMode = analyticsMode,
                        chartPoints = chartPoints,
                        exportImportMessage = exportImportMessage,
                        onDismissMessage = { viewModel.dismissExportImportMessage() },
                        onSelectMode = { viewModel.setAnalyticsMode(it) },
                        onSelectExercise = { viewModel.setAnalyticsExercise(it) },
                        onOpenExerciseDetail = { viewModel.openExerciseDetails(it) },
                        onDeleteSession = { viewModel.deleteWorkoutSession(it) },
                        onOpenCalendar = { viewModel.navigateTo(AppScreen.CALENDAR) },
                        onExportCsvToUri = { uri -> viewModel.exportCsvToUri(uri) { /* complete */ } },
                        onShareCsv = {
                            viewModel.shareCsv(context) { shareUri ->
                                if (shareUri != null) {
                                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/csv"
                                        putExtra(Intent.EXTRA_STREAM, shareUri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, "Udostępnij kopię treningów CSV"))
                                }
                            }
                        },
                        onImportCsvFromUri = { uri -> viewModel.importCsvFromUri(uri) },
                        onBack = { viewModel.navigateTo(AppScreen.HOME) }
                    )
                }

                AppScreen.CALENDAR -> {
                    CalendarScreen(
                        sessions = allSessions,
                        allSetLogs = allSetLogs,
                        latestSession = latestSession,
                        selectedDate = calendarSelectedDate,
                        displayedMonth = calendarMonth,
                        onSelectDate = { viewModel.selectCalendarDate(it) },
                        onNextMonth = { viewModel.nextCalendarMonth() },
                        onPreviousMonth = { viewModel.previousCalendarMonth() },
                        onResetToToday = { viewModel.resetCalendarToToday() },
                        onDeleteSession = { viewModel.deleteWorkoutSession(it) },
                        onBack = { viewModel.navigateTo(AppScreen.HOME) }
                    )
                }
            }
        }

        if (currentScreen != AppScreen.ACTIVE_WORKOUT && activeWorkoutState.isActive) {
            ActiveWorkoutMiniBar(
                state = activeWorkoutState,
                onExpand = { viewModel.navigateTo(AppScreen.ACTIVE_WORKOUT) },
                onTogglePlayPause = { viewModel.togglePlayPause() },
                onSkipInterval = { viewModel.skipInterval() },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }

        // Exercise Progression & Detail Dialog
        detailExerciseName?.let { exName ->
            val matchingPlanEx = exercises.find { it.name.equals(exName, ignoreCase = true) }
            ExerciseDetailDialog(
                exerciseName = exName,
                muscleGroup = matchingPlanEx?.muscleGroup.orEmpty(),
                targetSets = matchingPlanEx?.targetSets ?: 3,
                repsRange = matchingPlanEx?.repsRangeLabel.orEmpty(),
                selectedMetric = detailMetric,
                chartPoints = detailExerciseChartPoints,
                logs = detailExerciseLogs,
                onSelectMetric = { viewModel.setDetailMetric(it) },
                onDismiss = { viewModel.closeExerciseDetails() }
            )
        }
    }
}

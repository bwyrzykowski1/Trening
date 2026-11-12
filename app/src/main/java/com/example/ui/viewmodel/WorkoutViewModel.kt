package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.WorkoutRepository
import com.example.data.csv.CsvManager
import com.example.data.model.DayType
import com.example.data.model.PlanExerciseEntity
import com.example.data.model.SetLogEntity
import com.example.data.model.WorkoutSessionEntity
import com.example.ui.components.ChartDataPoint
import com.example.ui.components.IntervalPhase
import com.example.ui.util.HapticUtil
import com.example.ui.util.SoundCueUtil
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.YearMonth
import java.util.Date
import java.util.Locale

enum class AppScreen {
    HOME,
    ACTIVE_WORKOUT,
    SUMMARY,
    EDIT_PLAN,
    ANALYTICS,
    CALENDAR
}

enum class AnalyticsMode(val label: String) {
    TONNAGE("Tonaż sesji (kg)"),
    MAX_WEIGHT("Ciężar maksymalny (kg)"),
    ESTIMATED_1RM("Szacowane 1RM (kg)")
}

enum class ExerciseMetric(val label: String) {
    WEIGHT("Ciężar (kg)"),
    REPS("Powtórzenia (reps)"),
    ESTIMATED_1RM("Szacowane 1RM (kg)")
}

data class ActiveWorkoutState(
    val isActive: Boolean = false,
    val dayType: DayType = DayType.PUSH,
    val exercises: List<PlanExerciseEntity> = emptyList(),
    val currentExerciseIndex: Int = 0,
    val currentSetNumber: Int = 1,
    val intervalPhase: IntervalPhase = IntervalPhase.EXERCISE,
    val timerSecondsRemaining: Int = 120,
    val timerTotalSeconds: Int = 120,
    val isTimerRunning: Boolean = false,
    val startTimestamp: Long = 0L,
    val recordedLogs: List<SetLogEntity> = emptyList(),
    val suggestedLastSet: SetLogEntity? = null,
    val inputWeight: String = "",
    val inputReps: String = "",
    val isSoundEnabled: Boolean = true
) {
    val currentExercise: PlanExerciseEntity?
        get() = exercises.getOrNull(currentExerciseIndex)

    val isLastSetOfWorkout: Boolean
        get() {
            val ex = currentExercise ?: return true
            val isLastExercise = currentExerciseIndex == exercises.lastIndex
            val isLastSet = currentSetNumber >= ex.targetSets
            return isLastExercise && isLastSet
        }

    val isLastSetOfCurrentExercise: Boolean
        get() {
            val ex = currentExercise ?: return true
            return currentSetNumber >= ex.targetSets
        }

    val nextExercise: PlanExerciseEntity?
        get() = exercises.getOrNull(currentExerciseIndex + 1)
}

class WorkoutViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: WorkoutRepository
    private var timerJob: Job? = null

    // Screen navigation state
    private val _currentScreen = MutableStateFlow(AppScreen.HOME)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    // Selected Day on Home Screen
    private val _selectedDay = MutableStateFlow(DayType.getSuggestedForToday())
    val selectedDay: StateFlow<DayType> = _selectedDay.asStateFlow()

    // Active Workout state
    private val _activeWorkout = MutableStateFlow(ActiveWorkoutState())
    val activeWorkout: StateFlow<ActiveWorkoutState> = _activeWorkout.asStateFlow()

    // Last completed session for summary
    private val _summarySession = MutableStateFlow<WorkoutSessionEntity?>(null)
    val summarySession: StateFlow<WorkoutSessionEntity?> = _summarySession.asStateFlow()

    private val _summarySetLogs = MutableStateFlow<List<SetLogEntity>>(emptyList())
    val summarySetLogs: StateFlow<List<SetLogEntity>> = _summarySetLogs.asStateFlow()

    // Analytics state
    private val _analyticsMode = MutableStateFlow(AnalyticsMode.TONNAGE)
    val analyticsMode: StateFlow<AnalyticsMode> = _analyticsMode.asStateFlow()

    private val _selectedAnalyticsExercise = MutableStateFlow<String?>(null)
    val selectedAnalyticsExercise: StateFlow<String?> = _selectedAnalyticsExercise.asStateFlow()

    // Export & Import notification message
    private val _exportImportMessage = MutableStateFlow<String?>(null)
    val exportImportMessage: StateFlow<String?> = _exportImportMessage.asStateFlow()

    init {
        val database = AppDatabase.getInstance(application)
        repository = WorkoutRepository(database.workoutDao())
        viewModelScope.launch {
            repository.preloadDefaultsIfEmpty()
        }
    }

    // Exercises for selected day
    val exercisesForSelectedDay: StateFlow<List<PlanExerciseEntity>> = _selectedDay
        .flatMapLatest { day -> repository.getExercisesForDay(day.code) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All past sessions
    val allSessions: StateFlow<List<WorkoutSessionEntity>> = repository.allSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All distinct exercises that have been logged
    val allLoggedExercises: StateFlow<List<String>> = repository.allLoggedExercises
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Chart points derived from state
    val chartPoints: StateFlow<List<ChartDataPoint>> = combine(
        _analyticsMode,
        _selectedAnalyticsExercise,
        allSessions,
        repository.getAllSetLogs()
    ) { mode, exercise, sessions, allLogs ->
        val dateFormat = SimpleDateFormat("dd.MM", Locale.getDefault())
        when (mode) {
            AnalyticsMode.TONNAGE -> {
                sessions.reversed().map { session ->
                    ChartDataPoint(
                        timestamp = session.dateMillis,
                        value = session.totalTonnageKg,
                        label = "${dateFormat.format(Date(session.dateMillis))} (${session.dayType})"
                    )
                }
            }
            AnalyticsMode.MAX_WEIGHT -> {
                val targetName = exercise ?: allLogs.firstOrNull()?.exerciseName
                if (targetName == null) emptyList()
                else {
                    // Group by session/date and find max weight per session
                    val logsForEx = allLogs.filter { it.exerciseName == targetName }
                    val grouped = logsForEx.groupBy { it.sessionId }
                    grouped.map { (_, logs) ->
                        val maxLog = logs.maxByOrNull { it.weightKg } ?: logs.first()
                        ChartDataPoint(
                            timestamp = maxLog.timestamp,
                            value = maxLog.weightKg,
                            label = "${dateFormat.format(Date(maxLog.timestamp))}: ${maxLog.weightKg} kg"
                        )
                    }.sortedBy { it.timestamp }
                }
            }
            AnalyticsMode.ESTIMATED_1RM -> {
                val targetName = exercise ?: allLogs.firstOrNull()?.exerciseName
                if (targetName == null) emptyList()
                else {
                    val logsForEx = allLogs.filter { it.exerciseName == targetName }
                    val grouped = logsForEx.groupBy { it.sessionId }
                    grouped.map { (_, logs) ->
                        val best1RM = logs.maxByOrNull { it.estimated1RM } ?: logs.first()
                        ChartDataPoint(
                            timestamp = best1RM.timestamp,
                            value = best1RM.estimated1RM,
                            label = "${dateFormat.format(Date(best1RM.timestamp))}: ${String.format(Locale.getDefault(), "%.1f", best1RM.estimated1RM)} kg"
                        )
                    }.sortedBy { it.timestamp }
                }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All set logs flow
    val allSetLogs: StateFlow<List<SetLogEntity>> = repository.getAllSetLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Latest session for "when was the last workout" banner
    val latestSession: StateFlow<WorkoutSessionEntity?> = allSessions
        .map { sessions -> sessions.maxByOrNull { it.dateMillis } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Calendar state
    private val _calendarSelectedDate = MutableStateFlow(LocalDate.now())
    val calendarSelectedDate: StateFlow<LocalDate> = _calendarSelectedDate.asStateFlow()

    private val _calendarMonth = MutableStateFlow(YearMonth.now())
    val calendarMonth: StateFlow<YearMonth> = _calendarMonth.asStateFlow()

    fun selectCalendarDate(date: LocalDate) {
        _calendarSelectedDate.value = date
    }

    fun nextCalendarMonth() {
        _calendarMonth.update { it.plusMonths(1) }
    }

    fun previousCalendarMonth() {
        _calendarMonth.update { it.minusMonths(1) }
    }

    fun resetCalendarToToday() {
        _calendarMonth.value = YearMonth.now()
        _calendarSelectedDate.value = LocalDate.now()
    }

    // Exercise details & progression modal state
    private val _detailExerciseName = MutableStateFlow<String?>(null)
    val detailExerciseName: StateFlow<String?> = _detailExerciseName.asStateFlow()

    private val _detailMetric = MutableStateFlow(ExerciseMetric.WEIGHT)
    val detailMetric: StateFlow<ExerciseMetric> = _detailMetric.asStateFlow()

    fun openExerciseDetails(exerciseName: String) {
        _detailExerciseName.value = exerciseName
    }

    fun closeExerciseDetails() {
        _detailExerciseName.value = null
    }

    fun setDetailMetric(metric: ExerciseMetric) {
        _detailMetric.value = metric
    }

    // Detail logs for selected exercise
    val detailExerciseLogs: StateFlow<List<SetLogEntity>> = combine(
        _detailExerciseName,
        allSetLogs
    ) { name, logs ->
        if (name.isNullOrBlank()) emptyList()
        else logs.filter { it.exerciseName.equals(name, ignoreCase = true) }.sortedByDescending { it.timestamp }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Progression chart points for selected exercise (toggling WEIGHT vs REPS vs 1RM)
    val detailExerciseChartPoints: StateFlow<List<ChartDataPoint>> = combine(
        _detailExerciseName,
        _detailMetric,
        allSetLogs
    ) { name, metric, logs ->
        if (name.isNullOrBlank()) emptyList()
        else {
            val exLogs = logs.filter { it.exerciseName.equals(name, ignoreCase = true) }
            val grouped = exLogs.groupBy { it.sessionId }
            val dateFormat = SimpleDateFormat("dd.MM", Locale.getDefault())
            grouped.mapNotNull { (_, sessionLogs) ->
                if (sessionLogs.isEmpty()) null
                else {
                    val firstLog = sessionLogs.first()
                    when (metric) {
                        ExerciseMetric.WEIGHT -> {
                            val maxW = sessionLogs.maxOf { it.weightKg }
                            ChartDataPoint(
                                timestamp = firstLog.timestamp,
                                value = maxW,
                                label = "${dateFormat.format(Date(firstLog.timestamp))}: ${maxW} kg"
                            )
                        }
                        ExerciseMetric.REPS -> {
                            val maxR = sessionLogs.maxOf { it.reps }.toDouble()
                            ChartDataPoint(
                                timestamp = firstLog.timestamp,
                                value = maxR,
                                label = "${dateFormat.format(Date(firstLog.timestamp))}: ${maxR.toInt()} powt."
                            )
                        }
                        ExerciseMetric.ESTIMATED_1RM -> {
                            val best1RM = sessionLogs.maxOf { it.estimated1RM }
                            ChartDataPoint(
                                timestamp = firstLog.timestamp,
                                value = best1RM,
                                label = "${dateFormat.format(Date(firstLog.timestamp))}: ${String.format(Locale.getDefault(), "%.1f", best1RM)} kg"
                            )
                        }
                    }
                }
            }.sortedBy { it.timestamp }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Delete workout session
    fun deleteWorkoutSession(sessionId: Long) {
        viewModelScope.launch {
            repository.deleteWorkoutSession(sessionId)
            _exportImportMessage.value = "Sesja treningowa została usunięta z historii."
        }
    }

    // Navigation methods
    fun navigateTo(screen: AppScreen) {
        _currentScreen.value = screen
    }

    fun selectDay(dayType: DayType) {
        _selectedDay.value = dayType
    }

    // Start Workout
    fun startWorkout() {
        val exercises = exercisesForSelectedDay.value
        if (exercises.isEmpty()) return

        val firstExercise = exercises.first()
        _activeWorkout.value = ActiveWorkoutState(
            isActive = true,
            dayType = _selectedDay.value,
            exercises = exercises,
            currentExerciseIndex = 0,
            currentSetNumber = 1,
            intervalPhase = IntervalPhase.EXERCISE,
            timerSecondsRemaining = 120,
            timerTotalSeconds = 120,
            isTimerRunning = true,
            startTimestamp = System.currentTimeMillis(),
            recordedLogs = emptyList(),
            inputWeight = "",
            inputReps = ""
        )

        loadSuggestionForExercise(firstExercise.name)
        startTimerLoop()
        _currentScreen.value = AppScreen.ACTIVE_WORKOUT
    }

    private fun loadSuggestionForExercise(exerciseName: String) {
        viewModelScope.launch {
            val lastSet = repository.getLatestSetForExercise(exerciseName)
            _activeWorkout.update { state ->
                state.copy(
                    suggestedLastSet = lastSet,
                    inputWeight = if (state.inputWeight.isEmpty() && lastSet != null) {
                        String.format(Locale.US, "%.1f", lastSet.weightKg).removeSuffix(".0")
                    } else state.inputWeight,
                    inputReps = if (state.inputReps.isEmpty() && lastSet != null) {
                        lastSet.reps.toString()
                    } else state.inputReps
                )
            }
        }
    }

    fun applySuggestion() {
        val suggestion = _activeWorkout.value.suggestedLastSet ?: return
        _activeWorkout.update {
            it.copy(
                inputWeight = String.format(Locale.US, "%.1f", suggestion.weightKg).removeSuffix(".0"),
                inputReps = suggestion.reps.toString()
            )
        }
    }

    fun updateInputWeight(weight: String) {
        _activeWorkout.update { it.copy(inputWeight = weight) }
    }

    fun updateInputReps(reps: String) {
        _activeWorkout.update { it.copy(inputReps = reps) }
    }

    fun adjustWeight(delta: Double) {
        val current = _activeWorkout.value.inputWeight.toDoubleOrNull() ?: 0.0
        val next = (current + delta).coerceAtLeast(0.0)
        _activeWorkout.update {
            it.copy(inputWeight = String.format(Locale.US, "%.1f", next).removeSuffix(".0"))
        }
    }

    fun adjustReps(delta: Int) {
        val current = _activeWorkout.value.inputReps.toIntOrNull() ?: 0
        val next = (current + delta).coerceAtLeast(0)
        _activeWorkout.update { it.copy(inputReps = next.toString()) }
    }

    // Timer Controls
    fun togglePlayPause() {
        val current = _activeWorkout.value.isTimerRunning
        if (current) {
            pauseTimer()
        } else {
            resumeTimer()
        }
    }

    fun pauseTimer() {
        _activeWorkout.update { it.copy(isTimerRunning = false) }
        timerJob?.cancel()
    }

    fun resumeTimer() {
        _activeWorkout.update { it.copy(isTimerRunning = true) }
        startTimerLoop()
    }

    fun add30Seconds() {
        _activeWorkout.update {
            val newRemaining = it.timerSecondsRemaining + 30
            val newTotal = it.timerTotalSeconds.coerceAtLeast(newRemaining)
            it.copy(timerSecondsRemaining = newRemaining, timerTotalSeconds = newTotal)
        }
    }

    fun skipInterval() {
        transitionToNextInterval(isSkipped = true)
    }

    fun toggleSound() {
        _activeWorkout.update { it.copy(isSoundEnabled = !it.isSoundEnabled) }
    }

    private fun startTimerLoop() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000L)
                val current = _activeWorkout.value
                if (!current.isTimerRunning) break

                if (current.timerSecondsRemaining > 1) {
                    val nextSec = current.timerSecondsRemaining - 1
                    // 3-2-1 second countdown tick
                    if (current.isSoundEnabled && nextSec in 1..3) {
                        SoundCueUtil.playCountdownTick()
                    }
                    _activeWorkout.update {
                        it.copy(timerSecondsRemaining = nextSec)
                    }
                } else {
                    // Interval reached 0
                    HapticUtil.vibratePattern(getApplication())
                    transitionToNextInterval(isSkipped = false)
                }
            }
        }
    }

    private fun transitionToNextInterval(isSkipped: Boolean) {
        val current = _activeWorkout.value
        HapticUtil.vibrateShort(getApplication())

        if (current.intervalPhase == IntervalPhase.EXERCISE) {
            // Set ended -> play set ended audio cue
            if (current.isSoundEnabled) {
                SoundCueUtil.playSetEndedCue()
            }
            // Switch to Rest interval (2 min = 120s)
            _activeWorkout.update {
                it.copy(
                    intervalPhase = IntervalPhase.REST,
                    timerSecondsRemaining = 120,
                    timerTotalSeconds = 120,
                    isTimerRunning = true
                )
            }
        } else {
            // Rest ended -> play rest concluded chime (time to lift!)
            if (current.isSoundEnabled) {
                SoundCueUtil.playRestConcludedCue()
            }
            // Finished rest interval -> prepare next set / exercise
            advanceToNextSetOrExercise()
        }
    }

    // Log current set
    fun logSetAndRest() {
        val current = _activeWorkout.value
        val ex = current.currentExercise ?: return

        val weight = current.inputWeight.toDoubleOrNull() ?: 0.0
        val reps = current.inputReps.toIntOrNull() ?: ex.minReps

        val log = SetLogEntity(
            sessionId = 0L, // set upon session save
            exerciseName = ex.name,
            setNumber = current.currentSetNumber,
            weightKg = weight,
            reps = reps,
            timestamp = System.currentTimeMillis()
        )

        val updatedLogs = current.recordedLogs + log
        _activeWorkout.update {
            it.copy(
                recordedLogs = updatedLogs,
                intervalPhase = IntervalPhase.REST,
                timerSecondsRemaining = 120,
                timerTotalSeconds = 120,
                isTimerRunning = true
            )
        }

        if (current.isSoundEnabled) {
            SoundCueUtil.playSetEndedCue()
        }
        HapticUtil.vibrateShort(getApplication())
    }

    // Finish rest and proceed to the next set or exercise immediately
    fun finishRestAndStartNext() {
        val current = _activeWorkout.value
        if (current.intervalPhase == IntervalPhase.REST) {
            if (current.isSoundEnabled) {
                SoundCueUtil.playRestConcludedCue()
            }
            HapticUtil.vibrateShort(getApplication())
            advanceToNextSetOrExercise()
        }
    }

    // Advance to next set or exercise after rest
    private fun advanceToNextSetOrExercise() {
        val current = _activeWorkout.value
        val ex = current.currentExercise ?: return

        if (current.currentSetNumber < ex.targetSets) {
            // Next set of current exercise
            _activeWorkout.update {
                it.copy(
                    currentSetNumber = it.currentSetNumber + 1,
                    intervalPhase = IntervalPhase.EXERCISE,
                    timerSecondsRemaining = 120,
                    timerTotalSeconds = 120,
                    isTimerRunning = true
                )
            }
        } else {
            // Finished all sets for current exercise -> go to next exercise
            val nextExerciseIndex = current.currentExerciseIndex + 1
            if (nextExerciseIndex < current.exercises.size) {
                val nextExercise = current.exercises[nextExerciseIndex]
                _activeWorkout.update {
                    it.copy(
                        currentExerciseIndex = nextExerciseIndex,
                        currentSetNumber = 1,
                        intervalPhase = IntervalPhase.EXERCISE,
                        timerSecondsRemaining = 120,
                        timerTotalSeconds = 120,
                        isTimerRunning = true,
                        inputWeight = "",
                        inputReps = ""
                    )
                }
                loadSuggestionForExercise(nextExercise.name)
            } else {
                // Completed entire workout!
                finishWorkout()
            }
        }
    }

    // Manual next exercise if needed
    fun nextExercise() {
        val current = _activeWorkout.value
        val nextExerciseIndex = current.currentExerciseIndex + 1
        if (nextExerciseIndex < current.exercises.size) {
            val nextExercise = current.exercises[nextExerciseIndex]
            _activeWorkout.update {
                it.copy(
                    currentExerciseIndex = nextExerciseIndex,
                    currentSetNumber = 1,
                    intervalPhase = IntervalPhase.EXERCISE,
                    timerSecondsRemaining = 120,
                    timerTotalSeconds = 120,
                    isTimerRunning = true,
                    inputWeight = "",
                    inputReps = ""
                )
            }
            loadSuggestionForExercise(nextExercise.name)
        } else {
            finishWorkout()
        }
    }

    // Finish Workout & save to Room
    fun finishWorkout() {
        timerJob?.cancel()
        val current = _activeWorkout.value
        val durationSeconds = ((System.currentTimeMillis() - current.startTimestamp) / 1000).toInt().coerceAtLeast(60)

        viewModelScope.launch {
            val sessionId = repository.saveCompletedSession(
                dayType = current.dayType.code,
                durationSeconds = durationSeconds,
                setLogs = current.recordedLogs
            )

            val details = repository.getSessionDetails(sessionId)
            _summarySession.value = details.first
            _summarySetLogs.value = details.second

            _activeWorkout.value = ActiveWorkoutState()
            _currentScreen.value = AppScreen.SUMMARY
            if (current.isSoundEnabled) {
                SoundCueUtil.playWorkoutCompleteCue()
            }
            HapticUtil.vibratePattern(getApplication())
        }
    }

    // Cancel workout
    fun cancelWorkout() {
        timerJob?.cancel()
        _activeWorkout.value = ActiveWorkoutState()
        _currentScreen.value = AppScreen.HOME
    }

    // Plan editing CRUD
    fun addExerciseToPlan(
        dayType: DayType,
        name: String,
        targetSets: Int,
        minReps: Int,
        maxReps: Int,
        muscleGroup: String
    ) {
        viewModelScope.launch {
            val currentList = exercisesForSelectedDay.value
            val newExercise = PlanExerciseEntity(
                dayType = dayType.code,
                name = name.trim(),
                targetSets = targetSets,
                minReps = minReps,
                maxReps = maxReps,
                orderIndex = currentList.size,
                muscleGroup = muscleGroup.trim(),
                isCustom = true
            )
            repository.addExercise(newExercise)
        }
    }

    fun updateExerciseInPlan(exercise: PlanExerciseEntity) {
        viewModelScope.launch {
            repository.updateExercise(exercise)
        }
    }

    fun deleteExerciseFromPlan(exercise: PlanExerciseEntity) {
        viewModelScope.launch {
            repository.deleteExercise(exercise)
        }
    }

    fun moveExercise(exercise: PlanExerciseEntity, moveUp: Boolean) {
        viewModelScope.launch {
            val currentList = exercisesForSelectedDay.value.toMutableList()
            val index = currentList.indexOfFirst { it.id == exercise.id }
            if (index == -1) return@launch

            val targetIndex = if (moveUp) index - 1 else index + 1
            if (targetIndex in currentList.indices) {
                val temp = currentList[index]
                currentList[index] = currentList[targetIndex]
                currentList[targetIndex] = temp
                repository.reorderExercises(currentList)
            }
        }
    }

    fun resetPlanToDefaults() {
        viewModelScope.launch {
            repository.resetToDefaults()
        }
    }

    // Analytics controls
    fun setAnalyticsMode(mode: AnalyticsMode) {
        _analyticsMode.value = mode
    }

    fun setAnalyticsExercise(exerciseName: String?) {
        _selectedAnalyticsExercise.value = exerciseName
    }

    // CSV Export & Import actions
    fun dismissExportImportMessage() {
        _exportImportMessage.value = null
    }

    suspend fun getExportCsvContent(): String {
        return repository.exportAllDataToCsv()
    }

    fun exportCsvToUri(targetUri: Uri, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val csv = repository.exportAllDataToCsv()
            val success = CsvManager.writeCsvToUri(getApplication<Application>().contentResolver, targetUri, csv)
            if (success) {
                _exportImportMessage.value = "Historia treningów została pomyślnie wyeksportowana do pliku CSV."
                HapticUtil.vibrateShort(getApplication())
            } else {
                _exportImportMessage.value = "Błąd zapisu pliku CSV."
            }
            onComplete(success)
        }
    }

    fun shareCsv(context: Context, onShareUriReady: (Uri?) -> Unit) {
        viewModelScope.launch {
            val csv = repository.exportAllDataToCsv()
            val uri = CsvManager.createShareableFile(context, csv)
            onShareUriReady(uri)
        }
    }

    fun importCsvFromUri(sourceUri: Uri) {
        viewModelScope.launch {
            val content = CsvManager.readCsvFromUri(getApplication<Application>().contentResolver, sourceUri)
            if (content.isNullOrBlank()) {
                _exportImportMessage.value = "Nie udało się odczytać wybranego pliku CSV."
                return@launch
            }
            val summary = repository.importCsvData(content)
            _exportImportMessage.value = summary.message
            HapticUtil.vibrateShort(getApplication())
        }
    }
}

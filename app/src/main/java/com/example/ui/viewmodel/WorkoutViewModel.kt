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
import com.example.service.WorkoutTimerService
import com.example.ui.components.ChartDataPoint
import com.example.ui.components.IntervalPhase
import com.example.ui.util.ElapsedClock
import com.example.ui.util.HapticUtil
import com.example.ui.util.SoundCueUtil
import com.example.ui.util.SystemElapsedClock
import com.example.ui.util.WorkoutTimerMath
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
import kotlin.math.roundToInt

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
    val timerSecondsRemaining: Int = WorkoutTimerMath.DEFAULT_INTERVAL_SECONDS,
    val timerTotalSeconds: Int = WorkoutTimerMath.DEFAULT_INTERVAL_SECONDS,
    val isTimerRunning: Boolean = false,
    val intervalEndElapsedMs: Long = 0L,
    val pausedRemainingMs: Long = WorkoutTimerMath.DEFAULT_INTERVAL_MS,
    val startTimestamp: Long = 0L,
    val recordedLogs: List<SetLogEntity> = emptyList(),
    val suggestedLastSet: SetLogEntity? = null,
    val inputWeight: String = "",
    val inputReps: String = "",
    val isSoundEnabled: Boolean = true,
    val soundVolume: Float = 1.0f,
    val isLoudModeEnabled: Boolean = false
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

    val previousExercise: PlanExerciseEntity?
        get() = exercises.getOrNull(currentExerciseIndex - 1)

    val canGoToPreviousExercise: Boolean
        get() = currentExerciseIndex > 0

    val canGoToPreviousStep: Boolean
        get() = WorkoutSessionHelper.resolvePreviousStep(this) != null

    val currentSetRecordedLog: SetLogEntity?
        get() = WorkoutSessionHelper.findRecordedLog(recordedLogs, currentExercise?.name, currentSetNumber)
}

class WorkoutViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: WorkoutRepository
    private val clock: ElapsedClock = SystemElapsedClock
    private var timerJob: Job? = null
    private var lastCountdownBeepSecond: Int? = null

    // Screen navigation state
    private val _currentScreen = MutableStateFlow(AppScreen.HOME)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    // Selected Day on Home Screen
    private val _selectedDay = MutableStateFlow(DayType.getSuggestedForToday())
    val selectedDay: StateFlow<DayType> = _selectedDay.asStateFlow()

    private val soundPrefs = application.getSharedPreferences(PREFS_SOUND, Context.MODE_PRIVATE)

    // Active Workout state (initialized with persisted sound preferences)
    private val _activeWorkout = MutableStateFlow(createIdleWorkoutState())
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
        val nowElapsed = clock.elapsedRealtime()
        lastCountdownBeepSecond = null

        val soundEnabled = soundPrefs.getBoolean(KEY_SOUND_ENABLED, true)
        val savedVolume = SoundCueUtil.normalizeVolume(soundPrefs.getFloat(KEY_SOUND_VOLUME, 1.0f))
        val loudMode = soundPrefs.getBoolean(KEY_LOUD_MODE, false)

        _activeWorkout.value = ActiveWorkoutState(
            isActive = true,
            dayType = _selectedDay.value,
            exercises = exercises,
            currentExerciseIndex = 0,
            currentSetNumber = 1,
            intervalPhase = IntervalPhase.EXERCISE,
            timerSecondsRemaining = WorkoutTimerMath.DEFAULT_INTERVAL_SECONDS,
            timerTotalSeconds = WorkoutTimerMath.DEFAULT_INTERVAL_SECONDS,
            isTimerRunning = true,
            intervalEndElapsedMs = nowElapsed + WorkoutTimerMath.DEFAULT_INTERVAL_MS,
            pausedRemainingMs = WorkoutTimerMath.DEFAULT_INTERVAL_MS,
            startTimestamp = System.currentTimeMillis(),
            recordedLogs = emptyList(),
            inputWeight = "",
            inputReps = "",
            isSoundEnabled = soundEnabled,
            soundVolume = savedVolume,
            isLoudModeEnabled = loudMode
        )

        loadSuggestionForExercise(firstExercise.name)
        startTimerLoop()
        syncTimerNotification()
        _currentScreen.value = AppScreen.ACTIVE_WORKOUT
    }

    private fun loadSuggestionForExercise(exerciseName: String) {
        viewModelScope.launch {
            val lastSet = repository.getLatestSetForExercise(exerciseName)
            _activeWorkout.update { state ->
                if (!state.currentExercise?.name.equals(exerciseName, ignoreCase = true)) {
                    return@update state
                }
                state.copy(
                    suggestedLastSet = lastSet,
                    inputWeight = if (state.inputWeight.isEmpty() && lastSet != null) {
                        WorkoutSessionHelper.formatWeight(lastSet.weightKg)
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
                inputWeight = WorkoutSessionHelper.formatWeight(suggestion.weightKg),
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
        val current = _activeWorkout.value.inputWeight.replace(',', '.').toDoubleOrNull() ?: 0.0
        val next = (current + delta).coerceAtLeast(0.0)
        _activeWorkout.update {
            it.copy(inputWeight = WorkoutSessionHelper.formatWeight(next))
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
        val current = _activeWorkout.value
        if (!current.isActive || !current.isTimerRunning) return

        val remainingMs = if (current.intervalEndElapsedMs > 0L) {
            (current.intervalEndElapsedMs - clock.elapsedRealtime()).coerceAtLeast(0L)
        } else {
            current.timerSecondsRemaining * 1000L
        }

        timerJob?.cancel()
        _activeWorkout.update {
            it.copy(
                isTimerRunning = false,
                intervalEndElapsedMs = 0L,
                pausedRemainingMs = remainingMs,
                timerSecondsRemaining = WorkoutTimerMath.millisToDisplaySeconds(remainingMs)
            )
        }
        syncTimerNotification()
    }

    fun resumeTimer() {
        val current = _activeWorkout.value
        if (!current.isActive || current.isTimerRunning) return

        val nowElapsed = clock.elapsedRealtime()
        val remainingMs = current.pausedRemainingMs.coerceAtLeast(0L)
        lastCountdownBeepSecond = null

        _activeWorkout.update {
            it.copy(
                isTimerRunning = true,
                intervalEndElapsedMs = nowElapsed + remainingMs,
                timerSecondsRemaining = WorkoutTimerMath.millisToDisplaySeconds(remainingMs)
            )
        }
        startTimerLoop()
        syncTimerNotification()
    }

    fun add30Seconds() {
        val current = _activeWorkout.value
        if (!current.isActive) return

        val addedMs = 30_000L
        val nowElapsed = clock.elapsedRealtime()
        lastCountdownBeepSecond = null

        _activeWorkout.update {
            if (it.isTimerRunning) {
                val baseRemaining = if (it.intervalEndElapsedMs > 0L) {
                    (it.intervalEndElapsedMs - nowElapsed).coerceAtLeast(0L)
                } else {
                    it.timerSecondsRemaining * 1000L
                }
                val newRemainingMs = baseRemaining + addedMs
                val newRemainingSec = WorkoutTimerMath.millisToDisplaySeconds(newRemainingMs)
                val newTotal = it.timerTotalSeconds.coerceAtLeast(newRemainingSec)
                it.copy(
                    intervalEndElapsedMs = nowElapsed + newRemainingMs,
                    pausedRemainingMs = newRemainingMs,
                    timerSecondsRemaining = newRemainingSec,
                    timerTotalSeconds = newTotal
                )
            } else {
                val newRemainingMs = it.pausedRemainingMs + addedMs
                val newRemainingSec = WorkoutTimerMath.millisToDisplaySeconds(newRemainingMs)
                val newTotal = it.timerTotalSeconds.coerceAtLeast(newRemainingSec)
                it.copy(
                    pausedRemainingMs = newRemainingMs,
                    timerSecondsRemaining = newRemainingSec,
                    timerTotalSeconds = newTotal
                )
            }
        }
        syncTimerNotification()
    }

    fun skipInterval() {
        transitionToNextInterval(isSkipped = true)
    }

    fun toggleSound() {
        var nextEnabled = true
        var nextVolume = 1.0f
        var loudMode = false
        _activeWorkout.update { state ->
            nextEnabled = !state.isSoundEnabled
            nextVolume = if (nextEnabled && state.soundVolume <= 0.01f) 0.8f else state.soundVolume
            loudMode = state.isLoudModeEnabled
            state.copy(
                isSoundEnabled = nextEnabled,
                soundVolume = nextVolume
            )
        }
        saveSoundPrefs(nextEnabled, nextVolume, loudMode)
        if (nextEnabled) {
            SoundCueUtil.applySystemStreamVolume(getApplication(), nextVolume, loudMode)
        }
    }

    fun setSoundVolume(volume: Float, playPreview: Boolean = false) {
        val clamped = SoundCueUtil.normalizeVolume(volume)
        val enabled = clamped > 0.001f
        var loudMode = false
        _activeWorkout.update { state ->
            loudMode = state.isLoudModeEnabled
            state.copy(
                soundVolume = clamped,
                isSoundEnabled = enabled
            )
        }
        saveSoundPrefs(enabled, clamped, loudMode)
        SoundCueUtil.applySystemStreamVolume(getApplication(), clamped, loudMode)
        if (playPreview && enabled) {
            SoundCueUtil.playVolumePreviewCue(clamped, loudMode)
        }
    }

    fun adjustSoundVolume(delta: Float) {
        val current = _activeWorkout.value
        val baseVolume = if (current.isSoundEnabled) current.soundVolume else 0f
        val stepped = (((baseVolume + delta) * 10f).roundToInt() / 10f).coerceIn(0f, 1f)
        setSoundVolume(stepped, playPreview = true)
    }

    fun toggleLoudMode() {
        var nextLoud = false
        var volume = 1.0f
        var enabled = true
        _activeWorkout.update { state ->
            nextLoud = !state.isLoudModeEnabled
            volume = state.soundVolume
            enabled = state.isSoundEnabled
            state.copy(isLoudModeEnabled = nextLoud)
        }
        saveSoundPrefs(enabled, volume, nextLoud)
        if (enabled && volume > 0f) {
            SoundCueUtil.applySystemStreamVolume(getApplication(), volume, nextLoud)
            SoundCueUtil.playVolumePreviewCue(volume, nextLoud)
        }
    }

    fun testSoundCue() {
        val current = _activeWorkout.value
        val effectiveVolume = if (current.soundVolume > 0.001f) current.soundVolume else 0.8f
        if (!current.isSoundEnabled || current.soundVolume <= 0.001f) {
            setSoundVolume(effectiveVolume, playPreview = true)
        } else {
            SoundCueUtil.applySystemStreamVolume(getApplication(), effectiveVolume, current.isLoudModeEnabled)
            SoundCueUtil.playVolumePreviewCue(effectiveVolume, current.isLoudModeEnabled)
        }
    }

    private fun createIdleWorkoutState(): ActiveWorkoutState {
        return ActiveWorkoutState(
            isSoundEnabled = soundPrefs.getBoolean(KEY_SOUND_ENABLED, true),
            soundVolume = SoundCueUtil.normalizeVolume(soundPrefs.getFloat(KEY_SOUND_VOLUME, 1.0f)),
            isLoudModeEnabled = soundPrefs.getBoolean(KEY_LOUD_MODE, false)
        )
    }

    private fun saveSoundPrefs(enabled: Boolean, volume: Float, loudMode: Boolean) {
        soundPrefs.edit()
            .putBoolean(KEY_SOUND_ENABLED, enabled)
            .putFloat(KEY_SOUND_VOLUME, SoundCueUtil.normalizeVolume(volume))
            .putBoolean(KEY_LOUD_MODE, loudMode)
            .apply()
    }

    /**
     * Immediately reconciles the displayed countdown against real monotonic elapsed time
     * (SystemClock.elapsedRealtime). Called continuously by the timer loop AND whenever
     * MainActivity resumes from background or lock screen.
     */
    fun syncTimerWithClock() {
        val current = _activeWorkout.value
        if (!current.isActive || !current.isTimerRunning || current.intervalEndElapsedMs <= 0L) {
            return
        }

        val nowElapsed = clock.elapsedRealtime()
        val tick = WorkoutTimerMath.evaluateTick(current.intervalEndElapsedMs, nowElapsed)

        if (!tick.isExpired) {
            if (tick.remainingSeconds != current.timerSecondsRemaining) {
                if (current.isSoundEnabled && tick.remainingSeconds in 1..3 && lastCountdownBeepSecond != tick.remainingSeconds) {
                    lastCountdownBeepSecond = tick.remainingSeconds
                    SoundCueUtil.playCountdownTick(current.soundVolume, current.isLoudModeEnabled)
                }
                _activeWorkout.update {
                    it.copy(
                        timerSecondsRemaining = tick.remainingSeconds,
                        pausedRemainingMs = tick.remainingMs
                    )
                }
            }
        } else {
            // Interval reached 0 in real time
            lastCountdownBeepSecond = null
            HapticUtil.vibratePattern(getApplication())
            transitionToNextInterval(isSkipped = false, overshootMs = tick.overshootMs)
        }
    }

    private fun startTimerLoop() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(250L)
                val current = _activeWorkout.value
                if (!current.isActive || !current.isTimerRunning) break
                syncTimerWithClock()
            }
        }
    }

    private fun syncTimerNotification() {
        val state = _activeWorkout.value
        if (!state.isActive) {
            WorkoutTimerService.stop(getApplication())
            return
        }

        val phaseLabel = if (state.intervalPhase == IntervalPhase.EXERCISE) {
            "ĆWICZENIE"
        } else {
            "ODPOCZYNEK"
        }
        val ex = state.currentExercise
        val exerciseInfo = if (ex != null) {
            "Seria ${state.currentSetNumber}/${ex.targetSets} • ${ex.name}"
        } else {
            "Sesja ${state.dayType.code}"
        }

        val remainingMs = if (state.isTimerRunning && state.intervalEndElapsedMs > 0L) {
            (state.intervalEndElapsedMs - clock.elapsedRealtime()).coerceAtLeast(0L)
        } else {
            state.pausedRemainingMs.coerceAtLeast(0L)
        }

        WorkoutTimerService.startOrUpdate(
            context = getApplication(),
            phaseLabel = phaseLabel,
            exerciseInfo = exerciseInfo,
            isRunning = state.isTimerRunning,
            remainingMs = remainingMs
        )
    }

    private fun transitionToNextInterval(isSkipped: Boolean, overshootMs: Long = 0L) {
        val current = _activeWorkout.value
        if (!current.isActive) return

        val nextRemainingMs = if (isSkipped) {
            WorkoutTimerMath.DEFAULT_INTERVAL_MS
        } else {
            WorkoutTimerMath.nextIntervalRemainingMs(overshootMs)
        }
        val nextRemainingSec = WorkoutTimerMath.millisToDisplaySeconds(nextRemainingMs)
        val nowElapsed = clock.elapsedRealtime()
        lastCountdownBeepSecond = null

        HapticUtil.vibrateShort(getApplication())

        if (current.intervalPhase == IntervalPhase.EXERCISE) {
            // Set ended -> play set ended audio cue
            if (current.isSoundEnabled) {
                SoundCueUtil.playSetEndedCue(current.soundVolume, current.isLoudModeEnabled)
            }
            // Switch to Rest interval (2 min = 120s)
            _activeWorkout.update {
                it.copy(
                    intervalPhase = IntervalPhase.REST,
                    timerSecondsRemaining = nextRemainingSec,
                    timerTotalSeconds = WorkoutTimerMath.DEFAULT_INTERVAL_SECONDS,
                    intervalEndElapsedMs = nowElapsed + nextRemainingMs,
                    pausedRemainingMs = nextRemainingMs,
                    isTimerRunning = true
                )
            }
            startTimerLoop()
            syncTimerNotification()
        } else {
            // Rest ended -> play rest concluded chime (time to lift!)
            if (current.isSoundEnabled) {
                SoundCueUtil.playRestConcludedCue(current.soundVolume, current.isLoudModeEnabled)
            }
            // Finished rest interval -> prepare next set / exercise
            advanceToNextSetOrExercise(initialRemainingMs = nextRemainingMs)
        }
    }

    // Log current set (updates existing entry in place if re-editing the same exercise & set)
    fun logSetAndRest() {
        val current = _activeWorkout.value
        val ex = current.currentExercise ?: return

        val weight = current.inputWeight.replace(',', '.').toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
        val reps = current.inputReps.toIntOrNull()?.coerceAtLeast(0) ?: ex.minReps

        val log = SetLogEntity(
            sessionId = 0L, // set upon session save
            exerciseName = ex.name,
            setNumber = current.currentSetNumber,
            weightKg = weight,
            reps = reps,
            timestamp = System.currentTimeMillis()
        )

        val updatedLogs = WorkoutSessionHelper.upsertSetLog(current.recordedLogs, log)
        val nowElapsed = clock.elapsedRealtime()
        lastCountdownBeepSecond = null

        _activeWorkout.update {
            it.copy(
                recordedLogs = updatedLogs,
                intervalPhase = IntervalPhase.REST,
                timerSecondsRemaining = WorkoutTimerMath.DEFAULT_INTERVAL_SECONDS,
                timerTotalSeconds = WorkoutTimerMath.DEFAULT_INTERVAL_SECONDS,
                intervalEndElapsedMs = nowElapsed + WorkoutTimerMath.DEFAULT_INTERVAL_MS,
                pausedRemainingMs = WorkoutTimerMath.DEFAULT_INTERVAL_MS,
                isTimerRunning = true
            )
        }

        if (current.isSoundEnabled) {
            SoundCueUtil.playSetEndedCue(current.soundVolume, current.isLoudModeEnabled)
        }
        HapticUtil.vibrateShort(getApplication())
        startTimerLoop()
        syncTimerNotification()
    }

    // Finish rest and proceed to the next set or exercise immediately
    fun finishRestAndStartNext() {
        val current = _activeWorkout.value
        if (current.intervalPhase == IntervalPhase.REST) {
            if (current.isSoundEnabled) {
                SoundCueUtil.playRestConcludedCue(current.soundVolume, current.isLoudModeEnabled)
            }
            HapticUtil.vibrateShort(getApplication())
            advanceToNextSetOrExercise(initialRemainingMs = WorkoutTimerMath.DEFAULT_INTERVAL_MS)
        }
    }

    // Advance to next set or exercise after rest
    private fun advanceToNextSetOrExercise(initialRemainingMs: Long = WorkoutTimerMath.DEFAULT_INTERVAL_MS) {
        val current = _activeWorkout.value
        val ex = current.currentExercise ?: return
        val nowElapsed = clock.elapsedRealtime()
        val remainingSec = WorkoutTimerMath.millisToDisplaySeconds(initialRemainingMs)
        lastCountdownBeepSecond = null

        if (current.currentSetNumber < ex.targetSets) {
            val nextSet = current.currentSetNumber + 1
            val existingLog = WorkoutSessionHelper.findRecordedLog(current.recordedLogs, ex.name, nextSet)
            _activeWorkout.update {
                it.copy(
                    currentSetNumber = nextSet,
                    intervalPhase = IntervalPhase.EXERCISE,
                    timerSecondsRemaining = remainingSec,
                    timerTotalSeconds = WorkoutTimerMath.DEFAULT_INTERVAL_SECONDS,
                    intervalEndElapsedMs = nowElapsed + initialRemainingMs,
                    pausedRemainingMs = initialRemainingMs,
                    isTimerRunning = true,
                    inputWeight = existingLog?.let { log -> WorkoutSessionHelper.formatWeight(log.weightKg) } ?: it.inputWeight,
                    inputReps = existingLog?.reps?.toString() ?: it.inputReps
                )
            }
            startTimerLoop()
            syncTimerNotification()
        } else {
            // Finished all sets for current exercise -> go to next exercise
            val nextExerciseIndex = current.currentExerciseIndex + 1
            if (nextExerciseIndex < current.exercises.size) {
                val nextExercise = current.exercises[nextExerciseIndex]
                val existingLog = WorkoutSessionHelper.findRecordedLog(current.recordedLogs, nextExercise.name, 1)
                _activeWorkout.update {
                    it.copy(
                        currentExerciseIndex = nextExerciseIndex,
                        currentSetNumber = 1,
                        intervalPhase = IntervalPhase.EXERCISE,
                        timerSecondsRemaining = remainingSec,
                        timerTotalSeconds = WorkoutTimerMath.DEFAULT_INTERVAL_SECONDS,
                        intervalEndElapsedMs = nowElapsed + initialRemainingMs,
                        pausedRemainingMs = initialRemainingMs,
                        isTimerRunning = true,
                        inputWeight = existingLog?.let { log -> WorkoutSessionHelper.formatWeight(log.weightKg) } ?: "",
                        inputReps = existingLog?.reps?.toString() ?: ""
                    )
                }
                loadSuggestionForExercise(nextExercise.name)
                startTimerLoop()
                syncTimerNotification()
            } else {
                // Completed entire workout!
                finishWorkout()
            }
        }
    }

    /**
     * Steps back one phase/set/exercise and pauses the timer so the user can calmly
     * enter or edit weight & reps even after the timer expired.
     */
    fun previousStep() {
        val current = _activeWorkout.value
        if (!current.isActive) return
        val target = WorkoutSessionHelper.resolvePreviousStep(current) ?: return
        navigateToExerciseSet(
            targetExerciseIndex = target.exerciseIndex,
            targetSetNumber = target.setNumber,
            pauseTimer = true
        )
    }

    /**
     * Jumps directly to the previous exercise in the workout plan (paused on EXERCISE phase)
     * so weight & reps can be updated or logged retroactively.
     */
    fun previousExercise() {
        val current = _activeWorkout.value
        if (!current.isActive) return
        val target = WorkoutSessionHelper.resolvePreviousExercise(current) ?: return
        navigateToExerciseSet(
            targetExerciseIndex = target.exerciseIndex,
            targetSetNumber = target.setNumber,
            pauseTimer = true
        )
    }

    /**
     * Switches directly to a specific set number (1..targetSets) of the current exercise
     * to review or edit weight & reps.
     */
    fun selectSetNumber(setNumber: Int) {
        val current = _activeWorkout.value
        val ex = current.currentExercise ?: return
        val clampedSet = setNumber.coerceIn(1, ex.targetSets.coerceAtLeast(1))
        if (clampedSet == current.currentSetNumber && current.intervalPhase == IntervalPhase.EXERCISE) return
        navigateToExerciseSet(
            targetExerciseIndex = current.currentExerciseIndex,
            targetSetNumber = clampedSet,
            pauseTimer = true
        )
    }

    private fun navigateToExerciseSet(
        targetExerciseIndex: Int,
        targetSetNumber: Int,
        pauseTimer: Boolean
    ) {
        val current = _activeWorkout.value
        val targetExercise = current.exercises.getOrNull(targetExerciseIndex) ?: return
        val clampedSet = targetSetNumber.coerceIn(1, targetExercise.targetSets.coerceAtLeast(1))
        val isChangingExercise = targetExerciseIndex != current.currentExerciseIndex
        val existingLog = WorkoutSessionHelper.findRecordedLog(
            current.recordedLogs,
            targetExercise.name,
            clampedSet
        )
        val latestForExercise = current.recordedLogs.lastOrNull {
            it.exerciseName.equals(targetExercise.name, ignoreCase = true)
        }
        val prefillSource = existingLog ?: if (isChangingExercise) latestForExercise else null

        val prefilledWeight = when {
            prefillSource != null -> WorkoutSessionHelper.formatWeight(prefillSource.weightKg)
            !isChangingExercise -> current.inputWeight
            else -> ""
        }
        val prefilledReps = when {
            prefillSource != null -> prefillSource.reps.toString()
            !isChangingExercise -> current.inputReps
            else -> ""
        }

        val nowElapsed = clock.elapsedRealtime()
        lastCountdownBeepSecond = null

        if (pauseTimer) {
            timerJob?.cancel()
            _activeWorkout.update {
                it.copy(
                    currentExerciseIndex = targetExerciseIndex,
                    currentSetNumber = clampedSet,
                    intervalPhase = IntervalPhase.EXERCISE,
                    timerSecondsRemaining = WorkoutTimerMath.DEFAULT_INTERVAL_SECONDS,
                    timerTotalSeconds = WorkoutTimerMath.DEFAULT_INTERVAL_SECONDS,
                    intervalEndElapsedMs = 0L,
                    pausedRemainingMs = WorkoutTimerMath.DEFAULT_INTERVAL_MS,
                    isTimerRunning = false,
                    inputWeight = prefilledWeight,
                    inputReps = prefilledReps
                )
            }
        } else {
            _activeWorkout.update {
                it.copy(
                    currentExerciseIndex = targetExerciseIndex,
                    currentSetNumber = clampedSet,
                    intervalPhase = IntervalPhase.EXERCISE,
                    timerSecondsRemaining = WorkoutTimerMath.DEFAULT_INTERVAL_SECONDS,
                    timerTotalSeconds = WorkoutTimerMath.DEFAULT_INTERVAL_SECONDS,
                    intervalEndElapsedMs = nowElapsed + WorkoutTimerMath.DEFAULT_INTERVAL_MS,
                    pausedRemainingMs = WorkoutTimerMath.DEFAULT_INTERVAL_MS,
                    isTimerRunning = true,
                    inputWeight = prefilledWeight,
                    inputReps = prefilledReps
                )
            }
            startTimerLoop()
        }

        if (isChangingExercise) {
            loadSuggestionForExercise(targetExercise.name)
        }
        syncTimerNotification()
    }

    /**
     * Edits weight & reps of any already-recorded set in the current active session.
     */
    fun updateRecordedSet(index: Int, weightKg: Double, reps: Int) {
        val current = _activeWorkout.value
        val target = current.recordedLogs.getOrNull(index) ?: return
        val updatedLogs = WorkoutSessionHelper.updateSetLogAt(current.recordedLogs, index, weightKg, reps)
        val matchesCurrent = current.currentExercise?.name.equals(target.exerciseName, ignoreCase = true) &&
            current.currentSetNumber == target.setNumber

        _activeWorkout.update {
            it.copy(
                recordedLogs = updatedLogs,
                inputWeight = if (matchesCurrent) WorkoutSessionHelper.formatWeight(weightKg.coerceAtLeast(0.0)) else it.inputWeight,
                inputReps = if (matchesCurrent) reps.coerceAtLeast(0).toString() else it.inputReps
            )
        }
    }

    /**
     * Removes a recorded set entry from the current active session.
     */
    fun deleteRecordedSet(index: Int) {
        _activeWorkout.update {
            it.copy(recordedLogs = WorkoutSessionHelper.removeSetLogAt(it.recordedLogs, index))
        }
    }

    // Manual next exercise if needed
    fun nextExercise() {
        val current = _activeWorkout.value
        val nextExerciseIndex = current.currentExerciseIndex + 1
        if (nextExerciseIndex < current.exercises.size) {
            val nextExercise = current.exercises[nextExerciseIndex]
            val maxSets = nextExercise.targetSets.coerceAtLeast(1)
            val recordedForNext = current.recordedLogs.filter {
                it.exerciseName.equals(nextExercise.name, ignoreCase = true)
            }
            val nextSet = if (recordedForNext.isEmpty()) {
                1
            } else {
                (recordedForNext.maxOf { it.setNumber } + 1).coerceAtMost(maxSets)
            }
            navigateToExerciseSet(
                targetExerciseIndex = nextExerciseIndex,
                targetSetNumber = nextSet,
                pauseTimer = false
            )
        } else {
            finishWorkout()
        }
    }

    // Finish Workout & save to Room
    fun finishWorkout() {
        timerJob?.cancel()
        lastCountdownBeepSecond = null
        WorkoutTimerService.stop(getApplication())
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

            _activeWorkout.value = createIdleWorkoutState()
            _currentScreen.value = AppScreen.SUMMARY
            if (current.isSoundEnabled) {
                SoundCueUtil.playWorkoutCompleteCue(current.soundVolume, current.isLoudModeEnabled)
            }
            HapticUtil.vibratePattern(getApplication())
        }
    }

    // Cancel workout
    fun cancelWorkout() {
        timerJob?.cancel()
        lastCountdownBeepSecond = null
        WorkoutTimerService.stop(getApplication())
        _activeWorkout.value = createIdleWorkoutState()
        _currentScreen.value = AppScreen.HOME
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        WorkoutTimerService.stop(getApplication())
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

    companion object {
        private const val PREFS_SOUND = "ppl_sound_prefs"
        private const val KEY_SOUND_ENABLED = "sound_enabled"
        private const val KEY_SOUND_VOLUME = "sound_volume"
        private const val KEY_LOUD_MODE = "loud_mode_enabled"
    }
}

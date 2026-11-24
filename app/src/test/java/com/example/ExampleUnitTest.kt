package com.example

import com.example.data.DefaultWorkoutData
import com.example.data.model.DayType
import com.example.data.model.SetLogEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testDefaultWorkoutDataMatchesPdf() {
        val exercises = DefaultWorkoutData.getDefaultExercises()
        val pushExercises = exercises.filter { it.dayType == DayType.PUSH.code }
        val pullExercises = exercises.filter { it.dayType == DayType.PULL.code }
        val legsExercises = exercises.filter { it.dayType == DayType.LEGS.code }

        // PDF specifies:
        // PUSH: 7 exercises, 19 working sets
        assertEquals(7, pushExercises.size)
        assertEquals(19, pushExercises.sumOf { it.targetSets })

        // PULL: 7 exercises, 19 working sets
        assertEquals(7, pullExercises.size)
        assertEquals(19, pullExercises.sumOf { it.targetSets })

        // LEGS + dodatki: 8 exercises, 20 working sets
        assertEquals(8, legsExercises.size)
        assertEquals(20, legsExercises.sumOf { it.targetSets })
    }

    @Test
    fun testSetLogVolumeAndEstimated1RM() {
        val log = SetLogEntity(
            sessionId = 1L,
            exerciseName = "Wyciskanie sztangi na ławce płaskiej",
            setNumber = 1,
            weightKg = 80.0,
            reps = 8
        )

        assertEquals(640.0, log.volume, 0.001)
        // Brzycki formula: 80 * (36 / (37 - 8)) = 80 * (36 / 29) ≈ 99.31
        assertTrue(log.estimated1RM > 99.0 && log.estimated1RM < 100.0)
    }

    @Test
    fun testDayTypeCodes() {
        assertEquals(DayType.PUSH, DayType.fromCode("PUSH"))
        assertEquals(DayType.PULL, DayType.fromCode("PULL"))
        assertEquals(DayType.LEGS, DayType.fromCode("LEGS"))
        assertNotNull(DayType.getSuggestedForToday())
    }

    @Test
    fun testCsvGenerationAndParsingRoundtrip() {
        val session = com.example.data.model.WorkoutSessionEntity(
            id = 101L,
            dateMillis = 1731420000000L,
            dayType = "PUSH",
            durationSeconds = 2700,
            totalTonnageKg = 1840.0,
            completedSetsCount = 2
        )
        val logs = listOf(
            SetLogEntity(
                id = 1L,
                sessionId = 101L,
                exerciseName = "Wyciskanie sztangi na ławce płaskiej",
                setNumber = 1,
                weightKg = 80.0,
                reps = 8,
                timestamp = 1731420000000L
            ),
            SetLogEntity(
                id = 2L,
                sessionId = 101L,
                exerciseName = "Wyciskanie sztangi na ławce płaskiej",
                setNumber = 2,
                weightKg = 85.0,
                reps = 6,
                timestamp = 1731420100000L
            )
        )

        val csv = com.example.data.csv.CsvManager.generateCsv(listOf(session), logs)
        assertTrue(csv.contains("SessionId,DateMillis"))
        assertTrue(csv.contains("Wyciskanie sztangi na ławce płaskiej"))

        val parsed = com.example.data.csv.CsvManager.parseCsv(csv)
        assertEquals(1, parsed.size)
        val firstParsed = parsed[0]
        assertEquals(1731420000000L, firstParsed.session.dateMillis)
        assertEquals("PUSH", firstParsed.session.dayType)
        assertEquals(2700, firstParsed.session.durationSeconds)
        assertEquals(2, firstParsed.sets.size)
        assertEquals(80.0, firstParsed.sets[0].weightKg, 0.001)
        assertEquals(8, firstParsed.sets[0].reps)
        assertEquals(85.0, firstParsed.sets[1].weightKg, 0.001)
        assertEquals(6, firstParsed.sets[1].reps)
    }

    @Test
    fun testActiveWorkoutStateProgression() {
        val exercises = com.example.data.DefaultWorkoutData.getDefaultExercises().filter { it.dayType == "PUSH" }
        val stateSet1 = com.example.ui.viewmodel.ActiveWorkoutState(
            isActive = true,
            dayType = DayType.PUSH,
            exercises = exercises,
            currentExerciseIndex = 0,
            currentSetNumber = 1,
            intervalPhase = com.example.ui.components.IntervalPhase.EXERCISE
        )

        // Exercise 1 (Bench Press) has 3 sets
        org.junit.Assert.assertFalse(stateSet1.isLastSetOfCurrentExercise)
        org.junit.Assert.assertFalse(stateSet1.isLastSetOfWorkout)
        assertNotNull(stateSet1.nextExercise)

        // Set 3 of Exercise 1 (last set of current exercise, but not last set of workout)
        val stateSet3 = stateSet1.copy(currentSetNumber = 3, intervalPhase = com.example.ui.components.IntervalPhase.REST)
        assertTrue(stateSet3.isLastSetOfCurrentExercise)
        org.junit.Assert.assertFalse(stateSet3.isLastSetOfWorkout)
        assertEquals(exercises[1].name, stateSet3.nextExercise?.name)

        // Last set of last exercise
        val stateLast = stateSet1.copy(
            currentExerciseIndex = exercises.lastIndex,
            currentSetNumber = exercises.last().targetSets,
            intervalPhase = com.example.ui.components.IntervalPhase.REST
        )
        assertTrue(stateLast.isLastSetOfCurrentExercise)
        assertTrue(stateLast.isLastSetOfWorkout)
        org.junit.Assert.assertNull(stateLast.nextExercise)
    }

    @Test
    fun testDayTypeColorsAndMetrics() {
        val pushColor = com.example.ui.theme.getDayTypeColor("PUSH")
        val pullColor = com.example.ui.theme.getDayTypeColor("PULL")
        val legsColor = com.example.ui.theme.getDayTypeColor("LEGS")

        // Colors must be distinct for each workout type
        org.junit.Assert.assertNotEquals(pushColor, pullColor)
        org.junit.Assert.assertNotEquals(pushColor, legsColor)
        org.junit.Assert.assertNotEquals(pullColor, legsColor)

        // Exercise metrics
        val metrics = com.example.ui.viewmodel.ExerciseMetric.entries
        assertEquals(3, metrics.size)
        assertTrue(metrics.any { it.name == "WEIGHT" })
        assertTrue(metrics.any { it.name == "REPS" })
        assertTrue(metrics.any { it.name == "ESTIMATED_1RM" })

        // Calendar screen destination
        assertTrue(com.example.ui.viewmodel.AppScreen.entries.contains(com.example.ui.viewmodel.AppScreen.CALENDAR))
    }

    @Test
    fun testBackgroundMonotonicTimerAccuracy() {
        val startElapsed = 10_000L
        val endElapsed = startElapsed + com.example.ui.util.WorkoutTimerMath.DEFAULT_INTERVAL_MS // 130_000L

        // Simulate app frozen in background for 60 real seconds
        val after60s = com.example.ui.util.WorkoutTimerMath.evaluateTick(
            intervalEndElapsedMs = endElapsed,
            nowElapsedMs = startElapsed + 60_000L
        )
        org.junit.Assert.assertFalse(after60s.isExpired)
        assertEquals(60, after60s.remainingSeconds)
        assertEquals(60_000L, after60s.remainingMs)

        // Sub-second ceiling rounding
        assertEquals(120, com.example.ui.util.WorkoutTimerMath.millisToDisplaySeconds(120_000L))
        assertEquals(120, com.example.ui.util.WorkoutTimerMath.millisToDisplaySeconds(119_500L))
        assertEquals(119, com.example.ui.util.WorkoutTimerMath.millisToDisplaySeconds(119_000L))
        assertEquals(1, com.example.ui.util.WorkoutTimerMath.millisToDisplaySeconds(1L))
        assertEquals(0, com.example.ui.util.WorkoutTimerMath.millisToDisplaySeconds(0L))

        // Simulate interval expiring 15s ago while in background
        val expiredBy15s = com.example.ui.util.WorkoutTimerMath.evaluateTick(
            intervalEndElapsedMs = endElapsed,
            nowElapsedMs = endElapsed + 15_000L
        )
        assertTrue(expiredBy15s.isExpired)
        assertEquals(0, expiredBy15s.remainingSeconds)
        assertEquals(15_000L, expiredBy15s.overshootMs)
        assertEquals(105_000L, com.example.ui.util.WorkoutTimerMath.nextIntervalRemainingMs(expiredBy15s.overshootMs))

        // Long idle overshoot (> full interval) safely starts next phase fresh without negative time
        assertEquals(120_000L, com.example.ui.util.WorkoutTimerMath.nextIntervalRemainingMs(300_000L))
    }

    @Test
    fun testSoundCueVolumeAndBoostGain() {
        assertEquals(0f, com.example.ui.util.SoundCueUtil.normalizeVolume(-0.5f), 0.0001f)
        assertEquals(0.5f, com.example.ui.util.SoundCueUtil.normalizeVolume(0.5f), 0.0001f)
        assertEquals(1f, com.example.ui.util.SoundCueUtil.normalizeVolume(1.8f), 0.0001f)

        val segment = listOf(com.example.ui.util.SoundCueUtil.ToneSegment(1000.0, 50))

        val silentSamples = com.example.ui.util.SoundCueUtil.generatePcmSamples(segment, volume = 0f, boostGain = false)
        assertTrue(silentSamples.isNotEmpty())
        assertEquals(0, silentSamples.maxOf { kotlin.math.abs(it.toInt()) })

        val quietSamples = com.example.ui.util.SoundCueUtil.generatePcmSamples(segment, volume = 0.3f, boostGain = false)
        val normalFullSamples = com.example.ui.util.SoundCueUtil.generatePcmSamples(segment, volume = 1.0f, boostGain = false)
        val boostedSamples = com.example.ui.util.SoundCueUtil.generatePcmSamples(segment, volume = 1.0f, boostGain = true)

        val quietPeak = quietSamples.maxOf { kotlin.math.abs(it.toInt()) }
        val normalFullPeak = normalFullSamples.maxOf { kotlin.math.abs(it.toInt()) }
        val boostedPeak = boostedSamples.maxOf { kotlin.math.abs(it.toInt()) }

        assertTrue(quietPeak > 0)
        assertTrue(normalFullPeak > quietPeak * 2)
        assertTrue(boostedPeak > normalFullPeak)
    }

    @Test
    fun testWorkoutBackwardNavigationAndSetEditing() {
        val exercises = DefaultWorkoutData.getDefaultExercises().filter { it.dayType == DayType.PUSH.code }
        val ex1 = exercises[0] // 3 sets
        val ex2 = exercises[1]

        // 1. From REST phase -> previous step returns to EXERCISE for the exact same exercise & set
        val restState = com.example.ui.viewmodel.ActiveWorkoutState(
            isActive = true,
            dayType = DayType.PUSH,
            exercises = exercises,
            currentExerciseIndex = 0,
            currentSetNumber = 2,
            intervalPhase = com.example.ui.components.IntervalPhase.REST
        )
        val backFromRest = com.example.ui.viewmodel.WorkoutSessionHelper.resolvePreviousStep(restState)
        assertEquals(com.example.ui.viewmodel.StepTarget(0, 2), backFromRest)

        // 2. From EXERCISE set 2 -> previous step returns to set 1 of same exercise
        val exSet2 = restState.copy(intervalPhase = com.example.ui.components.IntervalPhase.EXERCISE)
        assertEquals(
            com.example.ui.viewmodel.StepTarget(0, 1),
            com.example.ui.viewmodel.WorkoutSessionHelper.resolvePreviousStep(exSet2)
        )

        // 3. From EXERCISE index 1, set 1 -> previous step goes to last set of previous exercise
        val ex2Set1 = restState.copy(
            currentExerciseIndex = 1,
            currentSetNumber = 1,
            intervalPhase = com.example.ui.components.IntervalPhase.EXERCISE
        )
        assertEquals(
            com.example.ui.viewmodel.StepTarget(0, ex1.targetSets),
            com.example.ui.viewmodel.WorkoutSessionHelper.resolvePreviousStep(ex2Set1)
        )

        // 4. Previous exercise jumps to last recorded set of previous exercise (or set 1 if none)
        val ex1Set2Log = SetLogEntity(sessionId = 0L, exerciseName = ex1.name, setNumber = 2, weightKg = 70.0, reps = 8)
        val ex2WithLogs = ex2Set1.copy(recordedLogs = listOf(ex1Set2Log))
        assertEquals(
            com.example.ui.viewmodel.StepTarget(0, 2),
            com.example.ui.viewmodel.WorkoutSessionHelper.resolvePreviousExercise(ex2WithLogs)
        )
        assertEquals(
            com.example.ui.viewmodel.StepTarget(0, 1),
            com.example.ui.viewmodel.WorkoutSessionHelper.resolvePreviousExercise(ex2Set1)
        )

        // 5. Upsert updates existing (exerciseName, setNumber) in place without duplicating
        val updatedSet2 = ex1Set2Log.copy(weightKg = 75.0, reps = 10)
        val afterUpsert = com.example.ui.viewmodel.WorkoutSessionHelper.upsertSetLog(listOf(ex1Set2Log), updatedSet2)
        assertEquals(1, afterUpsert.size)
        assertEquals(75.0, afterUpsert[0].weightKg, 0.001)
        assertEquals(10, afterUpsert[0].reps)

        // 6. Direct edit & remove by index
        val ex2Log = SetLogEntity(sessionId = 0L, exerciseName = ex2.name, setNumber = 1, weightKg = 30.0, reps = 10)
        val edited = com.example.ui.viewmodel.WorkoutSessionHelper.updateSetLogAt(afterUpsert + ex2Log, 1, 32.5, 12)
        assertEquals(32.5, edited[1].weightKg, 0.001)
        assertEquals(12, edited[1].reps)

        val removed = com.example.ui.viewmodel.WorkoutSessionHelper.removeSetLogAt(edited, 0)
        assertEquals(1, removed.size)
        assertEquals(ex2.name, removed[0].exerciseName)
    }
}

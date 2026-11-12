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
}

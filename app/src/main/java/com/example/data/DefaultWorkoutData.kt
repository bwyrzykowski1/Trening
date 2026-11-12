package com.example.data

import com.example.data.model.DayType
import com.example.data.model.PlanExerciseEntity

object DefaultWorkoutData {

    fun getDefaultExercises(): List<PlanExerciseEntity> {
        val list = mutableListOf<PlanExerciseEntity>()
        var order = 0

        // PUSH (Poniedziałek) - 7 ćwiczeń, 19 serii
        val pushExercises = listOf(
            Triple("Wyciskanie sztangi na ławce płaskiej", 3, 6 to 8 to "Klatka piersiowa"),
            Triple("Wyciskanie hantli na skosie dodatnim", 3, 8 to 12 to "Klatka piersiowa"),
            Triple("Wyciskanie hantli siedząc", 3, 8 to 10 to "Barki"),
            Triple("Unoszenie hantli bokiem", 3, 12 to 15 to "Barki"),
            Triple("Rozpiętki na bramie", 2, 12 to 15 to "Klatka piersiowa"),
            Triple("Prostowanie ramion na wyciągu", 3, 10 to 12 to "Triceps"),
            Triple("Prostowanie ramion nad głową na wyciągu", 2, 12 to 15 to "Triceps")
        )

        pushExercises.forEachIndexed { idx, item ->
            val repsAndGroup = item.third
            list.add(
                PlanExerciseEntity(
                    dayType = DayType.PUSH.code,
                    name = item.first,
                    targetSets = item.second,
                    minReps = repsAndGroup.first.first,
                    maxReps = repsAndGroup.first.second,
                    orderIndex = idx,
                    muscleGroup = repsAndGroup.second,
                    isCustom = false
                )
            )
        }

        // PULL (Środa) - 7 ćwiczeń, 19 serii
        val pullExercises = listOf(
            Triple("Podciąganie na drążku", 3, 6 to 10 to "Plecy"),
            Triple("Wiosłowanie na maszynie z podparciem", 3, 8 to 10 to "Plecy"),
            Triple("Ściąganie drążka do klatki", 3, 10 to 12 to "Plecy"),
            Triple("Wiosłowanie na wyciągu", 2, 10 to 12 to "Plecy"),
            Triple("Reverse pec deck", 3, 12 to 15 to "Tył barków"),
            Triple("Uginanie ramion ze sztangą", 3, 8 to 12 to "Biceps"),
            Triple("Uginanie hantli na ławce skośnej", 2, 10 to 12 to "Biceps")
        )

        pullExercises.forEachIndexed { idx, item ->
            val repsAndGroup = item.third
            list.add(
                PlanExerciseEntity(
                    dayType = DayType.PULL.code,
                    name = item.first,
                    targetSets = item.second,
                    minReps = repsAndGroup.first.first,
                    maxReps = repsAndGroup.first.second,
                    orderIndex = idx,
                    muscleGroup = repsAndGroup.second,
                    isCustom = false
                )
            )
        }

        // LEGS + dodatki (Piątek) - 8 ćwiczeń, 20 serii
        val legsExercises = listOf(
            Triple("Przysiady ze sztangą", 3, 6 to 10 to "Nogi"),
            Triple("Martwy ciąg rumuński", 3, 8 to 10 to "Nogi / Pośladki"),
            Triple("Suwnica", 2, 10 to 12 to "Nogi"),
            Triple("Uginanie nóg na maszynie", 2, 10 to 15 to "Nogi / Dwugłowe"),
            Triple("Wspięcia na palce", 3, 12 to 15 to "Łydki"),
            Triple("Unoszenie nóg w zwisie", 3, 12 to 15 to "Brzuch"),
            Triple("Wyciskanie na maszynie na klatkę", 2, 10 to 12 to "Dodatki (Klatka)"),
            Triple("Unoszenie hantli bokiem", 2, 12 to 15 to "Dodatki (Barki)")
        )

        legsExercises.forEachIndexed { idx, item ->
            val repsAndGroup = item.third
            list.add(
                PlanExerciseEntity(
                    dayType = DayType.LEGS.code,
                    name = item.first,
                    targetSets = item.second,
                    minReps = repsAndGroup.first.first,
                    maxReps = repsAndGroup.first.second,
                    orderIndex = idx,
                    muscleGroup = repsAndGroup.second,
                    isCustom = false
                )
            )
        }

        return list
    }
}

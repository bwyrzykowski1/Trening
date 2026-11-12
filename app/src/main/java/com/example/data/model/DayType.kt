package com.example.data.model

import java.time.DayOfWeek
import java.time.LocalDate

enum class DayType(
    val code: String,
    val title: String,
    val recommendedDayOfWeek: DayOfWeek,
    val recommendedDayName: String,
    val muscles: String
) {
    PUSH(
        code = "PUSH",
        title = "PUSH — Klatka, Barki, Triceps",
        recommendedDayOfWeek = DayOfWeek.MONDAY,
        recommendedDayName = "Poniedziałek",
        muscles = "Klatka piersiowa • barki • triceps"
    ),
    PULL(
        code = "PULL",
        title = "PULL — Plecy, Tył barków, Biceps",
        recommendedDayOfWeek = DayOfWeek.WEDNESDAY,
        recommendedDayName = "Środa",
        muscles = "Plecy • tył barków • biceps"
    ),
    LEGS(
        code = "LEGS",
        title = "LEGS + Dodatki — Nogi, Brzuch, Góra",
        recommendedDayOfWeek = DayOfWeek.FRIDAY,
        recommendedDayName = "Piątek",
        muscles = "Nogi • pośladki • łydki • brzuch + dodatkowa góra ciała"
    );

    companion object {
        fun fromCode(code: String): DayType = entries.find { it.code == code } ?: PUSH

        fun getSuggestedForToday(): DayType {
            return when (LocalDate.now().dayOfWeek) {
                DayOfWeek.MONDAY, DayOfWeek.TUESDAY -> PUSH
                DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY -> PULL
                DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY -> LEGS
                else -> PUSH
            }
        }
    }
}

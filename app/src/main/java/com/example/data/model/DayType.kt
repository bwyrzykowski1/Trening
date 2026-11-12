package com.example.data.model

import java.time.DayOfWeek
import java.time.LocalDate

import androidx.compose.ui.graphics.Color

enum class DayType(
    val code: String,
    val title: String,
    val recommendedDayOfWeek: DayOfWeek,
    val recommendedDayName: String,
    val muscles: String,
    val color: Color
) {
    PUSH(
        code = "PUSH",
        title = "PUSH — Klatka, Barki, Triceps",
        recommendedDayOfWeek = DayOfWeek.MONDAY,
        recommendedDayName = "Poniedziałek",
        muscles = "Klatka piersiowa • barki • triceps",
        color = Color(0xFFFF6B4A)
    ),
    PULL(
        code = "PULL",
        title = "PULL — Plecy, Tył barków, Biceps",
        recommendedDayOfWeek = DayOfWeek.WEDNESDAY,
        recommendedDayName = "Środa",
        muscles = "Plecy • tył barków • biceps",
        color = Color(0xFF00D2FF)
    ),
    LEGS(
        code = "LEGS",
        title = "LEGS + Dodatki — Nogi, Brzuch, Góra",
        recommendedDayOfWeek = DayOfWeek.FRIDAY,
        recommendedDayName = "Piątek",
        muscles = "Nogi • pośladki • łydki • brzuch + dodatkowa góra ciała",
        color = Color(0xFF22C55E)
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

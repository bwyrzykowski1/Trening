package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Primary workout accents
val CyanAccent = Color(0xFF00D2FF)
val CyanAccentDark = Color(0xFF008BA3)
val LimeAccent = Color(0xFF22C55E)
val AmberAccent = Color(0xFFF59E0B)
val CoralAccent = Color(0xFFEF4444)

// Workout Day Type Colors
val PushColor = Color(0xFFFF6B4A) // Energetic Flame Orange
val PullColor = Color(0xFF00D2FF) // Electric Sky Cyan
val LegsColor = Color(0xFF22C55E) // Vibrant Emerald Lime

fun getDayTypeColor(dayType: String): Color {
    return when (dayType.trim().uppercase()) {
        "PUSH" -> PushColor
        "PULL" -> PullColor
        "LEGS" -> LegsColor
        else -> CyanAccent
    }
}

// Background & Surface
val DarkBackground = Color(0xFF0B0F17)
val DarkSurface = Color(0xFF151C28)
val DarkSurfaceVariant = Color(0xFF1E2838)
val DarkBorder = Color(0xFF2D3B4F)

// Text & Highlights
val TextPrimary = Color(0xFFF8FAFC)
val TextSecondary = Color(0xFF94A3B8)
val TextMuted = Color(0xFF64748B)

// Cards & Indicators
val CardHighlight = Color(0xFF1A2332)
val RestTimerColor = Color(0xFF38BDF8)
val WorkTimerColor = Color(0xFFF43F5E)

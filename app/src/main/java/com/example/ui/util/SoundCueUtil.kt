package com.example.ui.util

import android.media.AudioManager
import android.media.ToneGenerator

object SoundCueUtil {
    private var toneGenerator: ToneGenerator? = null

    private fun getGenerator(): ToneGenerator? {
        if (toneGenerator == null) {
            try {
                toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 95)
            } catch (_: Exception) {
                // Fallback for devices with audio focus restrictions
            }
        }
        return toneGenerator
    }

    /**
     * Short beep for 3, 2, 1 countdown warnings before interval conclusion
     */
    fun playCountdownTick() {
        try {
            getGenerator()?.startTone(ToneGenerator.TONE_PROP_BEEP, 100)
        } catch (_: Exception) {}
    }

    /**
     * Strong double tone when a work set ends
     */
    fun playSetEndedCue() {
        try {
            getGenerator()?.startTone(ToneGenerator.TONE_CDMA_ALERT_NETWORK_LITE, 350)
        } catch (_: Exception) {}
    }

    /**
     * Crisp upbeat chime when a rest interval concludes (time to lift!)
     */
    fun playRestConcludedCue() {
        try {
            getGenerator()?.startTone(ToneGenerator.TONE_PROP_ACK, 400)
        } catch (_: Exception) {}
    }

    /**
     * Celebratory chime when the entire workout finishes
     */
    fun playWorkoutCompleteCue() {
        try {
            getGenerator()?.startTone(ToneGenerator.TONE_CDMA_HIGH_L, 500)
        } catch (_: Exception) {}
    }
}

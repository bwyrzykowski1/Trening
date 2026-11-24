package com.example.ui.util

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.ToneGenerator
import java.util.concurrent.Executors
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

object SoundCueUtil {
    private const val SAMPLE_RATE = 44100
    private val audioExecutor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "workout-sound-cue").apply { isDaemon = true }
    }

    data class ToneSegment(val frequencyHz: Double, val durationMs: Int)

    fun normalizeVolume(volume: Float): Float = volume.coerceIn(0f, 1f)

    /**
     * Adjusts the device's Android audio stream volume (MUSIC or ALARM) proportionally to
     * the in-app slider so turning volume up inside the app genuinely increases speaker output.
     */
    fun applySystemStreamVolume(context: Context, volume: Float, useAlarmStream: Boolean) {
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
            val stream = if (useAlarmStream) AudioManager.STREAM_ALARM else AudioManager.STREAM_MUSIC
            val maxVolume = audioManager.getStreamMaxVolume(stream)
            if (maxVolume > 0) {
                val targetIndex = (normalizeVolume(volume) * maxVolume).roundToInt().coerceIn(0, maxVolume)
                audioManager.setStreamVolume(stream, targetIndex, 0)
            }
        } catch (_: Exception) {
            // Safely ignore in unit tests or restricted audio environments
        }
    }

    /**
     * Generates full-scale 16-bit PCM samples with fundamental + 2nd harmonic
     * (significantly louder and clearer on smartphone speakers than ToneGenerator).
     */
    internal fun generatePcmSamples(
        segments: List<ToneSegment>,
        volume: Float,
        boostGain: Boolean = false
    ): ShortArray {
        val clampedVol = normalizeVolume(volume)
        val totalSamples = segments.sumOf { (SAMPLE_RATE * it.durationMs) / 1000 }
        if (totalSamples <= 0) return ShortArray(0)

        val buffer = ShortArray(totalSamples)
        if (clampedVol <= 0f) return buffer

        // Rich harmonic mix cuts through gym noise cleanly; boostGain pushes near 0 dBFS peak
        val peakScale = (if (boostGain) 0.98 else 0.85) * clampedVol * Short.MAX_VALUE
        var offset = 0

        for (segment in segments) {
            val count = (SAMPLE_RATE * segment.durationMs) / 1000
            if (segment.frequencyHz <= 0.0 || count <= 0) {
                offset += count
                continue
            }

            val fadeSamples = (SAMPLE_RATE * 0.008).toInt().coerceAtMost(count / 2).coerceAtLeast(1)
            val omega1 = 2.0 * PI * segment.frequencyHz / SAMPLE_RATE
            val omega2 = 2.0 * PI * (segment.frequencyHz * 2.0) / SAMPLE_RATE

            for (i in 0 until count) {
                val envelope = when {
                    i < fadeSamples -> i.toDouble() / fadeSamples
                    i >= count - fadeSamples -> (count - 1 - i).toDouble() / fadeSamples
                    else -> 1.0
                }
                val wave = 0.78 * sin(omega1 * i) + 0.22 * sin(omega2 * i)
                val sample = (wave * envelope * peakScale)
                    .roundToInt()
                    .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                buffer[offset + i] = sample.toShort()
            }
            offset += count
        }

        return buffer
    }

    private fun playToneSegments(
        segments: List<ToneSegment>,
        volume: Float,
        useAlarmStream: Boolean,
        fallbackTone: Int,
        fallbackDurationMs: Int
    ) {
        val clampedVol = normalizeVolume(volume)
        if (clampedVol <= 0f) return

        audioExecutor.execute {
            val played = playViaAudioTrack(segments, clampedVol, useAlarmStream)
            if (!played) {
                playViaToneGenerator(fallbackTone, fallbackDurationMs, clampedVol, useAlarmStream)
            }
        }
    }

    private fun playViaAudioTrack(
        segments: List<ToneSegment>,
        volume: Float,
        useAlarmStream: Boolean
    ): Boolean {
        var track: AudioTrack? = null
        return try {
            val samples = generatePcmSamples(segments, volume, boostGain = useAlarmStream)
            if (samples.isEmpty()) return true

            val bufferBytes = samples.size * 2
            val usage = if (useAlarmStream) {
                AudioAttributes.USAGE_ALARM
            } else {
                AudioAttributes.USAGE_MEDIA
            }

            val audioAttributes = AudioAttributes.Builder()
                .setUsage(usage)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            val audioFormat = AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(SAMPLE_RATE)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build()

            track = AudioTrack.Builder()
                .setAudioAttributes(audioAttributes)
                .setAudioFormat(audioFormat)
                .setBufferSizeInBytes(bufferBytes)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            if (track.state != AudioTrack.STATE_INITIALIZED) {
                track.release()
                return false
            }

            track.write(samples, 0, samples.size)
            track.play()

            val totalMs = segments.sumOf { it.durationMs }.toLong() + 40L
            Thread.sleep(totalMs)
            true
        } catch (_: Exception) {
            false
        } finally {
            try {
                track?.stop()
                track?.release()
            } catch (_: Exception) {}
        }
    }

    private fun playViaToneGenerator(
        toneType: Int,
        durationMs: Int,
        volume: Float,
        useAlarmStream: Boolean
    ) {
        var generator: ToneGenerator? = null
        try {
            val stream = if (useAlarmStream) AudioManager.STREAM_ALARM else AudioManager.STREAM_MUSIC
            val toneVol = (normalizeVolume(volume) * 100f).roundToInt().coerceIn(1, 100)
            generator = ToneGenerator(stream, toneVol)
            generator.startTone(toneType, durationMs)
            Thread.sleep(durationMs.toLong() + 40L)
        } catch (_: Exception) {
            // Fallback for devices with audio focus restrictions
        } finally {
            try {
                generator?.release()
            } catch (_: Exception) {}
        }
    }

    /**
     * Quick preview beep when adjusting volume or tapping Test
     */
    fun playVolumePreviewCue(volume: Float = 1.0f, useAlarmStream: Boolean = false) {
        playToneSegments(
            segments = listOf(ToneSegment(1046.5, 130)),
            volume = volume,
            useAlarmStream = useAlarmStream,
            fallbackTone = ToneGenerator.TONE_PROP_BEEP,
            fallbackDurationMs = 130
        )
    }

    /**
     * Short beep for 3, 2, 1 countdown warnings before interval conclusion
     */
    fun playCountdownTick(volume: Float = 1.0f, useAlarmStream: Boolean = false) {
        playToneSegments(
            segments = listOf(ToneSegment(880.0, 95)),
            volume = volume,
            useAlarmStream = useAlarmStream,
            fallbackTone = ToneGenerator.TONE_PROP_BEEP,
            fallbackDurationMs = 100
        )
    }

    /**
     * Strong double tone when a work set ends
     */
    fun playSetEndedCue(volume: Float = 1.0f, useAlarmStream: Boolean = false) {
        playToneSegments(
            segments = listOf(
                ToneSegment(1046.5, 130),
                ToneSegment(0.0, 60),
                ToneSegment(1318.5, 220)
            ),
            volume = volume,
            useAlarmStream = useAlarmStream,
            fallbackTone = ToneGenerator.TONE_CDMA_ALERT_NETWORK_LITE,
            fallbackDurationMs = 350
        )
    }

    /**
     * Crisp upbeat triple chime when a rest interval concludes (time to lift!)
     */
    fun playRestConcludedCue(volume: Float = 1.0f, useAlarmStream: Boolean = false) {
        playToneSegments(
            segments = listOf(
                ToneSegment(880.0, 100),
                ToneSegment(0.0, 45),
                ToneSegment(1174.7, 100),
                ToneSegment(0.0, 45),
                ToneSegment(1568.0, 260)
            ),
            volume = volume,
            useAlarmStream = useAlarmStream,
            fallbackTone = ToneGenerator.TONE_PROP_ACK,
            fallbackDurationMs = 400
        )
    }

    /**
     * Celebratory chime when the entire workout finishes
     */
    fun playWorkoutCompleteCue(volume: Float = 1.0f, useAlarmStream: Boolean = false) {
        playToneSegments(
            segments = listOf(
                ToneSegment(784.0, 100),
                ToneSegment(0.0, 35),
                ToneSegment(987.8, 100),
                ToneSegment(0.0, 35),
                ToneSegment(1174.7, 110),
                ToneSegment(0.0, 35),
                ToneSegment(1568.0, 320)
            ),
            volume = volume,
            useAlarmStream = useAlarmStream,
            fallbackTone = ToneGenerator.TONE_CDMA_HIGH_L,
            fallbackDurationMs = 500
        )
    }
}

package com.example.data.csv

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.model.SetLogEntity
import com.example.data.model.WorkoutSessionEntity
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ParsedWorkoutData(
    val session: WorkoutSessionEntity,
    val sets: List<SetLogEntity>
)

data class ImportSummary(
    val sessionsImported: Int,
    val setsImported: Int,
    val sessionsSkipped: Int,
    val message: String
)

object CsvManager {

    private const val CSV_HEADER = "SessionId,DateMillis,DateFormatted,DayType,DurationSeconds,ExerciseName,SetNumber,WeightKg,Reps,VolumeKg,Estimated1RM"

    fun generateCsv(sessions: List<WorkoutSessionEntity>, setLogs: List<SetLogEntity>): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val builder = java.lang.StringBuilder()
        builder.append(CSV_HEADER).append("\n")

        val logsBySessionId = setLogs.groupBy { it.sessionId }

        for (session in sessions) {
            val logs = logsBySessionId[session.id]
            val dateFormatted = dateFormat.format(Date(session.dateMillis))

            if (!logs.isNullOrEmpty()) {
                for (log in logs) {
                    val escapedName = escapeCsv(log.exerciseName)
                    val est1RM = String.format(Locale.US, "%.2f", log.estimated1RM)
                    val vol = String.format(Locale.US, "%.2f", log.volume)
                    val weight = String.format(Locale.US, "%.2f", log.weightKg)

                    builder.append(session.id).append(",")
                        .append(session.dateMillis).append(",")
                        .append(escapeCsv(dateFormatted)).append(",")
                        .append(session.dayType).append(",")
                        .append(session.durationSeconds).append(",")
                        .append(escapedName).append(",")
                        .append(log.setNumber).append(",")
                        .append(weight).append(",")
                        .append(log.reps).append(",")
                        .append(vol).append(",")
                        .append(est1RM)
                        .append("\n")
                }
            } else {
                // Session with no logged sets
                builder.append(session.id).append(",")
                    .append(session.dateMillis).append(",")
                    .append(escapeCsv(dateFormatted)).append(",")
                    .append(session.dayType).append(",")
                    .append(session.durationSeconds).append(",")
                    .append(",,,,,\n")
            }
        }

        return builder.toString()
    }

    fun parseCsv(csvText: String): List<ParsedWorkoutData> {
        val lines = csvText.lines()
        if (lines.isEmpty()) return emptyList()

        // Key: Pair of dateMillis and dayType
        val sessionMap = mutableMapOf<Pair<Long, String>, MutableList<SetLogEntity>>()
        val durationMap = mutableMapOf<Pair<Long, String>, Int>()

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("SessionId", ignoreCase = true)) {
                continue
            }

            val tokens = splitCsvLine(trimmed)
            if (tokens.size < 5) continue

            val dateMillis = tokens.getOrNull(1)?.toLongOrNull() ?: continue
            val dayType = tokens.getOrNull(3)?.trim() ?: "PPL"
            val duration = tokens.getOrNull(4)?.toIntOrNull() ?: 0

            val key = Pair(dateMillis, dayType)
            durationMap[key] = duration

            val list = sessionMap.getOrPut(key) { mutableListOf() }

            if (tokens.size >= 9) {
                val exerciseName = tokens.getOrNull(5)?.trim() ?: ""
                val setNumber = tokens.getOrNull(6)?.toIntOrNull() ?: 1
                val weight = tokens.getOrNull(7)?.toDoubleOrNull() ?: 0.0
                val reps = tokens.getOrNull(8)?.toIntOrNull() ?: 0

                if (exerciseName.isNotEmpty()) {
                    list.add(
                        SetLogEntity(
                            sessionId = 0L,
                            exerciseName = exerciseName,
                            setNumber = setNumber,
                            weightKg = weight,
                            reps = reps,
                            timestamp = dateMillis
                        )
                    )
                }
            }
        }

        val result = mutableListOf<ParsedWorkoutData>()
        for ((key, sets) in sessionMap) {
            val (dateMillis, dayType) = key
            val duration = durationMap[key] ?: 0
            val totalTonnage = sets.sumOf { it.weightKg * it.reps }

            val session = WorkoutSessionEntity(
                id = 0L,
                dateMillis = dateMillis,
                dayType = dayType,
                durationSeconds = duration,
                totalTonnageKg = totalTonnage,
                completedSetsCount = sets.size
            )
            result.add(ParsedWorkoutData(session, sets))
        }

        return result
    }

    fun writeCsvToUri(contentResolver: ContentResolver, uri: Uri, csvContent: String): Boolean {
        return try {
            contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(csvContent.toByteArray(Charsets.UTF_8))
                outputStream.flush()
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    fun readCsvFromUri(contentResolver: ContentResolver, uri: Uri): String? {
        return try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
                    reader.readText()
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    fun createShareableFile(context: Context, csvContent: String): Uri? {
        return try {
            val cacheDir = File(context.cacheDir, "exports")
            if (!cacheDir.exists()) cacheDir.mkdirs()

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val file = File(cacheDir, "PPL_trening_backup_$timestamp.csv")

            FileOutputStream(file).use { fos ->
                fos.write(csvContent.toByteArray(Charsets.UTF_8))
                fos.flush()
            }

            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun escapeCsv(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }

    private fun splitCsvLine(line: String): List<String> {
        val tokens = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        var i = 0

        while (i < line.length) {
            val c = line[i]
            if (c == '\"') {
                if (inQuotes && i + 1 < line.length && line[i + 1] == '\"') {
                    sb.append('\"')
                    i++
                } else {
                    inQuotes = !inQuotes
                }
            } else if (c == ',' && !inQuotes) {
                tokens.add(sb.toString())
                sb.clear()
            } else {
                sb.append(c)
            }
            i++
        }
        tokens.add(sb.toString())
        return tokens
    }
}

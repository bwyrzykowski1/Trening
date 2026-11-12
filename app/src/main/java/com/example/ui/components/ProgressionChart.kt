package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.LimeAccent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

data class ChartDataPoint(
    val timestamp: Long,
    val value: Double,
    val label: String,
    val secondaryValue: Double? = null
)

@Composable
fun ProgressionChart(
    title: String,
    unit: String,
    points: List<ChartDataPoint>,
    modifier: Modifier = Modifier,
    accentColor: Color = CyanAccent
) {
    var selectedPointIndex by remember { mutableStateOf<Int?>(null) }
    val progress = remember { Animatable(0f) }

    LaunchedEffect(points) {
        progress.snapTo(0f)
        progress.animateTo(1f, animationSpec = tween(durationMillis = 650))
        selectedPointIndex = points.indices.lastOrNull()
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(accentColor.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (points.isNotEmpty()) {
                            val maxVal = points.maxOf { it.value }
                            Text(
                                text = "Rekord: ${String.format(Locale.getDefault(), "%.1f", maxVal)} $unit",
                                style = MaterialTheme.typography.labelSmall,
                                color = LimeAccent
                            )
                        }
                    }
                }

                // Selected point details badge
                val selected = selectedPointIndex?.let { points.getOrNull(it) } ?: points.lastOrNull()
                if (selected != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = accentColor.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = "${String.format(Locale.getDefault(), "%.1f", selected.value)} $unit",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = accentColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (points.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Brak danych do wykresu. Ukończ trening, aby zobaczyć progresję!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(points) {
                                detectTapGestures { offset ->
                                    if (points.isNotEmpty()) {
                                        val stepX = size.width / (points.size - 1).coerceAtLeast(1)
                                        val clickedIndex = ((offset.x + stepX / 2) / stepX)
                                            .toInt()
                                            .coerceIn(0, points.size - 1)
                                        selectedPointIndex = clickedIndex
                                    }
                                }
                            }
                    ) {
                        val width = size.width
                        val height = size.height
                        val paddingBottom = 24.dp.toPx()
                        val paddingTop = 16.dp.toPx()
                        val chartHeight = height - paddingBottom - paddingTop

                        val minVal = (points.minOf { it.value } * 0.9).coerceAtLeast(0.0)
                        val maxVal = max(points.maxOf { it.value } * 1.1, minVal + 10.0)
                        val valRange = (maxVal - minVal).coerceAtLeast(1.0)

                        // Draw horizontal subtle grid lines
                        val gridLines = 3
                        for (i in 0..gridLines) {
                            val y = paddingTop + (chartHeight / gridLines) * i
                            drawLine(
                                color = Color.White.copy(alpha = 0.08f),
                                start = Offset(0f, y),
                                end = Offset(width, y),
                                strokeWidth = 1.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
                            )
                        }

                        if (points.size == 1) {
                            // Single point: draw a nice bar in center
                            val p = points[0]
                            val barW = 36.dp.toPx()
                            val barH = ((p.value - minVal) / valRange * chartHeight * progress.value).toFloat()
                            val x = width / 2 - barW / 2
                            val y = height - paddingBottom - barH

                            drawRoundRect(
                                brush = Brush.verticalGradient(
                                    colors = listOf(accentColor, accentColor.copy(alpha = 0.4f)),
                                    startY = y,
                                    endY = height - paddingBottom
                                ),
                                topLeft = Offset(x, y),
                                size = Size(barW, barH),
                                cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                            )
                            drawCircle(
                                color = Color.White,
                                radius = 5.dp.toPx(),
                                center = Offset(width / 2, y)
                            )
                        } else {
                            val stepX = width / (points.size - 1)
                            val path = Path()
                            val fillPath = Path()

                            val coordinates = points.mapIndexed { index, dataPoint ->
                                val x = index * stepX
                                val normalizedVal = (dataPoint.value - minVal) / valRange
                                val y = height - paddingBottom - (normalizedVal * chartHeight * progress.value).toFloat()
                                Offset(x, y)
                            }

                            coordinates.forEachIndexed { index, offset ->
                                if (index == 0) {
                                    path.moveTo(offset.x, offset.y)
                                    fillPath.moveTo(offset.x, height - paddingBottom)
                                    fillPath.lineTo(offset.x, offset.y)
                                } else {
                                    val prev = coordinates[index - 1]
                                    val ctrlX1 = (prev.x + offset.x) / 2
                                    val ctrlY1 = prev.y
                                    val ctrlX2 = (prev.x + offset.x) / 2
                                    val ctrlY2 = offset.y
                                    path.cubicTo(ctrlX1, ctrlY1, ctrlX2, ctrlY2, offset.x, offset.y)
                                    fillPath.cubicTo(ctrlX1, ctrlY1, ctrlX2, ctrlY2, offset.x, offset.y)
                                }
                            }

                            fillPath.lineTo(coordinates.last().x, height - paddingBottom)
                            fillPath.close()

                            // Fill gradient
                            drawPath(
                                path = fillPath,
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        accentColor.copy(alpha = 0.35f),
                                        accentColor.copy(alpha = 0.0f)
                                    ),
                                    startY = paddingTop,
                                    endY = height - paddingBottom
                                )
                            )

                            // Stroke line
                            drawPath(
                                path = path,
                                color = accentColor,
                                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                            )

                            // Points
                            coordinates.forEachIndexed { index, offset ->
                                val isSelected = index == selectedPointIndex
                                val pointRadius = if (isSelected) 6.dp.toPx() else 4.dp.toPx()
                                drawCircle(
                                    color = if (isSelected) Color.White else accentColor,
                                    radius = pointRadius,
                                    center = offset
                                )
                                drawCircle(
                                    color = accentColor,
                                    radius = pointRadius,
                                    center = offset,
                                    style = Stroke(width = 2.dp.toPx())
                                )
                            }
                        }
                    }
                }

                // X-Axis labels
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val dateFormat = SimpleDateFormat("dd.MM", Locale.getDefault())
                    val firstLabel = points.firstOrNull()?.let { dateFormat.format(Date(it.timestamp)) } ?: ""
                    val lastLabel = points.lastOrNull()?.let { dateFormat.format(Date(it.timestamp)) } ?: ""
                    val midLabel = if (points.size > 2) {
                        dateFormat.format(Date(points[points.size / 2].timestamp))
                    } else ""

                    Text(text = firstLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (midLabel.isNotEmpty()) {
                        Text(text = midLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(text = lastLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

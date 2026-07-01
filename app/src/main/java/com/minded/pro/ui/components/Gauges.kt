package com.minded.pro.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.minded.pro.ui.theme.TrackColor

/**
 * A 270° arc gauge for a 0..100 metric. The arc sweeps and the centre number
 * animates whenever [value] changes.
 */
@Composable
fun MetricGauge(
    value: Int,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    val clamped = value.coerceIn(0, 100)
    val fraction by animateFloatAsState(
        targetValue = clamped / 100f,
        animationSpec = tween(durationMillis = 450),
        label = "gauge",
    )
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(116.dp)) {
            val stroke = 15.dp.toPx()
            val arcSize = Size(size.width - stroke, size.height - stroke)
            val topLeft = Offset(stroke / 2f, stroke / 2f)
            drawArc(
                color = TrackColor,
                startAngle = START_ANGLE,
                sweepAngle = SWEEP_ANGLE,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            drawArc(
                color = accent,
                startAngle = START_ANGLE,
                sweepAngle = SWEEP_ANGLE * fraction,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }
        Text(
            text = clamped.toString(),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/**
 * A full-circle ring showing sensor contact quality as a percentage, where
 * [accent] is chosen by the caller to reflect how good the contact is.
 */
@Composable
fun ContactRing(
    percent: Int,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    val clamped = percent.coerceIn(0, 100)
    val fraction by animateFloatAsState(
        targetValue = clamped / 100f,
        animationSpec = tween(durationMillis = 450),
        label = "contact-ring",
    )
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(96.dp)) {
            val stroke = 12.dp.toPx()
            val arcSize = Size(size.width - stroke, size.height - stroke)
            val topLeft = Offset(stroke / 2f, stroke / 2f)
            drawArc(
                color = TrackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            drawArc(
                color = accent,
                startAngle = -90f,
                sweepAngle = 360f * fraction,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }
        Text(
            text = "$clamped%",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

private const val START_ANGLE = 135f
private const val SWEEP_ANGLE = 270f

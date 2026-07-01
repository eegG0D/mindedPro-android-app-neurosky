package com.minded.pro.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.minded.pro.ui.theme.TrackColor
import kotlin.math.abs
import kotlin.math.max

/**
 * A scrolling line trace of the most recent raw EEG samples. The vertical
 * scale auto-fits the largest sample in view so a flat signal still reads
 * clearly.
 */
@Composable
fun WaveformChart(
    samples: List<Int>,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier) {
        val midY = size.height / 2f
        drawLine(
            color = TrackColor,
            start = Offset(0f, midY),
            end = Offset(size.width, midY),
            strokeWidth = 1.dp.toPx(),
        )
        if (samples.size < 2) return@Canvas

        val peak = max(samples.maxOf { abs(it) }, 64)
        val scaleY = midY / (peak * 1.15f)
        val stepX = size.width / (samples.size - 1)

        val path = Path()
        samples.forEachIndexed { index, value ->
            val x = index * stepX
            val y = midY - value * scaleY
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(
            path = path,
            color = accent,
            style = Stroke(
                width = 2.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            ),
        )
    }
}

package com.milauncher.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

// Iconos de línea fina dibujados a mano (en una cuadrícula de 24x24 como el boceto)

@Composable
fun StarIcon(size: Dp = 13.dp, color: Color = Texto, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) {
        val u = this.size.width / 24f
        val c = Offset(12f * u, 12.5f * u)
        val path = Path()
        for (i in 0 until 10) {
            val r = if (i % 2 == 0) 9.5f * u else 4.2f * u
            val a = Math.toRadians((-90 + i * 36).toDouble())
            val p = Offset(c.x + (r * cos(a)).toFloat(), c.y + (r * sin(a)).toFloat())
            if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
        }
        path.close()
        drawPath(path, color, style = Stroke(width = 1.6f * u, join = StrokeJoin.Round))
    }
}

@Composable
fun NotebookIcon(size: Dp = 22.dp, color: Color = Texto, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) {
        val u = this.size.width / 24f
        val stroke = Stroke(width = 1.3f * u, cap = StrokeCap.Round, join = StrokeJoin.Round)
        drawRoundRect(
            color = color,
            topLeft = Offset(5f * u, 3f * u),
            size = Size(14f * u, 18f * u),
            cornerRadius = CornerRadius(2f * u),
            style = stroke,
        )
        drawLine(color, Offset(9f * u, 3f * u), Offset(9f * u, 21f * u), 1.3f * u)
        drawLine(color, Offset(12.5f * u, 8f * u), Offset(16f * u, 8f * u), 1.3f * u, StrokeCap.Round)
        drawLine(color, Offset(12.5f * u, 11.5f * u), Offset(16f * u, 11.5f * u), 1.3f * u, StrokeCap.Round)
    }
}

/** Asa de arrastre: tres líneas finas. */
@Composable
fun HandleIcon(size: Dp = 20.dp, color: Color = TextoSuave, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) {
        val u = this.size.width / 24f
        listOf(8f, 12f, 16f).forEach { y ->
            drawLine(color, Offset(5f * u, y * u), Offset(19f * u, y * u), 1.3f * u, StrokeCap.Round)
        }
    }
}

// Controles de música, mismo trazo fino

@Composable
fun PlayIcon(size: Dp = 20.dp, color: Color = Texto, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) {
        val u = this.size.width / 24f
        val path = Path().apply {
            moveTo(8f * u, 5f * u)
            lineTo(19f * u, 12f * u)
            lineTo(8f * u, 19f * u)
            close()
        }
        drawPath(path, color, style = Stroke(width = 1.3f * u, join = StrokeJoin.Round))
    }
}

@Composable
fun PauseIcon(size: Dp = 20.dp, color: Color = Texto, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) {
        val u = this.size.width / 24f
        drawLine(color, Offset(9f * u, 5f * u), Offset(9f * u, 19f * u), 1.3f * u, StrokeCap.Round)
        drawLine(color, Offset(15f * u, 5f * u), Offset(15f * u, 19f * u), 1.3f * u, StrokeCap.Round)
    }
}

/** Siguiente (forward = true) o anterior (forward = false). */
@Composable
fun SkipIcon(forward: Boolean, size: Dp = 18.dp, color: Color = Texto, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) {
        val u = this.size.width / 24f
        // Dibujamos "siguiente" y, para "anterior", lo reflejamos
        fun x(v: Float) = if (forward) v * u else (24f - v) * u
        val path = Path().apply {
            moveTo(x(6f), 6f * u)
            lineTo(x(15f), 12f * u)
            lineTo(x(6f), 18f * u)
            close()
        }
        val stroke = Stroke(width = 1.3f * u, cap = StrokeCap.Round, join = StrokeJoin.Round)
        drawPath(path, color, style = stroke)
        drawLine(color, Offset(x(18f), 6f * u), Offset(x(18f), 18f * u), 1.3f * u, StrokeCap.Round)
    }
}

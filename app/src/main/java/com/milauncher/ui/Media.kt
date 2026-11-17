package com.milauncher.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.milauncher.NotifListener
import com.milauncher.NowPlaying

/** Línea muy tenue bajo la música, para separarla de las favoritas. */
private val Separador = Color(0xFF1E1E1E)

/**
 * Música bajo el reloj, estilo "ecualizador": se ve como una app más de la lista.
 * En lugar de icono, tres barritas finas que se mueven mientras suena.
 * Tocar el título abre la app de música; a la derecha, pausa/reproducir y siguiente.
 */
@Composable
fun NowPlayingRow(np: NowPlaying, modifier: Modifier = Modifier) {
    val context = LocalContext.current

    Column(modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Barritas + título: la misma alineación que las filas de apps
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable {
                        context.packageManager.getLaunchIntentForPackage(np.packageName)
                            ?.let { context.startSafely(it) }
                    }
                    .padding(start = 52.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Equalizer(playing = np.isPlaying)
                Spacer(Modifier.width(26.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = np.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Light,
                        letterSpacing = 0.3.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (np.artist.isNotBlank()) {
                        Text(
                            text = np.artist,
                            modifier = Modifier.padding(top = 2.dp),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Light,
                            color = TextoSuave,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            MediaButton(
                label = if (np.isPlaying) "Pausar" else "Reproducir",
                onClick = { NotifListener.playPause() },
            ) {
                if (np.isPlaying) PauseIcon(size = 18.dp) else PlayIcon(size = 18.dp)
            }
            MediaButton("Siguiente", onClick = { NotifListener.next() }) {
                SkipIcon(forward = true, size = 16.dp)
            }
        }
        Box(
            Modifier
                .padding(start = 52.dp, end = 8.dp)
                .fillMaxWidth()
                .height(1.dp)
                .background(Separador)
        )
    }
}

/** Tres barras finas. Se mueven solo mientras suena; en pausa se quedan bajitas. */
@Composable
private fun Equalizer(playing: Boolean) {
    val transition = rememberInfiniteTransition(label = "ecualizador")
    fun bar(millis: Int) = infiniteRepeatable<Float>(
        animation = tween(millis, easing = FastOutSlowInEasing),
        repeatMode = RepeatMode.Reverse,
    )
    val b1 by transition.animateFloat(0.25f, 0.75f, bar(450), label = "b1")
    val b2 by transition.animateFloat(0.65f, 0.2f, bar(350), label = "b2")
    val b3 by transition.animateFloat(0.4f, 0.85f, bar(550), label = "b3")
    val heights = if (playing) listOf(b1, b2, b3) else listOf(0.15f, 0.15f, 0.15f)

    Canvas(
        Modifier
            .size(24.dp)
            .semantics { contentDescription = if (playing) "Sonando" else "En pausa" }
    ) {
        val u = size.width / 24f
        val stroke = 2f * u
        val bottom = 20f * u
        val maxH = 16f * u
        listOf(8f, 12f, 16f).forEachIndexed { i, x ->
            drawLine(
                color = Texto,
                start = Offset(x * u, bottom),
                end = Offset(x * u, bottom - maxH * heights[i]),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
        }
    }
}

@Composable
private fun MediaButton(label: String, onClick: () -> Unit, icon: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        icon()
    }
}

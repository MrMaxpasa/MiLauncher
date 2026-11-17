package com.milauncher.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos

/** Alto de cada elemento de la columna (estrella, letras y libreta: todos iguales). */
private val ITEM_HEIGHT = 18.dp

/** Cuánto se apartan hacia la izquierda los elementos más cercanos al dedo. */
private val WAVE_SHIFT = 34.dp
/** A cuántos elementos de distancia llega la ola. */
private const val WAVE_RADIUS_ITEMS = 4.5f
/** Cuánto crecen los elementos cercanos al dedo (0.35 = un 35 %). */
private const val WAVE_GROW = 0.35f
/** Distancia a la izquierda de la letra guía grande. */
private val GUIDE_SHIFT = 100.dp
private val GUIDE_SIZE = 84.dp

/** Letras sin ninguna app: se ven, pero más apagadas. */
private val SinApps = Color(0xFF5A5A5A)

/** Lo que hay en la columna, de arriba abajo. */
private sealed interface BarItem {
    data object Star : BarItem
    data class Letter(val value: String) : BarItem
    data object Notebook : BarItem
}

/**
 * Columna de la derecha: estrella, alfabeto completo y libreta, todo en una pieza.
 *
 * Al deslizar el dedo por cualquier punto de la columna:
 *  - los elementos cercanos se apartan hacia la izquierda y crecen (efecto ola),
 *  - aparece la letra (o el icono) actual en grande junto al dedo,
 *  - vibra un poco en cada cambio.
 * Pasar por la estrella vuelve a favoritas; soltar sobre la libreta abre las notas.
 * Al soltar, todo vuelve a su sitio con un muelle que rebota.
 */
@Composable
fun AlphabetBar(
    letters: List<String>,
    available: Set<String>,
    selected: String?,
    onLetter: (String) -> Unit,
    onFavorites: () -> Unit,
    onNotebook: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val itemPx = with(density) { ITEM_HEIGHT.toPx() }
    val shiftPx = with(density) { WAVE_SHIFT.toPx() }
    val guideShiftPx = with(density) { GUIDE_SHIFT.toPx() }
    val guideHalfPx = with(density) { (GUIDE_SIZE / 2).toPx() }
    val radiusPx = itemPx * WAVE_RADIUS_ITEMS

    val items: List<BarItem> = remember(letters) {
        listOf(BarItem.Star) + letters.map { BarItem.Letter(it) } + BarItem.Notebook
    }

    val scope = rememberCoroutineScope()
    // 0 = reposo, 1 = dedo encima. Con el muelle pasa un poco de 0 al soltar: eso es el rebote.
    val strength = remember { Animatable(0f) }
    var fingerY by remember { mutableFloatStateOf(0f) }
    var guide by remember { mutableStateOf<BarItem>(BarItem.Star) }

    val currentItems by rememberUpdatedState(items)
    val currentOnLetter by rememberUpdatedState(onLetter)
    val currentOnFavorites by rememberUpdatedState(onFavorites)
    val currentOnNotebook by rememberUpdatedState(onNotebook)

    /** Desplaza y agranda un elemento según lo cerca que esté su centro del dedo. */
    fun GraphicsLayerScope.applyWave(centerY: Float) {
        val s = strength.value
        val f = falloff(abs(centerY - fingerY), radiusPx)
        translationX = -shiftPx * s * f
        val grow = 1f + WAVE_GROW * s.coerceAtLeast(0f) * f
        scaleX = grow
        scaleY = grow
    }

    Box(modifier.width(44.dp)) {
        Column(
            modifier = Modifier
                .width(44.dp)
                .pointerInput(itemPx) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        var last: BarItem? = null

                        fun track(y: Float) {
                            val list = currentItems
                            fingerY = y.coerceIn(0f, list.size * itemPx)
                            val item = list[(y / itemPx).toInt().coerceIn(0, list.lastIndex)]
                            if (item != last) {
                                last = item
                                guide = item
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                when (item) {
                                    BarItem.Star -> currentOnFavorites()
                                    is BarItem.Letter -> currentOnLetter(item.value)
                                    BarItem.Notebook -> Unit // se abre al soltar encima
                                }
                            }
                        }

                        scope.launch { strength.animateTo(1f, tween(durationMillis = 140)) }
                        track(down.position.y)
                        down.consume()

                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) break
                            track(change.position.y)
                            change.consume()
                        }

                        if (last == BarItem.Notebook) currentOnNotebook()

                        scope.launch {
                            strength.animateTo(
                                targetValue = 0f,
                                animationSpec = spring(
                                    dampingRatio = 0.4f, // < 1 = rebota
                                    stiffness = Spring.StiffnessLow,
                                ),
                            )
                        }
                    }
                },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            items.forEachIndexed { i, item ->
                val centerY = (i + 0.5f) * itemPx
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(ITEM_HEIGHT)
                        // graphicsLayer: se recalcula en cada fotograma sin rehacer el diseño
                        .graphicsLayer { applyWave(centerY) }
                        .then(
                            when (item) {
                                BarItem.Star -> Modifier.semantics {
                                    contentDescription = "Favoritas"
                                    onClick { onFavorites(); true }
                                }
                                BarItem.Notebook -> Modifier.semantics {
                                    contentDescription = "Abrir notas"
                                    onClick { onNotebook(); true }
                                }
                                is BarItem.Letter -> Modifier
                            }
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    when (item) {
                        BarItem.Star -> StarIcon(
                            size = 12.dp,
                            color = if (selected == null) Texto else TextoSuave,
                        )
                        // 13dp: la libreta queda del mismo alto que las letras, con el mismo hueco que entre ellas
                        BarItem.Notebook -> NotebookIcon(size = 13.dp)
                        is BarItem.Letter -> Text(
                            text = item.value,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Normal,
                            color = when {
                                item.value !in available -> SinApps
                                selected == null || item.value == selected -> Texto
                                else -> TextoSuave
                            },
                        )
                    }
                }
            }
        }

        // Guía grande y fina junto al dedo (letra, estrella o libreta)
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .requiredSize(GUIDE_SIZE)
                .graphicsLayer {
                    alpha = strength.value.coerceIn(0f, 1f)
                    translationX = -guideShiftPx
                    translationY = fingerY - guideHalfPx
                },
            contentAlignment = Alignment.Center,
        ) {
            when (val g = guide) {
                BarItem.Star -> StarIcon(size = 44.dp)
                BarItem.Notebook -> NotebookIcon(size = 48.dp)
                is BarItem.Letter -> Text(
                    text = g.value,
                    fontSize = 64.sp,
                    fontWeight = FontWeight.Thin,
                    textAlign = TextAlign.Center,
                    color = Texto,
                )
            }
        }
    }
}

/** 1 justo bajo el dedo, baja suavemente (coseno²) hasta 0 en el borde de la ola. */
private fun falloff(distance: Float, radius: Float): Float {
    if (distance >= radius) return 0f
    val c = cos((PI / 2).toFloat() * distance / radius)
    return c * c
}

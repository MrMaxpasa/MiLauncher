package com.milauncher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.milauncher.AppNotification
import com.milauncher.data.AppInfo

private val ROW_HEIGHT = 52.dp

/**
 * Las apps favoritas de la pantalla de inicio.
 * En modo ordenar, cada fila muestra un asa (≡) a la derecha para arrastrarla.
 */
@Composable
fun FavoritesList(
    favorites: List<AppInfo>,
    reordering: Boolean,
    onToggleFavorite: (AppInfo) -> Unit,
    onStartReorder: () -> Unit,
    onMove: (key: String, delta: Int) -> Boolean,
    onDone: () -> Unit,
    notifications: Map<String, List<AppNotification>> = emptyMap(),
) {
    val rowPx = with(LocalDensity.current) { ROW_HEIGHT.toPx() }
    val haptic = LocalHapticFeedback.current
    var draggingKey by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val currentOnMove by rememberUpdatedState(onMove)

    Column {
        if (reordering) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 52.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Arrastra ≡ para ordenar",
                    modifier = Modifier.weight(1f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Light,
                    color = TextoSuave,
                )
                Text(
                    text = "Listo",
                    modifier = Modifier
                        .clickable(onClick = onDone)
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    fontSize = 14.sp,
                    letterSpacing = 1.sp,
                )
            }
        }

        favorites.forEach { app ->
            // key(): la fila "viaja" con la app al reordenar, así no se corta el arrastre
            key(app.key) {
                if (!reordering) {
                    AppRow(
                        app = app,
                        isFavorite = true,
                        onToggleFavorite = { onToggleFavorite(app) },
                        onReorder = onStartReorder,
                        notifications = notifications[app.packageName].orEmpty(),
                    )
                } else {
                    val isDragging = draggingKey == app.key
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(ROW_HEIGHT)
                            .zIndex(if (isDragging) 1f else 0f)
                            .graphicsLayer { translationY = if (isDragging) dragOffset else 0f }
                            .padding(start = 40.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isDragging) Panel else Color.Transparent)
                            .padding(start = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AppIconImage(app.icon)
                        Spacer(Modifier.width(26.dp))
                        Text(
                            text = app.label,
                            modifier = Modifier.weight(1f),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Light,
                            letterSpacing = 0.3.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .semantics { contentDescription = "Arrastrar ${app.label}" }
                                .pointerInput(app.key) {
                                    detectVerticalDragGestures(
                                        onDragStart = {
                                            draggingKey = app.key
                                            dragOffset = 0f
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        },
                                        onDragEnd = { draggingKey = null; dragOffset = 0f },
                                        onDragCancel = { draggingKey = null; dragOffset = 0f },
                                    ) { change, dy ->
                                        change.consume()
                                        dragOffset += dy
                                        // Al pasar media fila, intercambiamos con la vecina
                                        if (dragOffset > rowPx / 2 && currentOnMove(app.key, 1)) {
                                            dragOffset -= rowPx
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        } else if (dragOffset < -rowPx / 2 && currentOnMove(app.key, -1)) {
                                            dragOffset += rowPx
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            HandleIcon()
                        }
                    }
                }
            }
        }
    }
}

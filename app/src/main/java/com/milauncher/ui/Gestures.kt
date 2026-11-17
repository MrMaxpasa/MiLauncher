package com.milauncher.ui

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Baja la cortina de notificaciones del sistema.
 * Android no tiene una función pública para esto; los launchers usan esta llamada
 * (con el permiso EXPAND_STATUS_BAR). Si algún día deja de funcionar, no se cierra nada.
 */
@SuppressLint("WrongConstant", "PrivateApi")
fun expandNotifications(context: Context) {
    try {
        val service = context.getSystemService("statusbar") ?: return
        Class.forName("android.app.StatusBarManager")
            .getMethod("expandNotificationsPanel")
            .invoke(service)
    } catch (e: Exception) {
        // Sin soporte en este móvil
    }
}

/** Deslizar hacia abajo más de [thresholdPx] → bajar las notificaciones. */
fun Modifier.swipeDownForNotifications(context: Context, thresholdPx: Float): Modifier =
    pointerInput(thresholdPx) {
        var total = 0f
        detectVerticalDragGestures(
            onDragStart = { total = 0f },
            onDragEnd = { if (total > thresholdPx) expandNotifications(context) },
        ) { _, dy -> total += dy }
    }

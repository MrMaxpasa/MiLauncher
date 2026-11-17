package com.milauncher.data

import androidx.compose.ui.graphics.ImageBitmap

/** Icono ya dibujado. monochrome = true si viene de la capa monocromática de Android 13+. */
data class AppIcon(
    val bitmap: ImageBitmap,
    val monochrome: Boolean,
)

/** Una app instalada que se puede abrir. */
data class AppInfo(
    val label: String,
    val packageName: String,
    val activityName: String,
    /** Letra del alfabeto lateral: "A".."Z" o "#". */
    val letter: String,
    val icon: AppIcon,
) {
    /** Identificador único (paquete + actividad), lo usamos para guardar favoritas. */
    val key: String get() = "$packageName/$activityName"
}

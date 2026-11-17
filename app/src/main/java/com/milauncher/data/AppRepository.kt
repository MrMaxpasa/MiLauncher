package com.milauncher.data

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.Collator
import java.text.Normalizer

/** Lee las apps instaladas y prepara sus iconos. */
class AppRepository(private val context: Context) {

    suspend fun loadApps(): List<AppInfo> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        // Dibujamos los iconos al doble de 24dp para que se vean nítidos
        val iconPx = (24 * context.resources.displayMetrics.density * 2).toInt()
        val collator = Collator.getInstance().apply { strength = Collator.PRIMARY }

        @Suppress("DEPRECATION")
        pm.queryIntentActivities(intent, 0)
            .filter { it.activityInfo.packageName != context.packageName }
            .map { ri ->
                val label = ri.loadLabel(pm).toString().trim()
                AppInfo(
                    label = label,
                    packageName = ri.activityInfo.packageName,
                    activityName = ri.activityInfo.name,
                    letter = letterFor(label),
                    icon = renderIcon(ri.loadIcon(pm), iconPx),
                )
            }
            .sortedWith(compareBy<AppInfo> { it.letter != "#" }.thenBy(collator) { it.label })
    }

    companion object {
        /** "Ábaco" -> "A", "Ñu" -> "N", "7-Zip" -> "#" */
        fun letterFor(label: String): String {
            val first = label.firstOrNull() ?: return "#"
            val base = Normalizer.normalize(first.toString(), Normalizer.Form.NFD)
                .replace(Regex("\\p{Mn}+"), "")
                .uppercase()
            val c = base.firstOrNull() ?: return "#"
            return if (c in 'A'..'Z') c.toString() else "#"
        }

        /**
         * En Android 13+ muchas apps traen una versión monocromática del icono
         * (la de los "iconos temáticos"). Si existe la usamos: en blanco sobre negro
         * queda muy cerca del estilo de línea. Si no, usamos el icono normal en gris.
         */
        fun renderIcon(drawable: Drawable, sizePx: Int): AppIcon {
            if (Build.VERSION.SDK_INT >= 33 && drawable is AdaptiveIconDrawable) {
                val mono = drawable.monochrome
                if (mono != null) {
                    return AppIcon(drawToBitmap(mono, sizePx, cropAdaptive = true), monochrome = true)
                }
            }
            return AppIcon(drawToBitmap(drawable, sizePx, cropAdaptive = false), monochrome = false)
        }

        private fun drawToBitmap(d: Drawable, size: Int, cropAdaptive: Boolean) =
            Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).also { bmp ->
                val canvas = Canvas(bmp)
                if (cropAdaptive) {
                    // Las capas adaptativas miden 108dp pero solo se ven los 72dp centrales
                    val extra = size / 4
                    d.setBounds(-extra, -extra, size + extra, size + extra)
                } else {
                    d.setBounds(0, 0, size, size)
                }
                d.draw(canvas)
            }.asImageBitmap()
    }
}

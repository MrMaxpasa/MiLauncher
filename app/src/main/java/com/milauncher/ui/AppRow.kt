package com.milauncher.ui

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import com.milauncher.AppNotification
import com.milauncher.NotifListener
import com.milauncher.data.AppIcon
import com.milauncher.data.AppInfo

private val GrisFiltro = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })

@Composable
fun AppIconImage(icon: AppIcon, modifier: Modifier = Modifier) {
    Image(
        bitmap = icon.bitmap,
        contentDescription = null,
        modifier = modifier.size(24.dp),
        colorFilter = if (icon.monochrome) ColorFilter.tint(Texto) else GrisFiltro,
        alpha = if (icon.monochrome) 1f else 0.85f,
    )
}

/** Una fila de app: icono + nombre. Toque = abrir, pulsación larga = menú. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppRow(
    app: AppInfo,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
    /** Si no es null, el menú ofrece "Ordenar favoritas" (solo en la pantalla de inicio). */
    onReorder: (() -> Unit)? = null,
    /** Notificaciones de esta app (la más reciente primero). */
    notifications: List<AppNotification> = emptyList(),
) {
    val context = LocalContext.current
    var menu by remember { mutableStateOf(false) }
    val latest = notifications.firstOrNull()

    Box(modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .combinedClickable(
                    onClick = { launchApp(context, app) },
                    onLongClick = { menu = true },
                )
                .padding(start = 52.dp, end = 16.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppIconImage(app.icon)
            Spacer(Modifier.width(26.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = app.label,
                        modifier = Modifier.weight(1f, fill = false),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Light,
                        letterSpacing = 0.3.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (latest != null) {
                        Spacer(Modifier.width(8.dp))
                        // Puntito: hay notificaciones
                        Box(
                            Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(Texto)
                        )
                    }
                }
                if (latest != null) {
                    val more = if (notifications.size > 1) "  +${notifications.size - 1}" else ""
                    Text(
                        text = latest.line + more,
                        modifier = Modifier
                            .padding(top = 2.dp)
                            // Tocar la línea abre la notificación; si no se puede, la app
                            .clickable {
                                if (!NotifListener.open(context, latest)) launchApp(context, app)
                            },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Light,
                        color = TextoSuave,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            DropdownMenuItem(
                text = { Text(if (isFavorite) "Quitar de favoritas" else "Añadir a favoritas") },
                onClick = { menu = false; onToggleFavorite() },
            )
            if (onReorder != null) {
                DropdownMenuItem(
                    text = { Text("Ordenar favoritas") },
                    onClick = { menu = false; onReorder() },
                )
            }
            if (latest != null) {
                DropdownMenuItem(
                    text = { Text("Borrar notificaciones") },
                    onClick = { menu = false; NotifListener.dismissAll(app.packageName) },
                )
            }
            DropdownMenuItem(
                text = { Text("Información de la app") },
                onClick = {
                    menu = false
                    context.startSafely(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                            .setData(Uri.parse("package:${app.packageName}"))
                    )
                },
            )
            DropdownMenuItem(
                text = { Text("Desinstalar") },
                onClick = {
                    menu = false
                    context.startSafely(
                        Intent(Intent.ACTION_DELETE).setData(Uri.parse("package:${app.packageName}"))
                    )
                },
            )
        }
    }
}

fun launchApp(context: Context, app: AppInfo) {
    val intent = Intent(Intent.ACTION_MAIN)
        .addCategory(Intent.CATEGORY_LAUNCHER)
        .setComponent(ComponentName(app.packageName, app.activityName))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
    context.startSafely(intent)
}

/** Abre una pantalla sin que la app se cierre si no existe en este móvil. */
fun Context.startSafely(intent: Intent) {
    try {
        startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(this, "No se ha podido abrir", Toast.LENGTH_SHORT).show()
    } catch (e: SecurityException) {
        Toast.makeText(this, "No se ha podido abrir", Toast.LENGTH_SHORT).show()
    }
}

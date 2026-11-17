package com.milauncher.ui

import android.appwidget.AppWidgetProviderInfo
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.milauncher.CalendarState
import com.milauncher.DayCell
import com.milauncher.NotesWidgetHost
import com.milauncher.fitTo
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val FMT_MONTH = DateTimeFormatter.ofPattern("LLLL", Locale("es", "ES"))

/**
 * Panel del cuaderno: sube desde abajo al tocar la libreta.
 * Arriba la semana (de Google Calendar), debajo el widget de Keep.
 */
@Composable
fun NotesPanel(
    visible: Boolean,
    calendar: CalendarState,
    widgets: NotesWidgetHost,
    onClose: () -> Unit,
    onRequestCalendar: () -> Unit,
    onPickWidget: (AppWidgetProviderInfo) -> Unit,
) {
    BackHandler(enabled = visible, onBack = onClose)

    Box(Modifier.fillMaxSize()) {
        // Fondo oscurecido: tocar fuera cierra el panel
        AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onClose,
                    )
            )
        }

        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically { it },
            exit = slideOutVertically { it },
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.8f)
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .background(Panel)
                    // Que los toques dentro del panel no lleguen al fondo
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {},
                    )
                    .navigationBarsPadding()
                    .padding(horizontal = 18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Asa
                Box(
                    Modifier
                        .align(Alignment.CenterHorizontally)
                        .size(64.dp, 24.dp)
                        .clickable(onClick = onClose),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        Modifier
                            .size(36.dp, 4.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF555555))
                    )
                }

                Header(
                    showChange = widgets.widgetId != null,
                    onChange = { widgets.remove() },
                )

                if (calendar.hasPermission) {
                    WeekStrip(calendar.days)
                } else {
                    ConnectCalendar(onRequestCalendar)
                }

                Box(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(bottom = 12.dp)
                ) {
                    val id = widgets.widgetId
                    if (id != null) {
                        HostedWidget(widgets, id)
                    } else {
                        WidgetPicker(widgets, onPickWidget)
                    }
                }
            }
        }
    }
}

@Composable
private fun Header(showChange: Boolean, onChange: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Notas", fontSize = 26.sp, fontWeight = FontWeight.ExtraLight, letterSpacing = 1.sp)
        Spacer(Modifier.width(14.dp))
        Text(
            text = LocalDate.now().format(FMT_MONTH).uppercase(Locale("es", "ES")),
            fontSize = 11.sp,
            letterSpacing = 3.sp,
            color = TextoSuave,
        )
        Spacer(Modifier.weight(1f))
        if (showChange) {
            Text(
                text = "Cambiar",
                modifier = Modifier
                    .clickable(onClick = onChange)
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                fontSize = 12.sp,
                letterSpacing = 1.sp,
                color = TextoSuave,
            )
        }
    }
}

/** 7 días desde hoy: hoy con círculo fino, punto en los días con eventos. */
@Composable
private fun WeekStrip(days: List<DayCell>) {
    Row(Modifier.fillMaxWidth()) {
        days.forEach { d ->
            Column(
                Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(d.letter, fontSize = 11.sp, letterSpacing = 1.sp, color = TextoSuave)
                Box(
                    Modifier
                        .size(34.dp)
                        .then(if (d.isToday) Modifier.border(1.dp, Texto, CircleShape) else Modifier),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("${d.date.dayOfMonth}", fontSize = 17.sp, fontWeight = FontWeight.Light)
                }
                Box(
                    Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(if (d.hasEvent) Texto else Color.Transparent)
                )
            }
        }
    }
}

@Composable
private fun ConnectCalendar(onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, Linea, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text("Mostrar mi calendario", fontSize = 14.sp, fontWeight = FontWeight.Light, letterSpacing = 0.5.sp)
    }
}

/** El widget real de Keep, ocupando todo el hueco disponible. */
@Composable
private fun HostedWidget(widgets: NotesWidgetHost, id: Int) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val w = maxWidth.value
        val h = maxHeight.value
        key(id) {
            AndroidView(
                factory = { ctx -> widgets.createView(ctx, id) },
                update = { view -> view.fitTo(w, h) },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/** Primera vez: elegir qué widget de Keep mostrar. */
@Composable
private fun WidgetPicker(widgets: NotesWidgetHost, onPick: (AppWidgetProviderInfo) -> Unit) {
    val providers = remember { widgets.availableProviders() }
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Elige el widget de notas",
            modifier = Modifier.padding(horizontal = 4.dp),
            fontSize = 14.sp,
            fontWeight = FontWeight.Light,
            color = TextoSuave,
        )
        if (providers.isEmpty()) {
            Text(
                text = "No hay widgets disponibles. ¿Está instalado Google Keep?",
                modifier = Modifier.padding(horizontal = 4.dp),
                fontSize = 14.sp,
                fontWeight = FontWeight.Light,
            )
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(providers, key = { it.provider.flattenToString() }) { info ->
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .border(1.dp, Linea, RoundedCornerShape(20.dp))
                        .clickable { onPick(info) }
                        .padding(horizontal = 18.dp, vertical = 16.dp)
                ) {
                    Text(widgets.label(info), fontSize = 17.sp, fontWeight = FontWeight.Light)
                }
            }
        }
    }
}

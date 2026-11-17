package com.milauncher.ui

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import android.provider.AlarmClock
import android.provider.CalendarContract
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.Normalizer
import kotlin.math.roundToInt
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

private val ES = Locale("es", "ES")
private val FMT_DAY = DateTimeFormatter.ofPattern("EEE", ES)
private val FMT_MONTH = DateTimeFormatter.ofPattern("MMM", ES)
private val FMT_TIME = DateTimeFormatter.ofPattern("HH:mm")
private val DAY_SIZE = 96.sp
private val CLOCK_HEIGHT = 100.dp

/**
 * Dónde pasa la línea de datos, medido desde la base de las letras grandes,
 * en fracción del tamaño de letra. Las mayúsculas de Jost miden 0,70, así que
 * 0,35 = justo por la mitad de la palabra. Más alto = la línea sube.
 * (0,11 la pondría por debajo, dejando ver la barra de la A).
 */
private const val BAND_ABOVE_BASELINE = 0.35f

/**
 * El reloj: el día en letras enormes y finas ("SAB"), cruzado por una línea
 * con la hora, la fecha y el próximo evento (o la batería si no hay).
 *
 * Toques: hora → alarmas, fecha → calendario, evento → calendario / batería → uso de batería.
 */
@Composable
fun ClockHeader(nextEvent: String?, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var now by remember { mutableStateOf(LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES)) }
    var battery by remember { mutableIntStateOf(batteryLevel(context)) }

    // Comprobamos cada segundo, pero solo redibujamos cuando cambia el minuto
    LaunchedEffect(Unit) {
        while (true) {
            val t = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES)
            if (t != now) {
                now = t
                battery = batteryLevel(context)
            }
            delay(1_000)
        }
    }

    // Sin tildes: en letras tan grandes "SÁB" o "MIÉ" rompen el dibujo del reloj
    val day = now.format(FMT_DAY).uppercase(ES).trimEnd('.').withoutAccents()
    val date = "${now.dayOfMonth} ${now.format(FMT_MONTH).uppercase(ES).trimEnd('.').withoutAccents()}"
    val time = now.format(FMT_TIME)
    val third = nextEvent ?: "$battery%"

    val openCalendar = { context.startSafely(calendarIntent()) }
    val labels = listOf(
        time to { context.startSafely(Intent(AlarmClock.ACTION_SHOW_ALARMS)) },
        date to openCalendar,
        third to if (nextEvent != null) openCalendar else {
            { context.startSafely(Intent(Intent.ACTION_POWER_USAGE_SUMMARY)) }
        },
    )

    val density = LocalDensity.current
    val dayFontPx = with(density) { DAY_SIZE.toPx() }
    val boxHeightPx = with(density) { CLOCK_HEIGHT.toPx() }
    // Se calcula al medir el texto real: así la línea cae en el sitio exacto en cualquier pantalla
    var bandOffsetPx by remember { mutableIntStateOf(0) }

    Box(
        modifier.height(CLOCK_HEIGHT),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = day,
            fontSize = DAY_SIZE,
            fontWeight = FontWeight.Thin,
            letterSpacing = 6.sp,
            color = Texto,
            // Que mida su alto real (más que la caja) y quede centrado, sin recortarse
            modifier = Modifier.wrapContentHeight(unbounded = true),
            style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
            onTextLayout = { layout ->
                // El texto está centrado en la caja: su parte de arriba queda en (caja - texto) / 2
                val textTop = (boxHeightPx - layout.size.height) / 2f
                val bandCenter = textTop + layout.firstBaseline - BAND_ABOVE_BASELINE * dayFontPx
                bandOffsetPx = (bandCenter - boxHeightPx / 2f).roundToInt()
            },
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 34.dp)
                .offset { IntOffset(0, bandOffsetPx) },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            labels.forEach { (text, onClick) ->
                Text(
                    text = text,
                    modifier = Modifier
                        .clickable(onClick = onClick)
                        .minimumInteractiveComponentSize() // zona de toque cómoda (48dp)
                        .background(Fondo) // tapa las letras grandes: el efecto "cortado"
                        .padding(horizontal = 7.dp, vertical = 1.dp),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Normal,
                    letterSpacing = 3.sp,
                    color = Texto,
                    maxLines = 1,
                )
            }
        }
    }
}

/** Abre el calendario del móvil en el día de hoy. */
private fun calendarIntent(): Intent {
    val uri = CalendarContract.CONTENT_URI.buildUpon().appendPath("time").also {
        ContentUris.appendId(it, System.currentTimeMillis())
    }.build()
    return Intent(Intent.ACTION_VIEW).setData(uri)
}

private fun String.withoutAccents(): String =
    Normalizer.normalize(this, Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "")

private fun batteryLevel(context: Context): Int {
    val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
    return bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
}

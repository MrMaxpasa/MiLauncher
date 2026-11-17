package com.milauncher.data

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

data class CalendarEvent(
    val title: String,
    val begin: Long,
    val allDay: Boolean,
) {
    /** Los eventos de día completo se guardan a medianoche UTC; los demás, en hora local. */
    fun localDate(zone: ZoneId): LocalDate =
        Instant.ofEpochMilli(begin).atZone(if (allDay) ZoneOffset.UTC else zone).toLocalDate()
}

/** Lee (solo lectura) los eventos del calendario del móvil, p. ej. Google Calendar. */
class CalendarRepository(private val context: Context) {

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) ==
            PackageManager.PERMISSION_GRANTED

    suspend fun events(fromMillis: Long, toMillis: Long): List<CalendarEvent> =
        withContext(Dispatchers.IO) {
            if (!hasPermission()) return@withContext emptyList()

            val uri = CalendarContract.Instances.CONTENT_URI.buildUpon().also {
                ContentUris.appendId(it, fromMillis)
                ContentUris.appendId(it, toMillis)
            }.build()
            val projection = arrayOf(
                CalendarContract.Instances.TITLE,
                CalendarContract.Instances.BEGIN,
                CalendarContract.Instances.ALL_DAY,
            )
            val selection = "${CalendarContract.Instances.VISIBLE} = 1"
            val result = mutableListOf<CalendarEvent>()

            try {
                context.contentResolver.query(
                    uri, projection, selection, null, "${CalendarContract.Instances.BEGIN} ASC"
                )?.use { c ->
                    while (c.moveToNext()) {
                        result += CalendarEvent(
                            title = c.getString(0) ?: "",
                            begin = c.getLong(1),
                            allDay = c.getInt(2) == 1,
                        )
                    }
                }
            } catch (e: SecurityException) {
                // Permiso retirado mientras la app estaba abierta
            }
            result
        }
}

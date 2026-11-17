package com.milauncher

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.milauncher.data.AppInfo
import com.milauncher.data.AppRepository
import com.milauncher.data.CalendarEvent
import com.milauncher.data.CalendarRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Un día de la tira de 7 días del cuaderno. */
data class DayCell(
    val date: LocalDate,
    val letter: String,
    val isToday: Boolean,
    val hasEvent: Boolean,
)

data class CalendarState(
    val hasPermission: Boolean = false,
    val days: List<DayCell> = emptyList(),
    /** Texto para el reloj, p. ej. "LUN · PROGRA". null si no hay nada próximo. */
    val nextEvent: String? = null,
)

class LauncherViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = AppRepository(app)
    private val calendarRepo = CalendarRepository(app)
    private val prefs = app.getSharedPreferences("launcher", Context.MODE_PRIVATE)

    private val _apps = MutableStateFlow<List<AppInfo>>(emptyList())
    val apps: StateFlow<List<AppInfo>> = _apps.asStateFlow()

    // Favoritas: guardamos solo las claves, en orden, en SharedPreferences
    private val _favoriteKeys = MutableStateFlow(loadFavoriteKeys())
    val favoriteKeys: StateFlow<List<String>> = _favoriteKeys.asStateFlow()

    val favorites: StateFlow<List<AppInfo>> =
        combine(_apps, _favoriteKeys) { apps, keys ->
            keys.mapNotNull { k -> apps.find { it.key == k } }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** null = pantalla de inicio; una letra = lista completa de apps en esa letra. */
    private val _drawerLetter = MutableStateFlow<String?>(null)
    val drawerLetter: StateFlow<String?> = _drawerLetter.asStateFlow()

    /** Modo "ordenar favoritas" (aparece el asa para arrastrar). */
    private val _reordering = MutableStateFlow(false)
    val reordering: StateFlow<Boolean> = _reordering.asStateFlow()

    /** ¿Tiene Mi Launcher "Acceso a notificaciones"? (notificaciones + música) */
    private val _notificationAccess = MutableStateFlow(NotifListener.isEnabled(app))
    val notificationAccess: StateFlow<Boolean> = _notificationAccess.asStateFlow()

    private val _notificationPromptDismissed = MutableStateFlow(prefs.getBoolean(KEY_NOTIF_PROMPT, false))
    val notificationPromptDismissed: StateFlow<Boolean> = _notificationPromptDismissed.asStateFlow()

    private val _notesOpen = MutableStateFlow(false)
    val notesOpen: StateFlow<Boolean> = _notesOpen.asStateFlow()

    private val _calendar = MutableStateFlow(CalendarState(days = buildDays(emptySet())))
    val calendar: StateFlow<CalendarState> = _calendar.asStateFlow()

    fun refresh() {
        viewModelScope.launch { _apps.value = repo.loadApps() }
        _notificationAccess.value = NotifListener.isEnabled(getApplication<Application>())
        refreshCalendar()
    }

    fun refreshCalendar() {
        viewModelScope.launch {
            val zone = ZoneId.systemDefault()
            val today = LocalDate.now()
            val from = today.atStartOfDay(zone).toInstant().toEpochMilli()
            val to = today.plusDays(LOOKAHEAD_DAYS).atStartOfDay(zone).toInstant().toEpochMilli()

            val events = calendarRepo.events(from, to)
            val datesWithEvents = events.map { it.localDate(zone) }.toSet()
            val now = System.currentTimeMillis()
            val next = events.firstOrNull { e ->
                if (e.allDay) !e.localDate(zone).isBefore(today) else e.begin >= now
            }

            _calendar.value = CalendarState(
                hasPermission = calendarRepo.hasPermission(),
                days = buildDays(datesWithEvents),
                nextEvent = next?.let { nextEventLabel(it, today, zone) },
            )
        }
    }

    fun showLetter(letter: String) {
        _notesOpen.value = false
        _drawerLetter.value = letter
    }

    fun goHome() {
        _notesOpen.value = false
        _reordering.value = false
        _drawerLetter.value = null
    }

    fun startReorder() {
        _drawerLetter.value = null
        _reordering.value = true
    }

    fun stopReorder() { _reordering.value = false }

    /**
     * Mueve la favorita [key] una posición arriba (delta = -1) o abajo (delta = +1).
     * Devuelve true si se ha movido.
     */
    fun moveFavorite(key: String, delta: Int): Boolean {
        // Calculado al momento (no desde `favorites`) para que dos movimientos seguidos no usen datos viejos
        val installed = _apps.value.map { it.key }.toSet()
        val visible = _favoriteKeys.value.filter { it in installed }
        val from = visible.indexOf(key)
        val to = from + delta
        if (from < 0 || to !in visible.indices) return false
        val reordered = visible.toMutableList().apply { add(to, removeAt(from)) }
        // Las favoritas de apps desinstaladas se quedan al final por si vuelven
        val hidden = _favoriteKeys.value.filter { it !in reordered }
        saveFavorites(reordered + hidden)
        return true
    }

    fun openNotes() {
        _notesOpen.value = true
        refreshCalendar()
    }

    fun closeNotes() { _notesOpen.value = false }

    fun dismissNotificationPrompt() {
        _notificationPromptDismissed.value = true
        prefs.edit().putBoolean(KEY_NOTIF_PROMPT, true).apply()
    }

    fun toggleFavorite(app: AppInfo) {
        val current = _favoriteKeys.value
        saveFavorites(if (app.key in current) current - app.key else current + app.key)
    }

    private fun saveFavorites(keys: List<String>) {
        _favoriteKeys.value = keys
        prefs.edit().putString(KEY_FAVORITES, keys.joinToString(SEPARATOR)).apply()
    }

    private fun loadFavoriteKeys(): List<String> =
        prefs.getString(KEY_FAVORITES, null)
            ?.split(SEPARATOR)
            ?.filter { it.isNotBlank() }
            ?: emptyList()

    private companion object {
        const val KEY_FAVORITES = "favoritas"
        const val KEY_NOTIF_PROMPT = "aviso_notificaciones_oculto"
        const val SEPARATOR = "|"
        const val LOOKAHEAD_DAYS = 7L // el próximo evento del reloj: solo esta semana

        val ES: Locale = Locale("es", "ES")
        val FMT_DAY: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE", ES)

        /** Siete días empezando hoy, como en el boceto. */
        fun buildDays(withEvents: Set<LocalDate>): List<DayCell> {
            val today = LocalDate.now()
            return (0L until 7L).map { i ->
                val d = today.plusDays(i)
                DayCell(d, dayLetter(d.dayOfWeek), i == 0L, d in withEvents)
            }
        }

        fun dayLetter(day: DayOfWeek): String = when (day) {
            DayOfWeek.MONDAY -> "L"
            DayOfWeek.TUESDAY -> "M"
            DayOfWeek.WEDNESDAY -> "X"
            DayOfWeek.THURSDAY -> "J"
            DayOfWeek.FRIDAY -> "V"
            DayOfWeek.SATURDAY -> "S"
            DayOfWeek.SUNDAY -> "D"
        }

        /** "HOY · PROGRA", "MAÑ · MARCAS", "LUN · PROGRA"... */
        fun nextEventLabel(e: CalendarEvent, today: LocalDate, zone: ZoneId): String {
            val date = e.localDate(zone)
            val prefix = when (date) {
                today -> "HOY"
                today.plusDays(1) -> "MAÑ"
                else -> date.format(FMT_DAY).uppercase(ES).trimEnd('.')
            }
            val title = e.title.trim().uppercase(ES).take(8).trimEnd()
            return if (title.isEmpty()) prefix else "$prefix · $title"
        }
    }
}

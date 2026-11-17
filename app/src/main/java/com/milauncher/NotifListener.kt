package com.milauncher

import android.app.ActivityOptions
import android.app.Notification
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Build
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationManagerCompat
import androidx.core.os.BundleCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Una notificación resumida para mostrarla bajo el nombre de la app. */
data class AppNotification(
    val key: String,
    val title: String,
    val text: String,
    val time: Long,
    val contentIntent: PendingIntent?,
    val autoCancel: Boolean,
) {
    /** "Ana: hola, ¿qué tal?" */
    val line: String
        get() = when {
            title.isNotBlank() && text.isNotBlank() -> "$title: $text"
            else -> title.ifBlank { text }
        }
}

/** Lo que está sonando ahora mismo (Spotify, YouTube Music...). */
data class NowPlaying(
    val packageName: String,
    val title: String,
    val artist: String,
    val isPlaying: Boolean,
)

/**
 * Servicio que Android nos conecta cuando el usuario activa
 * "Acceso a notificaciones" para Mi Launcher. Con él:
 *  - leemos las notificaciones para mostrarlas junto a cada app (mejora 1),
 *  - leemos y controlamos la música que suena (mejora 5).
 */
class NotifListener : NotificationListenerService() {

    private var sessionManager: MediaSessionManager? = null
    private var controller: MediaController? = null

    /** Sesiones de música que nos da el sistema (vía 1). */
    private var systemSessions: List<MediaController> = emptyList()
    /** Sesiones sacadas de las notificaciones de música, p. ej. la de Spotify (vía 2). */
    private var notificationSessions: List<MediaController> = emptyList()

    private val component: ComponentName
        get() = ComponentName(this, NotifListener::class.java)

    private val sessionsListener =
        MediaSessionManager.OnActiveSessionsChangedListener { list ->
            systemSessions = list.orEmpty()
            pickController()
        }

    private val controllerCallback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) = publishMedia()
        override fun onMetadataChanged(metadata: MediaMetadata?) = publishMedia()
        override fun onSessionDestroyed() {
            systemSessions = activeSessions()
            controller?.unregisterCallback(this)
            controller = null
            pickController()
        }
    }

    override fun onListenerConnected() {
        instance = this
        _connected.value = true

        val sm = getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
        sessionManager = sm
        try {
            sm.addOnActiveSessionsChangedListener(sessionsListener, component)
        } catch (e: Exception) {
            // Algunos móviles no lo permiten: nos queda la vía 2 (notificaciones)
        }
        systemSessions = activeSessions()
        publishNotifications() // también busca música en las notificaciones
    }

    override fun onListenerDisconnected() {
        instance = null
        _connected.value = false
        try {
            sessionManager?.removeOnActiveSessionsChangedListener(sessionsListener)
        } catch (e: Exception) {
        }
        controller?.unregisterCallback(controllerCallback)
        controller = null
        systemSessions = emptyList()
        notificationSessions = emptyList()
        _notifications.value = emptyMap()
        _nowPlaying.value = null
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) = publishNotifications()

    override fun onNotificationRemoved(sbn: StatusBarNotification?) = publishNotifications()

    // ---------- Notificaciones ----------

    private fun publishNotifications() {
        val active: Array<StatusBarNotification> = try {
            activeNotifications ?: emptyArray()
        } catch (e: Exception) {
            emptyArray()
        }

        // Vía 2 para la música: las notificaciones de reproductor llevan su sesión dentro
        notificationSessions = active.mapNotNull { sbn ->
            mediaToken(sbn)?.let { token ->
                try {
                    MediaController(this, token)
                } catch (e: Exception) {
                    null
                }
            }
        }
        pickController()

        _notifications.value = active
            .filter { sbn ->
                sbn.packageName != packageName &&
                    !sbn.isOngoing && // nada de "subiendo archivo..."
                    mediaToken(sbn) == null && // la música ya sale en su propia fila
                    (sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY) == 0
            }
            .map { sbn ->
                val n = sbn.notification
                sbn.packageName to AppNotification(
                    key = sbn.key,
                    title = n.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty(),
                    text = n.extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty(),
                    time = sbn.postTime,
                    contentIntent = n.contentIntent,
                    autoCancel = (n.flags and Notification.FLAG_AUTO_CANCEL) != 0,
                )
            }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, list) -> list.sortedByDescending { it.time } }
    }

    private fun mediaToken(sbn: StatusBarNotification): MediaSession.Token? =
        BundleCompat.getParcelable(
            sbn.notification.extras,
            Notification.EXTRA_MEDIA_SESSION,
            MediaSession.Token::class.java,
        )

    // ---------- Música ----------

    private fun activeSessions(): List<MediaController> = try {
        sessionManager?.getActiveSessions(component).orEmpty()
    } catch (e: SecurityException) {
        emptyList()
    }

    /** Elige qué música mostrar: primero la que está sonando; si no, la primera que haya. */
    private fun pickController() {
        val all = (systemSessions + notificationSessions).distinctBy { it.sessionToken }
        val chosen = all.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
            ?: all.firstOrNull { it.sessionToken == controller?.sessionToken }
            ?: all.firstOrNull()
        if (chosen?.sessionToken != controller?.sessionToken) {
            controller?.unregisterCallback(controllerCallback)
            controller = chosen
            chosen?.registerCallback(controllerCallback)
        }
        publishMedia()
    }

    private fun publishMedia() {
        val c = controller
        val md = c?.metadata
        val title = md?.getString(MediaMetadata.METADATA_KEY_TITLE).orEmpty()
        _nowPlaying.value = if (c == null || title.isBlank()) {
            null
        } else {
            NowPlaying(
                packageName = c.packageName,
                title = title,
                artist = md?.getString(MediaMetadata.METADATA_KEY_ARTIST)
                    ?: md?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST).orEmpty(),
                isPlaying = (c.playbackState?.state ?: -1) in PLAYING_STATES,
            )
        }
    }

    companion object {
        private val PLAYING_STATES = setOf(
            PlaybackState.STATE_PLAYING,
            PlaybackState.STATE_BUFFERING,
            PlaybackState.STATE_FAST_FORWARDING,
            PlaybackState.STATE_REWINDING,
        )

        @Volatile
        private var instance: NotifListener? = null

        private val _notifications = MutableStateFlow<Map<String, List<AppNotification>>>(emptyMap())
        /** Notificaciones por paquete (la más reciente primero). */
        val notifications: StateFlow<Map<String, List<AppNotification>>> = _notifications.asStateFlow()

        private val _nowPlaying = MutableStateFlow<NowPlaying?>(null)
        val nowPlaying: StateFlow<NowPlaying?> = _nowPlaying.asStateFlow()

        private val _connected = MutableStateFlow(false)
        val connected: StateFlow<Boolean> = _connected.asStateFlow()

        /** ¿Ha dado el usuario "Acceso a notificaciones" a Mi Launcher? */
        fun isEnabled(context: Context): Boolean =
            NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)

        /** Pantalla de Ajustes donde se activa el permiso (directa al interruptor si se puede). */
        fun settingsIntent(context: Context): Intent =
            if (Build.VERSION.SDK_INT >= 30) {
                Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS).putExtra(
                    Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME,
                    ComponentName(context, NotifListener::class.java).flattenToString(),
                )
            } else {
                Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            }

        /** Abre una notificación como si la tocaras en la barra. Devuelve false si no se pudo. */
        fun open(context: Context, n: AppNotification): Boolean {
            val pi = n.contentIntent ?: return false
            return try {
                if (Build.VERSION.SDK_INT >= 34) {
                    val options = ActivityOptions.makeBasic()
                        .setPendingIntentBackgroundActivityStartMode(
                            ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
                        )
                    pi.send(context, 0, null, null, null, null, options.toBundle())
                } else {
                    pi.send()
                }
                if (n.autoCancel) instance?.cancelNotification(n.key)
                true
            } catch (e: PendingIntent.CanceledException) {
                false
            }
        }

        /** Borra todas las notificaciones de una app. */
        fun dismissAll(packageName: String) {
            val service = instance ?: return
            _notifications.value[packageName]?.forEach { service.cancelNotification(it.key) }
        }

        fun playPause() {
            val c = instance?.controller ?: return
            if ((c.playbackState?.state ?: -1) in PLAYING_STATES) {
                c.transportControls.pause()
            } else {
                c.transportControls.play()
            }
        }

        fun next() { instance?.controller?.transportControls?.skipToNext() }

        fun previous() { instance?.controller?.transportControls?.skipToPrevious() }
    }
}

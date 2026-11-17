package com.milauncher

import android.app.Activity
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.SizeF
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Se encarga del widget de notas (Keep) que vive dentro del panel del cuaderno.
 * Las notas no se guardan aquí: siguen en Keep, nosotros solo mostramos su widget.
 */
class NotesWidgetHost(private val activity: Activity) {

    private val host = AppWidgetHost(activity.applicationContext, HOST_ID)
    private val manager = AppWidgetManager.getInstance(activity)
    private val prefs = activity.getSharedPreferences("launcher", Context.MODE_PRIVATE)

    /** Id del widget colocado, o null si aún no se ha elegido ninguno. */
    var widgetId by mutableStateOf(loadSavedId())
        private set

    private var pendingId: Int? = null
    private var pendingInfo: AppWidgetProviderInfo? = null

    fun startListening() = host.startListening()
    fun stopListening() = host.stopListening()

    /** Widgets disponibles: los de Keep primero; si no hay Keep, todos. */
    fun availableProviders(): List<AppWidgetProviderInfo> {
        val all = manager.installedProviders
        val keep = all.filter { it.provider.packageName == KEEP_PACKAGE }
        return keep.ifEmpty { all }
    }

    fun label(info: AppWidgetProviderInfo): String =
        info.loadLabel(activity.packageManager) ?: info.provider.className

    /**
     * Paso 1 de colocar un widget: pedir un id y permiso para usarlo.
     * Si Android necesita confirmación, devuelve el Intent que hay que lanzar.
     */
    fun choose(info: AppWidgetProviderInfo): Intent? {
        cancelPending()
        val id = host.allocateAppWidgetId()
        pendingId = id
        pendingInfo = info
        return if (manager.bindAppWidgetIdIfAllowed(id, info.provider)) {
            configureOrFinish()
            null
        } else {
            Intent(AppWidgetManager.ACTION_APPWIDGET_BIND)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, info.provider)
        }
    }

    /** Resultado del diálogo "¿Permitir que Mi Launcher cree widgets?". */
    fun onBindResult(granted: Boolean) {
        if (granted) configureOrFinish() else cancelPending()
    }

    /** Resultado de la pantalla de configuración del propio widget (si la tiene). */
    fun onConfigureResult(resultCode: Int) {
        val id = pendingId ?: return
        if (resultCode == Activity.RESULT_OK) finish(id) else cancelPending()
    }

    fun remove() {
        widgetId?.let { host.deleteAppWidgetId(it) }
        widgetId = null
        prefs.edit().remove(KEY_WIDGET).apply()
    }

    fun createView(context: Context, id: Int): AppWidgetHostView =
        host.createView(context, id, manager.getAppWidgetInfo(id))

    private fun configureOrFinish() {
        val id = pendingId ?: return
        val info = pendingInfo ?: return
        val optional = Build.VERSION.SDK_INT >= 31 &&
            (info.widgetFeatures and AppWidgetProviderInfo.WIDGET_FEATURE_CONFIGURATION_OPTIONAL) != 0
        if (info.configure != null && !optional) {
            try {
                host.startAppWidgetConfigureActivityForResult(activity, id, 0, REQUEST_CONFIGURE, null)
            } catch (e: ActivityNotFoundException) {
                finish(id)
            } catch (e: SecurityException) {
                finish(id)
            }
        } else {
            finish(id)
        }
    }

    private fun finish(id: Int) {
        widgetId?.let { if (it != id) host.deleteAppWidgetId(it) }
        widgetId = id
        prefs.edit().putInt(KEY_WIDGET, id).apply()
        pendingId = null
        pendingInfo = null
    }

    private fun cancelPending() {
        pendingId?.let { host.deleteAppWidgetId(it) }
        pendingId = null
        pendingInfo = null
    }

    private fun loadSavedId(): Int? {
        val id = prefs.getInt(KEY_WIDGET, -1)
        return if (id != -1 && manager.getAppWidgetInfo(id) != null) id else null
    }

    companion object {
        const val REQUEST_CONFIGURE = 4201
        private const val HOST_ID = 1024
        private const val KEY_WIDGET = "widget_notas"
        private const val KEEP_PACKAGE = "com.google.android.keep"
    }
}

/** Le dice al widget cuánto espacio tiene, en dp, para que se dibuje a ese tamaño. */
fun AppWidgetHostView.fitTo(widthDp: Float, heightDp: Float) {
    if (Build.VERSION.SDK_INT >= 31) {
        updateAppWidgetSize(Bundle(), listOf(SizeF(widthDp, heightDp)))
    } else {
        @Suppress("DEPRECATION")
        updateAppWidgetSize(null, widthDp.toInt(), heightDp.toInt(), widthDp.toInt(), heightDp.toInt())
    }
}

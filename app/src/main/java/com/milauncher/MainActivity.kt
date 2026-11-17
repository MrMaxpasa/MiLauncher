package com.milauncher

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.milauncher.ui.HomeScreen
import com.milauncher.ui.LauncherTheme

class MainActivity : ComponentActivity() {

    private val vm: LauncherViewModel by viewModels()
    private lateinit var notesWidget: NotesWidgetHost

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        notesWidget = NotesWidgetHost(this)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        setContent {
            LauncherTheme {
                HomeScreen(vm, notesWidget)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        notesWidget.startListening()
    }

    override fun onStop() {
        super.onStop()
        notesWidget.stopListening()
    }

    override fun onResume() {
        super.onResume()
        // Recargamos al volver por si se ha instalado algo o ha cambiado el calendario
        vm.refresh()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Pulsar "inicio" estando ya en el launcher: volver a la pantalla principal
        vm.goHome()
    }

    @Deprecated("La configuración de widgets de Android aún usa este mecanismo")
    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == NotesWidgetHost.REQUEST_CONFIGURE) {
            notesWidget.onConfigureResult(resultCode)
        }
    }
}

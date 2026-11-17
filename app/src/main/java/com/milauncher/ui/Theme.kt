package com.milauncher.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.milauncher.R

// Paleta del boceto
val Fondo = Color(0xFF000000) // negro puro: en OLED (Nothing Phone 2) ahorra batería
val Panel = Color(0xFF1B1B1B)
val Texto = Color(0xFFF2F2F2)
val TextoSuave = Color(0xFFA8A8A8)
val Linea = Color(0xFF3A3A3A)

val Jost = FontFamily(
    Font(R.font.jost_thin, FontWeight.Thin),
    Font(R.font.jost_extralight, FontWeight.ExtraLight),
    Font(R.font.jost_light, FontWeight.Light),
    Font(R.font.jost_regular, FontWeight.Normal),
)

private val base = Typography()
private val LauncherTypography = Typography(
    bodyLarge = base.bodyLarge.copy(fontFamily = Jost),
    bodyMedium = base.bodyMedium.copy(fontFamily = Jost),
    bodySmall = base.bodySmall.copy(fontFamily = Jost),
    labelLarge = base.labelLarge.copy(fontFamily = Jost),
    titleMedium = base.titleMedium.copy(fontFamily = Jost),
)

private val LauncherColors = darkColorScheme(
    background = Fondo,
    surface = Panel,
    surfaceContainer = Panel,
    onSurface = Texto,
    onBackground = Texto,
    primary = Texto,
)

@Composable
fun LauncherTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = LauncherColors, typography = LauncherTypography) {
        ProvideTextStyle(TextStyle(fontFamily = Jost, color = Texto), content = content)
    }
}

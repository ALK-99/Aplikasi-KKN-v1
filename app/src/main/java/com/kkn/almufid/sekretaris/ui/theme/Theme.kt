package com.kkn.almufid.sekretaris.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val Skema = darkColorScheme(
    primary = VioletTerang,
    onPrimary = Color.White,
    secondary = Pink,
    background = Ground,
    onBackground = TeksUtama,
    surface = Surface1,
    onSurface = TeksUtama,
    surfaceVariant = Surface2,
    onSurfaceVariant = TeksRedup,
    outline = TeksRedup,
    error = Merah,
    secondaryContainer = Surface2,
    onSecondaryContainer = TeksUtama,
    surfaceContainer = Surface1,
    surfaceContainerHigh = Surface2,
    surfaceContainerHighest = Surface2
)

/**
 * Tema tunggal gelap (identitas visual tetap, tanpa dynamic color).
 * Catatan 120Hz: semua animasi di app ini memakai graphicsLayer / state
 * yang dibaca di fase draw, jadi tidak memicu recomposition tiap frame.
 * Itu yang menjaga gerakan tetap mulus di layar 120Hz.
 */
@Composable
fun SekretarisKKNTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Ground.toArgb()
            window.navigationBarColor = Ground.toArgb()
            val kontrol = WindowCompat.getInsetsController(window, view)
            kontrol.isAppearanceLightStatusBars = false
            kontrol.isAppearanceLightNavigationBars = false
        }
    }
    MaterialTheme(colorScheme = Skema, typography = Typography, content = content)
}

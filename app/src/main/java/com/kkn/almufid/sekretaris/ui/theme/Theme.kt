package com.kkn.almufid.sekretaris.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Tema terang dan gelap (identitas visual tetap, tanpa dynamic color).
 * Mode dipilih di Pengaturan > Tampilan dan disimpan di HP.
 * Catatan 120Hz: semua animasi di app ini memakai graphicsLayer / state
 * yang dibaca di fase draw, jadi tidak memicu recomposition tiap frame.
 */
/** Bentuk kotak hampir tajam, seperti kertas dan stabilo. */
val Bentuk = Shapes(
    extraSmall = RoundedCornerShape(2.dp), small = RoundedCornerShape(3.dp),
    medium = RoundedCornerShape(3.dp), large = RoundedCornerShape(4.dp), extraLarge = RoundedCornerShape(4.dp)
)

@Composable
fun SekretarisKKNTheme(content: @Composable () -> Unit) {
    Palet.sistemGelap = isSystemInDarkTheme()
    val gelap = Palet.gelap

    val skema = if (gelap) {
        darkColorScheme(
            primary = VioletTerang, onPrimary = TintaStabilo, secondary = Pink,
            background = Ground, onBackground = TeksUtama, surface = Surface1, onSurface = TeksUtama,
            surfaceVariant = Surface2, onSurfaceVariant = TeksRedup, outline = TeksRedup, error = Merah,
            secondaryContainer = Surface2, onSecondaryContainer = TeksUtama,
            surfaceContainer = Surface1, surfaceContainerHigh = Surface2, surfaceContainerHighest = Surface2
        )
    } else {
        lightColorScheme(
            primary = VioletTerang, onPrimary = Color.White, secondary = Pink,
            background = Ground, onBackground = TeksUtama, surface = Surface1, onSurface = TeksUtama,
            surfaceVariant = Surface2, onSurfaceVariant = TeksRedup, outline = TeksRedup, error = Merah,
            secondaryContainer = Surface2, onSecondaryContainer = TeksUtama,
            surfaceContainer = Surface1, surfaceContainerHigh = Surface2, surfaceContainerHighest = Surface2
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Ground.toArgb()
            window.navigationBarColor = Ground.toArgb()
            val kontrol = WindowCompat.getInsetsController(window, view)
            kontrol.isAppearanceLightStatusBars = !gelap
            kontrol.isAppearanceLightNavigationBars = !gelap
        }
    }
    MaterialTheme(colorScheme = skema, typography = Typography, shapes = Bentuk, content = content)
}

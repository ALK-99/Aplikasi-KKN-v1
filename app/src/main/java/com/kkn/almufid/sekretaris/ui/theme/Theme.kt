package com.kkn.almufid.sekretaris.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val SkemaTerang = lightColorScheme(
    primary = BiruUtama,
    onPrimary = Color.White,
    secondary = BiruGelap,
    background = AbuTerang,
    surface = Color.White,
    error = MerahError
)

private val SkemaGelap = darkColorScheme(
    primary = BiruMuda,
    secondary = BiruUtama,
    background = AbuGelap,
    error = MerahError
)

/**
 * Catatan soal kelancaran 120Hz:
 * Sejak Android 11, sistem otomatis menaikkan refresh rate layar ke
 * maksimum yang didukung device selama konten di layar benar-benar
 * butuh frame rate tinggi (animasi berjalan) dan tidak ada operasi berat
 * di main thread yang menyebabkan frame drop. Yang kita kendalikan dari
 * sisi kode: (1) semua query Room dijalankan di coroutine/IO dispatcher
 * (lihat Repository & ViewModel), bukan di main thread, dan (2) hindari
 * recomposition berlebihan dengan state yang di-scope seminimal mungkin
 * per Composable. Tidak ada manifest flag khusus yang wajib ditambahkan.
 */
@Composable
fun SekretarisKKNTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> SkemaGelap
        else -> SkemaTerang
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

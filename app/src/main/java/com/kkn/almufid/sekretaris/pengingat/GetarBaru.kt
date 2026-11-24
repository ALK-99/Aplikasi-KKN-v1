package com.kkn.almufid.sekretaris.pengingat

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import com.kkn.almufid.sekretaris.data.cloud.KoordinasiRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

private fun getar(c: Context) {
    try {
        val v: Vibrator = if (Build.VERSION.SDK_INT >= 31)
            (c.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        else @Suppress("DEPRECATION") (c.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator)
        val pola = longArrayOf(0, 200, 120, 200, 120, 350)
        if (Build.VERSION.SDK_INT >= 26) v.vibrate(VibrationEffect.createWaveform(pola, -1))
        else @Suppress("DEPRECATION") v.vibrate(pola, -1)
    } catch (_: Exception) { /* tidak semua HP punya getar */ }
}

/**
 * Selama aplikasi terbuka: HP bergetar saat ada pengumuman atau polling BARU masuk.
 * Pengumuman/polling yang sudah ada saat pertama dimuat tidak membuat getar.
 */
@Composable
fun PemantauGetar() {
    val c = LocalContext.current.applicationContext
    LaunchedEffect(Unit) {
        val repo = KoordinasiRepository()
        coroutineScope {
            async {
                var dikenal: Set<String>? = null
                repo.alirPengumuman().collect { l ->
                    val ids = l.map { it.id }.toSet()
                    if (dikenal != null && ids.any { it !in dikenal!! }) getar(c)
                    dikenal = ids
                }
            }
            async {
                var dikenal: Set<String>? = null
                repo.alirPolling().collect { l ->
                    val ids = l.map { it.id }.toSet()
                    if (dikenal != null && ids.any { it !in dikenal!! }) getar(c)
                    dikenal = ids
                }
            }
        }
    }
}

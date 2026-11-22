package com.kkn.almufid.sekretaris

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kkn.almufid.sekretaris.ui.GenericViewModelFactory
import com.kkn.almufid.sekretaris.ui.RootApp
import com.kkn.almufid.sekretaris.ui.theme.SekretarisKKNTheme
import com.kkn.almufid.sekretaris.update.AppState
import com.kkn.almufid.sekretaris.update.UpdateDialog
import com.kkn.almufid.sekretaris.update.UpdateViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mintaRefreshRateTertinggi()
        tandaiPengingatDibuka(intent)
        mintaIzinNotifikasi()
        setContent {
            SekretarisKKNTheme {
                val updateVm: UpdateViewModel = viewModel(
                    factory = GenericViewModelFactory { UpdateViewModel(applicationContext) }
                )
                LaunchedEffect(Unit) { updateVm.cekSekaliSaatBuka() }

                RootApp()
                UpdateDialog(updateVm)
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        tandaiPengingatDibuka(intent)
    }

    /** Membuka app lewat notifikasi pengingat berarti pengingat itu sudah dilihat, jadi tidak diulang lagi. */
    private fun tandaiPengingatDibuka(intent: android.content.Intent?) {
        val kunci = intent?.getStringExtra(com.kkn.almufid.sekretaris.pengingat.PengingatWorker.EXTRA_KUNCI) ?: return
        kunci.split("|").filter { it.isNotBlank() }.forEach {
            com.kkn.almufid.sekretaris.pengingat.PengingatPref.tandaiDibuka(this, it)
        }
        intent.removeExtra(com.kkn.almufid.sekretaris.pengingat.PengingatWorker.EXTRA_KUNCI)
    }

    private fun mintaIzinNotifikasi() {
        if (Build.VERSION.SDK_INT < 33) return
        try {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 7001)
            }
        } catch (e: Exception) { }
    }

    override fun onStart() {
        super.onStart()
        AppState.diDepan = true
    }

    override fun onStop() {
        AppState.diDepan = false
        super.onStop()
    }

    /**
     * Banyak HP (termasuk kelas menengah) menahan aplikasi di 60Hz kecuali
     * diminta. Di sini kita minta mode layar dengan refresh rate tertinggi
     * pada resolusi yang sama, mis. 120Hz. Jika HP tidak mendukung, tidak
     * terjadi apa-apa (dibungkus try/catch agar tidak pernah membuat crash).
     */
    @Suppress("DEPRECATION")
    private fun mintaRefreshRateTertinggi() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
        try {
            val layar = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) display else windowManager.defaultDisplay
            layar ?: return
            val sekarang = layar.mode
            val terbaik = layar.supportedModes
                .filter { it.physicalWidth == sekarang.physicalWidth && it.physicalHeight == sekarang.physicalHeight }
                .maxByOrNull { it.refreshRate } ?: return
            window.attributes = window.attributes.apply { preferredDisplayModeId = terbaik.modeId }
        } catch (e: Exception) {
            // abaikan: tetap jalan di refresh rate bawaan
        }
    }
}

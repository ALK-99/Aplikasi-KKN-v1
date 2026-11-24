package com.kkn.almufid.sekretaris

import android.app.Application
import com.kkn.almufid.sekretaris.data.AppDatabase
import com.kkn.almufid.sekretaris.update.UpdateWorker

/**
 * Sengaja TIDAK memakai Hilt/Dagger. Untuk aplikasi seukuran ini,
 * dependency injection manual (database dibuat sekali di sini, lalu
 * diteruskan ke ViewModel lewat Factory) jauh lebih sedikit titik
 * gagalnya saat first build dibanding menambah plugin KAPT/Hilt yang
 * punya banyak syarat kecocokan versi.
 */
class SekretarisApp : Application() {
    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }

    override fun onCreate() {
        super.onCreate()
        com.kkn.almufid.sekretaris.ui.theme.Palet.muat(this)
        // Cek update berkala di latar belakang (senyap, hanya saat ada internet).
        try { UpdateWorker.jadwalkan(this) } catch (e: Exception) { /* jangan crash karena ini */ }
        try {
            com.kkn.almufid.sekretaris.pengingat.PengingatWorker.buatSaluran(this)
            com.kkn.almufid.sekretaris.pengingat.PengingatWorker.jadwalkan(this)
            com.kkn.almufid.sekretaris.pengingat.PengingatSholat.jadwalkan(this)
        } catch (e: Exception) { /* pengingat tidak boleh membuat app crash */ }
    }
}

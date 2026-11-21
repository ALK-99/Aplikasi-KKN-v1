package com.kkn.almufid.sekretaris.update

import android.content.Context

/** Penanda kecil status update, dibagi antara Worker, Receiver, dan ViewModel. */
object PrefUpdate {
    private fun p(c: Context) = c.getSharedPreferences("update_prefs", Context.MODE_PRIVATE)

    /** true = pemasangan senyap tidak berhasil, perlu konfirmasi manual lewat pop-up. */
    fun menunggu(c: Context): Boolean = p(c).getBoolean("menunggu", false)
    fun setMenunggu(c: Context, nilai: Boolean) { p(c).edit().putBoolean("menunggu", nilai).apply() }

    fun versiDiunduh(c: Context): String? = p(c).getString("versi_diunduh", null)
    fun setVersiDiunduh(c: Context, versi: String?) { p(c).edit().putString("versi_diunduh", versi).apply() }
}

/** Dipakai Worker supaya tidak memasang update ketika app sedang dipakai di layar. */
object AppState {
    @Volatile var diDepan: Boolean = false
}

package com.kkn.almufid.sekretaris.ui.screens.ibadah

import android.content.Context

/** Kota sholat yang dipilih pengguna. Kunci baru ("kota_nama") karena daftar kota sudah dipangkas jadi tiga. */
object KotaPilihan {
    private fun p(c: Context) = c.getSharedPreferences("ibadah_prefs", Context.MODE_PRIVATE)

    fun indeks(c: Context): Int {
        val nama = p(c).getString("kota_nama", null)
        val i = DAFTAR_KOTA.indexOfFirst { it.nama == nama }
        return if (i >= 0) i else DAFTAR_KOTA.indexOfFirst { it.nama == "Yogyakarta" }.coerceAtLeast(0)
    }

    fun simpan(c: Context, indeks: Int) {
        p(c).edit().putString("kota_nama", DAFTAR_KOTA[indeks.coerceIn(0, DAFTAR_KOTA.size - 1)].nama).apply()
    }

    fun kota(c: Context): Kota = DAFTAR_KOTA[indeks(c)]
}

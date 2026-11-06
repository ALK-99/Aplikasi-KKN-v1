package com.kkn.almufid.sekretaris.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

private const val NAMA_FILE = "sesi_aman"
private const val KUNCI_INGAT_SAYA = "ingat_saya"
private const val KUNCI_NIM_DIINGAT = "nim_diingat"
private const val KUNCI_NIM_TERAKHIR = "nim_terakhir_gagal"
private const val KUNCI_JUMLAH_GAGAL = "jumlah_gagal_berurutan"

/** Batas percobaan login gagal sebelum pop-up "hubungi admin" muncul (poin B.2). */
const val BATAS_GAGAL_LOGIN = 3

/**
 * Penyimpanan lokal terenkripsi (bukan SharedPreferences biasa) karena
 * yang disimpan di sini berkaitan langsung dengan sesi akun: status
 * "ingat saya" dan riwayat percobaan login gagal. Tidak pernah menyimpan
 * sandi di sini — sesi login sesungguhnya tetap ditangani Firebase Auth
 * SDK sendiri secara terenkripsi di penyimpanan internalnya.
 */
class SesiLokal(context: Context) {
    private val prefs: SharedPreferences = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context, NAMA_FILE, masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    } catch (e: Exception) {
        // Fallback non-terenkripsi jika Keystore perangkat bermasalah
        // (sangat jarang) — lebih baik fitur tetap jalan daripada crash.
        context.getSharedPreferences(NAMA_FILE, Context.MODE_PRIVATE)
    }

    var ingatSaya: Boolean
        get() = prefs.getBoolean(KUNCI_INGAT_SAYA, false)
        set(value) = prefs.edit().putBoolean(KUNCI_INGAT_SAYA, value).apply()

    /** NIM yang diprefill di kolom login saat "Ingat saya" dicentang. Null = tidak ada. */
    var nimDiingat: String?
        get() = prefs.getString(KUNCI_NIM_DIINGAT, null)
        set(value) = prefs.edit().putString(KUNCI_NIM_DIINGAT, value).apply()

    fun catatGagalLogin(nim: String): Int {
        val nimSebelumnya = prefs.getString(KUNCI_NIM_TERAKHIR, null)
        val jumlahBaru = if (nimSebelumnya == nim) prefs.getInt(KUNCI_JUMLAH_GAGAL, 0) + 1 else 1
        prefs.edit()
            .putString(KUNCI_NIM_TERAKHIR, nim)
            .putInt(KUNCI_JUMLAH_GAGAL, jumlahBaru)
            .apply()
        return jumlahBaru
    }

    fun resetGagalLogin() {
        prefs.edit().remove(KUNCI_NIM_TERAKHIR).remove(KUNCI_JUMLAH_GAGAL).apply()
    }
}

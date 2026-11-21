package com.kkn.almufid.sekretaris.update

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import java.io.File

/**
 * Pemasangan lewat PackageInstaller dengan opsi "tanpa konfirmasi pengguna".
 * Di Android 12+ opsi ini dihormati jika app ini sudah tercatat sebagai
 * pemasang versi sebelumnya (setelah satu kali konfirmasi manual). Di
 * versi/HP lain sistem akan meminta konfirmasi; hasilnya ditangani
 * [HasilUpdateReceiver] dan pop-up update menjadi cadangan.
 */
object UpdateInstaller {
    const val AKSI = "com.kkn.almufid.sekretaris.UPDATE_HASIL"

    /** @return true jika sesi berhasil diserahkan ke sistem (hasil akhirnya datang ke receiver). */
    fun pasangSenyap(context: Context, apk: File): Boolean = try {
        val installer = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            params.setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
        }
        val id = installer.createSession(params)
        installer.openSession(id).use { sesi ->
            apk.inputStream().use { masuk ->
                sesi.openWrite("update.apk", 0, apk.length()).use { keluar ->
                    masuk.copyTo(keluar)
                    sesi.fsync(keluar)
                }
            }
            val intent = Intent(context, HasilUpdateReceiver::class.java).setAction(AKSI)
            val flag = PendingIntent.FLAG_UPDATE_CURRENT or
                (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0)
            val pi = PendingIntent.getBroadcast(context, id, intent, flag)
            sesi.commit(pi.intentSender)
        }
        true
    } catch (e: Exception) {
        false
    }
}

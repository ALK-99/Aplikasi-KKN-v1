package com.kkn.almufid.sekretaris.update

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller

/** Menerima hasil pemasangan dari sistem dan menandai apakah perlu konfirmasi manual. */
class HasilUpdateReceiver : BroadcastReceiver() {
    @Suppress("DEPRECATION")
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.getIntExtra(PackageInstaller.EXTRA_STATUS, -1)) {
            PackageInstaller.STATUS_SUCCESS -> PrefUpdate.setMenunggu(context, false)
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                // Sistem butuh konfirmasi. Coba tampilkan; kalau diblokir (app di latar),
                // pop-up update akan muncul saat app dibuka berikutnya.
                PrefUpdate.setMenunggu(context, true)
                val konfirmasi = intent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
                try {
                    konfirmasi?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    if (konfirmasi != null) context.startActivity(konfirmasi)
                } catch (e: Exception) {
                    // abaikan
                }
            }
            else -> PrefUpdate.setMenunggu(context, true)
        }
    }
}

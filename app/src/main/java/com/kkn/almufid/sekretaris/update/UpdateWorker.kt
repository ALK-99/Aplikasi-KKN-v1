package com.kkn.almufid.sekretaris.update

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Cek + unduh + pasang update di latar belakang saat ada internet,
 * tanpa pop-up. Pemasangan ditunda selama app sedang dipakai di layar.
 */
class UpdateWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val ctx = applicationContext
        val info = UpdateChecker.cek()
        if (info == null) {
            PrefUpdate.setMenunggu(ctx, false)
            return Result.success()
        }
        // Pemasangan senyap sudah dicoba dan butuh konfirmasi manual: jangan
        // unduh ulang tiap siklus, tunggu pengguna menanggapi pop-up.
        if (PrefUpdate.menunggu(ctx)) return Result.success()

        return try {
            val apk = File(ctx.cacheDir, "updates/update.apk")
            if (!(apk.exists() && PrefUpdate.versiDiunduh(ctx) == info.versiBaru)) {
                UpdateChecker.unduh(ctx, info.urlApk) { }
                PrefUpdate.setVersiDiunduh(ctx, info.versiBaru)
            }
            if (AppState.diDepan) return Result.retry()
            if (!UpdateInstaller.pasangSenyap(ctx, apk)) PrefUpdate.setMenunggu(ctx, true)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        private val butuhInternet = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        /** Cek berkala tiap 6 jam selama ada internet. Aman dipanggil berulang (KEEP). */
        fun jadwalkan(context: Context) {
            val permintaan = PeriodicWorkRequestBuilder<UpdateWorker>(6, TimeUnit.HOURS)
                .setConstraints(butuhInternet)
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork("cek-update-berkala", ExistingPeriodicWorkPolicy.KEEP, permintaan)
        }

        /** Dipanggil saat app dibuka dan ada versi baru: jalankan segera di latar belakang. */
        fun jalankanSekarang(context: Context) {
            val permintaan = OneTimeWorkRequestBuilder<UpdateWorker>()
                .setConstraints(butuhInternet)
                .build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork("cek-update-sekarang", ExistingWorkPolicy.KEEP, permintaan)
        }
    }
}

package com.kkn.almufid.sekretaris.pengingat

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import com.kkn.almufid.sekretaris.MainActivity
import com.kkn.almufid.sekretaris.R
import com.kkn.almufid.sekretaris.data.auth.AuthRepository
import com.kkn.almufid.sekretaris.data.cloud.DesaRepository
import com.kkn.almufid.sekretaris.data.cloud.KoordinasiRepository
import com.kkn.almufid.sekretaris.data.entity.StatusProker
import com.kkn.almufid.sekretaris.ui.screens.absensi.awalHari
import com.kkn.almufid.sekretaris.widget.WidgetKKN
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/** Satu hal yang perlu diperhatikan pengguna. [kunci] unik per kejadian (dan per hari untuk yang berulang harian). */
class ItemPengingat(val kunci: String, val judul: String, val keterangan: String)

/** Pengaturan dan catatan "sudah dibuka" untuk pengingat. */
object PengingatPref {
    private const val PREF = "pengingat"
    private const val HARI5 = 5L * 24 * 60 * 60 * 1000

    private fun p(c: Context) = c.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    fun aktif(c: Context): Boolean = try { p(c).getBoolean("aktif", true) } catch (e: Exception) { true }
    fun atur(c: Context, nilai: Boolean) { p(c).edit().putBoolean("aktif", nilai).apply() }

    /** Dicatat saat pengguna membuka app lewat notifikasi, supaya pengingat itu berhenti berulang. */
    fun tandaiDibuka(c: Context, kunci: String) {
        val sekarang = System.currentTimeMillis()
        val baru = (p(c).getStringSet("dibuka", emptySet()) ?: emptySet())
            .filter { (it.substringAfterLast('@').toLongOrNull() ?: 0L) > sekarang - HARI5 }
            .toMutableSet()
        baru += "$kunci@$sekarang"
        p(c).edit().putStringSet("dibuka", baru).apply()
    }

    fun sudahDibuka(c: Context, kunci: String): Boolean =
        (p(c).getStringSet("dibuka", emptySet()) ?: emptySet()).any { it.substringBeforeLast('@') == kunci }
}

/**
 * Memeriksa tugas, jadwal proker, absensi harian, dan pengumuman tiap 15 menit.
 * Selama ada hal yang belum dibuka pengguna, notifikasi diulang dengan getar dan lampu berkedip
 * (jika HP punya lampu notifikasi) sampai pengguna membukanya.
 */
class PengingatWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val ctx = applicationContext
        val uid = try { FirebaseAuth.getInstance().currentUser?.uid } catch (e: Exception) { null } ?: return Result.success()
        if (!PengingatPref.aktif(ctx)) {
            NotificationManagerCompat.from(ctx).cancel(ID_NOTIF)
            return Result.success()
        }
        val semua = try { kumpulkan(uid) } catch (e: Exception) { return Result.retry() }
        val belum = semua.filter { !PengingatPref.sudahDibuka(ctx, it.kunci) }
        ctx.getSharedPreferences("widget", Context.MODE_PRIVATE).edit().putInt("pengingat", belum.size).apply()
        if (belum.isEmpty()) NotificationManagerCompat.from(ctx).cancel(ID_NOTIF) else tampilkan(ctx, belum)
        try { WidgetKKN.perbarui(ctx) } catch (e: Exception) { }
        return Result.success()
    }

    private suspend fun <T> ambil(aliran: Flow<List<T>>): List<T> =
        withTimeoutOrNull(12_000) { aliran.first() } ?: emptyList()

    private suspend fun kumpulkan(uid: String): List<ItemPengingat> {
        val ctx = applicationContext
        val desa = DesaRepository(AuthRepository())
        val koordinasi = KoordinasiRepository()
        val sekarang = System.currentTimeMillis()
        val hariIni = awalHari(sekarang)
        val besok = hariIni + 2L * 24 * 60 * 60 * 1000
        val kodeHari = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date(sekarang))
        val jamSekarang = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val hasil = mutableListOf<ItemPengingat>()

        // Tugas untuk saya yang belum selesai dan tenggatnya dekat atau lewat.
        ambil(koordinasi.alirTugas())
            .filter { it.untukUid == uid && !it.selesai && (it.deadlineMillis == 0L || it.deadlineMillis <= sekarang + 24L * 3600 * 1000) }
            .forEach {
                val lewat = it.deadlineMillis in 1 until sekarang
                hasil += ItemPengingat("tugas:${it.id}", "Tugas: ${it.judul}", if (lewat) "Sudah lewat tenggat" else "Tenggat kurang dari 24 jam")
            }

        // Proker yang akan mulai atau sudah lewat jadwal tapi belum selesai.
        val proker = ambil(desa.alirProker())
        proker.filter { it.status != StatusProker.SELESAI && it.status != StatusProker.BATAL }.forEach {
            when {
                it.mulaiMillis in hariIni until besok ->
                    hasil += ItemPengingat("proker-mulai:${it.id}:$kodeHari", "Proker dimulai: ${it.namaProker}", "Jadwal mulai hari ini atau besok")
                it.selesaiMillis in 1 until hariIni ->
                    hasil += ItemPengingat("proker-lewat:${it.id}", "Proker lewat jadwal: ${it.namaProker}", "Belum ditandai selesai")
            }
        }
        val aktifProker = proker.filter { it.status != StatusProker.BATAL }
        ctx.getSharedPreferences("widget", Context.MODE_PRIVATE).edit()
            .putInt("proker_total", aktifProker.size)
            .putInt("proker_selesai", aktifProker.count { it.status == StatusProker.SELESAI }).apply()

        // Absensi hari ini belum dicatat (setelah pukul 16.00).
        if (jamSekarang >= 16) {
            val adaAnggota = ambil(desa.alirAnggota()).any { it.aktif }
            if (adaAnggota && ambil(desa.alirAbsensi(hariIni)).isEmpty()) {
                hasil += ItemPengingat("absen:$kodeHari", "Absensi hari ini belum dicatat", "Catat kehadiran sebelum hari berganti")
            }
        }

        // Pengumuman yang belum dibaca dalam 3 hari terakhir.
        ambil(koordinasi.alirPengumuman())
            .filter { uid !in it.dibacaOleh && it.tanggalMillis >= sekarang - 3L * 24 * 3600 * 1000 }
            .forEach { hasil += ItemPengingat("pengumuman:${it.id}", "Pengumuman: ${it.judul.ifBlank { "(tanpa judul)" }}", "Belum kamu baca") }

        return hasil
    }

    private fun tampilkan(ctx: Context, belum: List<ItemPengingat>) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        buatSaluran(ctx)
        val buka = PendingIntent.getActivity(
            ctx, 0,
            Intent(ctx, MainActivity::class.java)
                .putExtra(EXTRA_KUNCI, belum.joinToString("|") { it.kunci })
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val gaya = NotificationCompat.InboxStyle().setBigContentTitle("${belum.size} pengingat belum dibuka")
        belum.take(6).forEach { gaya.addLine("${it.judul} · ${it.keterangan}") }
        val notif = NotificationCompat.Builder(ctx, SALURAN)
            .setSmallIcon(R.drawable.ic_notif)
            .setContentTitle(if (belum.size == 1) belum[0].judul else "${belum.size} pengingat belum dibuka")
            .setContentText(if (belum.size == 1) belum[0].keterangan else belum.first().judul)
            .setStyle(gaya)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setColor(Color.parseColor("#5B4BDB"))
            .setLights(Color.parseColor("#D92B6B"), 400, 600)
            .setVibrate(longArrayOf(0, 250, 150, 250))
            .setOnlyAlertOnce(false)
            .setAutoCancel(true)
            .setContentIntent(buka)
            .build()
        try { NotificationManagerCompat.from(ctx).notify(ID_NOTIF, notif) } catch (e: SecurityException) { }
    }

    companion object {
        const val SALURAN = "pengingat"
        const val ID_NOTIF = 4101
        const val EXTRA_KUNCI = "pengingat_kunci"

        fun buatSaluran(ctx: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (nm.getNotificationChannel(SALURAN) != null) return
            nm.createNotificationChannel(
                NotificationChannel(SALURAN, "Pengingat KKN", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Tugas, jadwal proker, absensi, dan pengumuman yang belum dibuka"
                    enableLights(true)
                    lightColor = Color.parseColor("#D92B6B")
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 250, 150, 250)
                }
            )
        }

        /** Dijalankan tiap 15 menit (batas terkecil Android). Aman dipanggil berulang. */
        fun jadwalkan(ctx: Context) {
            val permintaan = PeriodicWorkRequestBuilder<PengingatWorker>(15, TimeUnit.MINUTES).build()
            WorkManager.getInstance(ctx)
                .enqueueUniquePeriodicWork("pengingat-berkala", ExistingPeriodicWorkPolicy.KEEP, permintaan)
        }
    }
}

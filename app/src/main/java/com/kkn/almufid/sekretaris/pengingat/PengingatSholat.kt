package com.kkn.almufid.sekretaris.pengingat

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.kkn.almufid.sekretaris.MainActivity
import com.kkn.almufid.sekretaris.R
import com.kkn.almufid.sekretaris.ui.screens.ibadah.KotaPilihan
import com.kkn.almufid.sekretaris.ui.screens.ibadah.WaktuSholat
import java.util.Calendar
import java.util.TimeZone

private val PESAN_ISLAMI = listOf(
    "Sesungguhnya sholat itu adalah kewajiban yang ditentukan waktunya atas orang-orang beriman. (QS. An-Nisa: 103)",
    "Perkara pertama yang dihisab dari seorang hamba pada hari kiamat adalah sholatnya. (HR. At-Tirmidzi)",
    "Jagalah sholat-sholatmu dan sholat wustha, dan berdirilah karena Allah dengan khusyuk. (QS. Al-Baqarah: 238)",
    "Sholat adalah tiang agama. Siapa menegakkannya berarti menegakkan agama. (HR. Al-Baihaqi)",
    "Yang membedakan antara seseorang dengan kekufuran adalah meninggalkan sholat. (HR. Muslim)",
    "Maka dirikanlah sholat untuk mengingat-Ku. (QS. Thaha: 14)",
    "Sholat berjamaah lebih utama 27 derajat daripada sholat sendirian. (HR. Bukhari dan Muslim)",
    "Rehatkan kami dengan sholat, wahai Bilal. (HR. Abu Dawud)",
)

/** Pengingat sholat: notifikasi 10 menit sebelum adzan memakai jadwal offline kota pilihan. Jalan tanpa internet. */
object PengingatSholat {
    const val SALURAN = "sholat"
    const val ID_NOTIF = 4202
    const val MENIT_SEBELUM = 10
    private const val PREF = "pengingat_sholat"

    fun aktif(c: Context): Boolean = c.getSharedPreferences(PREF, Context.MODE_PRIVATE).getBoolean("aktif", true)
    fun atur(c: Context, nilai: Boolean) {
        c.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putBoolean("aktif", nilai).apply()
        jadwalkan(c)
    }

    fun buatSaluran(ctx: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(SALURAN) != null) return
        nm.createNotificationChannel(
            NotificationChannel(SALURAN, "Pengingat sholat", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Pengingat 10 menit sebelum adzan"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 300, 200, 300)
            }
        )
    }

    /** Waktu (millis) dan nama sholat berikutnya yang pengingatnya belum lewat. */
    private fun berikutnya(ctx: Context): Pair<Long, String>? {
        val wib = TimeZone.getTimeZone("Asia/Jakarta")
        val kota = KotaPilihan.kota(ctx)
        val sekarang = System.currentTimeMillis()
        for (tambah in 0..1) {
            val kal = Calendar.getInstance(wib).apply { timeInMillis = sekarang; add(Calendar.DAY_OF_MONTH, tambah) }
            val j = WaktuSholat.hitung(kota.lintang, kota.bujur, 7.0, kal.get(Calendar.YEAR), kal.get(Calendar.MONTH) + 1, kal.get(Calendar.DAY_OF_MONTH))
            val tengah = Calendar.getInstance(wib).apply {
                timeInMillis = kal.timeInMillis
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            listOf("Subuh" to j.subuh, "Dzuhur" to j.dzuhur, "Ashar" to j.ashar, "Maghrib" to j.maghrib, "Isya" to j.isya)
                .forEach { (nama, jam) ->
                    val adzan = tengah + (jam * 3_600_000).toLong()
                    val ingat = adzan - MENIT_SEBELUM * 60_000L
                    if (ingat > sekarang + 5_000) return ingat to "$nama|${WaktuSholat.format(jam)}"
                }
        }
        return null
    }

    private fun intentAlarm(ctx: Context, info: String): PendingIntent =
        PendingIntent.getBroadcast(
            ctx, 7001, Intent(ctx, SholatReceiver::class.java).putExtra("info", info),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    /** Aman dipanggil berulang: selalu menimpa alarm sebelumnya dengan yang berikutnya. */
    fun jadwalkan(ctx: Context) {
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (!aktif(ctx)) { am.cancel(intentAlarm(ctx, "")); return }
        val (waktu, info) = berikutnya(ctx) ?: return
        try {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, waktu, intentAlarm(ctx, info))
        } catch (e: Exception) { /* jangan crash */ }
    }

    fun tampilkan(ctx: Context, info: String) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        buatSaluran(ctx)
        val (nama, jam) = info.split("|").let { (it.getOrNull(0) ?: "Sholat") to (it.getOrNull(1) ?: "") }
        val pesan = PESAN_ISLAMI.random()
        val buka = PendingIntent.getActivity(
            ctx, 0, Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notif = NotificationCompat.Builder(ctx, SALURAN)
            .setSmallIcon(R.drawable.ic_notif)
            .setContentTitle("$MENIT_SEBELUM menit lagi adzan $nama ($jam)")
            .setContentText("Bersiap wudhu dan menunaikan sholat tepat waktu.")
            .setStyle(NotificationCompat.BigTextStyle().bigText("Bersiap wudhu dan menunaikan sholat tepat waktu.\n\n$pesan"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setColor(Color.parseColor("#1B7F5C"))
            .setVibrate(longArrayOf(0, 300, 200, 300))
            .setAutoCancel(true)
            .setContentIntent(buka)
            .build()
        try { NotificationManagerCompat.from(ctx).notify(ID_NOTIF, notif) } catch (e: SecurityException) { }
    }
}

class SholatReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        try {
            if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
                PengingatSholat.jadwalkan(context)
            } else if (PengingatSholat.aktif(context)) {
                PengingatSholat.tampilkan(context, intent.getStringExtra("info") ?: "")
                PengingatSholat.jadwalkan(context)
            }
        } finally { pending.finish() }
    }
}

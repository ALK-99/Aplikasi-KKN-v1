package com.kkn.almufid.sekretaris.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.kkn.almufid.sekretaris.MainActivity
import com.kkn.almufid.sekretaris.R
import com.kkn.almufid.sekretaris.ui.screens.ibadah.DAFTAR_KOTA
import com.kkn.almufid.sekretaris.ui.screens.ibadah.WaktuSholat
import java.util.Calendar
import java.util.TimeZone

/**
 * Widget layar utama: sholat berikutnya (Yogyakarta, dihitung di HP), progres proker,
 * dan jumlah pengingat belum dibuka. Angka proker dan pengingat diperbarui oleh PengingatWorker
 * tiap 15 menit, sehingga widget tidak perlu internet sendiri.
 */
class WidgetKKN : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        perbarui(context)
    }

    companion object {
        fun perbarui(context: Context) {
            val manajer = AppWidgetManager.getInstance(context)
            val ids = manajer.getAppWidgetIds(ComponentName(context, WidgetKKN::class.java))
            if (ids.isEmpty()) return
            val tampilan = RemoteViews(context.packageName, R.layout.widget_kkn)

            val kota = com.kkn.almufid.sekretaris.ui.screens.ibadah.KotaPilihan.kota(context)
            val kal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Jakarta"))
            val jadwal = WaktuSholat.hitung(
                kota.lintang, kota.bujur, 7.0,
                kal.get(Calendar.YEAR), kal.get(Calendar.MONTH) + 1, kal.get(Calendar.DAY_OF_MONTH)
            )
            val waktu = listOf(
                "Subuh" to jadwal.subuh, "Dzuhur" to jadwal.dzuhur, "Ashar" to jadwal.ashar,
                "Maghrib" to jadwal.maghrib, "Isya" to jadwal.isya,
            )
            fun menit(jam: Double) = Math.round(jam * 60.0).toInt()
            val sekarang = kal.get(Calendar.HOUR_OF_DAY) * 60 + kal.get(Calendar.MINUTE)
            val berikut = waktu.firstOrNull { menit(it.second) > sekarang }
            val (nama, jam) = berikut ?: waktu.first()
            val sisa = if (berikut != null) menit(jam) - sekarang else menit(jam) + 1440 - sekarang
            tampilan.setTextViewText(R.id.widget_sholat, "$nama ${WaktuSholat.format(jam)}")
            tampilan.setTextViewText(
                R.id.widget_sisa,
                "Yogyakarta · " + if (sisa >= 60) "${sisa / 60} j ${sisa % 60} m lagi" else "$sisa menit lagi"
            )

            val pref = context.getSharedPreferences("widget", Context.MODE_PRIVATE)
            val total = pref.getInt("proker_total", 0)
            val selesai = pref.getInt("proker_selesai", 0)
            tampilan.setTextViewText(
                R.id.widget_proker,
                if (total == 0) "Proker: belum ada data" else "Proker: ${selesai * 100 / total}% ($selesai/$total selesai)"
            )
            val pengingat = pref.getInt("pengingat", 0)
            tampilan.setTextViewText(
                R.id.widget_pengingat,
                if (pengingat > 0) "$pengingat pengingat belum dibuka" else "Tidak ada pengingat"
            )

            tampilan.setOnClickPendingIntent(
                R.id.widget_akar,
                PendingIntent.getActivity(
                    context, 1, Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
            ids.forEach { manajer.updateAppWidget(it, tampilan) }
        }
    }
}

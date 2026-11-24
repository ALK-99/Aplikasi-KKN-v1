package com.kkn.almufid.sekretaris.ui.screens.ibadah

import kotlin.math.*

data class Kota(val nama: String, val lintang: Double, val bujur: Double)

val DAFTAR_KOTA = listOf(
    Kota("Jakarta", -6.2088, 106.8456),
    Kota("Yogyakarta", -7.7956, 110.3695),
    Kota("Surabaya", -7.2575, 112.7521),
)

/** Jam dalam desimal (mis. 4.5 = 04:30), sudah dalam zona waktu yang diminta. */
data class JadwalSholat(
    val subuh: Double, val terbit: Double, val dzuhur: Double,
    val ashar: Double, val maghrib: Double, val isya: Double,
)

/**
 * Perhitungan waktu sholat offline (tanpa internet/API) memakai parameter
 * Kemenag RI: Subuh 20°, Isya 18°, Ashar mazhab Syafi'i (bayangan = 1x tinggi),
 * ihtiyath +2 menit. Hasilnya perkiraan; selalu cocokkan dengan jadwal resmi setempat.
 */
object WaktuSholat {
    private fun rad(d: Double) = Math.toRadians(d)
    private fun deg(r: Double) = Math.toDegrees(r)
    private fun fix(a: Double, b: Double): Double { val r = a - b * floor(a / b); return r }

    private fun julianDay(tahun: Int, bulan: Int, hari: Int): Double {
        var y = tahun; var m = bulan
        if (m <= 2) { y -= 1; m += 12 }
        val a = floor(y / 100.0)
        val b = 2 - a + floor(a / 4.0)
        return floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + hari + b - 1524.5
    }

    fun hitung(lintang: Double, bujur: Double, zonaJam: Double, tahun: Int, bulan: Int, hari: Int): JadwalSholat {
        val d = julianDay(tahun, bulan, hari) + 0.5 - 2451545.0 // tengah hari
        val g = fix(357.529 + 0.98560028 * d, 360.0)
        val q = fix(280.459 + 0.98564736 * d, 360.0)
        val l = fix(q + 1.915 * sin(rad(g)) + 0.020 * sin(rad(2 * g)), 360.0)
        val e = 23.439 - 0.00000036 * d
        val ra = fix(deg(atan2(cos(rad(e)) * sin(rad(l)), cos(rad(l)))) / 15.0, 24.0)
        val eqt = q / 15.0 - ra
        val dekl = deg(asin(sin(rad(e)) * sin(rad(l))))

        val dzuhur = 12.0 + zonaJam - bujur / 15.0 - eqt

        fun selisih(sudut: Double): Double {
            val v = (-sin(rad(sudut)) - sin(rad(dekl)) * sin(rad(lintang))) / (cos(rad(dekl)) * cos(rad(lintang)))
            return deg(acos(v.coerceIn(-1.0, 1.0))) / 15.0
        }
        fun ashar(faktor: Double): Double {
            val sudut = -deg(atan(1.0 / (faktor + tan(rad(abs(lintang - dekl))))))
            return selisih(sudut)
        }

        val ihtiyath = 2.0 / 60.0
        return JadwalSholat(
            subuh = dzuhur - selisih(20.0) + ihtiyath,
            terbit = dzuhur - selisih(0.833) - ihtiyath,
            dzuhur = dzuhur + ihtiyath,
            ashar = dzuhur + ashar(1.0) + ihtiyath,
            maghrib = dzuhur + selisih(0.833) + ihtiyath,
            isya = dzuhur + selisih(18.0) + ihtiyath,
        )
    }

    /** Arah kiblat dalam derajat searah jarum jam dari utara sejati. */
    fun arahKiblat(lintang: Double, bujur: Double): Double {
        val lk = rad(21.4225); val bk = rad(39.8262)
        val l1 = rad(lintang); val dB = bk - rad(bujur)
        val y = sin(dB)
        val x = cos(l1) * tan(lk) - sin(l1) * cos(dB)
        return fix(deg(atan2(y, x)), 360.0)
    }

    fun format(jam: Double): String {
        val total = Math.round(fix(jam, 24.0) * 60.0).toInt() % 1440
        return "%02d:%02d".format(total / 60, total % 60)
    }

    fun namaArah(derajat: Double): String {
        val arah = listOf("Utara", "Timur Laut", "Timur", "Tenggara", "Selatan", "Barat Daya", "Barat", "Barat Laut")
        return arah[((derajat + 22.5) / 45.0).toInt() % 8]
    }
}

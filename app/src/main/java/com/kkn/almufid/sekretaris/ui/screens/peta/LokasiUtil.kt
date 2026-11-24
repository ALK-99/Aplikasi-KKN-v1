package com.kkn.almufid.sekretaris.ui.screens.peta

import android.annotation.SuppressLint
import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import kotlin.math.*

/** Satu tempat hasil pencarian terdekat, sudah dihitung jaraknya dari titik pusat. */
data class Tempat(val nama: String, val lat: Double, val lng: Double, val jarak: Double, val arah: String)

/** Titik terkunci (posko). Disimpan dengan nama; koordinat kosong bila belum bisa dicari. */
data class TitikPosko(val nama: String, val lat: Double?, val lng: Double?)

object LokasiUtil {
    private const val PREF = "peta_prefs"

    fun bacaPosko(c: Context): TitikPosko {
        val p = c.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        val lat = p.getString("posko_lat", null)?.toDoubleOrNull()
        val lng = p.getString("posko_lng", null)?.toDoubleOrNull()
        return TitikPosko(p.getString("posko", "") ?: "", lat, lng)
    }

    fun simpanPosko(c: Context, t: TitikPosko) {
        c.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit()
            .putString("posko", t.nama.trim())
            .putString("posko_lat", t.lat?.toString())
            .putString("posko_lng", t.lng?.toString())
            .apply()
    }

    fun bacaRiwayat(c: Context): List<String> =
        (c.getSharedPreferences(PREF, Context.MODE_PRIVATE).getString("riwayat", "") ?: "")
            .split("\n").filter { it.isNotBlank() }

    fun tambahRiwayat(c: Context, q: String) {
        val baru = (listOf(q.trim()) + bacaRiwayat(c).filter { !it.equals(q.trim(), true) }).filter { it.isNotBlank() }.take(8)
        c.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putString("riwayat", baru.joinToString("\n")).apply()
    }

    fun hapusRiwayat(c: Context) {
        c.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().remove("riwayat").apply()
    }

    /** Alamat terakhir yang berhasil dibaca saat online, dipakai sebagai patokan ketika offline. */
    fun simpanAlamatTerakhir(c: Context, lat: Double, lng: Double, alamat: String) {
        c.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit()
            .putString("alamat_akhir", "$lat|$lng|$alamat").apply()
    }

    fun bacaAlamatTerakhir(c: Context): Triple<Double, Double, String>? {
        val s = c.getSharedPreferences(PREF, Context.MODE_PRIVATE).getString("alamat_akhir", null) ?: return null
        val p = s.split("|", limit = 3)
        if (p.size < 3) return null
        return Triple(p[0].toDoubleOrNull() ?: return null, p[1].toDoubleOrNull() ?: return null, p[2])
    }

    /** Cari koordinat dari teks alamat (butuh internet). Null bila gagal. */
    @Suppress("DEPRECATION")
    suspend fun cariKoordinat(c: Context, teks: String): Pair<Double, Double>? = withContext(Dispatchers.IO) {
        try {
            if (!Geocoder.isPresent()) return@withContext null
            val l = withTimeoutOrNull(8000) { Geocoder(c, Locale("id", "ID")).getFromLocationName(teks, 1) }
            l?.firstOrNull()?.let { it.latitude to it.longitude }
        } catch (e: Exception) { null }
    }

    /** Alamat dari koordinat (butuh internet). Null bila offline. */
    @Suppress("DEPRECATION")
    suspend fun cariAlamat(c: Context, lat: Double, lng: Double): String? = withContext(Dispatchers.IO) {
        try {
            if (!Geocoder.isPresent()) return@withContext null
            val a: Address? = withTimeoutOrNull(8000) { Geocoder(c, Locale("id", "ID")).getFromLocation(lat, lng, 1) }?.firstOrNull()
            a?.let {
                listOfNotNull(it.thoroughfare, it.subLocality, it.locality, it.subAdminArea, it.adminArea)
                    .distinct().joinToString(", ").ifBlank { it.getAddressLine(0) }
            }
        } catch (e: Exception) { null }
    }

    /** Ambil posisi GPS saat ini. Bekerja tanpa internet. Butuh izin lokasi. Null bila tidak dapat sinyal GPS. */
    @SuppressLint("MissingPermission")
    suspend fun posisiSekarang(c: Context, batasMs: Long = 20000): Location? {
        val lm = c.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val penyedia = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER)
            .filter { try { lm.isProviderEnabled(it) } catch (e: Exception) { false } }
        val terakhir = penyedia.mapNotNull { try { lm.getLastKnownLocation(it) } catch (e: Exception) { null } }
            .maxByOrNull { it.time }
        val segar = withTimeoutOrNull(batasMs) {
            suspendCancellableCoroutine<Location?> { k ->
                val pendengar = object : LocationListener {
                    override fun onLocationChanged(l: Location) { lm.removeUpdates(this); if (k.isActive) k.resume(l) {} }
                    @Deprecated("") override fun onStatusChanged(p: String?, s: Int, e: Bundle?) {}
                    override fun onProviderEnabled(p: String) {}
                    override fun onProviderDisabled(p: String) {}
                }
                val aktif = penyedia.filter { it != LocationManager.PASSIVE_PROVIDER }
                if (aktif.isEmpty()) { k.resume(null) {}; return@suspendCancellableCoroutine }
                aktif.forEach { try { lm.requestLocationUpdates(it, 0L, 0f, pendengar, Looper.getMainLooper()) } catch (_: Exception) {} }
                k.invokeOnCancellation { lm.removeUpdates(pendengar) }
            }
        }
        return segar ?: terakhir
    }

    private val KATEGORI_OSM: Map<String, List<String>> = mapOf(
        "Masjid" to listOf("[\"amenity\"=\"place_of_worship\"][\"religion\"=\"muslim\"]"),
        "Puskesmas" to listOf("[\"amenity\"~\"^(clinic|doctors|hospital)\$\"]"),
        "Rumah sakit" to listOf("[\"amenity\"=\"hospital\"]"),
        "Apotek" to listOf("[\"amenity\"=\"pharmacy\"]"),
        "SPBU" to listOf("[\"amenity\"=\"fuel\"]"),
        "ATM" to listOf("[\"amenity\"=\"atm\"]", "[\"amenity\"=\"bank\"]"),
        "Warung makan" to listOf("[\"amenity\"~\"^(restaurant|cafe|fast_food|food_court)\$\"]"),
        "Minimarket" to listOf("[\"shop\"~\"^(convenience|supermarket)\$\"]"),
    )

    private fun kirimOverpass(kueri: String): String? {
        for (server in listOf("https://overpass-api.de/api/interpreter", "https://overpass.kumi.systems/api/interpreter")) {
            try {
                val c = java.net.URL(server).openConnection() as java.net.HttpURLConnection
                try {
                    c.requestMethod = "POST"; c.connectTimeout = 12000; c.readTimeout = 30000; c.doOutput = true
                    c.setRequestProperty("User-Agent", "KKN-Al-Mushlih-Sekretaris/1.0")
                    c.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                    c.outputStream.use { it.write(("data=" + java.net.URLEncoder.encode(kueri, "UTF-8")).toByteArray()) }
                    if (c.responseCode in 200..299) return c.inputStream.bufferedReader().use { it.readText() }
                } finally { c.disconnect() }
            } catch (e: Exception) { /* coba server berikutnya */ }
        }
        return null
    }

    /**
     * Tempat terdekat dari titik pusat, DIURUTKAN menurut jarak sebenarnya (data OpenStreetMap, gratis).
     * Radius diperlebar bertahap (2, 5, 12, 30 km) sampai ketemu sedikitnya 3 tempat.
     * Hasil null = tidak ada koneksi / server sibuk; daftar kosong = tidak ada tempat dalam 30 km.
     */
    suspend fun cariTerdekat(kategori: String, lat: Double, lng: Double): List<Tempat>? = withContext(Dispatchers.IO) {
        val filter = KATEGORI_OSM[kategori] ?: return@withContext emptyList<Tempat>()
        var terakhir: List<Tempat> = emptyList()
        for (r in listOf(2000, 5000, 12000, 30000)) {
            val kueri = "[out:json][timeout:25];(" + filter.joinToString("") { "nwr(around:$r,${String.format(Locale.US, "%.6f", lat)},${String.format(Locale.US, "%.6f", lng)})$it;" } + ");out center 120;"
            val teks = kirimOverpass(kueri) ?: return@withContext null
            val el = try { org.json.JSONObject(teks).optJSONArray("elements") } catch (e: Exception) { null } ?: return@withContext null
            val daftar = mutableListOf<Tempat>()
            for (i in 0 until el.length()) {
                val o = el.getJSONObject(i)
                val la = if (o.has("lat")) o.getDouble("lat") else o.optJSONObject("center")?.optDouble("lat") ?: continue
                val lo = if (o.has("lon")) o.getDouble("lon") else o.optJSONObject("center")?.optDouble("lon") ?: continue
                val nama = o.optJSONObject("tags")?.optString("name").orEmpty().ifBlank { "$kategori (tanpa nama)" }
                daftar.add(Tempat(nama, la, lo, jarakMeter(lat, lng, la, lo), arah(lat, lng, la, lo)))
            }
            terakhir = daftar.sortedBy { it.jarak }.take(12)
            if (terakhir.size >= 3) break
        }
        terakhir
    }

    fun jarakMeter(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val r = 6371000.0
        val p1 = Math.toRadians(lat1); val p2 = Math.toRadians(lat2)
        val dp = p2 - p1; val dl = Math.toRadians(lng2 - lng1)
        val a = sin(dp / 2).pow(2) + cos(p1) * cos(p2) * sin(dl / 2).pow(2)
        return 2 * r * asin(sqrt(a))
    }

    fun arah(lat1: Double, lng1: Double, lat2: Double, lng2: Double): String {
        val p1 = Math.toRadians(lat1); val p2 = Math.toRadians(lat2); val dl = Math.toRadians(lng2 - lng1)
        val y = sin(dl) * cos(p2); val x = cos(p1) * sin(p2) - sin(p1) * cos(p2) * cos(dl)
        val d = (Math.toDegrees(atan2(y, x)) + 360) % 360
        return listOf("utara", "timur laut", "timur", "tenggara", "selatan", "barat daya", "barat", "barat laut")[((d + 22.5) / 45).toInt() % 8]
    }

    fun teksJarak(m: Double) = if (m < 1000) "${m.roundToInt()} m" else String.format(Locale("id", "ID"), "%.1f km", m / 1000)

    fun derajat(v: Double, lintang: Boolean): String {
        val h = if (lintang) (if (v >= 0) "U" else "S") else (if (v >= 0) "T" else "B")
        val a = abs(v); val d = a.toInt(); val m = ((a - d) * 60).toInt(); val s = ((a - d) * 60 - m) * 60
        return String.format(Locale.US, "%d°%02d'%04.1f\"%s", d, m, s, h)
    }

    /** Kode Plus (Open Location Code), 10 digit. Dihitung di HP, tanpa internet. */
    fun kodePlus(lat0: Double, lng0: Double): String {
        val abjad = "23456789CFGHJMPQRVWX"
        var la = floor((min(max(lat0, -90.0), 90.0) + 90) * 8000).toLong()
        var lo = floor((((lng0 + 180) % 360) + 360) % 360 * 8000).toLong()
        if (la >= 180L * 8000) la = 180L * 8000 - 1
        val dl = IntArray(5); val dg = IntArray(5)
        for (i in 4 downTo 0) { dl[i] = (la % 20).toInt(); dg[i] = (lo % 20).toInt(); la /= 20; lo /= 20 }
        val sb = StringBuilder()
        for (i in 0 until 5) { sb.append(abjad[dl[i]]).append(abjad[dg[i]]); if (i == 3) sb.append('+') }
        return sb.toString()
    }
}

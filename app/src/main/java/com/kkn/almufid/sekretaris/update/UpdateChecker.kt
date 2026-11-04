package com.kkn.almufid.sekretaris.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.kkn.almufid.sekretaris.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class InfoUpdate(
    val versiBaru: String,
    val catatan: String,
    val urlApk: String
)

/**
 * Cek versi terbaru di GitHub Releases (repo publik), unduh APK, lalu
 * buka installer Android. Semua fungsi jaringan aman dipanggil dari
 * coroutine mana pun karena berpindah ke Dispatchers.IO sendiri.
 * Kegagalan jaringan TIDAK pernah membuat app crash: cek() cukup
 * mengembalikan null, sehingga app tetap bisa dipakai penuh offline.
 */
object UpdateChecker {

    private const val PLACEHOLDER_OWNER = "GANTI_USERNAME_GITHUB"

    /** @return info update jika ada versi lebih baru, null jika tidak ada / gagal / belum dikonfigurasi. */
    suspend fun cek(): InfoUpdate? = withContext(Dispatchers.IO) {
        if (BuildConfig.GITHUB_OWNER == PLACEHOLDER_OWNER) return@withContext null
        try {
            val url = URL("https://api.github.com/repos/${BuildConfig.GITHUB_OWNER}/${BuildConfig.GITHUB_REPO}/releases/latest")
            val koneksi = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "SekretarisKKN-App")
            }
            try {
                if (koneksi.responseCode != 200) return@withContext null
                val json = JSONObject(koneksi.inputStream.bufferedReader().use { it.readText() })
                val tag = json.optString("tag_name").removePrefix("v").removePrefix("V")
                if (tag.isBlank() || !lebihBaru(tag, BuildConfig.VERSION_NAME)) return@withContext null

                val assets = json.optJSONArray("assets") ?: return@withContext null
                var urlApk: String? = null
                for (i in 0 until assets.length()) {
                    val aset = assets.getJSONObject(i)
                    if (aset.optString("name").endsWith(".apk", ignoreCase = true)) {
                        urlApk = aset.optString("browser_download_url")
                        break
                    }
                }
                if (urlApk.isNullOrBlank()) return@withContext null
                InfoUpdate(tag, json.optString("body").trim(), urlApk)
            } finally {
                koneksi.disconnect()
            }
        } catch (e: Exception) {
            null
        }
    }

    /** Membandingkan "1.2.10" vs "1.2.9" per angka (bukan per huruf), jadi 10 > 9 dengan benar. */
    internal fun lebihBaru(baru: String, sekarang: String): Boolean {
        val a = baru.split(".").map { it.takeWhile(Char::isDigit).toIntOrNull() ?: 0 }
        val b = sekarang.split(".").map { it.takeWhile(Char::isDigit).toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrElse(i) { 0 }
            val y = b.getOrElse(i) { 0 }
            if (x != y) return x > y
        }
        return false
    }

    /** Mengunduh APK ke cache/updates/. [onProgress] menerima 0..100 (atau -1 jika ukuran tidak diketahui). */
    suspend fun unduh(context: Context, urlApk: String, onProgress: (Int) -> Unit): File =
        withContext(Dispatchers.IO) {
            val folder = File(context.cacheDir, "updates").apply { mkdirs() }
            folder.listFiles()?.forEach { it.delete() } // buang sisa unduhan lama
            val tujuan = File(folder, "update.apk")

            val koneksi = (URL(urlApk).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15000
                readTimeout = 30000
                setRequestProperty("User-Agent", "SekretarisKKN-App")
            }
            try {
                if (koneksi.responseCode != 200) throw IllegalStateException("Server membalas ${koneksi.responseCode}")
                val total = koneksi.contentLengthLong
                var terunduh = 0L
                koneksi.inputStream.use { masuk ->
                    tujuan.outputStream().use { keluar ->
                        val buffer = ByteArray(16 * 1024)
                        var terakhir = -2
                        while (true) {
                            val baca = masuk.read(buffer)
                            if (baca < 0) break
                            keluar.write(buffer, 0, baca)
                            terunduh += baca
                            val persen = if (total > 0) (terunduh * 100 / total).toInt() else -1
                            if (persen != terakhir) { terakhir = persen; onProgress(persen) }
                        }
                    }
                }
                tujuan
            } finally {
                koneksi.disconnect()
            }
        }

    /**
     * Membuka installer sistem. Android 8+ mewajibkan izin "pasang aplikasi
     * tidak dikenal" per-aplikasi; jika belum diberikan, user diarahkan ke
     * halaman pengaturannya dulu (fungsi mengembalikan false).
     */
    fun pasang(context: Context, apk: File): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.packageManager.canRequestPackageInstalls()) {
            context.startActivity(
                Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            return false
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apk)
        context.startActivity(
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        return true
    }
}

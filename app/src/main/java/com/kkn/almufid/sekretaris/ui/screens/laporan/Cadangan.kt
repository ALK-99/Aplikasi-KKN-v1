package com.kkn.almufid.sekretaris.ui.screens.laporan

import android.content.Context
import android.net.Uri
import com.kkn.almufid.sekretaris.data.AppDatabase
import com.kkn.almufid.sekretaris.data.cloud.DesaRepository
import com.kkn.almufid.sekretaris.data.entity.JenisJurnal
import com.kkn.almufid.sekretaris.data.entity.JurnalEntry
import com.kkn.almufid.sekretaris.data.entity.NotaEntry
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Cadangan lengkap dalam satu berkas ZIP:
 *  - data.json       : data yang bisa dipulihkan (jurnal & nota pribadi di HP, data desa di cloud)
 *  - baca-saja.json  : ringkasan yang mudah dibaca manusia (termasuk data bersama seluruh kelompok)
 *  - foto/...        : foto nota di HP
 * Semua murni API bawaan Android/Java, tanpa library tambahan dan tanpa biaya.
 */
object Cadangan {
    const val FORMAT = "kkn-backup"
    const val VERSI = 2
    private const val BATAS_DATA = 30 * 1024 * 1024
    private const val BATAS_FOTO = 12 * 1024 * 1024

    class Pratinjau(
        val desaId: String,
        val desaNama: String,
        val dibuat: Long,
        val jurnal: Int,
        val nota: Int,
        val foto: Int,
        val dataDesa: Int,
        val json: JSONObject,
        val folderKerja: File,
    )

    private fun folderLaporan(context: Context) = File(context.cacheDir, "laporan").apply { mkdirs() }
    private fun folderPulih(context: Context) = File(context.cacheDir, "pulih")

    fun bersihkan(context: Context) { folderPulih(context).deleteRecursively() }

    // ------------------------------------------------------------ membuat

    private fun keJson(peta: Map<String, Any?>): JSONObject {
        val o = JSONObject()
        for ((k, v) in peta) {
            when (v) {
                null -> {}
                is String, is Boolean, is Long, is Int, is Double -> o.put(k, v as Any)
                else -> o.put(k, v.toString())
            }
        }
        return o
    }

    suspend fun buatZip(context: Context, db: AppDatabase, desa: DesaRepository, ringkasan: File): File {
        val sekarang = System.currentTimeMillis()
        val identitas = desa.identitasDesa()
        val root = JSONObject()
            .put("format", FORMAT).put("versi", VERSI).put("dibuat", sekarang)
            .put("desaId", identitas?.first ?: "").put("desaNama", identitas?.second ?: "")

        // Data lokal (jurnal pribadi + nota beserta foto).
        val fotoUntukZip = mutableListOf<File>()
        val lokal = JSONObject()
        lokal.put("jurnal", JSONArray().also { a ->
            db.jurnalDao().semuaJurnal().first().forEach {
                a.put(
                    JSONObject().put("tanggal", it.tanggalMillis).put("jenis", it.jenis.name)
                        .put("judul", it.judul).put("isi", it.isi).put("peserta", it.pesertaRapat)
                )
            }
        })
        lokal.put("nota", JSONArray().also { a ->
            db.notaDao().semuaNota().first().forEach {
                val o = JSONObject().put("tanggal", it.tanggalMillis).put("keterangan", it.keterangan)
                    .put("nominal", it.nominal).put("proker", it.prokerTerkait)
                val f = it.fotoPath?.let { p -> File(p) }
                if (f != null && f.exists()) {
                    o.put("foto", f.name)
                    fotoUntukZip += f
                }
                a.put(o)
            }
        })
        root.put("lokal", lokal)

        // Data desa di cloud (dokumen mentah, termasuk riwayat sebagai arsip).
        val desaJson = JSONObject()
        for (nama in desa.koleksiCadangan + "riwayat") {
            val dok = desa.ambilMentah(nama)
            desaJson.put(nama, JSONArray().also { a ->
                dok.orEmpty().forEach { (id, isi) -> a.put(JSONObject().put("id", id).put("data", keJson(isi))) }
            })
        }
        root.put("desa", desaJson)

        val nama = "cadangan-kkn-" + SimpleDateFormat("yyyyMMdd-HHmm", Locale("id", "ID")).format(Date(sekarang)) + ".zip"
        val hasil = File(folderLaporan(context), nama)
        ZipOutputStream(FileOutputStream(hasil)).use { zip ->
            zip.putNextEntry(ZipEntry("data.json"))
            zip.write(root.toString().toByteArray(Charsets.UTF_8))
            zip.closeEntry()
            zip.putNextEntry(ZipEntry("baca-saja.json"))
            zip.write(ringkasan.readBytes())
            zip.closeEntry()
            fotoUntukZip.distinctBy { it.name }.forEach { f ->
                zip.putNextEntry(ZipEntry("foto/${f.name}"))
                f.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
            }
        }
        return hasil
    }

    // ------------------------------------------------------------ membaca

    /** Membuka ZIP dan memeriksa isinya tanpa mengubah data apa pun. */
    fun baca(context: Context, uri: Uri): Pratinjau {
        val folder = folderPulih(context).apply { deleteRecursively(); mkdirs() }
        var teksJson: String? = null
        var jumlahFoto = 0
        val masuk = context.contentResolver.openInputStream(uri)
            ?: throw IllegalArgumentException("Berkas tidak bisa dibuka.")
        masuk.use { raw ->
            ZipInputStream(raw).use { zip ->
                while (true) {
                    val e = zip.nextEntry ?: break
                    if (e.isDirectory) continue
                    when {
                        e.name == "data.json" -> teksJson = salinDenganBatas(zip, BATAS_DATA).toString(Charsets.UTF_8)
                        e.name.startsWith("foto/") -> {
                            // Hanya nama berkas yang dipakai, jadi path berbahaya (../) tidak berpengaruh.
                            val nama = File(e.name).name
                            if (nama.isNotBlank()) {
                                File(folder, nama).writeBytes(salinDenganBatas(zip, BATAS_FOTO).toByteArray())
                                jumlahFoto++
                            }
                        }
                    }
                }
            }
        }
        val isi = teksJson ?: throw IllegalArgumentException("Ini bukan berkas cadangan KKN (data.json tidak ditemukan).")
        val json = try { JSONObject(isi) } catch (e: Exception) {
            throw IllegalArgumentException("Isi cadangan rusak dan tidak bisa dibaca.")
        }
        if (json.optString("format") != FORMAT) throw IllegalArgumentException("Ini bukan berkas cadangan KKN.")
        if (json.optInt("versi", 0) > VERSI) {
            throw IllegalArgumentException("Cadangan dibuat oleh versi aplikasi yang lebih baru. Perbarui aplikasi dulu.")
        }
        val lokal = json.optJSONObject("lokal")
        val desa = json.optJSONObject("desa")
        val jumlahDesa = DesaKoleksi.sumber.sumOf { desa?.optJSONArray(it)?.length() ?: 0 }
        return Pratinjau(
            desaId = json.optString("desaId"), desaNama = json.optString("desaNama"),
            dibuat = json.optLong("dibuat"),
            jurnal = lokal?.optJSONArray("jurnal")?.length() ?: 0,
            nota = lokal?.optJSONArray("nota")?.length() ?: 0,
            foto = jumlahFoto, dataDesa = jumlahDesa, json = json, folderKerja = folder,
        )
    }

    private fun salinDenganBatas(masuk: java.io.InputStream, batas: Int): ByteArrayOutputStream {
        val keluar = ByteArrayOutputStream()
        val buf = ByteArray(8192)
        var total = 0
        while (true) {
            val n = masuk.read(buf)
            if (n < 0) break
            total += n
            if (total > batas) throw IllegalArgumentException("Berkas cadangan terlalu besar atau tidak wajar.")
            keluar.write(buf, 0, n)
        }
        return keluar
    }

    /** Nama koleksi desa yang dipulihkan (sama dengan DesaRepository.koleksiCadangan). */
    private object DesaKoleksi { val sumber = listOf("anggota", "absensi", "proker", "surat", "inventaris", "nota", "bukti", "ajar") }

    // ------------------------------------------------------------ memulihkan

    private fun JSONObject.kePeta(): Map<String, Any?> {
        val hasil = mutableMapOf<String, Any?>()
        for (k in keys()) {
            when (val v = get(k)) {
                JSONObject.NULL -> {}
                is Int -> hasil[k] = v.toLong()
                is Long, is Double, is Boolean, is String -> hasil[k] = v
                else -> hasil[k] = v.toString()
            }
        }
        return hasil
    }

    fun dataDesa(json: JSONObject): Map<String, List<Pair<String, Map<String, Any?>>>> {
        val desa = json.optJSONObject("desa") ?: return emptyMap()
        return DesaKoleksi.sumber.associateWith { nama ->
            val a = desa.optJSONArray(nama) ?: JSONArray()
            (0 until a.length()).mapNotNull { i ->
                val o = a.optJSONObject(i) ?: return@mapNotNull null
                val id = o.optString("id")
                val data = o.optJSONObject("data")
                if (id.isBlank() || data == null) null else id to data.kePeta()
            }
        }
    }

    /**
     * Memulihkan jurnal & nota pribadi. Yang sudah ada (tanggal dan isinya sama) dilewati,
     * jadi memulihkan dua kali tidak menggandakan data. Hasil: (jurnal baru, nota baru).
     */
    suspend fun pulihkanLokal(context: Context, db: AppDatabase, p: Pratinjau): Pair<Int, Int> {
        val lokal = p.json.optJSONObject("lokal") ?: return 0 to 0
        var jurnalBaru = 0
        var notaBaru = 0

        val adaJurnal = db.jurnalDao().semuaJurnal().first().map { it.tanggalMillis to it.judul }.toMutableSet()
        val aj = lokal.optJSONArray("jurnal") ?: JSONArray()
        for (i in 0 until aj.length()) {
            val o = aj.optJSONObject(i) ?: continue
            val kunci = o.optLong("tanggal") to o.optString("judul")
            if (!adaJurnal.add(kunci)) continue
            db.jurnalDao().tambah(
                JurnalEntry(
                    tanggalMillis = kunci.first,
                    jenis = runCatching { JenisJurnal.valueOf(o.optString("jenis")) }.getOrDefault(JenisJurnal.HARIAN),
                    judul = kunci.second, isi = o.optString("isi"), pesertaRapat = o.optString("peserta"),
                )
            )
            jurnalBaru++
        }

        val adaNota = db.notaDao().semuaNota().first().map { Triple(it.tanggalMillis, it.keterangan, it.nominal) }.toMutableSet()
        val folderFoto = File(context.filesDir, "nota_photos").apply { mkdirs() }
        val an = lokal.optJSONArray("nota") ?: JSONArray()
        for (i in 0 until an.length()) {
            val o = an.optJSONObject(i) ?: continue
            val kunci = Triple(o.optLong("tanggal"), o.optString("keterangan"), o.optLong("nominal"))
            if (!adaNota.add(kunci)) continue
            var fotoPath: String? = null
            val namaFoto = o.optString("foto")
            if (namaFoto.isNotBlank()) {
                val sumber = File(p.folderKerja, File(namaFoto).name)
                if (sumber.exists()) {
                    val tujuan = File(folderFoto, "pulih_${sumber.name}")
                    sumber.copyTo(tujuan, overwrite = true)
                    fotoPath = tujuan.absolutePath
                }
            }
            db.notaDao().tambah(
                NotaEntry(
                    tanggalMillis = kunci.first, keterangan = kunci.second, nominal = kunci.third,
                    prokerTerkait = o.optString("proker"), fotoPath = fotoPath,
                )
            )
            notaBaru++
        }
        return jurnalBaru to notaBaru
    }
}

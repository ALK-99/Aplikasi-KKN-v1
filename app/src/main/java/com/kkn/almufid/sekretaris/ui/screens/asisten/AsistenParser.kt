package com.kkn.almufid.sekretaris.ui.screens.asisten

import com.kkn.almufid.sekretaris.data.cloud.AbsensiDesa
import com.kkn.almufid.sekretaris.data.cloud.AnggotaDesa
import com.kkn.almufid.sekretaris.data.cloud.Kas
import com.kkn.almufid.sekretaris.data.cloud.NotaDesa
import com.kkn.almufid.sekretaris.data.cloud.ProkerDesa
import com.kkn.almufid.sekretaris.data.cloud.SuratDesa
import com.kkn.almufid.sekretaris.data.cloud.Tugas
import com.kkn.almufid.sekretaris.data.entity.JenisSurat
import com.kkn.almufid.sekretaris.data.entity.StatusProker
import com.kkn.almufid.sekretaris.ui.formatRupiah
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Aksi yang diusulkan asisten. Tidak pernah dijalankan sebelum pengguna menekan Simpan. */
sealed class AksiAsisten {
    data class CatatKas(val masuk: Boolean, val jumlah: Long, val kategori: String, val keterangan: String) : AksiAsisten()
    data class BuatTugas(val judul: String, val deskripsi: String, val untuk: String, val tenggat: Long) : AksiAsisten()
    data class BuatPengumuman(val judul: String, val isi: String) : AksiAsisten()
    data class TambahBarang(val nama: String, val jumlah: Int, val kondisi: String) : AksiAsisten()
    data class CatatSurat(val masuk: Boolean, val nomor: String, val perihal: String) : AksiAsisten()
}

data class HasilAsisten(val balasan: String, val aksi: AksiAsisten? = null)

/** Potret data desa yang dipakai untuk menjawab pertanyaan. */
class KonteksAsisten(
    val kas: List<Kas>,
    val tugas: List<Tugas>,
    val proker: List<ProkerDesa>,
    val nota: List<NotaDesa>,
    val surat: List<SuratDesa>,
    val absensi: List<AbsensiDesa>,
    val anggota: List<AnggotaDesa>,
    val namaAkun: List<String>,
    val namaSaya: String,
)

object AsistenParser {
    private val fmtTgl = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))
    private fun tgl(ms: Long) = fmtTgl.format(Date(ms))
    private fun kapital(s: String) = s.replaceFirstChar { it.uppercase() }

    private class Uang(val n: Long, val bersatuan: Boolean, val teks: String)

    private val RE_UANG = Regex("""(?:rp\.?\s*)?(\d{1,3}(?:\.\d{3})+|\d+(?:,\d+)?)\s*(juta|jt|ribu|rb|k)?\b""", RegexOption.IGNORE_CASE)

    private fun uangDari(t: String): Uang? {
        val semua = RE_UANG.findAll(t).map { m ->
            val mentah = m.groupValues[1]
            val angka = if (mentah.contains(".")) mentah.replace(".", "").toDouble() else mentah.replace(",", ".").toDouble()
            val s = m.groupValues[2].lowercase()
            val kali = when {
                s == "juta" || s == "jt" -> 1_000_000.0
                s == "ribu" || s == "rb" || s == "k" -> 1_000.0
                else -> 1.0
            }
            Uang(Math.round(angka * kali), s.isNotEmpty(), m.value)
        }.toList()
        return semua.firstOrNull { it.bersatuan } ?: semua.firstOrNull { it.n >= 1000 }
    }

    /** Tenggat dari kata seperti besok, lusa, hari Jumat, tanggal 12. 0 bila tidak ada. */
    fun tenggatDari(t: String): Long {
        val q = t.lowercase()
        val h = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }
        fun tambah(d: Int): Long { val x = h.clone() as Calendar; x.add(Calendar.DAY_OF_YEAR, d); return x.timeInMillis }
        if (q.contains("hari ini")) return tambah(0)
        if (q.contains("besok")) return tambah(1)
        if (q.contains("lusa")) return tambah(2)
        Regex("""(\d+)\s*hari lagi""").find(q)?.let { return tambah(it.groupValues[1].toInt()) }
        if (q.contains("minggu depan")) return tambah(7)
        val hari = listOf("minggu", "senin", "selasa", "rabu", "kamis", "jumat", "sabtu")
        for (i in 0..6) {
            val cocok = Regex("""\b${hari[i]}\b""").containsMatchIn(q) || (i == 5 && Regex("""jum'?at""").containsMatchIn(q))
            if (cocok) {
                var d = (i + 1 - h.get(Calendar.DAY_OF_WEEK) + 7) % 7
                if (d == 0) d = 7
                return tambah(d)
            }
        }
        Regex("""tanggal\s*(\d{1,2})""").find(q)?.let {
            val x = h.clone() as Calendar
            x.set(Calendar.DAY_OF_MONTH, it.groupValues[1].toInt().coerceIn(1, 28))
            if (x.before(h)) x.add(Calendar.MONTH, 1)
            return x.timeInMillis
        }
        return 0L
    }

    private fun bersih(t: String, buang: List<Regex>): String {
        var s = t
        buang.forEach { s = it.replace(s, " ") }
        return s.replace(Regex("""\s+"""), " ").trim(' ', ':', ',', '-')
    }
    private fun lit(s: String) = Regex(Regex.escape(s))
    private val I = RegexOption.IGNORE_CASE

    fun ringkas(a: AksiAsisten): String = when (a) {
        is AksiAsisten.CatatKas -> "Kas ${if (a.masuk) "MASUK" else "KELUAR"} ${formatRupiah(a.jumlah)} · ${a.keterangan.ifBlank { a.kategori }}"
        is AksiAsisten.BuatTugas -> "Tugas \"${a.judul}\"" + (if (a.untuk.isNotBlank()) " untuk ${a.untuk}" else "") + ", tenggat ${tgl(if (a.tenggat > 0) a.tenggat else tenggatDari("besok"))}"
        is AksiAsisten.BuatPengumuman -> "Pengumuman \"${a.judul}\""
        is AksiAsisten.TambahBarang -> "Inventaris: ${a.nama} × ${a.jumlah} (${a.kondisi})"
        is AksiAsisten.CatatSurat -> "Surat ${if (a.masuk) "masuk" else "keluar"}: ${a.perihal}" + (if (a.nomor.isNotBlank()) " (${a.nomor})" else "")
    }

    private fun saldo(k: List<Kas>): Triple<Long, Long, Long> {
        val m = k.filter { it.masuk }.sumOf { it.jumlah }
        val o = k.filter { !it.masuk }.sumOf { it.jumlah }
        return Triple(m, o, m - o)
    }

    private fun awalHari(ms: Long): Long = Calendar.getInstance().apply { timeInMillis = ms; set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis

    fun pahami(teks: String, k: KonteksAsisten): HasilAsisten {
        val q = teks.lowercase().trim()
        val uang = uangDari(teks)
        val tanya = Regex("""^(berapa|siapa|apa|apakah|tampilkan|lihat|cek|ringkasan|rekap|status|daftar|bagaimana)|\?$""").containsMatchIn(q) ||
            (!Regex("catat|tambah|buat|input|bikin|kirim").containsMatchIn(q) && Regex("saldo|ringkasan|rekap").containsMatchIn(q))
        if (tanya) {
            if (Regex("saldo|kas").containsMatchIn(q)) {
                val (m, o, s) = saldo(k.kas)
                return HasilAsisten("Kas kelompok: pemasukan ${formatRupiah(m)}, pengeluaran ${formatRupiah(o)}, saldo ${formatRupiah(s)}.")
            }
            if (q.contains("tugas")) {
                val t = k.tugas.filter { !it.selesai }
                return HasilAsisten(
                    if (t.isEmpty()) "Semua tugas sudah selesai. Mantap."
                    else "Ada ${t.size} tugas belum selesai:\n" + t.take(12).joinToString("\n") {
                        "• ${it.judul}" + (if (it.untukNama.isNotBlank()) " (${it.untukNama})" else "") + (if (it.deadlineMillis > 0) ", tenggat ${tgl(it.deadlineMillis)}" else "")
                    }
                )
            }
            if (Regex("proker|program").containsMatchIn(q)) {
                fun c(s: StatusProker) = k.proker.count { it.status == s }
                return HasilAsisten("Proker: ${k.proker.size} total. Selesai ${c(StatusProker.SELESAI)}, berjalan ${c(StatusProker.BERJALAN)}, belum mulai ${c(StatusProker.BELUM_MULAI)}, batal ${c(StatusProker.BATAL)}.")
            }
            if (Regex("absen|hadir").containsMatchIn(q)) {
                val h = awalHari(System.currentTimeMillis())
                val a = k.absensi.filter { it.tanggalMillis in h until h + 86_400_000L }
                val aktif = k.anggota.count { it.aktif }
                return HasilAsisten(if (a.isEmpty()) "Absensi hari ini belum dicatat." else "Hari ini ${a.count { it.status == "HADIR" }} hadir dari $aktif anggota aktif (${a.size} sudah dicatat).")
            }
            if (Regex("nota|pengeluaran|belanja").containsMatchIn(q)) {
                return HasilAsisten("${k.nota.size} nota/kwitansi, total ${formatRupiah(k.nota.sumOf { it.nominal })}.")
            }
            if (q.contains("surat")) {
                return HasilAsisten("${k.surat.size} surat tercatat (${k.surat.count { it.jenis == JenisSurat.MASUK }} masuk, ${k.surat.count { it.jenis == JenisSurat.KELUAR }} keluar).")
            }
            if (Regex("ringkasan|rekap|hari ini").containsMatchIn(q)) {
                val s = saldo(k.kas).third
                return HasilAsisten("Ringkasan: saldo kas ${formatRupiah(s)}; ${k.tugas.count { !it.selesai }} tugas belum selesai; proker selesai ${k.proker.count { it.status == StatusProker.SELESAI }}/${k.proker.size}; ${k.nota.size} nota; ${k.surat.size} surat.")
            }
        }
        if (uang != null && Regex("""\bkas\b|pemasukan|pengeluaran|iuran|dana masuk|dana keluar""").containsMatchIn(q)) {
            val masuk = !Regex("keluar|pengeluaran|bayar|beli|belanja|biaya").containsMatchIn(q) ||
                (Regex("masuk|terima|iuran|pemasukan").containsMatchIn(q) && !q.contains("keluar"))
            val ket = kapital(bersih(teks, listOf(lit(uang.teks), Regex("catat|tambah|input|bikin", I), Regex("""\bkas\b""", I), Regex("""\b(masuk|keluar)\b""", I), Regex("""\bsebesar\b|\bsejumlah\b|\buntuk\b""", I))))
            return HasilAsisten("Saya siap mencatat ini ke kas. Cek dulu, lalu tekan Simpan.", AksiAsisten.CatatKas(masuk, uang.n, if (masuk) "Pemasukan" else "Pengeluaran", ket))
        }
        if (q.contains("pengumuman")) {
            val isi = bersih(teks, listOf(Regex("""buat(kan)?|tambah|bikin|kirim""", I), Regex("pengumuman", I)))
            if (isi.isNotEmpty()) {
                return HasilAsisten("Pengumuman siap dibuat.", AksiAsisten.BuatPengumuman(kapital(isi.split(Regex("""\s+""")).take(7).joinToString(" ")), kapital(isi)))
            }
        }
        if (Regex("""\btugas\b""").containsMatchIn(q) && Regex("buat|tambah|bikin|beri|kasih|catat").containsMatchIn(q)) {
            val m = Regex("""\buntuk\s+([A-Za-zÀ-ÿ'.\- ]+?)(?=\s+(besok|lusa|hari ini|minggu|senin|selasa|rabu|kamis|jumat|sabtu|tanggal|\d)|$)""", I).find(teks)
            val untuk = m?.groupValues?.get(1)?.trim() ?: ""
            val buang = mutableListOf(
                Regex("""buat(kan)?|tambah|bikin|beri|kasih|catat""", I), Regex("""\btugas\b""", I),
                Regex("""\b(besok|lusa|hari ini|minggu depan|\d+\s*hari lagi|tanggal\s*\d+)\b""", I), Regex("""\b(senin|selasa|rabu|kamis|jumat|sabtu)\b""", I)
            )
            if (m != null) buang.add(0, lit(m.value))
            val judul = kapital(bersih(teks, buang))
            if (judul.isNotEmpty()) return HasilAsisten("Tugas siap dibuat.", AksiAsisten.BuatTugas(judul, "", untuk, tenggatDari(teks)))
        }
        if (Regex("inventaris|barang").containsMatchIn(q)) {
            val m = Regex("""(\d+)\s*(buah|unit|pcs|biji|set|lembar|botol|rim)?""", I).find(teks)
            val n = m?.groupValues?.get(1)?.toIntOrNull() ?: 1
            val kond = Regex("""kondisi\s+(\w+)""").find(q)?.groupValues?.get(1)
            val buang = mutableListOf(Regex("""tambah(kan)?|catat|input|masukkan|bikin""", I), Regex("""inventaris|barang""", I), Regex("""kondisi\s+\w+""", I), Regex("""\bke\b""", I))
            if (m != null) buang.add(0, lit(m.value))
            val nama = kapital(bersih(teks, buang))
            if (nama.isNotEmpty()) return HasilAsisten("Barang siap ditambahkan ke inventaris.", AksiAsisten.TambahBarang(nama, n, kond?.let { kapital(it) } ?: "Baik"))
        }
        if (Regex("surat (masuk|keluar)").containsMatchIn(q)) {
            val masuk = q.contains("surat masuk")
            val no = Regex("""nomor\s*[:\-]?\s*([\w/.\-]+)""", I).find(teks)?.groupValues?.get(1) ?: ""
            val perihal = kapital(bersih(teks, listOf(Regex("catat|tambah|input|bikin", I), Regex("surat (masuk|keluar)", I), Regex("""nomor\s*[:\-]?\s*[\w/.\-]+""", I), Regex("perihal|tentang", I))))
            if (perihal.isNotEmpty()) return HasilAsisten("Surat siap dicatat di register.", AksiAsisten.CatatSurat(masuk, no, perihal))
        }
        return HasilAsisten("Saya belum paham. Coba contoh: \"Catat kas masuk 50 ribu iuran\", \"Tugas buat laporan untuk Fahmi besok\", \"Tambah inventaris spidol 5 buah\", \"Berapa saldo kas?\". Untuk bahasa bebas, aktifkan Mode pintar (gratis) di bawah.")
    }

    // ---------- Mode pintar: Google Gemini lewat kunci API gratis milik pengguna ----------

    private fun konteksTeks(k: KonteksAsisten): String {
        val (m, o, s) = saldo(k.kas)
        val t = k.tugas.filter { !it.selesai }.take(15).joinToString("; ") { "${it.judul} (${it.untukNama.ifBlank { "-" }}, tenggat ${if (it.deadlineMillis > 0) tgl(it.deadlineMillis) else "-"})" }
        val p = k.proker.take(15).joinToString("; ") { "${it.namaProker} [${it.status}]" }
        return "Tanggal hari ini: ${SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())}. Pengguna: ${k.namaSaya}.\n" +
            "Anggota: ${k.namaAkun.joinToString(", ")}.\nSaldo kas: $s (masuk $m, keluar $o). Total nota: ${k.nota.sumOf { it.nominal }}.\n" +
            "Tugas belum selesai: ${t.ifBlank { "tidak ada" }}.\nProker: ${p.ifBlank { "belum ada" }}."
    }

    /** Dipanggil dari thread IO. Melempar exception bila gagal; pemanggil jatuh balik ke [pahami]. */
    fun tanyaGemini(teks: String, kunci: String, model: String, k: KonteksAsisten): HasilAsisten {
        val sistem = "Kamu asisten sekretaris dan bendahara KKN. Jawab singkat dalam bahasa Indonesia. Balas HANYA JSON: {\"balasan\": string, \"aksi\": null atau objek}. Jenis aksi:\n" +
            "{\"jenis\":\"kas\",\"masuk\":bool,\"jumlah\":angka rupiah,\"kategori\":string,\"keterangan\":string}\n" +
            "{\"jenis\":\"tugas\",\"judul\":string,\"deskripsi\":string,\"untuk\":nama anggota atau \"\",\"tenggat\":\"YYYY-MM-DD\" atau \"\"}\n" +
            "{\"jenis\":\"pengumuman\",\"judul\":string,\"isi\":string}\n" +
            "{\"jenis\":\"inventaris\",\"nama\":string,\"jumlah\":angka,\"kondisi\":string}\n" +
            "{\"jenis\":\"surat\",\"jenisSurat\":\"MASUK\" atau \"KELUAR\",\"nomor\":string,\"perihal\":string}\n" +
            "Jika pengguna hanya bertanya, jawab dari DATA dan aksi null. Jangan mengarang angka yang tidak ada di DATA. Jika ragu, tanyakan di balasan dan aksi null.\nDATA:\n" + konteksTeks(k)
        val badan = JSONObject()
            .put("systemInstruction", JSONObject().put("parts", org.json.JSONArray().put(JSONObject().put("text", sistem))))
            .put("contents", org.json.JSONArray().put(JSONObject().put("role", "user").put("parts", org.json.JSONArray().put(JSONObject().put("text", teks)))))
            .put("generationConfig", JSONObject().put("responseMimeType", "application/json").put("temperature", 0.2))
        val c = URL("https://generativelanguage.googleapis.com/v1beta/models/${model.trim()}:generateContent").openConnection() as HttpURLConnection
        try {
            c.requestMethod = "POST"; c.connectTimeout = 15000; c.readTimeout = 30000; c.doOutput = true
            c.setRequestProperty("Content-Type", "application/json"); c.setRequestProperty("x-goog-api-key", kunci)
            c.outputStream.use { it.write(badan.toString().toByteArray(Charsets.UTF_8)) }
            val kode = c.responseCode
            val isi = (if (kode in 200..299) c.inputStream else c.errorStream).bufferedReader().use { it.readText() }
            if (kode !in 200..299) throw IllegalStateException("$kode ${isi.take(160)}")
            val bagian = JSONObject(isi).getJSONArray("candidates").getJSONObject(0).getJSONObject("content").getJSONArray("parts")
            val sb = StringBuilder(); for (i in 0 until bagian.length()) sb.append(bagian.getJSONObject(i).optString("text"))
            val o = JSONObject(sb.toString().removePrefix("```json").removeSuffix("```").trim())
            val a = o.optJSONObject("aksi")
            val aksi: AksiAsisten? = a?.let {
                when (it.optString("jenis")) {
                    "kas" -> AksiAsisten.CatatKas(it.optBoolean("masuk", true), it.optLong("jumlah"), it.optString("kategori"), it.optString("keterangan"))
                    "tugas" -> AksiAsisten.BuatTugas(it.optString("judul"), it.optString("deskripsi"), it.optString("untuk"), tenggatIso(it.optString("tenggat")))
                    "pengumuman" -> AksiAsisten.BuatPengumuman(it.optString("judul"), it.optString("isi"))
                    "inventaris" -> AksiAsisten.TambahBarang(it.optString("nama"), it.optInt("jumlah", 1).coerceAtLeast(1), it.optString("kondisi").ifBlank { "Baik" })
                    "surat" -> AksiAsisten.CatatSurat(it.optString("jenisSurat") == "MASUK", it.optString("nomor"), it.optString("perihal"))
                    else -> null
                }
            }
            return HasilAsisten(o.optString("balasan", "Selesai."), aksi)
        } finally { c.disconnect() }
    }

    private fun tenggatIso(s: String): Long = try {
        if (s.isBlank()) 0L else (SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(s)!!.time + (23 * 60 + 59) * 60_000L)
    } catch (e: Exception) { 0L }
}

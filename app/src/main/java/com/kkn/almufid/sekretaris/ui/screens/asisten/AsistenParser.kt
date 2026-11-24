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

data class HasilAsisten(val balasan: String, val aksi: List<AksiAsisten> = emptyList(), val ingat: List<String> = emptyList()) {
    constructor(balasan: String, satu: AksiAsisten) : this(balasan, listOf(satu))
}

class Lampiran(val nama: String, val teks: String?, val pdfBase64: String?)
class RiwayatItem(val saya: Boolean, val teks: String)

const val MODEL_BAWAAN = "gemini-flash-latest, gemini-2.5-flash, gemini-flash-lite-latest, gemini-2.5-flash-lite"

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
    val ingatan: List<String> = emptyList(),
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

    private fun sosial(q: String, teks: String, k: KonteksAsisten): HasilAsisten? {
        val nama = k.namaSaya.split(" ").firstOrNull().orEmpty()
        if (Regex("^(halo|hai|hi|hey|assalamu|salam|selamat (pagi|siang|sore|malam))").containsMatchIn(q)) return HasilAsisten("Halo $nama. Ada yang mau dicatat atau ditanyakan hari ini?")
        if (Regex("siapa (kamu|anda)|kamu siapa|nama (kamu|mu)").containsMatchIn(q)) return HasilAsisten("Saya asisten KKN Al-Mushlih. Saya bantu sekretaris dan bendahara mencatat kas, tugas, pengumuman, inventaris, dan surat, serta menjawab soal data desa. Aktifkan Mode pintar untuk ngobrol bebas dan membaca file.")
        if (Regex("bisa apa|bantuan|cara pakai|fitur").containsMatchIn(q)) return HasilAsisten("Saya bisa: catat kas masuk/keluar, buat tugas, pengumuman, inventaris, surat; jawab saldo, tugas, proker, absensi. Dengan Mode pintar saya juga bisa ngobrol, membaca PDF/Word/Excel, dan mengingat kesulitanmu.")
        if (Regex("terima kasih|makasih|thanks").containsMatchIn(q)) return HasilAsisten("Sama-sama. Panggil saya kapan saja.")
        Regex("""^ingat(?:kan)?(?: bahwa| ya)?[\s:,]+(.+)""").find(q)?.let { return HasilAsisten("Siap, saya ingat: ${it.groupValues[1]}", emptyList(), listOf(it.groupValues[1])) }
        if (Regex("apa (yang )?(kamu )?ingat|ingatanmu").containsMatchIn(q)) return HasilAsisten(
            if (k.ingatan.isEmpty()) "Belum ada yang saya ingat. Katakan \"ingat bahwa ...\" untuk menyimpan." else "Yang saya ingat:\n" + k.ingatan.joinToString("\n") { "• $it" })
        return null
    }

    fun pahami(teks: String, k: KonteksAsisten): HasilAsisten {
        val q = teks.lowercase().trim()
        val uang = uangDari(teks)
        sosial(q, teks, k)?.let { return it }
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
        return HasilAsisten("Saya belum paham perintah itu dalam mode biasa. Contoh: \"Catat kas masuk 50 ribu iuran\", \"Tugas buat laporan untuk Fahmi besok\", \"Tambah inventaris spidol 5 buah\", \"Berapa saldo kas?\". Untuk ngobrol bebas dan membaca file, aktifkan Mode pintar (gratis) lewat tombol pengaturan.")
    }

    // ---------- Mode pintar: Google Gemini lewat kunci API gratis milik pengguna ----------

    private fun konteksTeks(k: KonteksAsisten): String {
        val (m, o, s) = saldo(k.kas)
        val t = k.tugas.filter { !it.selesai }.take(15).joinToString("; ") { "${it.judul} (${it.untukNama.ifBlank { "-" }}, tenggat ${if (it.deadlineMillis > 0) tgl(it.deadlineMillis) else "-"})" }
        val p = k.proker.take(15).joinToString("; ") { "${it.namaProker} [${it.status}]" }
        val kt = k.kas.take(12).joinToString("; ") { "${if (it.masuk) "+" else "-"}${it.jumlah} ${it.keterangan.ifBlank { it.kategori }}" }
        return "Tanggal hari ini: ${SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())}. Pengguna: ${k.namaSaya}.\n" +
            "Anggota: ${k.namaAkun.joinToString(", ")}.\nSaldo kas: $s (masuk $m, keluar $o). Total nota: ${k.nota.sumOf { it.nominal }}.\n" +
            "Tugas belum selesai: ${t.ifBlank { "tidak ada" }}.\nProker: ${p.ifBlank { "belum ada" }}.\nCatatan kas terbaru: ${kt.ifBlank { "kosong" }}."
    }

    private fun aksiDariJson(it: JSONObject): AksiAsisten? = when (it.optString("jenis")) {
        "kas" -> AksiAsisten.CatatKas(it.optBoolean("masuk", true), it.optLong("jumlah"), it.optString("kategori"), it.optString("keterangan"))
        "tugas" -> AksiAsisten.BuatTugas(it.optString("judul"), it.optString("deskripsi"), it.optString("untuk"), tenggatIso(it.optString("tenggat")))
        "pengumuman" -> AksiAsisten.BuatPengumuman(it.optString("judul"), it.optString("isi"))
        "inventaris" -> AksiAsisten.TambahBarang(it.optString("nama"), it.optInt("jumlah", 1).coerceAtLeast(1), it.optString("kondisi").ifBlank { "Baik" })
        "surat" -> AksiAsisten.CatatSurat(it.optString("jenisSurat") == "MASUK", it.optString("nomor"), it.optString("perihal"))
        else -> null
    }

    /**
     * Dipanggil dari thread IO. Mencoba tiap model dalam [modelCsv] berurutan; saat sibuk (503/429) diulang sekali
     * lalu pindah ke model berikutnya. Melempar exception berpesan ramah bila semua gagal.
     */
    fun tanyaGemini(
        teks: String, kunci: String, modelCsv: String, k: KonteksAsisten,
        riwayat: List<RiwayatItem>, lampiran: Lampiran?,
    ): HasilAsisten {
        val sistem = "Kamu \"Asisten KKN\", asisten pribadi yang cerdas, hangat, dan teliti untuk sekretaris dan bendahara KKN. Jawab dalam bahasa Indonesia yang jelas dan ringkas. Kamu boleh ngobrol, menjawab pertanyaan umum, menjelaskan, memberi saran, dan membantu mengelola data desa.\n" +
            "Balas HANYA JSON: {\"balasan\": string, \"aksi\": [objek...], \"ingat\": [string...]}.\n" +
            "\"aksi\" boleh kosong atau berisi banyak entri (maksimal 25) yang diusulkan untuk disimpan. Pengguna selalu mengonfirmasi, jadi usulkan bila data jelas. Jenis:\n" +
            "{\"jenis\":\"kas\",\"masuk\":bool,\"jumlah\":angka rupiah,\"kategori\":string,\"keterangan\":string}\n" +
            "{\"jenis\":\"tugas\",\"judul\":string,\"deskripsi\":string,\"untuk\":nama anggota atau \"\",\"tenggat\":\"YYYY-MM-DD\" atau \"\"}\n" +
            "{\"jenis\":\"pengumuman\",\"judul\":string,\"isi\":string}\n" +
            "{\"jenis\":\"inventaris\",\"nama\":string,\"jumlah\":angka,\"kondisi\":string}\n" +
            "{\"jenis\":\"surat\",\"jenisSurat\":\"MASUK\" atau \"KELUAR\",\"nomor\":string,\"perihal\":string}\n" +
            "Jika ada file: ringkas isinya, lakukan screening (cek duplikat, angka janggal, total tidak cocok, tanggal aneh, data kurang), lalu usulkan entri bila relevan.\n" +
            "\"ingat\": catatan singkat yang layak diingat jangka panjang tentang pengguna (kesulitan yang ia alami, kebiasaan, preferensi, konteks kegiatan). Kosongkan bila tidak ada hal baru.\n" +
            "Jawab pertanyaan data hanya dari DATA; jangan mengarang angka yang tidak ada. Bila ragu atau data kurang, tanyakan di \"balasan\".\n" +
            "INGATAN TENTANG PENGGUNA:\n" + (if (k.ingatan.isEmpty()) "(belum ada)" else k.ingatan.joinToString("\n") { "- $it" }) +
            "\nDATA DESA:\n" + konteksTeks(k)
        val isiTeks = teks + (lampiran?.teks?.let { "\n\n[ISI FILE ${lampiran.nama}]\n$it" } ?: "")
        val bagian = org.json.JSONArray().put(JSONObject().put("text", isiTeks))
        lampiran?.pdfBase64?.let { bagian.put(JSONObject().put("inlineData", JSONObject().put("mimeType", "application/pdf").put("data", it))) }
        val konten = org.json.JSONArray()
        riwayat.takeLast(12).forEach { konten.put(JSONObject().put("role", if (it.saya) "user" else "model").put("parts", org.json.JSONArray().put(JSONObject().put("text", it.teks)))) }
        konten.put(JSONObject().put("role", "user").put("parts", bagian))
        val badan = JSONObject()
            .put("systemInstruction", JSONObject().put("parts", org.json.JSONArray().put(JSONObject().put("text", sistem))))
            .put("contents", konten)
            .put("generationConfig", JSONObject().put("responseMimeType", "application/json").put("temperature", 0.4))
            .toString().toByteArray(Charsets.UTF_8)

        var galat = ""
        for (model in modelCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }) {
            for (percobaan in 0..1) {
                val c = URL("https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent").openConnection() as HttpURLConnection
                try {
                    c.requestMethod = "POST"; c.connectTimeout = 15000; c.readTimeout = 60000; c.doOutput = true
                    c.setRequestProperty("Content-Type", "application/json"); c.setRequestProperty("x-goog-api-key", kunci)
                    c.outputStream.use { it.write(badan) }
                    val kode = c.responseCode
                    val isi = (if (kode in 200..299) c.inputStream else c.errorStream)?.bufferedReader()?.use { it.readText() } ?: ""
                    if (kode in 200..299) {
                        val bag = JSONObject(isi).optJSONArray("candidates")?.optJSONObject(0)?.optJSONObject("content")?.optJSONArray("parts")
                        val sb = StringBuilder(); if (bag != null) for (i in 0 until bag.length()) sb.append(bag.getJSONObject(i).optString("text"))
                        if (sb.isEmpty()) { galat = "$model: jawaban kosong"; break }
                        val mentah = sb.toString().trim().removePrefix("```json").removeSuffix("```").trim()
                        val o = try { JSONObject(mentah) } catch (e: Exception) { JSONObject().put("balasan", mentah) }
                        val larik = o.optJSONArray("aksi")
                        val daftar = mutableListOf<AksiAsisten>()
                        if (larik != null) for (i in 0 until minOf(larik.length(), 25)) larik.optJSONObject(i)?.let { aksiDariJson(it) }?.let { daftar.add(it) }
                        else o.optJSONObject("aksi")?.let { aksiDariJson(it) }?.let { daftar.add(it) }
                        val ing = mutableListOf<String>(); o.optJSONArray("ingat")?.let { for (i in 0 until it.length()) ing.add(it.optString(i)) }
                        return HasilAsisten(o.optString("balasan", "Selesai."), daftar, ing)
                    }
                    val pesan = try { JSONObject(isi).getJSONObject("error").getString("message") } catch (e: Exception) { isi }
                    galat = "$model: $kode"
                    when {
                        kode in listOf(429, 500, 502, 503, 504) -> { if (percobaan == 0) Thread.sleep(1500); continue }
                        kode == 404 -> break
                        kode == 400 && pesan.contains("API key", true) -> throw IllegalStateException("Kunci API tidak valid. Periksa lagi kunci di pengaturan.")
                        kode == 401 || kode == 403 -> throw IllegalStateException("Kunci ditolak ($kode). Pastikan kunci dari aistudio.google.com/apikey.")
                        else -> throw IllegalStateException("$kode ${pesan.take(140)}")
                    }
                } catch (e: java.io.IOException) {
                    throw IllegalStateException("Tidak ada koneksi internet")
                } finally { c.disconnect() }
            }
        }
        throw IllegalStateException("Semua model sedang sibuk atau tidak tersedia ($galat)")
    }

    private fun tenggatIso(s: String): Long = try {
        if (s.isBlank()) 0L else (SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(s)!!.time + (23 * 60 + 59) * 60_000L)
    } catch (e: Exception) { 0L }
}

/** Membaca teks dari file unggahan. PDF dikirim utuh ke Gemini, jadi tidak lewat sini. */
object BacaFile {
    private fun dekode(s: String) = s.replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"").replace("&apos;", "'")
        .replace(Regex("&#(\\d+);")) { it.groupValues[1].toInt().toChar().toString() }.replace("&amp;", "&")

    private fun zip(bytes: ByteArray): Map<String, String> {
        val out = mutableMapOf<String, String>()
        java.util.zip.ZipInputStream(java.io.ByteArrayInputStream(bytes)).use { z ->
            var e = z.nextEntry
            while (e != null) {
                val n = e.name
                if (n == "word/document.xml" || n == "xl/sharedStrings.xml" || Regex("xl/worksheets/sheet\\d+\\.xml").matches(n)) out[n] = String(z.readBytes(), Charsets.UTF_8)
                e = z.nextEntry
            }
        }
        return out
    }

    fun teks(nama: String, bytes: ByteArray): String {
        val n = nama.lowercase()
        return when {
            n.endsWith(".csv") || n.endsWith(".txt") -> String(bytes, Charsets.UTF_8).take(40000)
            n.endsWith(".docx") -> {
                val x = zip(bytes)["word/document.xml"] ?: throw IllegalStateException("Isi Word tidak ditemukan")
                dekode(x.replace("</w:p>", "\n").replace("</w:tc>", " | ").replace("<w:tab/>", "\t").replace(Regex("<[^>]+>"), ""))
                    .replace(Regex("\n{3,}"), "\n\n").take(40000)
            }
            n.endsWith(".xlsx") -> {
                val z = zip(bytes)
                val ss = Regex("(?s)<si>(.*?)</si>").findAll(z["xl/sharedStrings.xml"] ?: "").map { m ->
                    dekode(Regex("(?s)<t[^>]*>(.*?)</t>").findAll(m.groupValues[1]).joinToString("") { it.groupValues[1] })
                }.toList()
                val sb = StringBuilder()
                for (k in z.keys.filter { it.contains("worksheets") }.sorted()) {
                    sb.append("\n# Lembar ").append(Regex("sheet(\\d+)").find(k)?.groupValues?.get(1)).append("\n")
                    var baris = 0
                    for (r in Regex("(?s)<row[^>]*>(.*?)</row>").findAll(z[k] ?: "")) {
                        if (baris++ > 300) break
                        val sel = Regex("(?s)<c ([^>]*?)(?:/>|>(.*?)</c>)").findAll(r.groupValues[1]).map { c ->
                            val tp = Regex("t=\"(\\w+)\"").find(c.groupValues[1])?.groupValues?.get(1)
                            val isi = c.groupValues[2]
                            val v = Regex("(?s)<v>(.*?)</v>").find(isi)?.groupValues?.get(1)
                            val tx = Regex("(?s)<t[^>]*>(.*?)</t>").find(isi)?.groupValues?.get(1)
                            when (tp) { "s" -> ss.getOrNull(v?.toIntOrNull() ?: -1) ?: ""; "inlineStr" -> dekode(tx ?: ""); else -> dekode(v ?: "") }
                        }.toList()
                        sb.append(sel.joinToString("\t").trimEnd('\t')).append("\n")
                    }
                }
                sb.toString().take(40000)
            }
            n.endsWith(".xls") -> throw IllegalStateException("Format .xls lama belum bisa dibaca. Simpan sebagai .xlsx atau .csv lalu unggah lagi.")
            else -> throw IllegalStateException("Format belum didukung. Pakai PDF, .docx, .xlsx, CSV, atau TXT.")
        }
    }
}

/** Penyimpanan lokal asisten: kunci, model, riwayat obrolan, ingatan, posisi bola. */
object AsistenStore {
    private fun p(c: android.content.Context) = c.getSharedPreferences("asisten_prefs", android.content.Context.MODE_PRIVATE)
    fun kunci(c: android.content.Context) = p(c).getString("gemini", "") ?: ""
    fun model(c: android.content.Context): String {
        val m = p(c).getString("model", "") ?: ""
        return if (m.isBlank() || m == "gemini-flash-latest") MODEL_BAWAAN else m
    }
    fun simpanPengaturan(c: android.content.Context, kunci: String, model: String) =
        p(c).edit().putString("gemini", kunci.trim()).putString("model", model.trim().ifBlank { MODEL_BAWAAN }).apply()

    private fun daftar(c: android.content.Context, k: String): List<String> {
        val a = try { org.json.JSONArray(p(c).getString(k, "[]")) } catch (e: Exception) { org.json.JSONArray() }
        return (0 until a.length()).map { a.optString(it) }
    }
    private fun simpanDaftar(c: android.content.Context, k: String, l: List<String>) { p(c).edit().putString(k, org.json.JSONArray(l).toString()).apply() }

    fun ingatan(c: android.content.Context) = daftar(c, "ingatan")
    fun tambahIngatan(c: android.content.Context, baru: List<String>) {
        val a = ingatan(c).toMutableList()
        baru.map { it.trim().take(200) }.filter { it.isNotEmpty() }.forEach { t -> if (a.none { it.equals(t, true) }) a.add(t) }
        simpanDaftar(c, "ingatan", a.takeLast(40))
    }
    fun hapusIngatan(c: android.content.Context, i: Int) { val a = ingatan(c).toMutableList(); if (i in a.indices) { a.removeAt(i); simpanDaftar(c, "ingatan", a) } }

    fun riwayat(c: android.content.Context): List<RiwayatItem> = daftar(c, "riwayat").map { RiwayatItem(it.startsWith("u|"), it.drop(2)) }
    fun tambahRiwayat(c: android.content.Context, saya: Boolean, teks: String) {
        simpanDaftar(c, "riwayat", (daftar(c, "riwayat") + ((if (saya) "u|" else "b|") + teks.take(1500))).takeLast(40))
    }
    fun hapusRiwayat(c: android.content.Context) = simpanDaftar(c, "riwayat", emptyList())

    fun kiri(c: android.content.Context) = p(c).getBoolean("bola_kiri", false)
    fun yFrac(c: android.content.Context) = p(c).getFloat("bola_y", 0.55f)
    fun mini(c: android.content.Context) = p(c).getBoolean("bola_mini", false)
    fun simpanBola(c: android.content.Context, kiri: Boolean, y: Float) { p(c).edit().putBoolean("bola_kiri", kiri).putFloat("bola_y", y).apply() }
    fun simpanMini(c: android.content.Context, m: Boolean) { p(c).edit().putBoolean("bola_mini", m).apply() }
}

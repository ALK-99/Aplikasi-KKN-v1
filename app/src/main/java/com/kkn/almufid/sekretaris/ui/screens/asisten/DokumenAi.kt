package com.kkn.almufid.sekretaris.ui.screens.asisten

import android.content.ContentValues
import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kkn.almufid.sekretaris.ui.screens.laporan.BlokDocx
import com.kkn.almufid.sekretaris.ui.screens.laporan.DocxWriter
import com.kkn.almufid.sekretaris.ui.screens.laporan.MIME_DOCX
import com.kkn.almufid.sekretaris.ui.screens.laporan.PembuatLaporan
import com.kkn.almufid.sekretaris.ui.theme.TeksRedup
import com.kkn.almufid.sekretaris.ui.theme.TeksUtama
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

const val MIME_XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
const val MIME_PDF = "application/pdf"

/** Dokumen yang diminta pengguna dan disusun AI: surat/laporan (docx, pdf) atau tabel (xlsx). */
class DataDokumen(
    val format: String,
    val nama: String,
    val kop: Boolean,
    val blok: List<BlokDocx>,
    val lembar: List<Pair<String, List<List<String>>>>,
)

object DokumenAi {
    private fun rata(s: String) = when (s.lowercase()) { "center", "right" -> s.lowercase(); "justify" -> "both"; else -> "left" }

    private fun barisDari(a: JSONArray?): List<List<String>> =
        if (a == null) emptyList() else (0 until a.length()).map { i ->
            val r = a.optJSONArray(i)
            if (r == null) listOf(a.opt(i)?.toString().orEmpty())
            else (0 until r.length()).map { j -> r.opt(j)?.let { if (it == JSONObject.NULL) "" else it.toString() }.orEmpty() }
        }

    fun dariJson(o: JSONObject): DataDokumen {
        val fm = o.optString("format", "docx").lowercase().let { if (it in listOf("docx", "pdf", "xlsx")) it else "docx" }
        val blok = mutableListOf<BlokDocx>()
        val b = o.optJSONArray("blok")
        if (b != null) for (i in 0 until b.length()) {
            val x = b.optJSONObject(i) ?: continue
            if (x.optString("t") == "tabel") blok.add(BlokDocx.Tabel(barisDari(x.optJSONArray("baris")), x.optBoolean("kepala", true)))
            else blok.add(BlokDocx.Paragraf(x.optString("teks"), x.optBoolean("tebal", false), rata(x.optString("rata")), 24))
        }
        val lembar = mutableListOf<Pair<String, List<List<String>>>>()
        val l = o.optJSONArray("lembar")
        if (l != null) for (i in 0 until minOf(l.length(), 8)) {
            val x = l.optJSONObject(i) ?: continue
            lembar.add(x.optString("nama", "Lembar${i + 1}") to barisDari(x.optJSONArray("baris")))
        }
        return DataDokumen(fm, o.optString("nama", "dokumen"), o.optBoolean("kop", false), blok, lembar)
    }

    fun namaAman(s: String) = s.replace(Regex("[^A-Za-z0-9\\- ]"), "").trim().replace(Regex("\\s+"), "-").take(60).ifBlank { "dokumen" }

    private fun blokLengkap(d: DataDokumen, desa: String): List<BlokDocx> {
        if (!d.kop) return d.blok
        val kop = listOf(
            BlokDocx.Paragraf("KELOMPOK KKN AL-MUSHLIH 2027", true, "center", 28),
            BlokDocx.Paragraf(if (desa.isNotBlank()) "Desa $desa" else "", false, "center", 24),
            BlokDocx.Paragraf("______________________________________________", false, "center", 20),
        )
        return kop + d.blok
    }

    /** Membuat berkas di folder cache (jalankan di thread IO). */
    fun bangun(context: Context, d: DataDokumen, desa: String, fmt: String): File {
        val dir = File(context.cacheDir, "laporan").apply { mkdirs() }
        val nama = namaAman(d.nama)
        return when (fmt) {
            "xlsx" -> File(dir, "$nama.xlsx").also { tulisXlsx(it, d) }
            "pdf" -> File(dir, "$nama.pdf").also { tulisPdf(it, blokLengkap(d, desa)) }
            else -> File(dir, "$nama.docx").also { DocxWriter.tulis(it, blokLengkap(d, desa)) }
        }
    }

    fun mimeDari(f: File) = when (f.extension) { "xlsx" -> MIME_XLSX; "pdf" -> MIME_PDF; else -> MIME_DOCX }

    /** Salin ke folder Unduhan (Android 10+). Mengembalikan true bila berhasil. */
    fun simpanKeUnduhan(context: Context, f: File): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
        return try {
            val v = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, f.name)
                put(MediaStore.Downloads.MIME_TYPE, mimeDari(f))
                put(MediaStore.Downloads.RELATIVE_PATH, "Download/")
            }
            val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, v) ?: return false
            context.contentResolver.openOutputStream(uri)?.use { out -> f.inputStream().use { it.copyTo(out) } } ?: return false
            true
        } catch (e: Exception) { false }
    }

    // ---------- XLSX ----------
    private fun esc(s: String) = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
        .replace(Regex("[\\u0000-\\u0008\\u000B\\u000C\\u000E-\\u001F]"), "")

    private fun kol(i0: Int): String { var i = i0 + 1; var s = ""; while (i > 0) { s = ('A' + (i - 1) % 26) + s; i = (i - 1) / 26 }; return s }

    private fun tulisXlsx(f: File, d: DataDokumen) {
        val ls = d.lembar.ifEmpty { listOf("Data" to emptyList()) }
        val h = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
        ZipOutputStream(f.outputStream().buffered()).use { z ->
            fun taruh(nama: String, isi: String) { z.putNextEntry(ZipEntry(nama)); z.write(isi.toByteArray()); z.closeEntry() }
            taruh("[Content_Types].xml", h + "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\"><Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/><Default Extension=\"xml\" ContentType=\"application/xml\"/><Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>" +
                ls.indices.joinToString("") { "<Override PartName=\"/xl/worksheets/sheet${it + 1}.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>" } + "</Types>")
            taruh("_rels/.rels", h + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/></Relationships>")
            taruh("xl/workbook.xml", h + "<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"><sheets>" +
                ls.mapIndexed { i, l -> "<sheet name=\"${esc(l.first.replace(Regex("[\\\\/?*\\[\\]:]"), " ").take(31).ifBlank { "Lembar${i + 1}" })}\" sheetId=\"${i + 1}\" r:id=\"rId${i + 1}\"/>" }.joinToString("") + "</sheets></workbook>")
            taruh("xl/_rels/workbook.xml.rels", h + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">" +
                ls.indices.joinToString("") { "<Relationship Id=\"rId${it + 1}\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet${it + 1}.xml\"/>" } + "</Relationships>")
            ls.forEachIndexed { i, l ->
                val sb = StringBuilder(h + "<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><sheetData>")
                l.second.forEachIndexed { ri, r ->
                    sb.append("<row r=\"${ri + 1}\">")
                    r.forEachIndexed { ci, c ->
                        val ref = kol(ci) + (ri + 1)
                        val angka = c.trim().takeIf { it.matches(Regex("-?(0|[1-9]\\d*)(\\.\\d+)?")) }
                        if (angka != null) sb.append("<c r=\"$ref\"><v>$angka</v></c>")
                        else sb.append("<c r=\"$ref\" t=\"inlineStr\"><is><t xml:space=\"preserve\">${esc(c)}</t></is></c>")
                    }
                    sb.append("</row>")
                }
                taruh("xl/worksheets/sheet${i + 1}.xml", sb.append("</sheetData></worksheet>").toString())
            }
        }
    }

    // ---------- PDF ----------
    private fun tulisPdf(f: File, blok: List<BlokDocx>) {
        val lebar = 595; val tinggi = 842; val m = 56f
        val isi = lebar - 2 * m
        val doc = PdfDocument()
        var nomor = 0; var halaman: PdfDocument.Page? = null; var y = 0f
        fun baru() { halaman?.let { doc.finishPage(it) }; nomor++; halaman = doc.startPage(PdfDocument.PageInfo.Builder(lebar, tinggi, nomor).create()); y = m }
        baru()
        fun cat(tebal: Boolean, ukuran: Float) = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(Typeface.SERIF, if (tebal) Typeface.BOLD else Typeface.NORMAL); textSize = ukuran; color = Color.BLACK
        }
        fun tata(t: String, p: TextPaint, w: Int, al: Layout.Alignment) = StaticLayout.Builder.obtain(t, 0, t.length, p, w).setAlignment(al).setLineSpacing(0f, 1.15f).build()
        for (b in blok) {
            when (b) {
                is BlokDocx.Paragraf -> {
                    val p = cat(b.tebal, b.ukuran / 2f)
                    val al = when (b.rata) { "center" -> Layout.Alignment.ALIGN_CENTER; "right" -> Layout.Alignment.ALIGN_OPPOSITE; else -> Layout.Alignment.ALIGN_NORMAL }
                    val teks = b.teks.ifEmpty { " " }
                    val lay = tata(teks, p, isi.toInt(), al)
                    for (i in 0 until lay.lineCount) {
                        val lh = (lay.getLineBottom(i) - lay.getLineTop(i)).toFloat()
                        if (y + lh > tinggi - m) baru()
                        val s = lay.getLineStart(i); val e = lay.getLineEnd(i)
                        val baris = teks.substring(s, e).trimEnd('\n', ' ')
                        val lb = p.measureText(baris)
                        val x = when (al) { Layout.Alignment.ALIGN_CENTER -> m + (isi - lb) / 2; Layout.Alignment.ALIGN_OPPOSITE -> m + isi - lb; else -> m }
                        halaman!!.canvas.drawText(baris, x, y + lay.getLineBaseline(i) - lay.getLineTop(i), p)
                        y += lh
                    }
                    y += 6f
                }
                is BlokDocx.Tabel -> {
                    val kolom = b.baris.maxOfOrNull { it.size } ?: 0
                    if (kolom == 0) continue
                    val lc = isi / kolom
                    b.baris.forEachIndexed { ri, r ->
                        val head = b.kepala && ri == 0
                        val p = cat(head, 10f)
                        val lays = (0 until kolom).map { tata(r.getOrElse(it) { "" }.ifEmpty { " " }, p, (lc - 8).toInt(), Layout.Alignment.ALIGN_NORMAL) }
                        val tr = (lays.maxOf { it.height } + 8).toFloat()
                        if (y + tr > tinggi - m) baru()
                        val c = halaman!!.canvas
                        val garis = Paint().apply { style = Paint.Style.STROKE; color = Color.DKGRAY; strokeWidth = 0.7f }
                        if (head) c.drawRect(m, y, m + isi, y + tr, Paint().apply { color = Color.rgb(233, 233, 242) })
                        lays.forEachIndexed { ci, lay ->
                            c.drawRect(m + ci * lc, y, m + (ci + 1) * lc, y + tr, garis)
                            c.save(); c.translate(m + ci * lc + 4, y + 4); lay.draw(c); c.restore()
                        }
                        y += tr
                    }
                    y += 10f
                }
            }
        }
        halaman?.let { doc.finishPage(it) }
        f.outputStream().use { doc.writeTo(it) }
        doc.close()
    }
}

/** Kartu di gelembung chat: pilih format, berkas dibuat, otomatis masuk folder Unduhan, bisa dibagikan. */
@Composable
fun KartuDokumen(a: AksiAsisten.BuatDokumen, namaDesa: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pesan by remember { mutableStateOf("") }
    var berkas by remember { mutableStateOf<File?>(null) }
    var sibuk by remember { mutableStateOf(false) }
    fun buat(fmt: String) {
        if (sibuk) return
        sibuk = true; pesan = "Menyusun berkas…"
        scope.launch {
            try {
                val f = withContext(Dispatchers.IO) { DokumenAi.bangun(context, a.data, namaDesa, fmt) }
                berkas = f
                val ok = withContext(Dispatchers.IO) { DokumenAi.simpanKeUnduhan(context, f) }
                if (ok) pesan = "Tersimpan di folder Unduhan: ${f.name}"
                else { pesan = "Berkas siap. Pilih tempat simpan lewat jendela berikut."; PembuatLaporan.bagikan(context, f, DokumenAi.mimeDari(f)) }
            } catch (e: Exception) { pesan = "Gagal membuat berkas: ${e.message?.take(100)}" }
            sibuk = false
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("📄 " + AsistenParser.ringkas(a), color = TeksUtama, fontSize = 14.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (a.data.format == "xlsx") Button(onClick = { buat("xlsx") }, enabled = !sibuk) { Text("Unduh Excel") }
            else {
                Button(onClick = { buat("docx") }, enabled = !sibuk) { Text("Unduh Word") }
                OutlinedButton(onClick = { buat("pdf") }, enabled = !sibuk) { Text("Unduh PDF") }
            }
        }
        if (pesan.isNotBlank()) Text(pesan, color = TeksRedup, fontSize = 12.sp)
        berkas?.let { f -> OutlinedButton(onClick = { PembuatLaporan.bagikan(context, f, DokumenAi.mimeDari(f)) }) { Text("Bagikan / kirim") } }
    }
}

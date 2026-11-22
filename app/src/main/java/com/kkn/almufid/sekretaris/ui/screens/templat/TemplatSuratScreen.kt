package com.kkn.almufid.sekretaris.ui.screens.templat

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kkn.almufid.sekretaris.data.cloud.DesaRepository
import com.kkn.almufid.sekretaris.data.entity.JenisSurat
import com.kkn.almufid.sekretaris.ui.components.KartuGelap
import com.kkn.almufid.sekretaris.ui.screens.laporan.PembuatLaporan
import com.kkn.almufid.sekretaris.ui.theme.*
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class JenisTemplat(val judul: String, val perihal: String) {
    UNDANGAN("Undangan", "Undangan"),
    PERMOHONAN("Permohonan", "Permohonan"),
    PEMBERITAHUAN("Pemberitahuan", "Pemberitahuan"),
    TERIMA_KASIH("Terima Kasih", "Ucapan Terima Kasih"),
}

private class IsiSurat(
    val jenis: JenisTemplat, val nomor: String, val lampiran: String, val perihal: String,
    val tujuan: String, val tempatTanggal: String, val inti: String,
    val hari: String, val waktu: String, val lokasi: String,
    val ketua: String, val sekretaris: String, val desa: String,
)

private fun rakit(s: IsiSurat): List<String> {
    val paragraf = mutableListOf<String>()
    paragraf += "Assalamu'alaikum Warahmatullahi Wabarakatuh."
    when (s.jenis) {
        JenisTemplat.UNDANGAN -> {
            paragraf += "Dengan hormat, kami mengundang Bapak/Ibu/Saudara/i untuk hadir pada kegiatan berikut."
            if (s.inti.isNotBlank()) paragraf += s.inti
            paragraf += "Hari/tanggal : ${s.hari.ifBlank { "-" }}\nWaktu : ${s.waktu.ifBlank { "-" }}\nTempat : ${s.lokasi.ifBlank { "-" }}"
            paragraf += "Demikian undangan ini kami sampaikan. Atas kehadiran dan perhatiannya kami ucapkan terima kasih."
        }
        JenisTemplat.PERMOHONAN -> {
            paragraf += "Dengan hormat, melalui surat ini kami bermaksud mengajukan permohonan sebagai berikut."
            if (s.inti.isNotBlank()) paragraf += s.inti
            if (s.hari.isNotBlank() || s.lokasi.isNotBlank()) paragraf += "Waktu pelaksanaan : ${s.hari.ifBlank { "-" }}\nTempat : ${s.lokasi.ifBlank { "-" }}"
            paragraf += "Demikian permohonan ini kami sampaikan. Besar harapan kami agar permohonan ini dapat dikabulkan. Atas perhatian dan kerja samanya kami ucapkan terima kasih."
        }
        JenisTemplat.PEMBERITAHUAN -> {
            paragraf += "Dengan hormat, melalui surat ini kami memberitahukan hal berikut."
            if (s.inti.isNotBlank()) paragraf += s.inti
            if (s.hari.isNotBlank() || s.lokasi.isNotBlank()) paragraf += "Hari/tanggal : ${s.hari.ifBlank { "-" }}\nTempat : ${s.lokasi.ifBlank { "-" }}"
            paragraf += "Demikian pemberitahuan ini kami sampaikan. Atas perhatiannya kami ucapkan terima kasih."
        }
        JenisTemplat.TERIMA_KASIH -> {
            paragraf += "Dengan hormat, kami mengucapkan terima kasih yang sebesar-besarnya atas bantuan dan dukungan yang telah diberikan."
            if (s.inti.isNotBlank()) paragraf += s.inti
            paragraf += "Semoga kebaikan Bapak/Ibu/Saudara/i dibalas dengan pahala yang berlipat ganda. Demikian ucapan terima kasih ini kami sampaikan."
        }
    }
    paragraf += "Wassalamu'alaikum Warahmatullahi Wabarakatuh."
    return paragraf
}

/** Menggambar surat resmi A4 (kop, nomor, isi, tanda tangan Ketua dan Sekretaris). */
private fun buatPdf(context: Context, s: IsiSurat): File {
    val doc = PdfDocument()
    val lebar = 595; val tinggi = 842; val m = 56f
    val isiLebar = (lebar - 2 * m).toInt()
    val halaman = doc.startPage(PdfDocument.PageInfo.Builder(lebar, tinggi, 1).create())
    val c = halaman.canvas
    var y = 48f

    val tebal = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 15f; typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD); color = 0xFF000000.toInt() }
    val sedang = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 11f; typeface = Typeface.SERIF; color = 0xFF000000.toInt() }
    val isi = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 12f; typeface = Typeface.SERIF; color = 0xFF000000.toInt() }
    val garis = Paint().apply { color = 0xFF000000.toInt(); strokeWidth = 2f }

    fun tengah(t: String, p: TextPaint) { c.drawText(t, (lebar - p.measureText(t)) / 2f, y, p); y += p.textSize + 4 }
    fun blok(t: String, p: TextPaint = isi, gapSesudah: Float = 8f) {
        val sl = StaticLayout.Builder.obtain(t, 0, t.length, p, isiLebar)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL).setLineSpacing(2f, 1f).build()
        c.save(); c.translate(m, y); sl.draw(c); c.restore()
        y += sl.height + gapSesudah
    }

    tengah("KELOMPOK KKN AL-MUSHLIH 2027", tebal)
    tengah(if (s.desa.isBlank()) "Sekretariat Posko KKN" else "Desa ${s.desa}", sedang)
    y += 2
    c.drawLine(m, y, lebar - m, y, garis); y += 3
    garis.strokeWidth = 0.8f; c.drawLine(m, y, lebar - m, y, garis); y += 18

    val kanan = lebar - m
    val tt = s.tempatTanggal
    c.drawText(tt, kanan - isi.measureText(tt), y, isi); y += 20
    blok("Nomor     : ${s.nomor.ifBlank { "-" }}\nLampiran : ${s.lampiran.ifBlank { "-" }}\nPerihal    : ${s.perihal}", isi, 14f)
    blok("Kepada Yth.\n${s.tujuan.ifBlank { "-" }}\ndi tempat", isi, 14f)
    rakit(s).forEach { blok(it) }

    y += 10
    val kiriX = m + 10f
    val kananX = lebar - m - 150f
    c.drawText("Ketua,", kiriX, y, isi); c.drawText("Sekretaris,", kananX, y, isi)
    y += 64
    c.drawText(s.ketua.ifBlank { "(................)" }, kiriX, y, isi)
    c.drawText(s.sekretaris.ifBlank { "(................)" }, kananX, y, isi)
    doc.finishPage(halaman)

    val folder = File(context.cacheDir, "laporan").apply { mkdirs() }
    val nama = "Surat_${s.jenis.judul.replace(" ", "")}_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.pdf"
    val file = File(folder, nama)
    file.outputStream().use { doc.writeTo(it) }
    doc.close()
    return file
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplatSuratScreen(desa: DesaRepository) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var jenis by remember { mutableStateOf(JenisTemplat.UNDANGAN) }
    var nomor by remember { mutableStateOf("") }
    var lampiran by remember { mutableStateOf("-") }
    var perihal by remember { mutableStateOf("") }
    var tujuan by remember { mutableStateOf("") }
    var tempatTanggal by remember {
        mutableStateOf("Yogyakarta, " + SimpleDateFormat("d MMMM yyyy", Locale("id", "ID")).format(Date()))
    }
    var inti by remember { mutableStateOf("") }
    var hari by remember { mutableStateOf("") }
    var waktu by remember { mutableStateOf("") }
    var lokasi by remember { mutableStateOf("") }
    var ketua by remember { mutableStateOf("") }
    var sekretaris by remember { mutableStateOf("") }
    var namaDesa by remember { mutableStateOf("") }
    var catat by remember { mutableStateOf(true) }
    var pesan by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp)) {
        Text("Pilih templat, isi data, lalu buat PDF surat resmi (kop, nomor, isi, tanda tangan).", color = TeksSoft, fontSize = 13.sp)
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            JenisTemplat.values().take(2).forEach {
                FilterChip(selected = jenis == it, onClick = { jenis = it }, label = { Text(it.judul) })
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            JenisTemplat.values().drop(2).forEach {
                FilterChip(selected = jenis == it, onClick = { jenis = it }, label = { Text(it.judul) })
            }
        }
        Spacer(Modifier.height(8.dp))
        KartuGelap(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp)) {
                Kolom("Nama desa (untuk kop)", namaDesa, { namaDesa = it })
                Kolom("Nomor surat", nomor, { nomor = it })
                Kolom("Lampiran", lampiran, { lampiran = it })
                Kolom("Perihal (kosong = ${jenis.perihal})", perihal, { perihal = it })
                Kolom("Kepada Yth. (nama / jabatan)", tujuan, { tujuan = it }, 2)
                Kolom("Tempat, tanggal", tempatTanggal, { tempatTanggal = it })
                Kolom("Isi inti / keterangan", inti, { inti = it }, 3)
                if (jenis != JenisTemplat.TERIMA_KASIH) {
                    Kolom(if (jenis == JenisTemplat.UNDANGAN) "Hari / tanggal acara" else "Hari / tanggal", hari, { hari = it })
                    if (jenis == JenisTemplat.UNDANGAN) Kolom("Waktu", waktu, { waktu = it })
                    Kolom("Tempat", lokasi, { lokasi = it })
                }
                Kolom("Nama Ketua", ketua, { ketua = it })
                Kolom("Nama Sekretaris", sekretaris, { sekretaris = it })
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Switch(checked = catat, onCheckedChange = { catat = it })
            Spacer(Modifier.width(10.dp))
            Text("Catat juga sebagai surat keluar", color = TeksUtama, fontSize = 13.sp)
        }
        Spacer(Modifier.height(10.dp))
        Button(
            onClick = {
                if (tujuan.isBlank()) { pesan = "Isi dulu bagian Kepada Yth."; return@Button }
                val judulPerihal = perihal.ifBlank { jenis.perihal }
                val isiSurat = IsiSurat(jenis, nomor, lampiran, judulPerihal, tujuan, tempatTanggal, inti, hari, waktu, lokasi, ketua, sekretaris, namaDesa)
                try {
                    val file = buatPdf(context, isiSurat)
                    PembuatLaporan.bagikan(context, file, "application/pdf")
                    pesan = "PDF dibuat. Pilih tujuan simpan/kirim."
                    if (catat) scope.launch { try { desa.tambahSurat(JenisSurat.KELUAR, nomor, judulPerihal, tujuan.lines().first()) } catch (e: Exception) { } }
                } catch (e: Exception) {
                    pesan = "Gagal membuat PDF: ${e.message}"
                }
            },
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) { Text("Buat PDF Surat", fontWeight = FontWeight.Bold) }
        pesan?.let { Text(it, color = Gold, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp)) }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun Kolom(label: String, nilai: String, ubah: (String) -> Unit, baris: Int = 1) {
    OutlinedTextField(
        value = nilai, onValueChange = ubah, label = { Text(label) },
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        minLines = baris, maxLines = if (baris > 1) 8 else 1, singleLine = baris == 1,
    )
}

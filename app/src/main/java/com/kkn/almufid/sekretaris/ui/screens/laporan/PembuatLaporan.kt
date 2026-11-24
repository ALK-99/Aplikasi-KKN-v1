package com.kkn.almufid.sekretaris.ui.screens.laporan

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import java.io.File

/** Pembuat berkas laporan (PDF, CSV, JSON) tanpa library tambahan: murni API bawaan Android. */
object PembuatLaporan {

    private fun folder(context: Context): File = File(context.cacheDir, "laporan").apply { mkdirs() }

    fun csv(context: Context, namaFile: String, header: List<String>, isi: List<List<String>>): File {
        fun kutip(s: String) = "\"" + s.replace("\"", "\"\"") + "\""
        val sb = StringBuilder("﻿") // BOM supaya Excel membaca UTF-8 dengan benar
        sb.append(header.joinToString(",") { kutip(it) }).append("\r\n")
        isi.forEach { baris -> sb.append(baris.joinToString(",") { kutip(it) }).append("\r\n") }
        return File(folder(context), namaFile).apply { writeText(sb.toString(), Charsets.UTF_8) }
    }

    fun teks(context: Context, namaFile: String, isi: String): File =
        File(folder(context), namaFile).apply { writeText(isi, Charsets.UTF_8) }

    /** Bila true, laporan "PDF" dibuat sebagai berkas Word (.docx) supaya bisa disunting dulu. */
    @Volatile var formatWord: Boolean = false

    fun docx(context: Context, namaFile: String, judul: String, subjudul: String, baris: List<String>): File {
        val file = File(folder(context), namaFile.removeSuffix(".pdf").removeSuffix(".docx") + ".docx")
        DocxWriter.tulis(file, DocxWriter.dariBaris(judul, subjudul, baris), mendatar = false)
        return file
    }

    fun pdf(context: Context, namaFile: String, judul: String, subjudul: String, baris: List<String>): File {
        if (formatWord) return docx(context, namaFile, judul, subjudul, baris)
        val doc = PdfDocument()
        val penulis = PenulisPdf(doc)
        penulis.halamanBaru()
        penulis.tulis(judul, penulis.catJudul)
        penulis.tulis(subjudul, penulis.catSub)
        penulis.garis()
        baris.forEach { penulis.tulis(it, penulis.catIsi) }
        penulis.selesai()
        val file = File(folder(context), namaFile)
        file.outputStream().use { doc.writeTo(it) }
        doc.close()
        return file
    }

    fun bagikan(context: Context, file: File, mime: String) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val kirim = Intent(Intent.ACTION_SEND)
            .setType(mime)
            .putExtra(Intent.EXTRA_STREAM, uri)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(
            Intent.createChooser(kirim, "Bagikan / simpan berkas").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    private class PenulisPdf(private val doc: PdfDocument) {
        private val lebar = 595
        private val tinggi = 842
        private val margin = 40f
        private var nomor = 0
        private var halaman: PdfDocument.Page? = null
        private var y = 0f

        val catJudul = Paint().apply { textSize = 16f; isFakeBoldText = true; isAntiAlias = true }
        val catSub = Paint().apply { textSize = 10f; color = Color.DKGRAY; isAntiAlias = true }
        val catIsi = Paint().apply { textSize = 11f; isAntiAlias = true }

        fun halamanBaru() {
            halaman?.let { doc.finishPage(it) }
            nomor += 1
            halaman = doc.startPage(PdfDocument.PageInfo.Builder(lebar, tinggi, nomor).create())
            y = margin + 16f
        }

        fun selesai() { halaman?.let { doc.finishPage(it) }; halaman = null }

        fun garis() {
            halaman?.canvas?.drawLine(margin, y, lebar - margin, y, catSub)
            y += 16f
        }

        fun tulis(teks: String, cat: Paint) {
            val lebarMax = lebar - 2 * margin
            for (paragraf in teks.split("\n")) {
                var sisa = paragraf
                if (sisa.isEmpty()) { y += cat.textSize + 4f; continue }
                while (sisa.isNotEmpty()) {
                    if (y > tinggi - margin - 10f) halamanBaru()
                    val muat = cat.breakText(sisa, true, lebarMax, null)
                    if (muat <= 0) break
                    val potong = if (muat < sisa.length) {
                        val spasi = sisa.lastIndexOf(' ', muat - 1)
                        if (spasi > 0) spasi + 1 else muat
                    } else muat
                    halaman?.canvas?.drawText(sisa.substring(0, potong).trimEnd(), margin, y, cat)
                    y += cat.textSize + 4f
                    sisa = sisa.substring(potong)
                }
            }
            y += 4f
        }
    }
}

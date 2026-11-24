package com.kkn.almufid.sekretaris.ui.screens.laporan

import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

const val MIME_DOCX = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"

sealed class BlokDocx {
    data class Paragraf(val teks: String, val tebal: Boolean = false, val rata: String = "left", val ukuran: Int = 22) : BlokDocx()
    data class Tabel(val baris: List<List<String>>, val kepala: Boolean = false) : BlokDocx()
}

/** Penulis .docx minimal tanpa library: hasilnya bisa dibuka dan disunting di Microsoft Word, WPS, atau Google Docs. */
object DocxWriter {
    private fun esc(s: String) = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
        .replace(Regex("[\\u0000-\\u0008\\u000B\\u000C\\u000E-\\u001F]"), "")

    private fun run(teks: String, tebal: Boolean, ukuran: Int): String {
        val bagian = teks.split("\n")
        val sb = StringBuilder("<w:r><w:rPr>")
        if (tebal) sb.append("<w:b/>")
        sb.append("<w:sz w:val=\"$ukuran\"/></w:rPr>")
        bagian.forEachIndexed { i, b ->
            if (i > 0) sb.append("<w:br/>")
            sb.append("<w:t xml:space=\"preserve\">").append(esc(b)).append("</w:t>")
        }
        return sb.append("</w:r>").toString()
    }

    private fun paragraf(p: BlokDocx.Paragraf) =
        "<w:p><w:pPr><w:spacing w:after=\"120\"/><w:jc w:val=\"${p.rata}\"/></w:pPr>${run(p.teks, p.tebal, p.ukuran)}</w:p>"

    private fun tabel(t: BlokDocx.Tabel): String {
        val b = "<w:top w:val=\"single\" w:sz=\"4\" w:color=\"444444\"/><w:left w:val=\"single\" w:sz=\"4\" w:color=\"444444\"/>" +
            "<w:bottom w:val=\"single\" w:sz=\"4\" w:color=\"444444\"/><w:right w:val=\"single\" w:sz=\"4\" w:color=\"444444\"/>" +
            "<w:insideH w:val=\"single\" w:sz=\"4\" w:color=\"444444\"/><w:insideV w:val=\"single\" w:sz=\"4\" w:color=\"444444\"/>"
        val sb = StringBuilder("<w:tbl><w:tblPr><w:tblW w:w=\"5000\" w:type=\"pct\"/><w:tblBorders>$b</w:tblBorders></w:tblPr>")
        t.baris.forEachIndexed { i, r ->
            sb.append("<w:tr>")
            r.forEach { c ->
                sb.append("<w:tc><w:tcPr>")
                if (t.kepala && i == 0) sb.append("<w:shd w:val=\"clear\" w:color=\"auto\" w:fill=\"E9E9F2\"/>")
                sb.append("</w:tcPr><w:p>").append(run(c, t.kepala && i == 0, 20)).append("</w:p></w:tc>")
            }
            sb.append("</w:tr>")
        }
        return sb.append("</w:tbl><w:p/>").toString()
    }

    fun tulis(file: File, blok: List<BlokDocx>, mendatar: Boolean = false) {
        val badan = StringBuilder()
        blok.forEach {
            badan.append(when (it) { is BlokDocx.Paragraf -> paragraf(it); is BlokDocx.Tabel -> tabel(it) })
        }
        val halaman = if (mendatar) "<w:pgSz w:w=\"16838\" w:h=\"11906\" w:orient=\"landscape\"/>" else "<w:pgSz w:w=\"11906\" w:h=\"16838\"/>"
        val dokumen = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
            "<w:document xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\"><w:body>$badan" +
            "<w:sectPr>$halaman<w:pgMar w:top=\"1134\" w:right=\"1134\" w:bottom=\"1134\" w:left=\"1134\" w:header=\"708\" w:footer=\"708\" w:gutter=\"0\"/></w:sectPr></w:body></w:document>"
        val tipe = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">" +
            "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/><Default Extension=\"xml\" ContentType=\"application/xml\"/>" +
            "<Override PartName=\"/word/document.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml\"/></Types>"
        val rels = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">" +
            "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"word/document.xml\"/></Relationships>"
        ZipOutputStream(file.outputStream()).use { z ->
            fun bagian(nama: String, isi: String) { z.putNextEntry(ZipEntry(nama)); z.write(isi.toByteArray(Charsets.UTF_8)); z.closeEntry() }
            bagian("[Content_Types].xml", tipe)
            bagian("_rels/.rels", rels)
            bagian("word/document.xml", dokumen)
        }
    }

    /** Baris berformat "a | b | c" yang berurutan dijadikan tabel; sisanya paragraf biasa. */
    fun dariBaris(judul: String, sub: String, baris: List<String>): List<BlokDocx> {
        val hasil = mutableListOf<BlokDocx>(BlokDocx.Paragraf(judul, true, "left", 32))
        if (sub.isNotBlank()) hasil += BlokDocx.Paragraf(sub, false, "left", 20)
        var tabel = mutableListOf<List<String>>()
        fun lepas() { if (tabel.isNotEmpty()) { hasil += BlokDocx.Tabel(tabel.toList(), true); tabel = mutableListOf() } }
        baris.forEach { b ->
            if (b.contains(" | ")) tabel += b.split(" | ").map { it.trim() }
            else if (b.isBlank() && tabel.isNotEmpty()) { /* baris kosong di dalam tabel diabaikan */ }
            else { lepas(); if (b.isNotBlank()) hasil += BlokDocx.Paragraf(b) }
        }
        lepas()
        return hasil
    }
}

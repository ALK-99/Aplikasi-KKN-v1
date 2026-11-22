package com.kkn.almufid.sekretaris.ui.developer

import android.content.Context
import com.kkn.almufid.sekretaris.data.auth.Peran
import com.kkn.almufid.sekretaris.ui.formatTanggal
import com.kkn.almufid.sekretaris.ui.screens.laporan.PembuatLaporan
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

enum class JenisEkspor(val judul: String, val keterangan: String, val mime: String) {
    PESERTA_CSV("Data Peserta (CSV)", "Semua peserta beserta statistik keaktifan, bisa dibuka di Excel", "text/csv"),
    AKTIVITAS_CSV("Log Aktivitas (CSV)", "Seluruh kegiatan terurut waktu", "text/csv"),
    REKAP_PDF("Rekap Peserta (PDF)", "Ringkasan per peserta untuk laporan ke pembimbing", "application/pdf"),
    DATABASE_JSON("Seluruh Database (JSON)", "Cadangan lengkap semua data cloud", "application/json"),
}

private fun tgl(millis: Long) = if (millis <= 0L) "-" else formatTanggal(millis)

fun buatBerkasEkspor(context: Context, jenis: JenisEkspor, d: DataDeveloper): File {
    val peserta = susunPeserta(d).sortedBy { it.profil.namaLengkap.lowercase() }
    val cap = "KKN Al-Mushlih 2027 · dibuat ${formatTanggal(System.currentTimeMillis())}"
    return when (jenis) {
        JenisEkspor.PESERTA_CSV -> PembuatLaporan.csv(
            context, "data-peserta.csv",
            listOf(
                "Nama", "NIM", "Email", "Desa", "Divisi", "Peran", "Status", "Terdaftar", "Terakhir aktif",
                "Jurnal", "Tugas selesai", "Tugas total", "Pengumuman dibaca", "Polling diikuti", "Skor"
            ),
            peserta.map {
                val p = it.profil
                listOf(
                    p.namaLengkap, p.nim, p.email, p.desa, p.divisi,
                    if (Peran.dariString(p.peran) == Peran.DEVELOPER) "Developer" else "Peserta",
                    if (p.akunDiblokir) "Diblokir" else "Aktif",
                    tgl(p.dibuatPadaMillis), tgl(p.terakhirAktifMillis),
                    it.jurnal.size.toString(), it.tugasSelesai.toString(), it.tugas.size.toString(),
                    "${it.pengumumanDibaca}/${it.pengumumanTotal}", "${it.suara}/${it.pollingTotal}", it.skor.toString()
                )
            }
        )
        JenisEkspor.AKTIVITAS_CSV -> PembuatLaporan.csv(
            context, "log-aktivitas.csv", listOf("Waktu", "Jenis", "Judul", "Rincian", "Oleh"),
            susunAktivitas(d).map { listOf(tgl(it.millis), it.jenis.label, it.judul, it.rincian.replace("\n", " "), it.oleh) }
        )
        JenisEkspor.REKAP_PDF -> {
            val baris = mutableListOf(
                "Peserta terdaftar : ${d.profil.size}",
                "Jurnal bersama    : ${d.jurnal.size}",
                "Tugas selesai     : ${d.tugas.count { it.selesai }} dari ${d.tugas.size}",
                ""
            )
            peserta.forEach {
                val p = it.profil
                baris += "${p.namaLengkap} (${p.nim})${if (p.akunDiblokir) " [DIBLOKIR]" else ""}"
                baris += "  Desa ${p.desa.ifBlank { "-" }} · Divisi ${p.divisi.ifBlank { "-" }}"
                baris += "  Jurnal ${it.jurnal.size} · Tugas ${it.tugasSelesai}/${it.tugas.size} · " +
                    "Pengumuman dibaca ${it.pengumumanDibaca}/${it.pengumumanTotal} · Polling ${it.suara}/${it.pollingTotal}"
                baris += "  Terakhir aktif: ${tgl(p.terakhirAktifMillis)}"
                baris += ""
            }
            PembuatLaporan.pdf(context, "rekap-peserta.pdf", "Rekap Peserta KKN", cap, baris)
        }
        JenisEkspor.DATABASE_JSON -> {
            val root = JSONObject()
            root.put("dibuat", System.currentTimeMillis())
            root.put("users", JSONArray().also { a ->
                d.profil.forEach {
                    a.put(
                        JSONObject().put("uid", it.uid).put("nama", it.namaLengkap).put("nim", it.nim)
                            .put("email", it.email).put("desa", it.desa).put("divisi", it.divisi)
                            .put("peran", it.peran).put("diblokir", it.akunDiblokir)
                            .put("dibuat", it.dibuatPadaMillis).put("terakhirAktif", it.terakhirAktifMillis)
                    )
                }
            })
            root.put("jurnal_bersama", JSONArray().also { a ->
                d.jurnal.forEach {
                    a.put(
                        JSONObject().put("id", it.id).put("penulisUid", it.penulisUid).put("penulis", it.penulisNama)
                            .put("desa", it.desa).put("jenis", it.jenis).put("judul", it.judul).put("isi", it.isi)
                            .put("peserta", it.pesertaRapat).put("tanggal", it.tanggalMillis)
                    )
                }
            })
            root.put("pengumuman", JSONArray().also { a ->
                d.pengumuman.forEach {
                    a.put(
                        JSONObject().put("id", it.id).put("judul", it.judul).put("isi", it.isi)
                            .put("penulis", it.penulisNama).put("tanggal", it.tanggalMillis)
                            .put("dibacaOleh", JSONArray(it.dibacaOleh))
                    )
                }
            })
            root.put("polling", JSONArray().also { a ->
                d.polling.forEach {
                    a.put(
                        JSONObject().put("id", it.id).put("pertanyaan", it.pertanyaan)
                            .put("opsi", JSONArray(it.opsi)).put("suara", JSONObject(it.suara))
                            .put("ditutup", it.ditutup).put("tanggal", it.tanggalMillis)
                    )
                }
            })
            root.put("tugas", JSONArray().also { a ->
                d.tugas.forEach {
                    a.put(
                        JSONObject().put("id", it.id).put("judul", it.judul).put("deskripsi", it.deskripsi)
                            .put("untukUid", it.untukUid).put("untuk", it.untukNama)
                            .put("tenggat", it.deadlineMillis).put("selesai", it.selesai)
                    )
                }
            })
            root.put("kas", JSONArray().also { a ->
                d.kas.forEach {
                    a.put(
                        JSONObject().put("id", it.id).put("masuk", it.masuk).put("jumlah", it.jumlah)
                            .put("kategori", it.kategori).put("keterangan", it.keterangan)
                            .put("penulis", it.penulisNama).put("tanggal", it.tanggalMillis)
                    )
                }
            })
            root.put("saran", JSONArray().also { a ->
                d.saran.forEach {
                    a.put(JSONObject().put("uid", it.uid).put("isi", it.isi).put("tanggal", it.dikirimMillis))
                }
            })
            PembuatLaporan.teks(context, "database-kkn.json", root.toString(2))
        }
    }
}

package com.kkn.almufid.sekretaris.ui.screens.laporan

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kkn.almufid.sekretaris.data.AppDatabase
import com.kkn.almufid.sekretaris.data.auth.HasilAuth
import com.kkn.almufid.sekretaris.data.cloud.DesaRepository
import com.kkn.almufid.sekretaris.data.cloud.JurnalBersamaRepository
import com.kkn.almufid.sekretaris.data.cloud.KoordinasiRepository
import com.kkn.almufid.sekretaris.ui.formatRupiah
import com.kkn.almufid.sekretaris.ui.formatTanggal
import com.kkn.almufid.sekretaris.ui.screens.proker.ringkasTanggal
import com.kkn.almufid.sekretaris.ui.screens.rekonsiliasi.Rekonsiliasi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

enum class JenisLaporan(val judul: String, val keterangan: String, val mime: String) {
    JURNAL_BERSAMA_PDF("Jurnal Bersama (PDF)", "Semua jurnal yang dibagikan, urut tanggal", "application/pdf"),
    JURNAL_PRIBADI_PDF("Jurnal Pribadi (PDF)", "Catatan pribadi di HP ini", "application/pdf"),
    KAS_PDF("Kas Kelompok (PDF)", "Saldo dan seluruh transaksi", "application/pdf"),
    REKAP_MINGGUAN_PDF("Rekap 7 Hari Terakhir (PDF)", "Jurnal, tugas, dan kas seminggu ini", "application/pdf"),
    REKAP_ABSENSI_PDF("Rekap Absensi Bulan Ini (PDF)", "Hadir, izin, sakit, dan alpa tiap anggota", "application/pdf"),
    ABSENSI_CSV("Absensi (CSV)", "Bisa dibuka di Excel", "text/csv"),
    INVENTARIS_CSV("Inventaris (CSV)", "Daftar barang dan kondisinya", "text/csv"),
    TUGAS_CSV("Tugas (CSV)", "Semua tugas, penerima, dan status", "text/csv"),
    LPJ_PROKER_PDF("Pertanggungjawaban Proker (PDF)", "Rencana, anggaran vs realisasi, nota, dan bukti per proker", "application/pdf"),
    REGISTER_SURAT_PDF("Register Surat (PDF)", "Surat masuk dan keluar, urut tanggal", "application/pdf"),
    REKONSILIASI_PDF("Rekonsiliasi Kas (PDF)", "Pengeluaran kas dicocokkan dengan nota dan kwitansi", "application/pdf"),
    BACKUP_ZIP("Cadangan Lengkap + Foto (ZIP)", "Data desa, jurnal & nota di HP, dan foto. Bisa dipulihkan", "application/zip"),
    BACKUP_JSON("Backup Semua Data (JSON)", "Cadangan data di HP + data bersama", "application/json"),
}

class LaporanViewModel(
    private val appContext: Context,
    private val db: AppDatabase,
    private val desa: DesaRepository,
    private val bersama: JurnalBersamaRepository,
    private val koordinasi: KoordinasiRepository,
) : ViewModel() {

    private val prefs = appContext.getSharedPreferences("sinkron_prefs", Context.MODE_PRIVATE)

    private val _cadanganTerakhir = MutableStateFlow(prefs.getLong("cadangan_terakhir", 0L))
    val cadanganTerakhir: StateFlow<Long> = _cadanganTerakhir.asStateFlow()

    private val _pratinjau = MutableStateFlow<Cadangan.Pratinjau?>(null)
    val pratinjau: StateFlow<Cadangan.Pratinjau?> = _pratinjau.asStateFlow()

    private val _sedangProses = MutableStateFlow(false)
    val sedangProses: StateFlow<Boolean> = _sedangProses.asStateFlow()

    private val _pesan = MutableStateFlow<String?>(null)
    val pesan: StateFlow<String?> = _pesan.asStateFlow()

    fun tutupPesan() { _pesan.value = null }

    /** Membaca berkas cadangan yang dipilih pengguna dan menampilkan ringkasannya, belum mengubah data. */
    fun bacaCadangan(uri: Uri) {
        if (_sedangProses.value) return
        _sedangProses.value = true
        viewModelScope.launch {
            try {
                _pratinjau.value = withContext(Dispatchers.IO) { Cadangan.baca(appContext, uri) }
            } catch (e: IllegalArgumentException) {
                _pesan.value = e.message ?: "Berkas tidak valid."
            } catch (e: Exception) {
                _pesan.value = "Gagal membaca berkas: ${e.message ?: "kesalahan tidak diketahui"}"
            } finally {
                _sedangProses.value = false
            }
        }
    }

    fun batalPulih() {
        _pratinjau.value = null
        Cadangan.bersihkan(appContext)
    }

    fun pulihkan() {
        val p = _pratinjau.value ?: return
        if (_sedangProses.value) return
        _sedangProses.value = true
        viewModelScope.launch {
            try {
                val (jurnal, nota) = withContext(Dispatchers.IO) { Cadangan.pulihkanLokal(appContext, db, p) }
                val bagianDesa = if (p.dataDesa > 0) {
                    when (val h = desa.pulihkanMentah(p.desaId, Cadangan.dataDesa(p.json))) {
                        is HasilAuth.Sukses -> "${h.data} data desa dipulihkan."
                        is HasilAuth.Gagal -> "Data desa BELUM dipulihkan: ${h.pesan}"
                    }
                } else "Cadangan ini tidak berisi data desa."
                _pesan.value = "Selesai. $jurnal jurnal dan $nota nota baru dipulihkan di HP ini (yang sudah ada dilewati). $bagianDesa"
                _pratinjau.value = null
                Cadangan.bersihkan(appContext)
            } catch (e: Exception) {
                _pesan.value = "Gagal memulihkan: ${e.message ?: "kesalahan tidak diketahui"}"
            } finally {
                _sedangProses.value = false
            }
        }
    }

    fun buat(jenis: JenisLaporan, onSiap: (File, String) -> Unit) {
        if (_sedangProses.value) return
        _sedangProses.value = true
        viewModelScope.launch {
            try {
                val file = withContext(Dispatchers.IO) { bangun(jenis) }
                onSiap(file, jenis.mime)
            } catch (e: Exception) {
                _pesan.value = "Gagal membuat laporan: ${e.message ?: "kesalahan tidak diketahui"}"
            } finally {
                _sedangProses.value = false
            }
        }
    }

    /** Data cloud diambil dengan batas waktu supaya tidak menggantung kalau offline dan belum pernah tersinkron. */
    private suspend fun <T> ambil(alir: Flow<List<T>>): List<T> =
        withTimeoutOrNull(8000) { alir.first() } ?: emptyList()

    private suspend fun bangun(jenis: JenisLaporan): File {
        val sekarang = System.currentTimeMillis()
        val cap = "KKN Al-Mushlih 2027 · dibuat ${formatTanggal(sekarang)}"
        return when (jenis) {
            JenisLaporan.JURNAL_BERSAMA_PDF -> {
                val data = ambil(bersama.alirSemua()).sortedBy { it.tanggalMillis }
                val baris = data.map {
                    "${formatTanggal(it.tanggalMillis)} · ${it.penulisNama}${if (it.desa.isNotBlank()) " (${it.desa})" else ""}\n" +
                        "${it.judul}\n${it.isi}" +
                        (if (it.pesertaRapat.isNotBlank()) "\nPeserta: ${it.pesertaRapat}" else "") + "\n"
                }
                PembuatLaporan.pdf(appContext, "jurnal-bersama.pdf", "Jurnal Bersama", cap, baris.ifEmpty { listOf("Belum ada data.") })
            }
            JenisLaporan.JURNAL_PRIBADI_PDF -> {
                val data = db.jurnalDao().semuaJurnal().first().sortedBy { it.tanggalMillis }
                val baris = data.map {
                    "${formatTanggal(it.tanggalMillis)} · ${it.jenis.name}\n${it.judul}\n${it.isi}" +
                        (if (it.pesertaRapat.isNotBlank()) "\nPeserta: ${it.pesertaRapat}" else "") + "\n"
                }
                PembuatLaporan.pdf(appContext, "jurnal-pribadi.pdf", "Jurnal Pribadi", cap, baris.ifEmpty { listOf("Belum ada data.") })
            }
            JenisLaporan.KAS_PDF -> {
                val data = ambil(koordinasi.alirKas()).sortedBy { it.tanggalMillis }
                val masuk = data.filter { it.masuk }.sumOf { it.jumlah }
                val keluar = data.filter { !it.masuk }.sumOf { it.jumlah }
                val baris = mutableListOf(
                    "Pemasukan : ${formatRupiah(masuk)}",
                    "Pengeluaran: ${formatRupiah(keluar)}",
                    "SALDO      : ${formatRupiah(masuk - keluar)}",
                    ""
                )
                data.forEach {
                    baris += "${formatTanggal(it.tanggalMillis)} | ${if (it.masuk) "MASUK " else "KELUAR"} | " +
                        "${formatRupiah(it.jumlah)} | ${it.kategori} ${it.keterangan}".trim()
                }
                PembuatLaporan.pdf(appContext, "kas.pdf", "Laporan Kas Kelompok", cap, baris)
            }
            JenisLaporan.REKAP_MINGGUAN_PDF -> {
                val batas = sekarang - 7L * 86_400_000L
                val jurnal = ambil(bersama.alirSemua()).filter { it.tanggalMillis >= batas }.sortedBy { it.tanggalMillis }
                val tugas = ambil(koordinasi.alirTugas())
                val kas = ambil(koordinasi.alirKas()).filter { it.tanggalMillis >= batas }
                val baris = mutableListOf<String>()
                baris += "KEGIATAN (${jurnal.size} jurnal bersama)"
                jurnal.forEach { baris += "- ${formatTanggal(it.tanggalMillis)}: ${it.judul} (${it.penulisNama})" }
                baris += ""
                baris += "TUGAS"
                baris += "Selesai ${tugas.count { it.selesai }} dari ${tugas.size} tugas"
                tugas.filter { !it.selesai && it.deadlineMillis + 86_400_000L < sekarang }
                    .forEach { baris += "- Terlambat: ${it.judul} (${it.untukNama})" }
                baris += ""
                baris += "KAS 7 HARI"
                baris += "Masuk ${formatRupiah(kas.filter { it.masuk }.sumOf { it.jumlah })}, " +
                    "keluar ${formatRupiah(kas.filter { !it.masuk }.sumOf { it.jumlah })}"
                PembuatLaporan.pdf(appContext, "rekap-mingguan.pdf", "Rekap 7 Hari Terakhir", cap, baris)
            }
            JenisLaporan.REKAP_ABSENSI_PDF -> {
                val kal = java.util.Calendar.getInstance().apply {
                    set(java.util.Calendar.DAY_OF_MONTH, 1)
                    set(java.util.Calendar.HOUR_OF_DAY, 0); set(java.util.Calendar.MINUTE, 0)
                    set(java.util.Calendar.SECOND, 0); set(java.util.Calendar.MILLISECOND, 0)
                }
                val awal = kal.timeInMillis
                val bulan = java.text.SimpleDateFormat("MMMM yyyy", java.util.Locale("id", "ID")).format(java.util.Date(awal))
                val data = ambil(desa.alirAbsensi()).filter { it.tanggalMillis >= awal }
                val baris = mutableListOf("Nama | Hadir | Izin | Sakit | Alpa | Persen hadir", "")
                data.groupBy { it.anggotaId }.values
                    .sortedBy { it.first().anggotaNama.lowercase() }
                    .forEach { l ->
                        val h = l.count { it.status == "HADIR" }
                        val i = l.count { it.status == "IZIN" }
                        val sk = l.count { it.status == "SAKIT" }
                        val a = l.count { it.status == "ALPA" }
                        val persen = if (l.isEmpty()) 0 else h * 100 / l.size
                        baris += "${l.first().anggotaNama} | $h | $i | $sk | $a | $persen%"
                    }
                if (data.isEmpty()) baris += "Belum ada data absensi bulan ini."
                PembuatLaporan.pdf(appContext, "rekap-absensi.pdf", "Rekap Absensi $bulan", cap, baris)
            }
            JenisLaporan.ABSENSI_CSV -> {
                val data = ambil(desa.alirAbsensi()).sortedWith(compareBy({ it.tanggalMillis }, { it.anggotaNama }))
                PembuatLaporan.csv(
                    appContext, "absensi.csv", listOf("Tanggal", "Nama", "Status", "Keterangan", "Dicatat oleh"),
                    data.map {
                        listOf(
                            formatTanggal(it.tanggalMillis).substringBefore(","), it.anggotaNama,
                            it.status.lowercase().replaceFirstChar { c -> c.uppercase() }, it.keterangan, it.jejak.diubahOlehNama
                        )
                    }
                )
            }
            JenisLaporan.INVENTARIS_CSV -> {
                val data = ambil(desa.alirInventaris()).sortedBy { it.namaBarang.lowercase() }
                PembuatLaporan.csv(
                    appContext, "inventaris.csv", listOf("Barang", "Jumlah", "Kondisi", "Keterangan", "Dicatat oleh"),
                    data.map { listOf(it.namaBarang, it.jumlah.toString(), it.kondisi, it.keterangan, it.jejak.dibuatOlehNama) }
                )
            }
            JenisLaporan.TUGAS_CSV -> {
                val data = ambil(koordinasi.alirTugas()).sortedBy { it.deadlineMillis }
                PembuatLaporan.csv(
                    appContext, "tugas.csv", listOf("Tugas", "Untuk", "Tenggat", "Status", "Keterangan"),
                    data.map { listOf(it.judul, it.untukNama, formatTanggal(it.deadlineMillis).substringBefore(","), if (it.selesai) "Selesai" else "Belum", it.deskripsi) }
                )
            }
            JenisLaporan.LPJ_PROKER_PDF -> {
                val proker = ambil(desa.alirProker()).sortedBy { it.mulaiMillis.takeIf { m -> m > 0L } ?: Long.MAX_VALUE }
                val nota = ambil(desa.alirNota())
                val bukti = desa.ambilMentah("bukti").orEmpty()
                    .filter { it.second["dihapus"] != true }
                    .groupingBy { it.second["prokerId"] as? String ?: "" }.eachCount()
                val baris = mutableListOf<String>()
                proker.forEachIndexed { i, p ->
                    val notaProker = nota.filter { it.prokerTerkait.trim().equals(p.namaProker.trim(), ignoreCase = true) }
                    baris += "${i + 1}. ${p.namaProker}  [${p.status.name.replace('_', ' ')}]"
                    if (p.picNama.isNotBlank() || p.divisiPenanggungJawab.isNotBlank()) {
                        baris += "   PIC: ${p.picNama.ifBlank { "-" }}  ·  Divisi: ${p.divisiPenanggungJawab.ifBlank { "-" }}"
                    }
                    baris += "   Jadwal: ${ringkasTanggal(p.mulaiMillis)} s.d. ${ringkasTanggal(p.selesaiMillis)}"
                    if (p.target.isNotBlank()) baris += "   Target: ${p.target}"
                    baris += "   Anggaran: ${formatRupiah(p.anggaran)}  ·  Realisasi: ${formatRupiah(p.realisasi)}  ·  " +
                        "Selisih: ${formatRupiah(p.anggaran - p.realisasi)}"
                    baris += "   Nota/kwitansi terkait: ${notaProker.size} (${formatRupiah(notaProker.sumOf { it.nominal })})  ·  " +
                        "Bukti foto: ${bukti[p.id] ?: 0}"
                    if (p.catatanEvaluasi.isNotBlank()) baris += "   Evaluasi: ${p.catatanEvaluasi}"
                    baris += ""
                }
                baris += "Total anggaran : ${formatRupiah(proker.sumOf { it.anggaran })}"
                baris += "Total realisasi: ${formatRupiah(proker.sumOf { it.realisasi })}"
                PembuatLaporan.pdf(appContext, "lpj-proker.pdf", "Laporan Pertanggungjawaban Program Kerja", cap, baris)
            }
            JenisLaporan.REGISTER_SURAT_PDF -> {
                val data = ambil(desa.alirSurat()).sortedBy { it.tanggalMillis }
                val baris = mutableListOf<String>()
                listOf(com.kkn.almufid.sekretaris.data.entity.JenisSurat.MASUK, com.kkn.almufid.sekretaris.data.entity.JenisSurat.KELUAR).forEach { jenis ->
                    val bagian = data.filter { it.jenis == jenis }
                    baris += "SURAT ${jenis.name} (${bagian.size})"
                    bagian.forEachIndexed { i, x ->
                        baris += "${i + 1}. ${formatTanggal(x.tanggalMillis)} | ${x.nomorSurat.ifBlank { "tanpa nomor" }} | " +
                            "${x.perihal} | ${x.tujuanAtauPengirim} | dicatat ${x.jejak.dibuatOlehNama.ifBlank { "-" }}"
                    }
                    baris += ""
                }
                PembuatLaporan.pdf(appContext, "register-surat.pdf", "Register Surat Masuk dan Keluar", cap, baris)
            }
            JenisLaporan.REKONSILIASI_PDF -> {
                val r = Rekonsiliasi.hitung(ambil(koordinasi.alirKas()), ambil(desa.alirNota()), ambil(desa.alirProker()))
                val baris = mutableListOf(
                    "Kas masuk            : ${formatRupiah(r.kasMasuk)}",
                    "Kas keluar           : ${formatRupiah(r.kasKeluar)}",
                    "Saldo kas            : ${formatRupiah(r.saldo)}",
                    "Total nota/kwitansi  : ${formatRupiah(r.totalNota)}",
                    "Selisih (kas keluar - nota): ${formatRupiah(r.selisih)}",
                    "Pengeluaran cocok nota: ${r.cocok.size}  ·  Nota tanpa foto: ${r.notaTanpaFoto}",
                    "",
                    "KAS KELUAR BELUM ADA NOTA (${r.kasTanpaNota.size})"
                )
                r.kasTanpaNota.forEach { baris += "${formatTanggal(it.tanggalMillis)} | ${formatRupiah(it.jumlah)} | ${it.kategori} ${it.keterangan}".trim() }
                baris += ""
                baris += "NOTA BELUM TERCATAT DI KAS (${r.notaTanpaKas.size})"
                r.notaTanpaKas.forEach { baris += "${formatTanggal(it.tanggalMillis)} | ${formatRupiah(it.nominal)} | ${it.jenis} ${it.keterangan}" }
                baris += ""
                baris += "Catatan: kas mencakup semua desa, nota hanya milik desa ini."
                PembuatLaporan.pdf(appContext, "rekonsiliasi-kas.pdf", "Rekonsiliasi Kas", cap, baris)
            }
            JenisLaporan.BACKUP_ZIP -> {
                val ringkasan = bangun(JenisLaporan.BACKUP_JSON)
                Cadangan.buatZip(appContext, db, desa, ringkasan).also {
                    prefs.edit().putLong("cadangan_terakhir", sekarang).apply()
                    _cadanganTerakhir.value = sekarang
                }
            }
            JenisLaporan.BACKUP_JSON -> {
                val root = JSONObject()
                root.put("dibuat", sekarang)
                root.put("jurnal_pribadi", JSONArray().also { a ->
                    db.jurnalDao().semuaJurnal().first().forEach {
                        a.put(JSONObject().put("tanggal", it.tanggalMillis).put("jenis", it.jenis.name)
                            .put("judul", it.judul).put("isi", it.isi).put("peserta", it.pesertaRapat))
                    }
                })
                root.put("desa", JSONObject().also { d ->
                    d.put("anggota", JSONArray().also { a ->
                        ambil(desa.alirAnggota()).forEach {
                            a.put(JSONObject().put("id", it.id).put("nama", it.nama).put("divisi", it.divisi)
                                .put("aktif", it.aktif).put("dibuatOleh", it.jejak.dibuatOlehNama))
                        }
                    })
                    d.put("absensi", JSONArray().also { a ->
                        ambil(desa.alirAbsensi()).forEach {
                            a.put(JSONObject().put("anggotaId", it.anggotaId).put("nama", it.anggotaNama)
                                .put("tanggal", it.tanggalMillis).put("hadir", it.hadir)
                                .put("keterangan", it.keterangan).put("dicatatOleh", it.jejak.diubahOlehNama))
                        }
                    })
                    d.put("proker", JSONArray().also { a ->
                        ambil(desa.alirProker()).forEach {
                            a.put(JSONObject().put("nama", it.namaProker).put("divisi", it.divisiPenanggungJawab)
                                .put("status", it.status.name).put("evaluasi", it.catatanEvaluasi))
                        }
                    })
                    d.put("surat", JSONArray().also { a ->
                        ambil(desa.alirSurat()).forEach {
                            a.put(JSONObject().put("tanggal", it.tanggalMillis).put("jenis", it.jenis.name)
                                .put("nomor", it.nomorSurat).put("perihal", it.perihal).put("pihak", it.tujuanAtauPengirim))
                        }
                    })
                    d.put("inventaris", JSONArray().also { a ->
                        ambil(desa.alirInventaris()).forEach {
                            a.put(JSONObject().put("barang", it.namaBarang).put("jumlah", it.jumlah)
                                .put("kondisi", it.kondisi).put("keterangan", it.keterangan))
                        }
                    })
                })
                // Data lama yang masih tersimpan di HP ini (sebelum dipindah ke desa).
                root.put("anggota", JSONArray().also { a ->
                    db.anggotaDao().semuaAnggota().first().forEach {
                        a.put(JSONObject().put("id", it.id).put("nama", it.nama).put("divisi", it.divisi))
                    }
                })
                root.put("absensi", JSONArray().also { a ->
                    db.absensiDao().semuaAbsensi().first().forEach {
                        a.put(JSONObject().put("anggotaId", it.anggotaId).put("tanggal", it.tanggalMillis)
                            .put("hadir", it.hadir).put("keterangan", it.keterangan))
                    }
                })
                root.put("proker", JSONArray().also { a ->
                    db.prokerDao().semuaProker().first().forEach {
                        a.put(JSONObject().put("nama", it.namaProker).put("divisi", it.divisiPenanggungJawab)
                            .put("status", it.status.name).put("evaluasi", it.catatanEvaluasi))
                    }
                })
                root.put("inventaris", JSONArray().also { a ->
                    db.inventarisDao().semuaInventaris().first().forEach {
                        a.put(JSONObject().put("barang", it.namaBarang).put("jumlah", it.jumlah)
                            .put("kondisi", it.kondisi).put("keterangan", it.keterangan))
                    }
                })
                root.put("surat", JSONArray().also { a ->
                    db.suratDao().semuaSurat().first().forEach {
                        a.put(JSONObject().put("tanggal", it.tanggalMillis).put("jenis", it.jenis.name)
                            .put("nomor", it.nomorSurat).put("perihal", it.perihal).put("pihak", it.tujuanAtauPengirim))
                    }
                })
                root.put("nota", JSONArray().also { a ->
                    db.notaDao().semuaNota().first().forEach {
                        a.put(JSONObject().put("tanggal", it.tanggalMillis).put("keterangan", it.keterangan)
                            .put("nominal", it.nominal).put("proker", it.prokerTerkait))
                    }
                })
                root.put("jurnal_bersama", JSONArray().also { a ->
                    ambil(bersama.alirSemua()).forEach {
                        a.put(JSONObject().put("tanggal", it.tanggalMillis).put("penulis", it.penulisNama).put("desa", it.desa)
                            .put("jenis", it.jenis).put("judul", it.judul).put("isi", it.isi).put("peserta", it.pesertaRapat))
                    }
                })
                root.put("tugas", JSONArray().also { a ->
                    ambil(koordinasi.alirTugas()).forEach {
                        a.put(JSONObject().put("judul", it.judul).put("untuk", it.untukNama)
                            .put("tenggat", it.deadlineMillis).put("selesai", it.selesai))
                    }
                })
                root.put("kas", JSONArray().also { a ->
                    ambil(koordinasi.alirKas()).forEach {
                        a.put(JSONObject().put("tanggal", it.tanggalMillis).put("masuk", it.masuk)
                            .put("jumlah", it.jumlah).put("kategori", it.kategori).put("keterangan", it.keterangan))
                    }
                })
                PembuatLaporan.teks(appContext, "backup-kkn.json", root.toString(2))
            }
        }
    }
}

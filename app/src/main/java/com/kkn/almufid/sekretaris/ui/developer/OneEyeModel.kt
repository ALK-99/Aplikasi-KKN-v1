package com.kkn.almufid.sekretaris.ui.developer

import com.kkn.almufid.sekretaris.data.auth.Peran
import com.kkn.almufid.sekretaris.data.auth.ProfilPengguna
import com.kkn.almufid.sekretaris.data.cloud.JurnalBersama
import com.kkn.almufid.sekretaris.data.cloud.Kas
import com.kkn.almufid.sekretaris.data.cloud.Pengumuman
import com.kkn.almufid.sekretaris.data.cloud.Polling
import com.kkn.almufid.sekretaris.data.cloud.Saran
import com.kkn.almufid.sekretaris.data.cloud.Tugas

internal const val SATU_HARI = 86_400_000L

/** Seluruh data cloud yang terlihat oleh developer, satu paket supaya mudah diolah. */
data class DataDeveloper(
    val profil: List<com.kkn.almufid.sekretaris.data.auth.ProfilPengguna> = emptyList(),
    val jurnal: List<JurnalBersama> = emptyList(),
    val pengumuman: List<Pengumuman> = emptyList(),
    val polling: List<Polling> = emptyList(),
    val tugas: List<Tugas> = emptyList(),
    val kas: List<Kas> = emptyList(),
    val saran: List<Saran> = emptyList(),
)

/** Gambaran lengkap satu peserta: profil + semua yang sudah dia lakukan di app. */
data class RingkasanPeserta(
    val profil: ProfilPengguna,
    val jurnal: List<JurnalBersama>,
    val tugas: List<Tugas>,
    val pengumumanDibaca: Int,
    val pengumumanTotal: Int,
    val suara: Int,
    val pollingTotal: Int,
    val skor: Int,
) {
    val tugasSelesai: Int get() = tugas.count { it.selesai }
    val adalahPeserta: Boolean get() = Peran.dariString(profil.peran) == Peran.PESERTA
}

fun susunPeserta(d: DataDeveloper): List<RingkasanPeserta> = d.profil.map { p ->
    val jurnal = d.jurnal.filter { it.penulisUid == p.uid }
    val tugas = d.tugas.filter { it.untukUid == p.uid }
    val dibaca = d.pengumuman.count { p.uid in it.dibacaOleh }
    val suara = d.polling.count { it.suara.containsKey(p.uid) }
    RingkasanPeserta(
        profil = p, jurnal = jurnal, tugas = tugas,
        pengumumanDibaca = dibaca, pengumumanTotal = d.pengumuman.size,
        suara = suara, pollingTotal = d.polling.size,
        // Bobot: menulis jurnal paling berharga, lalu menyelesaikan tugas, lalu membaca & memilih.
        skor = jurnal.size * 3 + tugas.count { it.selesai } * 2 + dibaca + suara,
    )
}

/** Statistik per kelompok (desa atau divisi). */
data class KelompokStat(
    val nama: String,
    val peserta: Int,
    val jurnal: Int,
    val tugasSelesai: Int,
    val tugasTotal: Int,
)

fun kelompokkan(daftar: List<RingkasanPeserta>, kunci: (ProfilPengguna) -> String): List<KelompokStat> =
    daftar.groupBy { kunci(it.profil).trim().ifBlank { "(belum diisi)" } }
        .map { (nama, isi) ->
            KelompokStat(
                nama = nama, peserta = isi.size,
                jurnal = isi.sumOf { it.jurnal.size },
                tugasSelesai = isi.sumOf { it.tugasSelesai },
                tugasTotal = isi.sumOf { it.tugas.size },
            )
        }
        .sortedByDescending { it.jurnal }

enum class JenisAktivitas(val label: String) {
    JURNAL("Jurnal"), PENGUMUMAN("Pengumuman"), POLLING("Polling"),
    TUGAS("Tugas"), KAS("Kas"), DAFTAR("Pendaftaran"), SARAN("Saran"),
}

data class Aktivitas(
    val jenis: JenisAktivitas,
    val judul: String,
    val rincian: String,
    val oleh: String,
    val millis: Long,
)

/** Linimasa gabungan semua kegiatan, terbaru di atas. */
fun susunAktivitas(d: DataDeveloper): List<Aktivitas> {
    val namaDariUid = d.profil.associate { it.uid to it.namaLengkap }
    val hasil = mutableListOf<Aktivitas>()
    d.jurnal.forEach {
        hasil += Aktivitas(
            JenisAktivitas.JURNAL, it.judul.ifBlank { "(tanpa judul)" },
            it.isi.take(160), it.penulisNama + if (it.desa.isNotBlank()) " · ${it.desa}" else "", it.tanggalMillis
        )
    }
    d.pengumuman.forEach {
        hasil += Aktivitas(
            JenisAktivitas.PENGUMUMAN, it.judul, "${it.isi.take(120)}\nDibaca ${it.dibacaOleh.size} orang",
            it.penulisNama, it.tanggalMillis
        )
    }
    d.polling.forEach {
        hasil += Aktivitas(
            JenisAktivitas.POLLING, it.pertanyaan,
            "${it.suara.size} suara · ${if (it.ditutup) "ditutup" else "terbuka"}", it.penulisNama, it.tanggalMillis
        )
    }
    d.tugas.forEach {
        hasil += Aktivitas(
            JenisAktivitas.TUGAS, it.judul,
            "Untuk ${it.untukNama} · ${if (it.selesai) "selesai" else "belum selesai"}", "Admin", it.tanggalMillis
        )
    }
    d.kas.forEach {
        hasil += Aktivitas(
            JenisAktivitas.KAS, (if (it.masuk) "Masuk " else "Keluar ") + it.jumlah,
            (it.kategori + " " + it.keterangan).trim(), it.penulisNama, it.tanggalMillis
        )
    }
    d.profil.filter { it.dibuatPadaMillis > 0 }.forEach {
        hasil += Aktivitas(
            JenisAktivitas.DAFTAR, "${it.namaLengkap} mendaftar",
            listOf(it.nim, it.desa, it.divisi).filter { s -> s.isNotBlank() }.joinToString(" · "),
            it.namaLengkap, it.dibuatPadaMillis
        )
    }
    d.saran.forEach {
        hasil += Aktivitas(
            JenisAktivitas.SARAN, it.isi.take(80), it.isi.take(200),
            namaDariUid[it.uid] ?: "Anonim", it.dikirimMillis
        )
    }
    return hasil.sortedByDescending { it.millis }
}

fun lalu(millis: Long, sekarang: Long = System.currentTimeMillis()): String {
    if (millis <= 0L) return "belum tercatat"
    val detik = (sekarang - millis) / 1000
    return when {
        detik < 60 -> "baru saja"
        detik < 3600 -> "${detik / 60} mnt lalu"
        detik < 86_400 -> "${detik / 3600} jam lalu"
        else -> "${detik / 86_400} hari lalu"
    }
}

fun ringkasNama(daftar: List<String>, batas: Int = 8): String =
    if (daftar.size <= batas) daftar.joinToString(", ")
    else daftar.take(batas).joinToString(", ") + " +${daftar.size - batas} lainnya"

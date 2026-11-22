package com.kkn.almufid.sekretaris.data.cloud

import com.kkn.almufid.sekretaris.data.entity.JenisSurat
import com.kkn.almufid.sekretaris.data.entity.StatusProker

/**
 * Jejak siapa yang membuat dan terakhir mengubah sebuah data. Diisi otomatis
 * oleh [DesaRepository] dan dicek aturan Firestore (uid pelaku harus sama
 * dengan akun yang login), jadi tidak bisa dipalsukan dari aplikasi.
 */
data class Jejak(
    val dibuatOlehNama: String = "",
    val dibuatPada: Long = 0L,
    val diubahOlehNama: String = "",
    val diubahPada: Long = 0L,
)

/** Anggota tidak pernah dihapus, hanya dinonaktifkan, supaya riwayat absensinya aman. */
data class AnggotaDesa(
    val id: String = "",
    val nama: String = "",
    val divisi: String = "",
    val aktif: Boolean = true,
    val jejak: Jejak = Jejak(),
)

/** [anggotaNama] adalah salinan nama saat dicatat, jadi laporan lama tetap benar walau anggota berubah/nonaktif. */
data class AbsensiDesa(
    val id: String = "",
    val anggotaId: String = "",
    val anggotaNama: String = "",
    val tanggalMillis: Long = 0L,
    val hadir: Boolean = false,
    val keterangan: String = "",
    val jejak: Jejak = Jejak(),
)

data class ProkerDesa(
    val id: String = "",
    val namaProker: String = "",
    val divisiPenanggungJawab: String = "",
    val status: StatusProker = StatusProker.BELUM_MULAI,
    val catatanEvaluasi: String = "",
    val jejak: Jejak = Jejak(),
)

data class SuratDesa(
    val id: String = "",
    val tanggalMillis: Long = 0L,
    val jenis: JenisSurat = JenisSurat.KELUAR,
    val nomorSurat: String = "",
    val perihal: String = "",
    val tujuanAtauPengirim: String = "",
    val jejak: Jejak = Jejak(),
)

data class InventarisDesa(
    val id: String = "",
    val namaBarang: String = "",
    val jumlah: Int = 0,
    val kondisi: String = "Baik",
    val keterangan: String = "",
    val jejak: Jejak = Jejak(),
)

/** Satu baris catatan perubahan (tidak bisa diubah atau dihapus setelah ditulis). */
data class RiwayatDesa(
    val id: String = "",
    val tabel: String = "",
    val aksi: String = "",
    val ringkas: String = "",
    val olehNama: String = "",
    val waktu: Long = 0L,
)

/** Data lama di HP (Room) yang belum dipindahkan ke desa. */
data class JumlahDataLama(
    val anggota: Int = 0,
    val absensi: Int = 0,
    val proker: Int = 0,
    val surat: Int = 0,
    val inventaris: Int = 0,
) {
    val total: Int get() = anggota + absensi + proker + surat + inventaris
}

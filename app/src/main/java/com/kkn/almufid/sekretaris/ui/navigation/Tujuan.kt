package com.kkn.almufid.sekretaris.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

/** Daftar halaman aplikasi. [rute] dipakai sebagai kunci navigasi (harus unik). */
enum class Tujuan(val rute: String, val judul: String, val ikon: ImageVector) {
    JURNAL("jurnal", "Jurnal & Notulen", Icons.Default.MenuBook),
    ABSENSI("absensi", "Absensi", Icons.Default.HowToReg),
    SURAT("surat", "Surat Masuk/Keluar", Icons.Default.Mail),
    DOKUMEN("dokumen", "Dokumen & PDF", Icons.Default.PictureAsPdf),
    PROKER("proker", "Program Kerja", Icons.Default.Checklist),
    ARSIP("arsip", "Arsip Nota", Icons.Default.Receipt),
    INVENTARIS("inventaris", "Inventaris Posko", Icons.Default.Inventory2),
    AJAR("ajar", "Jadwal Mengajar", Icons.Default.School),
    PENGUMUMAN("pengumuman", "Pengumuman", Icons.Default.Campaign),
    POLLING("polling", "Polling", Icons.Default.Poll),
    TUGAS("tugas", "Tugas & Kalender", Icons.Default.Event),
    KAS("kas", "Kas Kelompok", Icons.Default.AccountBalanceWallet),
    IBADAH("ibadah", "Waktu Sholat", Icons.Default.AccessTime),
    PETA("peta", "Peta", Icons.Default.Map),
    REKONSILIASI("rekonsiliasi", "Rekonsiliasi Kas", Icons.Default.AccountBalance),
    RIWAYAT("riwayat", "Riwayat Perubahan", Icons.Default.History),
    PROFIL("profil", "Profil Saya", Icons.Default.Person),
    PENGATURAN("pengaturan", "Pengaturan", Icons.Default.Settings),
    ASISTEN("asisten", "Asisten", Icons.Default.SmartToy),
    PENCARIAN("cari", "Pencarian", Icons.Default.Search);

    companion object {
        const val RUTE_HOME = "home"
    }
}

/** Satu divisi di dalam Menu Utama. [segera] = belum tersedia, tampil sebagai "Segera hadir". */
class DivisiMenu(
    val judul: String,
    val keterangan: String,
    val ikon: ImageVector,
    val isi: List<Tujuan>,
    val segera: Boolean = false
)

private val SEKRETARIS = DivisiMenu(
    "Sekretaris", "Absensi, surat, proker, inventaris, tugas, dokumen & PDF", Icons.Default.EditNote,
    listOf(Tujuan.ABSENSI, Tujuan.SURAT, Tujuan.PROKER, Tujuan.INVENTARIS, Tujuan.TUGAS, Tujuan.DOKUMEN)
)
private val BENDAHARA = DivisiMenu(
    "Bendahara", "Kas, nota dan kwitansi, rekonsiliasi", Icons.Default.AccountBalanceWallet,
    listOf(Tujuan.KAS, Tujuan.ARSIP, Tujuan.REKONSILIASI)
)
private val MEDIA = DivisiMenu(
    "Media", "Jurnal dan dokumentasi kegiatan", Icons.Default.PhotoCamera,
    listOf(Tujuan.JURNAL)
)
private val PENDIDIKAN = DivisiMenu(
    "Pendidikan", "Jadwal mengajar dan materi", Icons.Default.School, listOf(Tujuan.AJAR)
)

/**
 * Tiga kelompok menu di Beranda. Menu Utama dibagi lagi per divisi ([divisi]);
 * kelompok lain langsung berisi [isi]. Profil dibuka lewat foto profil di kiri atas,
 * Pencarian lewat ikon kaca pembesar, jadi keduanya tidak masuk daftar mana pun.
 */
enum class KategoriMenu(
    val judul: String,
    val keterangan: String,
    val ikon: ImageVector,
    val isi: List<Tujuan>,
    val divisi: List<DivisiMenu> = emptyList()
) {
    UTAMA(
        "Menu Utama", "Per divisi: Sekretaris, Bendahara, Media, Pendidikan", Icons.Default.Dashboard,
        emptyList(), listOf(SEKRETARIS, BENDAHARA, MEDIA, PENDIDIKAN)
    ),
    UMUM(
        "Menu Umum", "Asisten, pengumuman, polling, jadwal sholat, dan peta", Icons.Default.Public,
        listOf(Tujuan.ASISTEN, Tujuan.PENGUMUMAN, Tujuan.POLLING, Tujuan.IBADAH, Tujuan.PETA)
    ),
    LAINNYA(
        "Lainnya", "Riwayat dan pengaturan", Icons.Default.MoreHoriz,
        listOf(Tujuan.RIWAYAT, Tujuan.PENGATURAN)
    )
}

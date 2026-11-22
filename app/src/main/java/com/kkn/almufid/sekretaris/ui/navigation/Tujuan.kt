package com.kkn.almufid.sekretaris.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

/** Daftar halaman aplikasi. [rute] dipakai sebagai kunci navigasi (harus unik). */
enum class Tujuan(val rute: String, val judul: String, val ikon: ImageVector) {
    JURNAL("jurnal", "Jurnal & Notulen", Icons.Default.MenuBook),
    ABSENSI("absensi", "Absensi", Icons.Default.HowToReg),
    SURAT("surat", "Surat Masuk/Keluar", Icons.Default.Mail),
    PROKER("proker", "Program Kerja", Icons.Default.Checklist),
    ARSIP("arsip", "Arsip Nota", Icons.Default.Receipt),
    INVENTARIS("inventaris", "Inventaris Posko", Icons.Default.Inventory2),
    PENGUMUMAN("pengumuman", "Pengumuman", Icons.Default.Campaign),
    POLLING("polling", "Polling", Icons.Default.Poll),
    TUGAS("tugas", "Tugas & Kalender", Icons.Default.Event),
    KAS("kas", "Kas Kelompok", Icons.Default.AccountBalanceWallet),
    IBADAH("ibadah", "Waktu Sholat", Icons.Default.AccessTime),
    PETA("peta", "Peta", Icons.Default.Map),
    LAPORAN("laporan", "Laporan & Backup", Icons.Default.Description),
    PROFIL("profil", "Profil Saya", Icons.Default.Person),
    PENGATURAN("pengaturan", "Pengaturan", Icons.Default.Settings);

    companion object {
        const val RUTE_HOME = "home"
    }
}

/**
 * Tiga kelompok menu di Beranda. Profil tidak masuk daftar mana pun karena
 * dibuka lewat foto profil di kiri atas.
 */
enum class KategoriMenu(
    val judul: String,
    val keterangan: String,
    val ikon: ImageVector,
    val isi: List<Tujuan>
) {
    UTAMA(
        "Menu Utama", "Administrasi dan kegiatan harian KKN", Icons.Default.Dashboard,
        listOf(
            Tujuan.JURNAL, Tujuan.ABSENSI, Tujuan.SURAT, Tujuan.PROKER, Tujuan.ARSIP,
            Tujuan.INVENTARIS, Tujuan.PENGUMUMAN, Tujuan.TUGAS, Tujuan.KAS
        )
    ),
    UMUM(
        "Menu Umum", "Polling, jadwal sholat, dan peta", Icons.Default.Public,
        listOf(Tujuan.POLLING, Tujuan.IBADAH, Tujuan.PETA)
    ),
    LAINNYA(
        "Lainnya", "Laporan, backup, dan pengaturan", Icons.Default.MoreHoriz,
        listOf(Tujuan.LAPORAN, Tujuan.PENGATURAN)
    )
}

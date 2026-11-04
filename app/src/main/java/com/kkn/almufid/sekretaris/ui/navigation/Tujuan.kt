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
    INVENTARIS("inventaris", "Inventaris Posko", Icons.Default.Inventory2);

    companion object {
        val menuUtama: List<Tujuan> = entries
        const val RUTE_HOME = "home"
    }
}

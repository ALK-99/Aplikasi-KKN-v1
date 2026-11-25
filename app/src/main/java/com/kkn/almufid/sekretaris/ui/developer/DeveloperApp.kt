package com.kkn.almufid.sekretaris.ui.developer

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kkn.almufid.sekretaris.data.auth.AuthRepository
import com.kkn.almufid.sekretaris.data.cloud.JurnalBersamaRepository
import com.kkn.almufid.sekretaris.data.cloud.KoordinasiRepository
import com.kkn.almufid.sekretaris.ui.GenericViewModelFactory
import com.kkn.almufid.sekretaris.ui.auth.DialogInfo
import com.kkn.almufid.sekretaris.ui.components.AvatarGradien
import com.kkn.almufid.sekretaris.ui.components.KartuGelap
import com.kkn.almufid.sekretaris.ui.components.tekanPegas
import com.kkn.almufid.sekretaris.ui.formatRupiah
import com.kkn.almufid.sekretaris.ui.screens.koordinasi.KasScreen
import com.kkn.almufid.sekretaris.ui.screens.koordinasi.KoordinasiViewModel
import com.kkn.almufid.sekretaris.ui.screens.koordinasi.PengumumanScreen
import com.kkn.almufid.sekretaris.ui.screens.koordinasi.PollingScreen
import com.kkn.almufid.sekretaris.ui.screens.koordinasi.TugasScreen
import com.kkn.almufid.sekretaris.ui.screens.profil.ProfilScreen
import com.kkn.almufid.sekretaris.ui.screens.profil.ProfilViewModel
import com.kkn.almufid.sekretaris.ui.theme.*

private enum class HalamanDev(val judul: String) {
    BERANDA("Pusat Kendali"),
    ONE_EYE("One Eye"),
    PENGUMUMAN("Pengumuman"),
    POLLING("Polling"),
    TUGAS("Tugas & Kalender"),
    KAS("Kas Kelompok"),
    AKUN("Kelola Akun"),
    MODERASI("Moderasi Jurnal"),
    KOTAK("Kotak Masuk"),
    EKSPOR("Ekspor Data"),
    PREMIUM("Premium dan Kode"),
    PROFIL("Profil Saya"),
}

/**
 * Aplikasi sisi developer: menu sendiri yang berbeda dari peserta. Developer
 * (admin) bisa membuat pengumuman/polling/tugas/kas, memantau semua data lewat
 * One Eye, mengelola akun, memoderasi jurnal, membaca saran, dan mengekspor data.
 */
@Composable
fun DeveloperApp(authRepo: AuthRepository, onLogout: () -> Unit, onBukaPeserta: () -> Unit = {}) {
    // Kunci per akun supaya ViewModel tidak terbawa ke akun lain setelah ganti login.
    val kunci = authRepo.uidSaatIni ?: "-"
    val devVm: DeveloperViewModel = viewModel(
        key = "dev-$kunci",
        factory = GenericViewModelFactory { DeveloperViewModel(authRepo, KoordinasiRepository(), JurnalBersamaRepository()) }
    )
    val koorVm: KoordinasiViewModel = viewModel(
        key = "koor-$kunci",
        factory = GenericViewModelFactory { KoordinasiViewModel(KoordinasiRepository(), authRepo) }
    )
    val profilVm: ProfilViewModel = viewModel(
        key = "profil-$kunci",
        factory = GenericViewModelFactory { ProfilViewModel(authRepo) }
    )

    val data by devVm.data.collectAsState()
    val status by devVm.status.collectAsState()
    val pesan by devVm.pesan.collectAsState()
    var halaman by remember { mutableStateOf(HalamanDev.BERANDA) }
    BackHandler(enabled = halaman != HalamanDev.BERANDA) { halaman = HalamanDev.BERANDA }

    Column(Modifier.fillMaxSize()) {
        if (halaman != HalamanDev.BERANDA) HeaderDev(halaman.judul) { halaman = HalamanDev.BERANDA }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when (halaman) {
                HalamanDev.BERANDA -> BerandaDeveloper(devVm, data, status, onBuka = { halaman = it }, onLogout = onLogout, onBukaPeserta = onBukaPeserta)
                HalamanDev.ONE_EYE -> OneEyeScreen(devVm)
                HalamanDev.PENGUMUMAN -> PengumumanScreen(koorVm)
                HalamanDev.POLLING -> PollingScreen(koorVm)
                HalamanDev.TUGAS -> TugasScreen(koorVm)
                HalamanDev.KAS -> KasScreen(koorVm)
                HalamanDev.AKUN -> AkunScreen(devVm, data, status)
                HalamanDev.MODERASI -> ModerasiScreen(devVm, data, status.admin)
                HalamanDev.KOTAK -> KotakMasukScreen(data)
                HalamanDev.EKSPOR -> EksporScreen(devVm)
                HalamanDev.PREMIUM -> PremiumDevScreen()
                HalamanDev.PROFIL -> ProfilScreen(profilVm, onLogout = onLogout)
            }
        }
    }
    pesan?.let { DialogInfo(judul = "Info", pesan = it, onTutup = devVm::tutupPesan) }
}

@Composable
private fun HeaderDev(judul: String, onKembali: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.tekanPegas(onKembali).size(44.dp).clip(CircleShape).background(Surface1),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali", tint = TeksUtama)
        }
        Spacer(Modifier.width(14.dp))
        Text(judul, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
    }
}

private class MenuDev(
    val halaman: HalamanDev,
    val ikon: ImageVector,
    val warnaA: Color,
    val warnaB: Color,
    val keterangan: String,
)

private val MENU_SIARAN = listOf(
    MenuDev(HalamanDev.PENGUMUMAN, Icons.Default.Campaign, Color(0xFFD6452E), Color(0xFFFF8A6B), "Siarkan ke semua"),
    MenuDev(HalamanDev.POLLING, Icons.Default.Poll, Color(0xFF8A3FD6), Color(0xFFD08BFF), "Adakan pemungutan suara"),
    MenuDev(HalamanDev.TUGAS, Icons.Default.Event, Color(0xFF1F8F4E), Color(0xFF6BE09A), "Beri tugas dan tenggat"),
    MenuDev(HalamanDev.KAS, Icons.Default.AccountBalanceWallet, Color(0xFFB8860B), Color(0xFFFFD166), "Catat uang kelompok"),
)

private val MENU_KELOLA = listOf(
    MenuDev(HalamanDev.AKUN, Icons.Default.Group, Color(0xFF3B5BDB), Color(0xFF8FA8FF), "Blokir dan reset sandi"),
    MenuDev(HalamanDev.MODERASI, Icons.Default.Gavel, Color(0xFFC9780A), Color(0xFFFFB84D), "Sembunyikan jurnal"),
    MenuDev(HalamanDev.KOTAK, Icons.Default.Inbox, Color(0xFF0E8CA0), Color(0xFF5CE1E6), "Saran dari peserta"),
    MenuDev(HalamanDev.PREMIUM, Icons.Default.Star, Color(0xFFB8860B), Color(0xFFFFD166), "Kode akses dan kunci AI"),
    MenuDev(HalamanDev.EKSPOR, Icons.Default.Download, Color(0xFF5A6072), Color(0xFF9AA0BF), "CSV, PDF, dan JSON"),
)

@Composable
private fun BerandaDeveloper(
    vm: DeveloperViewModel, data: DataDeveloper, status: StatusDeveloper,
    onBuka: (HalamanDev) -> Unit, onLogout: () -> Unit, onBukaPeserta: () -> Unit
) {
    val sekarang = remember(data) { System.currentTimeMillis() }
    val aktif7 = data.profil.count { it.terakhirAktifMillis >= sekarang - 7 * SATU_HARI }
    val saldo = data.kas.filter { it.masuk }.sumOf { it.jumlah } - data.kas.filter { !it.masuk }.sumOf { it.jumlah }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Row(
                    Modifier.weight(1f).tekanPegas { onBuka(HalamanDev.PROFIL) },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AvatarGradien("DV", 46.dp)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Mode developer", color = TeksRedup, fontSize = 12.sp)
                        Text("Pusat Kendali", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
                Row(
                    Modifier.tekanPegas(onLogout).background(Surface1, RoundedCornerShape(14.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Logout, null, tint = Merah, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Keluar", color = Merah, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
        item {
            KartuGelap(Modifier.fillMaxWidth().tekanPegas(onBukaPeserta)) {
                Text("Buka tampilan peserta", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = VioletTerang)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Lihat dan pakai menu seperti peserta: Absensi, Proker, Arsip Nota, Rekonsiliasi Kas, Riwayat, " +
                        "Laporan & Backup. Tekan Kembali di Beranda untuk pulang ke Pusat Kendali.",
                    fontSize = 12.sp, color = TeksRedup, lineHeight = 17.sp
                )
            }
        }
        if (!status.sedangMuat && !status.admin) {
            item {
                KartuGelap(Modifier.fillMaxWidth()) {
                    Text("Mode lihat saja", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Gold)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Akun ini belum terdaftar sebagai admin, jadi belum bisa membuat pengumuman, polling, " +
                            "tugas, atau memblokir akun. Di Firebase Console > Firestore Database > Data, buat " +
                            "koleksi \"admins\", lalu dokumen dengan Document ID sama persis dengan UID di bawah ini, " +
                            "isi satu field apa saja (mis. aktif = true).",
                        fontSize = 12.sp, color = TeksRedup, lineHeight = 17.sp
                    )
                    Spacer(Modifier.height(6.dp))
                    SelectionContainer {
                        Text("UID: ${vm.uidSaya ?: "-"}", fontSize = 12.sp, color = TeksUtama, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                KartuAngka("${data.profil.size}/20", "Peserta", Modifier.weight(1f))
                KartuAngka("$aktif7", "Aktif 7 hari", Modifier.weight(1f))
                KartuAngka("${data.jurnal.size}", "Jurnal bersama", Modifier.weight(1f))
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                KartuAngka("${data.tugas.count { it.selesai }}/${data.tugas.size}", "Tugas selesai", Modifier.weight(1f))
                KartuAngka(formatRupiah(saldo), "Saldo kas", Modifier.weight(1f))
                KartuAngka("${data.saran.size}", "Saran masuk", Modifier.weight(1f))
            }
        }
        item {
            val bentuk = RoundedCornerShape(24.dp)
            Row(
                Modifier.fillMaxWidth().tekanPegas { onBuka(HalamanDev.ONE_EYE) }
                    .clip(bentuk).background(Brush.linearGradient(listOf(Violet, Pink))).padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.size(52.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Visibility, null, tint = Color.White, modifier = Modifier.size(28.dp))
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("One Eye", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    Text(
                        "Pantau semua peserta: profil, jurnal, tugas, suara polling, dan keaktifan.",
                        fontSize = 12.sp, color = Color.White, lineHeight = 16.sp
                    )
                }
            }
        }
        item { JudulBagian("Siaran dan koordinasi") }
        MENU_SIARAN.chunked(2).forEach { pasang -> item { BarisMenu(pasang, 0, onBuka) } }
        item { JudulBagian("Kelola") }
        MENU_KELOLA.chunked(2).forEach { pasang ->
            item { BarisMenu(pasang, if (pasang.any { it.halaman == HalamanDev.KOTAK }) data.saran.size else 0, onBuka) }
        }
    }
}

@Composable
private fun BarisMenu(pasang: List<MenuDev>, lencanaKotak: Int, onBuka: (HalamanDev) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        pasang.forEach { m ->
            KartuGelap(
                Modifier.weight(1f).tekanPegas { onBuka(m.halaman) }.height(118.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(40.dp).clip(RoundedCornerShape(13.dp))
                            .background(Brush.linearGradient(listOf(m.warnaA, m.warnaB))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(m.ikon, null, tint = Color.White, modifier = Modifier.size(22.dp))
                    }
                    if (m.halaman == HalamanDev.KOTAK && lencanaKotak > 0) {
                        Spacer(Modifier.width(8.dp))
                        Text("$lencanaKotak", fontSize = 11.sp, color = Gold, fontWeight = FontWeight.ExtraBold)
                    }
                }
                Column {
                    Text(m.halaman.judul, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(m.keterangan, fontSize = 11.sp, color = TeksRedup)
                }
            }
        }
        if (pasang.size == 1) Spacer(Modifier.weight(1f))
    }
}

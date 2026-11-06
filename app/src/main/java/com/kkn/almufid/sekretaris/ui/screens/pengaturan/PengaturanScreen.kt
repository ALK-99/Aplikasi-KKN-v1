package com.kkn.almufid.sekretaris.ui.screens.pengaturan

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.rememberCoroutineScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.kkn.almufid.sekretaris.BuildConfig
import com.kkn.almufid.sekretaris.ui.components.KartuGelap
import com.kkn.almufid.sekretaris.ui.components.tekanPegas
import com.kkn.almufid.sekretaris.ui.theme.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Nomor kontak admin aplikasi (bukan admin desa/KKN). Dipakai untuk
 * "lupa password" dan "saran & masukan" lewat WhatsApp.
 */
private const val NOMOR_ADMIN = "081384725967"

/**
 * Best-effort: kirim saran ke Firestore koleksi `feedback` kalau pengguna
 * sedang login (lihat firestore.rules). Gagal diam-diam (mis. offline)
 * karena jalur utama penyampaian saran tetap WhatsApp — Firestore di
 * sini hanya salinan arsip untuk developer.
 */
private fun simpanSaranKeFirestore(teks: String, scope: CoroutineScope) {
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
    scope.launch {
        try {
            FirebaseFirestore.getInstance().collection("feedback").add(
                mapOf("uid" to uid, "isi" to teks.trim(), "dikirimPadaMillis" to System.currentTimeMillis())
            )
        } catch (_: Exception) {
            // diam-diam saja, lihat catatan di atas
        }
    }
}

private enum class SubHalaman { DAFTAR, NOMOR_DARURAT, TENTANG, SARAN, LEGALITAS, FAQ }

@Composable
fun PengaturanScreen(onLogout: () -> Unit = {}) {
    var halaman by remember { mutableStateOf(SubHalaman.DAFTAR) }
    var dialogKeluarTerbuka by remember { mutableStateOf(false) }
    BackHandler(enabled = halaman != SubHalaman.DAFTAR) { halaman = SubHalaman.DAFTAR }

    val kembali = { halaman = SubHalaman.DAFTAR }

    AnimatedContent(
        targetState = halaman,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "pengaturan"
    ) { h ->
        when (h) {
            SubHalaman.DAFTAR -> DaftarPengaturan(onBuka = { halaman = it }, onMintaKeluar = { dialogKeluarTerbuka = true })
            SubHalaman.NOMOR_DARURAT -> SubHalamanBerjudul("Nomor Darurat", kembali) { NomorDaruratIsi() }
            SubHalaman.TENTANG -> SubHalamanBerjudul("Versi Aplikasi", kembali) { TentangIsi() }
            SubHalaman.SARAN -> SubHalamanBerjudul("Saran & Masukan", kembali) { SaranIsi() }
            SubHalaman.LEGALITAS -> SubHalamanBerjudul("Hak Legalitas & Ketentuan", kembali) { LegalitasIsi() }
            SubHalaman.FAQ -> SubHalamanBerjudul("FAQ", kembali) { FaqIsi() }
        }
    }

    if (dialogKeluarTerbuka) {
        AlertDialog(
            onDismissRequest = { dialogKeluarTerbuka = false },
            containerColor = Surface1,
            titleContentColor = TeksUtama,
            textContentColor = TeksSoft,
            title = { Text("Keluar Akun?", fontWeight = FontWeight.ExtraBold) },
            text = { Text("Anda akan kembali ke halaman login. Data yang sudah tersimpan tidak akan hilang.") },
            confirmButton = {
                TextButton(onClick = { dialogKeluarTerbuka = false; onLogout() }) {
                    Text("Keluar", color = Merah, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { dialogKeluarTerbuka = false }) { Text("Batal", color = TeksRedup) }
            }
        )
    }
}

/** Bungkus satu sub-halaman pengaturan dengan header + tombol kembali yang terlihat. */
@Composable
private fun SubHalamanBerjudul(judul: String, onKembali: () -> Unit, konten: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.tekanPegas(onKembali).size(40.dp).clip(CircleShape).background(Surface1),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali", tint = TeksUtama, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(12.dp))
            Text(judul, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
        }
        konten()
    }
}

@Composable
private fun DaftarPengaturan(onBuka: (SubHalaman) -> Unit, onMintaKeluar: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp)) {
        Text("Info, bantuan, dan kontak darurat", color = TeksRedup, fontSize = 13.sp)
        Spacer(Modifier.height(20.dp))

        ItemPengaturan(Icons.Default.LocalPolice, "Nomor Darurat", "Polisi, rumah sakit, pemadam kebakaran") {
            onBuka(SubHalaman.NOMOR_DARURAT)
        }
        ItemPengaturan(Icons.Default.Info, "Versi Aplikasi", "v${BuildConfig.VERSION_NAME} • oleh ${BuildConfig.GITHUB_OWNER}") {
            onBuka(SubHalaman.TENTANG)
        }
        ItemPengaturan(Icons.AutoMirrored.Filled.Chat, "Saran & Masukan", "Kirim masukan untuk pengembang") {
            onBuka(SubHalaman.SARAN)
        }
        ItemPengaturan(Icons.Default.Gavel, "Hak Legalitas & Ketentuan", "Syarat penggunaan aplikasi") {
            onBuka(SubHalaman.LEGALITAS)
        }
        ItemPengaturan(Icons.AutoMirrored.Filled.HelpOutline, "FAQ", "Pertanyaan yang sering ditanyakan") {
            onBuka(SubHalaman.FAQ)
        }
        ItemPengaturan(Icons.Default.Logout, "Keluar Akun", "Kembali ke halaman login", warna = Merah, onClick = onMintaKeluar)
    }
}

@Composable
private fun ItemPengaturan(ikon: ImageVector, judul: String, sub: String, warna: Color = VioletTerang, onClick: () -> Unit) {
    KartuGelap(Modifier.fillMaxWidth().padding(bottom = 12.dp).tekanPegas(onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(Surface2),
                contentAlignment = Alignment.Center
            ) {
                Icon(ikon, null, tint = warna, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(judul, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = if (warna == Merah) Merah else TeksUtama)
                Text(sub, fontSize = 12.sp, color = TeksRedup)
            }
            Icon(Icons.Default.ChevronRight, null, tint = TeksRedup)
        }
    }
}

// ---------------------------------------------------------------------
// Nomor Darurat
// ---------------------------------------------------------------------

private data class KontakDarurat(val nama: String, val keterangan: String, val nomor: String, val ikon: ImageVector, val warna: Color)

/**
 * TODO (wajib dicek sebelum rilis): nomor instansi lokal (RSUD Wonosari,
 * Polres Gunungkidul, Damkar Gunungkidul) di bawah ini BELUM diverifikasi
 * ke sumber resmi dan harus diisi/dikonfirmasi manual oleh pengembang.
 * Nomor panggilan darurat nasional (110/112/113/119) sudah baku secara
 * nasional dan aman dipakai apa adanya.
 */
private val daftarKontakDarurat = listOf(
    KontakDarurat("Polisi (Nasional)", "Call center Polri 24 jam", "110", Icons.Default.LocalPolice, Color(0xFF2E7BD6)),
    KontakDarurat("Nomor Darurat Terpadu", "NTPD — darurat segala jenis", "112", Icons.Default.Emergency, Pink),
    KontakDarurat("Pemadam Kebakaran (Nasional)", "Call center Damkar 24 jam", "113", Icons.Default.LocalFireDepartment, Color(0xFFC9780A)),
    KontakDarurat("Ambulans / Kemenkes", "Public Safety Center 119", "119", Icons.Default.LocalHospital, Color(0xFF0E8F6E)),
    KontakDarurat("Polres Gunungkidul", "TODO: isi nomor resmi terverifikasi", "", Icons.Default.LocalPolice, Color(0xFF2E7BD6)),
    KontakDarurat("RSUD Wonosari", "TODO: isi nomor IGD resmi terverifikasi", "", Icons.Default.LocalHospital, Color(0xFF0E8F6E)),
    KontakDarurat("Damkar Kab. Gunungkidul", "TODO: isi nomor resmi terverifikasi", "", Icons.Default.LocalFireDepartment, Color(0xFFC9780A)),
)

@Composable
private fun ColumnScope.NomorDaruratIsi() {
    val context = LocalContext.current
    LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 18.dp, vertical = 4.dp)) {
        items(daftarKontakDarurat) { k ->
                KartuGelap(
                    Modifier.fillMaxWidth().padding(bottom = 12.dp)
                        .tekanPegas {
                            if (k.nomor.isNotBlank()) {
                                context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${k.nomor}")))
                            }
                        }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(k.warna.copy(alpha = 0.22f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(k.ikon, null, tint = k.warna, modifier = Modifier.size(22.dp))
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(k.nama, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text(k.keterangan, fontSize = 12.sp, color = TeksRedup)
                        }
                        if (k.nomor.isNotBlank()) {
                            Icon(Icons.Default.Call, "Telepon", tint = Mint)
                        }
                    }
                }
            }
        item {
            Text(
                "Nomor nasional (110/112/113/119) berlaku di seluruh Indonesia termasuk wilayah Gunungkidul. " +
                    "Nomor instansi lokal perlu dikonfirmasi pengembang sebelum dipakai di lapangan.",
                fontSize = 11.sp, color = TeksRedup, modifier = Modifier.padding(vertical = 8.dp)
            )
        }
    }
}

// ---------------------------------------------------------------------
// Tentang / Versi Aplikasi
// ---------------------------------------------------------------------

@Composable
private fun ColumnScope.TentangIsi() {
    val uriHandler = LocalUriHandler.current
    Column(Modifier.padding(18.dp)) {
        KartuGelap(Modifier.fillMaxWidth()) {
            BarisInfo("Nama Aplikasi", "Sekretaris KKN")
            BarisInfo("Versi", BuildConfig.VERSION_NAME)
            BarisInfo("Kode Versi", BuildConfig.VERSION_CODE.toString())
            BarisInfo("Pemilik / Developer", BuildConfig.GITHUB_OWNER)
        }
        Spacer(Modifier.height(14.dp))
        KartuGelap(
            Modifier.fillMaxWidth().tekanPegas {
                uriHandler.openUri("https://github.com/${BuildConfig.GITHUB_OWNER}/${BuildConfig.GITHUB_REPO}")
            }
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Code, null, tint = VioletTerang)
                Spacer(Modifier.width(10.dp))
                Text("Lihat repository di GitHub", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun BarisInfo(label: String, nilai: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 13.sp, color = TeksRedup)
        Text(nilai, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

// ---------------------------------------------------------------------
// Saran & Masukan
// ---------------------------------------------------------------------

@Composable
private fun ColumnScope.SaranIsi() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var teks by remember { mutableStateOf("") }
    Column(Modifier.padding(18.dp)) {
        Text(
            "Tulis saran, laporan bug, atau masukan fitur. Pesan akan dikirim ke pengembang lewat WhatsApp.",
            fontSize = 13.sp, color = TeksRedup
        )
        Spacer(Modifier.height(14.dp))
        OutlinedTextField(
            value = teks, onValueChange = { teks = it },
            modifier = Modifier.fillMaxWidth().height(160.dp),
            placeholder = { Text("Tulis masukan di sini…") },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TeksUtama, unfocusedTextColor = TeksUtama,
                focusedBorderColor = VioletTerang, unfocusedBorderColor = Surface2,
                cursorColor = VioletTerang
            )
        )
        Spacer(Modifier.height(14.dp))
        KartuGelap(
            Modifier.fillMaxWidth().tekanPegas {
                if (teks.isNotBlank()) {
                    simpanSaranKeFirestore(teks, scope)
                    val pesan = Uri.encode("Saran/masukan untuk Sekretaris KKN:\n\n$teks")
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/62${NOMOR_ADMIN.removePrefix("0")}?text=$pesan"))
                    )
                }
            }
        ) {
            Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.AutoMirrored.Filled.Send, null, tint = Mint)
                Spacer(Modifier.width(8.dp))
                Text("Kirim via WhatsApp", fontWeight = FontWeight.Bold, color = Mint)
            }
        }
    }
}

// ---------------------------------------------------------------------
// Legalitas & Ketentuan
// ---------------------------------------------------------------------

@Composable
private fun ColumnScope.LegalitasIsi() {
    Column(Modifier.padding(18.dp).verticalScroll(rememberScrollState())) {
        TeksLegal(
            "Hak Cipta",
            "Aplikasi Sekretaris KKN dikembangkan untuk keperluan administrasi internal kelompok KKN. " +
                "Kode sumber dan desain aplikasi adalah milik pengembang (${BuildConfig.GITHUB_OWNER}) kecuali dinyatakan lain."
        )
        TeksLegal(
            "Syarat Penggunaan",
            "Aplikasi ini disediakan apa adanya untuk membantu pencatatan kegiatan KKN (jurnal, absensi, surat, " +
                "program kerja, nota, dan inventaris). Pengguna bertanggung jawab atas keakuratan data yang " +
                "dimasukkan."
        )
        TeksLegal(
            "Privasi Data",
            "Data yang dimasukkan disimpan secara lokal di perangkat pengguna, kecuali dinyatakan lain pada " +
                "fitur yang secara eksplisit mengunggah data ke server."
        )
        TeksLegal(
            "Tanggung Jawab",
            "Pengembang tidak bertanggung jawab atas kehilangan data akibat penghapusan aplikasi, kerusakan " +
                "perangkat, atau penyalahgunaan di luar kendali pengembang."
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Dokumen ini adalah draf awal dan sebaiknya ditinjau ulang sebelum aplikasi dipublikasikan secara luas.",
            fontSize = 11.sp, color = TeksRedup
        )
    }
}

@Composable
private fun TeksLegal(judul: String, isi: String) {
    Column(Modifier.padding(bottom = 18.dp)) {
        Text(judul, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = VioletTerang)
        Spacer(Modifier.height(6.dp))
        Text(isi, fontSize = 13.sp, color = TeksSoft, lineHeight = 19.sp)
    }
}

// ---------------------------------------------------------------------
// FAQ
// ---------------------------------------------------------------------

private val daftarFaq = listOf(
    "Apakah data tersimpan jika HP tidak ada internet?" to
        "Ya. Semua data operasional (jurnal, absensi, surat, proker, nota, inventaris) disimpan secara lokal di " +
        "perangkat dan tidak memerlukan internet, kecuali saat mengecek pembaruan aplikasi.",
    "Bagaimana cara memperbarui aplikasi ke versi terbaru?" to
        "Aplikasi otomatis mengecek versi terbaru di GitHub setiap kali dibuka. Jika ada versi baru, akan muncul " +
        "pop-up untuk mengunduh dan memasang pembaruan tanpa perlu hapus-install ulang.",
    "Data saya hilang setelah uninstall aplikasi, kenapa?" to
        "Karena saat ini data disimpan lokal di perangkat. Hindari uninstall sebelum mengekspor/mencadangkan data " +
        "penting.",
    "Bagaimana jika lupa kata sandi akun?" to
        "Hubungi admin aplikasi melalui WhatsApp di menu Saran & Masukan, atau langsung ke nomor admin yang " +
        "tertera.",
)

@Composable
private fun ColumnScope.FaqIsi() {
    LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 18.dp, vertical = 4.dp)) {
        items(daftarFaq) { (pertanyaan, jawaban) ->
            var terbuka by remember { mutableStateOf(false) }
            KartuGelap(
                Modifier.fillMaxWidth().padding(bottom = 12.dp).tekanPegas { terbuka = !terbuka }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(pertanyaan, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Icon(
                        if (terbuka) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        null, tint = TeksRedup
                    )
                }
                if (terbuka) {
                    Spacer(Modifier.height(8.dp))
                    Text(jawaban, fontSize = 13.sp, color = TeksSoft, lineHeight = 18.sp)
                }
            }
        }
    }
}


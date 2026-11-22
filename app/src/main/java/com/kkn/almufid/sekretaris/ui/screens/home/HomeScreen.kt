package com.kkn.almufid.sekretaris.ui.screens.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kkn.almufid.sekretaris.data.cloud.Pengumuman
import com.kkn.almufid.sekretaris.ui.components.*
import com.kkn.almufid.sekretaris.ui.navigation.KategoriMenu
import com.kkn.almufid.sekretaris.ui.navigation.Tujuan
import com.kkn.almufid.sekretaris.ui.screens.ibadah.DAFTAR_KOTA
import com.kkn.almufid.sekretaris.ui.screens.ibadah.WaktuSholat
import com.kkn.almufid.sekretaris.ui.theme.*
import kotlinx.coroutines.delay
import java.util.Calendar
import java.util.TimeZone

private const val TINGGI_KARTU_ATAS = 112

@Composable
fun HomeScreen(viewModel: HomeViewModel, onBuka: (Tujuan) -> Unit) {
    val r by viewModel.ringkasan.collectAsState()
    val pengumuman by viewModel.pengumumanTerbaru.collectAsState()
    val persen = if (r.prokerTotal == 0) 0 else r.prokerSelesai * 100 / r.prokerTotal
    val frac by animateFloatAsState(persen / 100f, tween(900), label = "persen")
    // -1 = semua kelompok menu tertutup. Disimpan supaya tetap terbuka saat kembali dari halaman lain.
    var terbuka by rememberSaveable { mutableStateOf(-1) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 32.dp)
    ) {
        // Foto profil = pintu ke halaman Profil Saya.
        Row(
            Modifier.tekanPegas { onBuka(Tujuan.PROFIL) },
            verticalAlignment = Alignment.CenterVertically
        ) {
            AvatarGradien("SK", 46.dp)
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Selamat datang", color = TeksRedup, fontSize = 12.sp)
                Text("Halo, Sekretaris", fontSize = 19.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
        Spacer(Modifier.height(20.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ProkerKotak(persen, frac, r.prokerSelesai, r.prokerTotal) { onBuka(Tujuan.PROKER) }
            PengumumanRingkas(
                p = pengumuman,
                belumDibaca = pengumuman?.let { viewModel.uidSaya !in it.dibacaOleh } == true,
                modifier = Modifier.weight(1f)
            ) { onBuka(Tujuan.PENGUMUMAN) }
        }
        Spacer(Modifier.height(12.dp))
        SholatYogyakarta { onBuka(Tujuan.IBADAH) }

        Spacer(Modifier.height(24.dp))
        Text("Menu", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(12.dp))
        KategoriMenu.entries.forEachIndexed { i, kategori ->
            KategoriKartu(
                kategori = kategori,
                terbuka = terbuka == i,
                onKlik = { terbuka = if (terbuka == i) -1 else i },
                onBuka = onBuka
            )
            Spacer(Modifier.height(12.dp))
        }
    }
}

/** Kotak kecil ringkasan program kerja (menggantikan kartu besar sebelumnya). */
@Composable
private fun ProkerKotak(persen: Int, frac: Float, selesai: Int, total: Int, onClick: () -> Unit) {
    val bentuk = RoundedCornerShape(22.dp)
    Column(
        Modifier.size(TINGGI_KARTU_ATAS.dp).tekanPegas(onClick)
            .clip(bentuk).background(GradUtama).padding(12.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text("Proker", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Text("$persen%", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
        Text(
            if (total == 0) "Belum ada" else "$selesai/$total selesai",
            fontSize = 11.sp, color = Color.White
        )
        BatangProgres(frac, latar = Color.White.copy(alpha = 0.3f), isi = SolidColor(Color.White))
    }
}

@Composable
private fun PengumumanRingkas(
    p: Pengumuman?, belumDibaca: Boolean, modifier: Modifier, onClick: () -> Unit
) {
    KartuGelap(modifier.height(TINGGI_KARTU_ATAS.dp).tekanPegas(onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Campaign, null, tint = Gold, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Pengumuman", fontSize = 11.sp, color = TeksRedup, fontWeight = FontWeight.Bold)
            if (belumDibaca) {
                Spacer(Modifier.width(6.dp))
                Box(Modifier.size(8.dp).clip(CircleShape).background(Merah))
            }
        }
        Spacer(Modifier.height(4.dp))
        if (p == null) {
            Text("Belum ada pengumuman", fontSize = 12.sp, color = TeksRedup)
        } else {
            Text(
                p.judul.ifBlank { "(tanpa judul)" }, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Text(
                p.isi, fontSize = 11.sp, color = TeksRedup, lineHeight = 15.sp,
                maxLines = 2, overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** Jadwal sholat khusus Yogyakarta (WIB), dihitung di HP tanpa internet. */
@Composable
private fun SholatYogyakarta(onClick: () -> Unit) {
    val kota = remember { DAFTAR_KOTA.first { it.nama == "Yogyakarta" } }
    val wib = remember { TimeZone.getTimeZone("Asia/Jakarta") }
    var sekarang by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) { delay(30_000); sekarang = System.currentTimeMillis() }
    }
    val kal = Calendar.getInstance(wib).apply { timeInMillis = sekarang }
    val jadwal = remember(kal.get(Calendar.YEAR), kal.get(Calendar.DAY_OF_YEAR)) {
        WaktuSholat.hitung(
            kota.lintang, kota.bujur, 7.0,
            kal.get(Calendar.YEAR), kal.get(Calendar.MONTH) + 1, kal.get(Calendar.DAY_OF_MONTH)
        )
    }
    val waktu = listOf(
        "Subuh" to jadwal.subuh, "Dzuhur" to jadwal.dzuhur, "Ashar" to jadwal.ashar,
        "Maghrib" to jadwal.maghrib, "Isya" to jadwal.isya,
    )
    fun menit(jam: Double) = Math.round(jam * 60.0).toInt()
    val menitSekarang = kal.get(Calendar.HOUR_OF_DAY) * 60 + kal.get(Calendar.MINUTE)
    val berikutnya = waktu.firstOrNull { menit(it.second) > menitSekarang }
    val namaBerikut = (berikutnya ?: waktu.first()).first
    val sisaMenit = if (berikutnya != null) menit(berikutnya.second) - menitSekarang
        else menit(waktu.first().second) + 1440 - menitSekarang
    val teksSisa = if (sisaMenit >= 60) "${sisaMenit / 60} j ${sisaMenit % 60} m lagi" else "$sisaMenit menit lagi"

    KartuGelap(Modifier.fillMaxWidth().tekanPegas(onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.AccessTime, null, tint = Mint, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Jadwal sholat · Yogyakarta", fontSize = 11.sp, color = TeksRedup, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                "$namaBerikut ${WaktuSholat.format((berikutnya ?: waktu.first()).second)}",
                fontSize = 20.sp, fontWeight = FontWeight.ExtraBold
            )
            Spacer(Modifier.width(10.dp))
            Text(teksSisa, fontSize = 12.sp, color = Mint, modifier = Modifier.padding(bottom = 3.dp))
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth()) {
            waktu.forEach { (nama, jam) ->
                val aktif = nama == namaBerikut
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(nama, fontSize = 10.sp, color = if (aktif) Mint else TeksRedup, textAlign = TextAlign.Center)
                    Text(
                        WaktuSholat.format(jam), fontSize = 13.sp,
                        fontWeight = if (aktif) FontWeight.ExtraBold else FontWeight.Medium,
                        color = if (aktif) Mint else TeksUtama
                    )
                }
            }
        }
    }
}

@Composable
private fun KategoriKartu(
    kategori: KategoriMenu, terbuka: Boolean, onKlik: () -> Unit, onBuka: (Tujuan) -> Unit
) {
    val (a, b) = when (kategori) {
        KategoriMenu.UTAMA -> Violet to VioletTerang
        KategoriMenu.UMUM -> Color(0xFF0E8F6E) to Mint
        KategoriMenu.LAINNYA -> Color(0xFF5A6072) to Color(0xFF9AA0BF)
    }
    Column {
        KartuGelap(Modifier.fillMaxWidth().tekanPegas(onKlik)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(44.dp).clip(RoundedCornerShape(14.dp))
                        .background(Brush.linearGradient(listOf(a, b))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(kategori.ikon, null, tint = Color.White, modifier = Modifier.size(24.dp))
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(kategori.judul, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                    Text(kategori.keterangan, fontSize = 11.sp, color = TeksRedup)
                }
                Icon(
                    if (terbuka) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    if (terbuka) "Tutup" else "Buka", tint = TeksRedup
                )
            }
        }
        AnimatedVisibility(visible = terbuka) {
            Column(Modifier.padding(top = 12.dp)) {
                kategori.isi.chunked(2).forEachIndexed { baris, pasang ->
                    Row(
                        Modifier.fillMaxWidth().padding(bottom = if (baris == (kategori.isi.size - 1) / 2) 0.dp else 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        pasang.forEach { tujuan ->
                            MenuKartu(tujuan, Modifier.weight(1f)) { onBuka(tujuan) }
                        }
                        // Baris terakhir ganjil: sisakan ruang supaya ukuran kartu tetap sama.
                        if (pasang.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuKartu(tujuan: Tujuan, modifier: Modifier, onClick: () -> Unit) {
    val (a, b) = when (tujuan) {
        Tujuan.JURNAL -> Violet to VioletTerang
        Tujuan.ABSENSI -> Color(0xFF0E8F6E) to Mint
        Tujuan.SURAT -> Color(0xFFC9780A) to Gold
        Tujuan.PROKER -> Color(0xFF2E7BD6) to Color(0xFF6BB6FF)
        Tujuan.ARSIP -> Pink to Color(0xFFFF7A9E)
        Tujuan.INVENTARIS -> Color(0xFF7A4BD6) to Color(0xFFC08BFF)
        Tujuan.PENGUMUMAN -> Color(0xFFD6452E) to Color(0xFFFF8A6B)
        Tujuan.POLLING -> Color(0xFF8A3FD6) to Color(0xFFD08BFF)
        Tujuan.TUGAS -> Color(0xFF1F8F4E) to Color(0xFF6BE09A)
        Tujuan.KAS -> Color(0xFFB8860B) to Color(0xFFFFD166)
        Tujuan.IBADAH -> Color(0xFF0B7A5C) to Color(0xFF2BD4A0)
        Tujuan.PETA -> Color(0xFF0E8CA0) to Color(0xFF5CE1E6)
        Tujuan.LAPORAN -> Color(0xFF3B5BDB) to Color(0xFF8FA8FF)
        Tujuan.PROFIL -> Color(0xFF0E7C9A) to Color(0xFF4FD1E8)
        Tujuan.PENGATURAN -> Color(0xFF5A6072) to Color(0xFF9AA0BF)
    }
    KartuGelap(
        modifier.tekanPegas(onClick).height(112.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Box(
            Modifier.size(42.dp).clip(RoundedCornerShape(14.dp))
                .background(Brush.linearGradient(listOf(a, b))),
            contentAlignment = Alignment.Center
        ) {
            Icon(tujuan.ikon, null, tint = Color.White, modifier = Modifier.size(22.dp))
        }
        Text(tujuan.judul, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

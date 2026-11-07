package com.kkn.almufid.sekretaris.ui.screens.home

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kkn.almufid.sekretaris.ui.components.*
import com.kkn.almufid.sekretaris.ui.navigation.Tujuan
import com.kkn.almufid.sekretaris.ui.theme.*

@Composable
fun HomeScreen(viewModel: HomeViewModel, onBuka: (Tujuan) -> Unit) {
    val r by viewModel.ringkasan.collectAsState()
    val persen = if (r.prokerTotal == 0) 0 else r.prokerSelesai * 100 / r.prokerTotal
    val frac by animateFloatAsState(persen / 100f, tween(900), label = "persen")

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 120.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AvatarGradien("SK", 46.dp)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Selamat datang", color = TeksRedup, fontSize = 12.sp)
                    Text("Halo, Sekretaris", fontSize = 19.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
            Spacer(Modifier.height(22.dp))
            // Bisa digeser ke samping supaya tidak terpotong di layar sempit
            // (5 lingkaran 62dp + jarak melebihi lebar layar HP kecil).
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                r.anggota.take(8).forEach { orang ->
                    StoryCincin(inisialDari(orang.nama), orang.nama.take(7)) { onBuka(Tujuan.ABSENSI) }
                }
                StoryCincin("+", "Anggota") { onBuka(Tujuan.ABSENSI) }
            }
            Spacer(Modifier.height(26.dp))
            HeroKartu(persen, frac, r.prokerSelesai, r.prokerTotal)
            Spacer(Modifier.height(26.dp))
            Text("Menu cepat", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(12.dp))
            Tujuan.entries.chunked(2).forEachIndexed { baris, pasang ->
                Row(
                    Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    pasang.forEachIndexed { k, tujuan ->
                        MenuKartu(tujuan, Modifier.weight(1f).munculBertahap(baris * 2 + k)) { onBuka(tujuan) }
                    }
                    // Baris terakhir ganjil: sisakan ruang kosong supaya kartu
                    // tidak melebar penuh dan ukurannya sama dengan baris lain.
                    if (pasang.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
        NavMelayang(Modifier.align(Alignment.BottomCenter), onBuka)
    }
}

@Composable
private fun StoryCincin(inisial: String, nama: String, onClick: () -> Unit) {
    val t = rememberInfiniteTransition(label = "story")
    val s by t.animateFloat(
        initialValue = 1f, targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "s"
    )
    Column(Modifier.tekanPegas(onClick), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.graphicsLayer { scaleX = s; scaleY = s }
                .size(62.dp).clip(CircleShape).background(GradStory).padding(3.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier.fillMaxSize().clip(CircleShape).background(Ground).padding(3.dp)
                    .clip(CircleShape).background(Surface1),
                contentAlignment = Alignment.Center
            ) {
                Text(inisial, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(nama, fontSize = 11.sp, color = TeksRedup, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun HeroKartu(persen: Int, frac: Float, selesai: Int, total: Int) {
    val t = rememberInfiniteTransition(label = "hero")
    val naik by t.animateFloat(
        initialValue = 0f, targetValue = -10f,
        animationSpec = infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "naik"
    )
    val bentuk = RoundedCornerShape(28.dp)
    Box(
        Modifier.fillMaxWidth().height(170.dp)
            .graphicsLayer {
                rotationX = 5f
                rotationY = -7f
                translationY = naik * density
                cameraDistance = 14f * density
            }
            .shadow(24.dp, bentuk, ambientColor = Pink, spotColor = Pink)
            .clip(bentuk).background(GradUtama).padding(20.dp)
    ) {
        Box(
            Modifier.align(Alignment.TopEnd).offset(40.dp, (-40).dp).size(140.dp)
                .clip(CircleShape).background(Color.White.copy(alpha = 0.16f))
        )
        Box(
            Modifier.align(Alignment.BottomEnd).offset(10.dp, 40.dp).size(90.dp)
                .clip(CircleShape).background(Color.White.copy(alpha = 0.12f))
        )
        Column(Modifier.align(Alignment.CenterStart)) {
            Text("Program kerja", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text("$persen%", fontSize = 44.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            Text(
                if (total == 0) "Belum ada proker tercatat" else "$selesai dari $total proker selesai",
                fontSize = 13.sp, color = Color.White
            )
            Spacer(Modifier.height(12.dp))
            BatangProgres(frac, latar = Color.White.copy(alpha = 0.3f), isi = SolidColor(Color.White))
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

@Composable
private fun NavMelayang(modifier: Modifier, onBuka: (Tujuan) -> Unit) {
    val bentuk = RoundedCornerShape(34.dp)
    Row(
        modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 20.dp).height(68.dp)
            .shadow(18.dp, bentuk).clip(bentuk).background(Surface2.copy(alpha = 0.96f)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        Icon(Icons.Default.Home, "Beranda", tint = Color.White, modifier = Modifier.size(26.dp))
        ItemNav(Tujuan.PROKER, onBuka)
        Box(
            Modifier.tekanPegas { onBuka(Tujuan.JURNAL) }.offset(y = (-14).dp).size(56.dp)
                .shadow(14.dp, CircleShape, ambientColor = Pink, spotColor = Pink)
                .clip(CircleShape).background(GradUtama),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Add, "Tambah jurnal", tint = Color.White, modifier = Modifier.size(28.dp))
        }
        ItemNav(Tujuan.ARSIP, onBuka)
        ItemNav(Tujuan.ABSENSI, onBuka)
    }
}

@Composable
private fun ItemNav(tujuan: Tujuan, onBuka: (Tujuan) -> Unit) {
    Box(Modifier.tekanPegas { onBuka(tujuan) }.size(44.dp), contentAlignment = Alignment.Center) {
        Icon(tujuan.ikon, tujuan.judul, tint = TeksRedup, modifier = Modifier.size(24.dp))
    }
}

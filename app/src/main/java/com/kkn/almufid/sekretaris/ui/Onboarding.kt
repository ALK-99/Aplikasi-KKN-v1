package com.kkn.almufid.sekretaris.ui

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kkn.almufid.sekretaris.ui.components.tekanPegas
import com.kkn.almufid.sekretaris.ui.theme.*

object PanduanPref {
    private const val PREF = "pengaturan_tampilan"
    fun sudahDilihat(context: Context): Boolean =
        try { context.getSharedPreferences(PREF, Context.MODE_PRIVATE).getBoolean("panduan_selesai", false) } catch (e: Exception) { true }

    fun tandai(context: Context) {
        try { context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putBoolean("panduan_selesai", true).apply() } catch (e: Exception) { }
    }
}

private class HalamanPanduan(val ikon: ImageVector, val judul: String, val isi: String)

private val halamanPanduan = listOf(
    HalamanPanduan(
        Icons.Default.Groups, "Selamat datang",
        "Aplikasi ini membantu seluruh pengurus KKN bekerja rapi: absensi, surat, proker, kas, sampai laporan, " +
            "dalam satu tempat yang sama di semua HP."
    ),
    HalamanPanduan(
        Icons.Default.Dashboard, "Menu per divisi",
        "Di Beranda, buka Menu Utama lalu pilih divisimu: Sekretaris, Bendahara, atau Media. " +
            "Pengumuman, polling, jadwal sholat, dan peta ada di Menu Umum."
    ),
    HalamanPanduan(
        Icons.Default.CloudSync, "Data selalu sinkron",
        "Data dibagi per desa. Apa yang kamu catat langsung terlihat oleh teman satu desa, " +
            "dan tetap bisa dibuka tanpa sinyal. Setiap perubahan tercatat di Riwayat Perubahan."
    ),
    HalamanPanduan(
        Icons.Default.Search, "Cari apa saja",
        "Ketuk ikon kaca pembesar di Beranda untuk mencari anggota, proker, surat, barang, nota, atau jurnal sekaligus."
    ),
    HalamanPanduan(
        Icons.Default.Backup, "Amankan datamu",
        "Buat Cadangan Lengkap di Lainnya > Laporan & Backup secara berkala. " +
            "Panduan ini bisa dibuka lagi kapan saja dari Pengaturan."
    ),
)

/** Panduan singkat untuk pengguna baru; bisa dilewati dan dibuka ulang dari Pengaturan. */
@Composable
fun Onboarding(onSelesai: () -> Unit) {
    var indeks by rememberSaveable { mutableIntStateOf(0) }
    val terakhir = indeks == halamanPanduan.lastIndex
    BackHandler(enabled = indeks > 0) { indeks-- }
    val h = halamanPanduan[indeks]

    Column(
        Modifier.fillMaxSize().background(Ground).padding(horizontal = 28.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            if (!terakhir) {
                Text(
                    "Lewati", color = TeksRedup, fontWeight = FontWeight.Bold, fontSize = 14.sp,
                    modifier = Modifier.tekanPegas(onSelesai).padding(8.dp)
                )
            }
        }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                Modifier.size(110.dp).clip(RoundedCornerShape(16.dp)).background(GradUtama),
                contentAlignment = Alignment.Center
            ) { Icon(h.ikon, null, tint = androidx.compose.ui.graphics.Color.White, modifier = Modifier.size(56.dp)) }
            Spacer(Modifier.height(32.dp))
            Text(h.judul, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
            Spacer(Modifier.height(14.dp))
            Text(h.isi, fontSize = 15.sp, color = TeksSoft, textAlign = TextAlign.Center, lineHeight = 22.sp)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 18.dp)) {
            halamanPanduan.indices.forEach { i ->
                Box(
                    Modifier.size(if (i == indeks) 22.dp else 8.dp, 8.dp).clip(CircleShape)
                        .background(if (i == indeks) VioletTerang else Surface2)
                )
            }
        }
        Box(
            Modifier.fillMaxWidth().tekanPegas { if (terakhir) onSelesai() else indeks++ }
                .clip(RoundedCornerShape(16.dp)).background(GradUtama).padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                if (terakhir) "Mulai" else "Lanjut", color = androidx.compose.ui.graphics.Color.White,
                fontWeight = FontWeight.ExtraBold, fontSize = 16.sp
            )
        }
    }
}

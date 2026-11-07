package com.kkn.almufid.sekretaris.ui.developer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kkn.almufid.sekretaris.data.auth.Peran
import com.kkn.almufid.sekretaris.data.auth.ProfilPengguna
import com.kkn.almufid.sekretaris.ui.auth.DialogInfo
import com.kkn.almufid.sekretaris.ui.components.AvatarGradien
import com.kkn.almufid.sekretaris.ui.components.KartuGelap
import com.kkn.almufid.sekretaris.ui.components.inisialDari
import com.kkn.almufid.sekretaris.ui.components.tekanPegas
import com.kkn.almufid.sekretaris.ui.theme.*

/**
 * Lobi developer: ringkasan akun terdaftar + daftar anggota, dan (khusus
 * akun yang terdaftar di koleksi `admins`) tombol blokir/buka blokir.
 * Menu developer lain menyusul bertahap.
 */
@Composable
fun DeveloperLobbyScreen(vm: DeveloperViewModel, onLogout: () -> Unit) {
    val status by vm.status.collectAsState()
    val pesan by vm.pesan.collectAsState()
    val diblokir = status.daftar.count { it.akunDiblokir }
    val aktif = status.daftar.size - diblokir

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 22.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Lobi Developer", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Kelola akun peserta KKN", fontSize = 12.sp, color = TeksRedup)
                }
                Row(
                    Modifier.tekanPegas(onLogout)
                        .background(Surface1, RoundedCornerShape(14.dp))
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
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                KartuAngka("${status.totalPendaftaran}", "Terdaftar", Modifier.weight(1f))
                KartuAngka("$aktif", "Aktif", Modifier.weight(1f))
                KartuAngka("$diblokir", "Diblokir", Modifier.weight(1f))
            }
        }
        if (!status.sedangMuat && !status.admin) {
            item {
                KartuGelap(Modifier.fillMaxWidth()) {
                    Text("Mode lihat saja", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Gold)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Akun ini belum terdaftar sebagai admin, jadi belum bisa memblokir akun. " +
                            "Di Firebase Console > Firestore, buat koleksi \"admins\" dengan ID dokumen " +
                            "= UID akun ini (isi field apa saja).",
                        fontSize = 12.sp, color = TeksRedup, lineHeight = 17.sp
                    )
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Anggota terdaftar", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                Text(
                    "Muat ulang", fontSize = 12.sp, color = VioletTerang, fontWeight = FontWeight.Bold,
                    modifier = Modifier.tekanPegas { vm.muat() }
                )
            }
        }
        if (status.sedangMuat) {
            item {
                Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = VioletTerang)
                }
            }
        }
        items(status.daftar) { p ->
            KartuAnggota(
                p = p,
                bisaBlokir = status.admin && p.uid != vm.uidSaya,
                onBlokir = { vm.aturBlokir(p.uid, !p.akunDiblokir) }
            )
        }
    }

    pesan?.let { DialogInfo(judul = "Info", pesan = it, onTutup = vm::tutupPesan) }
}

@Composable
private fun KartuAngka(angka: String, label: String, modifier: Modifier) {
    KartuGelap(modifier) {
        Text(angka, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
        Text(label, fontSize = 11.sp, color = TeksRedup)
    }
}

@Composable
private fun KartuAnggota(p: ProfilPengguna, bisaBlokir: Boolean, onBlokir: () -> Unit) {
    KartuGelap(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AvatarGradien(inisialDari(p.namaLengkap), 42.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    p.namaLengkap.ifBlank { "(tanpa nama)" },
                    fontWeight = FontWeight.Bold, fontSize = 14.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Text(
                    listOf(p.nim, p.divisi, p.desa).filter { it.isNotBlank() }.joinToString(" · "),
                    fontSize = 11.sp, color = TeksRedup, maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                val teksPeran = if (Peran.dariString(p.peran) == Peran.DEVELOPER) "Developer" else "Peserta"
                Text(
                    if (p.akunDiblokir) "$teksPeran · Diblokir" else teksPeran,
                    fontSize = 11.sp, color = if (p.akunDiblokir) Merah else Mint
                )
            }
            if (bisaBlokir) {
                Spacer(Modifier.width(8.dp))
                Text(
                    if (p.akunDiblokir) "Buka blokir" else "Blokir",
                    color = if (p.akunDiblokir) Mint else Merah,
                    fontWeight = FontWeight.Bold, fontSize = 12.sp,
                    modifier = Modifier.tekanPegas(onBlokir)
                        .background(Surface2, RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }
    }
}

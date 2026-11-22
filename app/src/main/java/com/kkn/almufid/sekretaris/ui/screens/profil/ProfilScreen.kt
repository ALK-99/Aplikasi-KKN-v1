package com.kkn.almufid.sekretaris.ui.screens.profil

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kkn.almufid.sekretaris.data.auth.Peran
import com.kkn.almufid.sekretaris.ui.auth.DialogInfo
import com.kkn.almufid.sekretaris.ui.auth.TombolUtama
import com.kkn.almufid.sekretaris.ui.auth.kolomWarna
import com.kkn.almufid.sekretaris.ui.components.AvatarGradien
import com.kkn.almufid.sekretaris.ui.components.KartuGelap
import com.kkn.almufid.sekretaris.ui.components.inisialDari
import com.kkn.almufid.sekretaris.ui.components.tekanPegas
import com.kkn.almufid.sekretaris.ui.theme.*

@Composable
fun ProfilScreen(vm: ProfilViewModel, onLogout: () -> Unit) {
    val profil by vm.profil.collectAsState()
    val sedangProses by vm.sedangProses.collectAsState()
    val pesan by vm.pesan.collectAsState()
    val p = profil

    if (p == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = VioletTerang)
        }
    } else {
        var nama by remember(p.uid, p.namaLengkap) { mutableStateOf(p.namaLengkap) }
        var divisi by remember(p.uid, p.divisi) { mutableStateOf(p.divisi) }

        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp).padding(bottom = 32.dp)
        ) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                AvatarGradien(inisialDari(p.namaLengkap), 84.dp)
                Spacer(Modifier.height(12.dp))
                Text(p.namaLengkap, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                Text(
                    if (Peran.dariString(p.peran) == Peran.DEVELOPER) "Developer" else "Peserta KKN",
                    fontSize = 13.sp, color = TeksRedup
                )
            }
            Spacer(Modifier.height(22.dp))

            KartuGelap(Modifier.fillMaxWidth()) {
                BarisData("NIM", p.nim)
                Spacer(Modifier.height(10.dp))
                BarisData("Email", p.email)
                Spacer(Modifier.height(10.dp))
                BarisData("Desa", p.desa.ifBlank { "-" })
                Spacer(Modifier.height(6.dp))
                Text(
                    "Desa dikunci supaya data tiap desa tidak tercampur. Kalau salah, minta developer mengubahnya.",
                    fontSize = 11.sp, color = TeksRedup
                )
            }
            Spacer(Modifier.height(18.dp))

            Text("Ubah data diri", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = nama, onValueChange = { nama = it }, label = { Text("Nama lengkap") },
                singleLine = true, modifier = Modifier.fillMaxWidth(), colors = kolomWarna()
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = divisi, onValueChange = { divisi = it }, label = { Text("Divisi") },
                singleLine = true, modifier = Modifier.fillMaxWidth(), colors = kolomWarna()
            )
            Spacer(Modifier.height(16.dp))
            TombolUtama(teks = "Simpan Perubahan", sedangProses = sedangProses) {
                vm.simpan(nama, divisi)
            }

            Spacer(Modifier.height(22.dp))
            Text("Keamanan", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(10.dp))
            KartuGelap(Modifier.fillMaxWidth().tekanPegas { if (!sedangProses) vm.kirimGantiSandi() }) {
                Text("Ganti sandi", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(Modifier.height(2.dp))
                Text(
                    "Kami kirim tautan ke email terdaftar untuk membuat sandi baru.",
                    fontSize = 12.sp, color = TeksRedup
                )
            }
            Spacer(Modifier.height(10.dp))
            KartuGelap(Modifier.fillMaxWidth().tekanPegas(onLogout)) {
                Text("Keluar dari akun", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Merah)
            }
        }
    }

    pesan?.let { DialogInfo(judul = "Info", pesan = it, onTutup = vm::tutupPesan) }
}

@Composable
private fun BarisData(label: String, nilai: String) {
    Column {
        Text(label, fontSize = 11.sp, color = TeksRedup)
        Text(nilai.ifBlank { "-" }, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

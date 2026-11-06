package com.kkn.almufid.sekretaris.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kkn.almufid.sekretaris.ui.components.tekanPegas
import com.kkn.almufid.sekretaris.ui.theme.*

@Composable
fun VerifikasiScreen(vm: AuthViewModel) {
    val sedangProses by vm.sedangProses.collectAsState()
    val pesanError by vm.pesanError.collectAsState()
    val popupSukses by vm.popupSukses.collectAsState()
    val data = vm.dataPendaftaran

    Column(
        Modifier.fillMaxSize().padding(horizontal = 30.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            Modifier.size(88.dp).background(Surface2, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.MarkEmailRead, null, tint = VioletTerang, modifier = Modifier.size(42.dp))
        }
        Spacer(Modifier.height(22.dp))
        Text("Verifikasi Email", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(10.dp))
        Text(
            "Kami sudah kirim tautan verifikasi ke ${data.emailGoogle.ifBlank { "email yang Anda daftarkan" }}. " +
                "Buka email itu, klik tautannya, lalu kembali ke sini dan tekan tombol di bawah.",
            fontSize = 13.sp, color = TeksRedup, textAlign = TextAlign.Center, lineHeight = 19.sp
        )
        Spacer(Modifier.height(30.dp))

        TombolUtama(teks = "Saya Sudah Verifikasi", sedangProses = sedangProses) {
            vm.cekVerifikasi()
        }
        Spacer(Modifier.height(16.dp))
        Text(
            "Belum dapat emailnya? Kirim ulang",
            fontSize = 13.sp, color = VioletTerang, fontWeight = FontWeight.Bold,
            modifier = Modifier.tekanPegas { vm.kirimUlangVerifikasi() }
        )
    }

    pesanError?.let { DialogInfo(judul = "Belum terverifikasi", pesan = it, onTutup = vm::tutupError) }

    if (popupSukses) {
        AlertDialog(
            onDismissRequest = {},
            containerColor = Surface1,
            titleContentColor = TeksUtama,
            textContentColor = TeksSoft,
            title = { Text("Pendaftaran Berhasil!", fontWeight = FontWeight.ExtraBold) },
            text = { Text("Akun Anda sudah aktif. Silakan masuk memakai NIM dan sandi yang baru saja dibuat.") },
            confirmButton = {
                TextButton(onClick = vm::lanjutKeLoginSetelahSukses) {
                    Text("OK", color = VioletTerang, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

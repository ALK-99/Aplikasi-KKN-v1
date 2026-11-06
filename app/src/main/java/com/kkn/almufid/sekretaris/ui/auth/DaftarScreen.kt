package com.kkn.almufid.sekretaris.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kkn.almufid.sekretaris.data.auth.Peran
import com.kkn.almufid.sekretaris.ui.components.KartuGelap
import com.kkn.almufid.sekretaris.ui.components.PilChip
import com.kkn.almufid.sekretaris.ui.components.tekanPegas
import com.kkn.almufid.sekretaris.ui.theme.*

@Composable
fun DaftarScreen(vm: AuthViewModel) {
    val sedangProses by vm.sedangProses.collectAsState()
    val pesanError by vm.pesanError.collectAsState()

    var peran by remember { mutableStateOf(Peran.PESERTA) }
    var nama by remember { mutableStateOf("") }
    var desa by remember { mutableStateOf("") }
    var divisi by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var nim by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var konfirmasi by remember { mutableStateOf("") }
    var sandiTerlihat by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack, "Kembali", tint = TeksUtama,
                modifier = Modifier.tekanPegas { vm.pindahTahap(TahapAuth.LOGIN) }
            )
            Spacer(Modifier.width(14.dp))
            Text("Daftar Akun", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
        }

        Column(Modifier.padding(horizontal = 18.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PilChip("Peserta KKN", peran == Peran.PESERTA, { peran = Peran.PESERTA }, Modifier.weight(1f))
                PilChip("Developer", peran == Peran.DEVELOPER, { peran = Peran.DEVELOPER }, Modifier.weight(1f))
            }
            Spacer(Modifier.height(20.dp))

            JudulSeksi("Profil")
            OutlinedTextField(
                value = nama, onValueChange = { nama = it }, label = { Text("Nama Lengkap") },
                singleLine = true, modifier = Modifier.fillMaxWidth(), colors = kolomWarna(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = desa, onValueChange = { desa = it }, label = { Text("Desa") },
                singleLine = true, modifier = Modifier.fillMaxWidth(), colors = kolomWarna(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = divisi, onValueChange = { divisi = it }, label = { Text("Divisi (opsional)") },
                singleLine = true, modifier = Modifier.fillMaxWidth(), colors = kolomWarna(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )

            Spacer(Modifier.height(24.dp))
            JudulSeksi("Buat Akun")
            OutlinedTextField(
                value = email, onValueChange = { email = it }, label = { Text("Akun Google (email)") },
                singleLine = true, modifier = Modifier.fillMaxWidth(), colors = kolomWarna(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next)
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = nim, onValueChange = { nim = it }, label = { Text("NIM") },
                singleLine = true, modifier = Modifier.fillMaxWidth(), colors = kolomWarna(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = password, onValueChange = { password = it }, label = { Text("Sandi") },
                singleLine = true, modifier = Modifier.fillMaxWidth(), colors = kolomWarna(),
                visualTransformation = if (sandiTerlihat) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { sandiTerlihat = !sandiTerlihat }) {
                        Icon(if (sandiTerlihat) Icons.Default.VisibilityOff else Icons.Default.Visibility, null, tint = TeksRedup)
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = konfirmasi, onValueChange = { konfirmasi = it }, label = { Text("Konfirmasi Sandi") },
                singleLine = true, modifier = Modifier.fillMaxWidth(), colors = kolomWarna(),
                visualTransformation = if (sandiTerlihat) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
            )

            Spacer(Modifier.height(26.dp))
            TombolUtama(teks = "Daftar", sedangProses = sedangProses) {
                vm.daftar(nama, desa, divisi, email, nim, password, konfirmasi, peran)
            }
            Spacer(Modifier.height(40.dp))
        }
    }

    pesanError?.let { DialogInfo(judul = "Tidak bisa daftar", pesan = it, onTutup = vm::tutupError) }
}

@Composable
private fun JudulSeksi(teks: String) {
    Text(teks, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = VioletTerang, modifier = Modifier.padding(bottom = 10.dp))
}

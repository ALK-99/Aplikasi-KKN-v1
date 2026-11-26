package com.kkn.almufid.sekretaris.ui.auth

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kkn.almufid.sekretaris.data.auth.Peran
import com.kkn.almufid.sekretaris.data.local.BATAS_GAGAL_LOGIN
import com.kkn.almufid.sekretaris.ui.components.KartuGelap
import com.kkn.almufid.sekretaris.ui.components.tekanPegas
import com.kkn.almufid.sekretaris.ui.theme.*

private const val NOMOR_ADMIN = "081384725967"

@Composable
fun LoginScreen(vm: AuthViewModel, onMasuk: (Peran) -> Unit) {
    val context = LocalContext.current
    val sedangProses by vm.sedangProses.collectAsState()
    val pesanError by vm.pesanError.collectAsState()
    val popupGagal by vm.popupGagalLogin.collectAsState()
    val popupLimit by vm.popupLimitLogin.collectAsState()

    var nim by remember { mutableStateOf(vm.nimDiingat.orEmpty()) }
    var password by remember { mutableStateOf("") }
    var ingatSaya by remember { mutableStateOf(vm.ingatSayaDisimpan) }
    var sandiTerlihat by remember { mutableStateOf(false) }
    var dialogReset by remember { mutableStateOf(false) }
    val pesanInfo by vm.pesanInfo.collectAsState()

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 26.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(72.dp))
        LogoBulat()
        Spacer(Modifier.height(18.dp))
        Text("KKN Al-Mushlih 2027", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(4.dp))
        Text("Masuk untuk melanjutkan", color = TeksRedup, fontSize = 13.sp)
        Spacer(Modifier.height(32.dp))

        OutlinedTextField(
            value = nim, onValueChange = { nim = it },
            label = { Text("NIM") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth(),
            colors = kolomWarna(),
        )
        Spacer(Modifier.height(14.dp))
        OutlinedTextField(
            value = password, onValueChange = { password = it },
            label = { Text("Sandi") },
            singleLine = true,
            visualTransformation = if (sandiTerlihat) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            trailingIcon = {
                IconButton(onClick = { sandiTerlihat = !sandiTerlihat }) {
                    Icon(if (sandiTerlihat) Icons.Default.VisibilityOff else Icons.Default.Visibility, null, tint = TeksRedup)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = kolomWarna(),
        )
        Spacer(Modifier.height(10.dp))

        Row(
            Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.tekanPegas { ingatSaya = !ingatSaya }) {
                Checkbox(
                    checked = ingatSaya, onCheckedChange = { ingatSaya = it },
                    colors = CheckboxDefaults.colors(checkedColor = VioletTerang, uncheckedColor = TeksRedup)
                )
                Text("Ingat saya", fontSize = 13.sp, color = TeksSoft)
            }
            Text(
                "Lupa sandi?", fontSize = 13.sp, color = VioletTerang, fontWeight = FontWeight.Bold,
                modifier = Modifier.tekanPegas { dialogReset = true }
            )
        }

        Spacer(Modifier.height(22.dp))
        TombolUtama(teks = "Masuk", sedangProses = sedangProses) {
            vm.login(nim, password, ingatSaya, onMasuk)
        }
        Spacer(Modifier.height(18.dp))

        Row {
            Text("Belum punya akun? ", fontSize = 13.sp, color = TeksRedup)
            Text(
                "Daftar", fontSize = 13.sp, color = VioletTerang, fontWeight = FontWeight.Bold,
                modifier = Modifier.tekanPegas { vm.pindahTahap(TahapAuth.DAFTAR) }
            )
        }
        Spacer(Modifier.height(40.dp))
    }

    pesanError?.let { DialogInfo(judul = "Tidak bisa masuk", pesan = it, onTutup = vm::tutupError) }
    pesanInfo?.let { DialogInfo(judul = "Cek email Anda", pesan = it, onTutup = vm::tutupInfo) }
    if (dialogReset) {
        var nimReset by remember { mutableStateOf(nim) }
        AlertDialog(
            onDismissRequest = { dialogReset = false },
            containerColor = Surface1,
            titleContentColor = TeksUtama,
            textContentColor = TeksSoft,
            title = { Text("Reset sandi", fontWeight = FontWeight.ExtraBold) },
            text = {
                Column {
                    Text("Masukkan NIM Anda. Tautan untuk membuat sandi baru akan dikirim ke email yang dipakai saat daftar.")
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = nimReset, onValueChange = { nimReset = it },
                        label = { Text("NIM") }, singleLine = true,
                        modifier = Modifier.fillMaxWidth(), colors = kolomWarna(),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { dialogReset = false; vm.kirimResetSandi(nimReset) }) {
                    Text("Kirim Email", color = VioletTerang, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { dialogReset = false }) { Text("Batal", color = TeksRedup) }
            }
        )
    }
    if (popupGagal) DialogInfo(judul = "Gagal Login!", pesan = "NIM atau sandi salah. Coba periksa lagi.", onTutup = vm::tutupPopupGagalLogin)
    if (popupLimit) {
        DialogInfo(
            judul = "Sabarin dulu, boss!",
            pesan = "Sudah $BATAS_GAGAL_LOGIN kali gagal login. Coba chat admin di $NOMOR_ADMIN kalau memang lupa akunnya.",
            onTutup = vm::tutupPopupLimitLogin
        )
    }
}

@Composable
internal fun LogoBulat() {
    Box(
        Modifier.size(84.dp)
            .background(GradUtama, androidx.compose.foundation.shape.CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text("KKN", color = androidx.compose.ui.graphics.Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
    }
}

@Composable
internal fun TombolUtama(teks: String, sedangProses: Boolean, onClick: () -> Unit) {
    val bentuk = RoundedCornerShape(3.dp)
    Box(
        Modifier.fillMaxWidth().height(52.dp)
            .tekanPegas(onClick = { if (!sedangProses) onClick() })
            .background(GradUtama, bentuk),
        contentAlignment = Alignment.Center
    ) {
        if (sedangProses) {
            CircularProgressIndicator(color = androidx.compose.ui.graphics.Color.White, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
        } else {
            Text(teks, color = androidx.compose.ui.graphics.Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
    }
}

@Composable
internal fun kolomWarna() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = TeksUtama, unfocusedTextColor = TeksUtama,
    focusedBorderColor = VioletTerang, unfocusedBorderColor = Surface2,
    focusedLabelColor = VioletTerang, unfocusedLabelColor = TeksRedup,
    cursorColor = VioletTerang,
)

@Composable
internal fun DialogInfo(judul: String, pesan: String, onTutup: () -> Unit) {
    AlertDialog(
        onDismissRequest = onTutup,
        containerColor = Surface1,
        titleContentColor = TeksUtama,
        textContentColor = TeksSoft,
        title = { Text(judul, fontWeight = FontWeight.ExtraBold) },
        text = { Text(pesan) },
        confirmButton = {
            TextButton(onClick = onTutup) { Text("OK", color = VioletTerang, fontWeight = FontWeight.Bold) }
        }
    )
}

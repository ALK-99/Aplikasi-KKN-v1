package com.kkn.almufid.sekretaris.ui.developer

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kkn.almufid.sekretaris.data.auth.HasilAuth
import com.kkn.almufid.sekretaris.data.cloud.KodePremium
import com.kkn.almufid.sekretaris.data.cloud.PremiumRepository
import com.kkn.almufid.sekretaris.ui.components.KartuGelap
import com.kkn.almufid.sekretaris.ui.components.PilChip
import com.kkn.almufid.sekretaris.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Developer: buat/cabut kode premium dan atur kunci AI bersama. */
@Composable
fun PremiumDevScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repo = remember { PremiumRepository() }
    var daftar by remember { mutableStateOf<List<KodePremium>>(emptyList()) }
    var hari by remember { mutableStateOf(30) }
    var hariBebas by remember { mutableStateOf("") }
    var catatan by remember { mutableStateOf("") }
    var kodeBaru by remember { mutableStateOf("") }
    var info by remember { mutableStateOf("") }
    var gemini by remember { mutableStateOf("") }
    var openrouter by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("") }
    val fmt = remember { SimpleDateFormat("dd MMM yyyy HH:mm", Locale("id", "ID")) }

    fun muat() { scope.launch { daftar = repo.daftarKode() } }
    LaunchedEffect(Unit) {
        muat()
        repo.ambilKunci()?.let { gemini = it.gemini; openrouter = it.openrouter; model = it.model }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        KartuGelap(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Buat kode akses", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(7, 30, 90, 180).forEach { PilChip("$it hari", hari == it && hariBebas.isBlank(), { hari = it; hariBebas = "" }, Modifier.weight(1f)) }
            }
            OutlinedTextField(hariBebas, { hariBebas = it.filter(Char::isDigit).take(4) }, label = { Text("Atau jumlah hari bebas") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(catatan, { catatan = it }, label = { Text("Catatan (untuk siapa)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Button(onClick = {
                val h = hariBebas.toIntOrNull() ?: hari
                if (h <= 0) { info = "Jumlah hari harus lebih dari 0."; return@Button }
                scope.launch {
                    when (val r = repo.buat(h, catatan)) {
                        is HasilAuth.Sukses -> { kodeBaru = r.data; info = "Kode dibuat untuk $h hari."; catatan = ""; muat() }
                        is HasilAuth.Gagal -> info = r.pesan
                    }
                }
            }) { Text("Buat kode") }
            if (kodeBaru.isNotBlank()) {
                SelectionContainer { Text(kodeBaru, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold) }
                OutlinedButton(onClick = {
                    (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("kode", kodeBaru))
                    info = "Kode disalin."
                }) { Text("Salin kode") }
            }
            if (info.isNotBlank()) Text(info, fontSize = 12.sp, color = TeksRedup)
        }

        KartuGelap(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Kunci AI bersama", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
            Text("Dibagikan otomatis ke akun premium (tidak tampil di layar mereka). Pakai kunci gratis dari aistudio.google.com/apikey TANPA menautkan akun penagihan, supaya tidak pernah ada tagihan. Cadangan: kunci gratis dari openrouter.ai/keys.", fontSize = 11.sp, color = TeksRedup)
            OutlinedTextField(gemini, { gemini = it.trim() }, label = { Text("Kunci Gemini") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
            OutlinedTextField(openrouter, { openrouter = it.trim() }, label = { Text("Kunci OpenRouter (cadangan)") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
            OutlinedTextField(model, { model = it }, label = { Text("Daftar model (kosong = bawaan)") }, modifier = Modifier.fillMaxWidth(), maxLines = 3)
            Button(onClick = { scope.launch { info = if (repo.simpanKunci(gemini, openrouter, model)) "Kunci AI tersimpan." else "Gagal menyimpan. Pastikan aturan Firestore terbaru sudah dipublikasikan." } }) { Text("Simpan kunci") }
        }

        Text("Kode yang sudah dibuat", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
        if (daftar.isEmpty()) Text("Belum ada kode.", fontSize = 12.sp, color = TeksRedup)
        daftar.forEach { k ->
            KartuGelap(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                SelectionContainer { Text(k.kode, fontWeight = FontWeight.Bold) }
                Text("${k.hari} hari" + if (k.catatan.isNotBlank()) " · ${k.catatan}" else "", fontSize = 12.sp)
                Text(
                    when {
                        k.dicabut -> "Dicabut"
                        k.dipakaiOleh.isNotEmpty() -> "Dipakai ${k.dipakaiNama.ifBlank { "pengguna" }} pada ${fmt.format(Date(k.dipakaiMillis))}"
                        else -> "Belum dipakai · dibuat ${fmt.format(Date(k.dibuatMillis))}"
                    },
                    fontSize = 11.sp, color = TeksRedup
                )
                if (!k.dicabut && k.dipakaiOleh.isEmpty()) OutlinedButton(onClick = { scope.launch { repo.cabut(k.kode); muat() } }) { Text("Cabut") }
            }
        }
    }
}

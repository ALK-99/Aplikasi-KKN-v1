package com.kkn.almufid.sekretaris.ui.screens.peta

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kkn.almufid.sekretaris.ui.components.KartuGelap
import com.kkn.almufid.sekretaris.ui.components.PilChip
import com.kkn.almufid.sekretaris.ui.theme.*

private val PENCARIAN_CEPAT = listOf(
    "Masjid", "Puskesmas", "Rumah sakit", "Apotek",
    "SPBU", "ATM", "Warung makan", "Minimarket",
)

private fun bacaPosko(context: Context): String =
    context.getSharedPreferences("peta_prefs", Context.MODE_PRIVATE).getString("posko", "") ?: ""

private fun simpanPosko(context: Context, teks: String) {
    context.getSharedPreferences("peta_prefs", Context.MODE_PRIVATE).edit().putString("posko", teks.trim()).apply()
}

/**
 * Membuka aplikasi peta bawaan HP (Google Maps dsb.) lewat intent. Gratis,
 * tanpa kunci API dan tanpa penagihan. Kalau tidak ada aplikasi peta,
 * dibuka lewat browser.
 */
private fun bukaPeta(context: Context, kueri: String) {
    val q = kueri.trim()
    if (q.isEmpty()) return
    val geo = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=" + Uri.encode(q)))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.startActivity(geo)
    } catch (e: Exception) {
        try {
            val web = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://www.google.com/maps/search/?api=1&query=" + Uri.encode(q))
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(web)
        } catch (_: Exception) { /* tidak ada aplikasi yang bisa membuka peta */ }
    }
}

@Composable
fun PetaScreen() {
    val context = LocalContext.current
    var cari by remember { mutableStateOf("") }
    var posko by remember { mutableStateOf(bacaPosko(context)) }
    var tersimpan by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(start = 18.dp, end = 18.dp, top = 6.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        KartuGelap(Modifier.fillMaxWidth()) {
            Text("Cari tempat", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = cari, onValueChange = { cari = it },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                placeholder = { Text("mis. Balai Desa Wonosalam") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { bukaPeta(context, cari) })
            )
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = { bukaPeta(context, cari) },
                enabled = cari.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Violet)
            ) { Text("Buka di peta") }
        }

        KartuGelap(Modifier.fillMaxWidth()) {
            Text("Cari di sekitar", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
            Text("Hasilnya ditampilkan di aplikasi peta, dekat lokasi kamu.", fontSize = 11.sp, color = TeksRedup)
            Spacer(Modifier.height(10.dp))
            PENCARIAN_CEPAT.chunked(2).forEach { pasang ->
                Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    pasang.forEach { nama ->
                        PilChip(nama, false, { bukaPeta(context, nama) }, Modifier.weight(1f))
                    }
                    if (pasang.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }

        KartuGelap(Modifier.fillMaxWidth()) {
            Text("Lokasi posko", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
            Text("Simpan nama atau alamat posko, lalu buka kapan saja.", fontSize = 11.sp, color = TeksRedup)
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = posko, onValueChange = { posko = it; tersimpan = false },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Nama atau alamat posko") }
            )
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { simpanPosko(context, posko); tersimpan = true },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Surface2)
                ) { Text(if (tersimpan) "Tersimpan" else "Simpan") }
                Button(
                    onClick = { bukaPeta(context, posko) },
                    enabled = posko.isNotBlank(),
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Violet)
                ) { Text("Buka posko") }
            }
        }

        Text(
            "Peta dibuka lewat aplikasi peta di HP kamu (mis. Google Maps). Gratis, tanpa kunci API.",
            fontSize = 11.sp, color = TeksRedup
        )
    }
}

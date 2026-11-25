package com.kkn.almufid.sekretaris.ui.screens.premium

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.kkn.almufid.sekretaris.data.auth.HasilAuth
import com.kkn.almufid.sekretaris.data.cloud.PremiumRepository
import com.kkn.almufid.sekretaris.data.cloud.StatusPremium
import com.kkn.almufid.sekretaris.ui.components.KartuGelap
import com.kkn.almufid.sekretaris.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Status premium + kolom tukar kode. Dipakai di Profil dan di pengaturan asisten. */
@Composable
fun KartuPremium(modifier: Modifier = Modifier, onBerubah: () -> Unit = {}) {
    val scope = rememberCoroutineScope()
    val repo = remember { PremiumRepository() }
    var st by remember { mutableStateOf<StatusPremium?>(null) }
    var kode by remember { mutableStateOf("") }
    var info by remember { mutableStateOf("") }
    var sibuk by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { st = repo.status() }
    val fmt = remember { SimpleDateFormat("dd MMM yyyy", Locale("id", "ID")) }

    KartuGelap(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Premium", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
        val s = st
        Text(
            when {
                s == null -> "Memeriksa status…"
                s.admin -> "Developer: premium aktif tanpa batas."
                s.aktif -> "Aktif sampai ${fmt.format(Date(s.sampaiMillis))} (sisa ${((s.sampaiMillis - System.currentTimeMillis()) / 86_400_000L).coerceAtLeast(0)} hari)."
                s.sampaiMillis > 0 -> "Masa aktif berakhir ${fmt.format(Date(s.sampaiMillis))}. Tukar kode baru untuk memperpanjang."
                else -> "Belum aktif. Minta kode akses ke developer."
            },
            fontSize = 13.sp
        )
        Text("Fitur premium (asisten AI, scan nota dan surat, laporan otomatis, bagikan ke Telegram/WA) butuh internet dan kode dari developer.", fontSize = 11.sp, color = TeksRedup)
        OutlinedTextField(kode, { kode = it.uppercase(); info = "" }, label = { Text("Kode akses (KKN-XXXX-XXXX)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        if (info.isNotBlank()) Text(info, fontSize = 12.sp)
        Button(enabled = !sibuk && kode.isNotBlank(), onClick = {
            scope.launch {
                sibuk = true; info = "Menukar kode…"
                val nama = FirebaseAuth.getInstance().currentUser?.displayName ?: FirebaseAuth.getInstance().currentUser?.email.orEmpty()
                when (val h = repo.tukar(kode, nama)) {
                    is HasilAuth.Sukses -> { info = "Berhasil. Premium aktif sampai ${fmt.format(Date(h.data))}."; kode = ""; st = repo.status(); onBerubah() }
                    is HasilAuth.Gagal -> info = h.pesan
                }
                sibuk = false
            }
        }) { Text(if (sibuk) "Menukar…" else "Tukar kode") }
    }
}

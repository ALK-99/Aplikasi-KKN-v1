package com.kkn.almufid.sekretaris.ui.screens.ajar

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kkn.almufid.sekretaris.data.cloud.AjarDesa
import com.kkn.almufid.sekretaris.ui.auth.DialogInfo
import com.kkn.almufid.sekretaris.ui.components.*
import com.kkn.almufid.sekretaris.ui.theme.*

@Composable
fun AjarScreen(viewModel: AjarViewModel) {
    val daftar by viewModel.daftar.collectAsState()
    val pesan by viewModel.pesan.collectAsState()
    var dialog by remember { mutableStateOf(false) }
    var diubah by remember { mutableStateOf<AjarDesa?>(null) }

    Box(Modifier.fillMaxSize()) {
        if (daftar.isEmpty()) {
            KosongState("Belum ada jadwal mengajar. Tekan + untuk menambah jadwal pertama.")
        } else {
            val perHari = daftar.groupBy { it.hari }
            LazyColumn(
                contentPadding = PaddingValues(18.dp, 8.dp, 18.dp, 110.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                perHari.keys.sorted().forEach { h ->
                    item(key = "hari-$h") {
                        Text(
                            NAMA_HARI.getOrElse(h - 1) { "Hari $h" }, color = VioletTerang,
                            fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                    items(perHari.getValue(h), key = { it.id }) { a ->
                        KartuGelap(Modifier.fillMaxWidth()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    if (a.jam.isNotBlank()) Text(a.jam, color = Gold, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Text(a.materi.ifBlank { "(tanpa materi)" }, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    if (a.lembaga.isNotBlank()) Text(a.lembaga, color = TeksSoft, fontSize = 13.sp)
                                    if (a.pengajarNama.isNotBlank()) Text("Pengajar: ${a.pengajarNama}", color = TeksSoft, fontSize = 12.sp)
                                    if (a.catatan.isNotBlank()) Text(a.catatan, color = TeksRedup, fontSize = 12.sp)
                                }
                                IconButton(onClick = { diubah = a }) { Icon(Icons.Default.Edit, "Ubah", tint = TeksRedup) }
                                IconButton(onClick = { viewModel.hapus(a) }) { Icon(Icons.Default.Delete, "Hapus", tint = TeksRedup) }
                            }
                        }
                    }
                }
            }
        }
        FabGradien(Icons.Default.Add, "Tambah jadwal", Modifier.align(Alignment.BottomEnd).padding(20.dp)) { dialog = true }
    }

    pesan?.let { DialogInfo(judul = "Info", pesan = it, onTutup = viewModel::tutupPesan) }

    if (dialog || diubah != null) {
        val a = diubah
        DialogUbah(
            judul = if (a == null) "Tambah Jadwal Mengajar" else "Ubah Jadwal",
            bidang = listOf(
                Bidang("Hari (Senin sampai Minggu)", a?.let { NAMA_HARI.getOrElse(it.hari - 1) { "" } } ?: ""),
                Bidang("Jam (mis. 16.00-17.30)", a?.jam ?: ""),
                Bidang("Lembaga / kelas", a?.lembaga ?: ""),
                Bidang("Materi", a?.materi ?: ""),
                Bidang("Pengajar", a?.pengajarNama ?: ""),
                Bidang("Catatan", a?.catatan ?: ""),
            ),
            onTutup = { dialog = false; diubah = null },
            onSimpan = { v ->
                if (viewModel.simpan(a, v[0], v[1], v[2], v[3], v[4], v[5])) { dialog = false; diubah = null }
            }
        )
    }
}

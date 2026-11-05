package com.kkn.almufid.sekretaris.ui.screens.proker

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kkn.almufid.sekretaris.data.entity.ProkerEntry
import com.kkn.almufid.sekretaris.data.entity.StatusProker
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.kkn.almufid.sekretaris.ui.components.*
import com.kkn.almufid.sekretaris.ui.theme.*

private fun warnaStatus(status: StatusProker): Color = when (status) {
    StatusProker.BELUM_MULAI -> TeksRedup
    StatusProker.BERJALAN -> Gold
    StatusProker.SELESAI -> Mint
    StatusProker.BATAL -> Merah
}

private fun labelStatus(status: StatusProker): String = when (status) {
    StatusProker.BELUM_MULAI -> "Belum mulai"
    StatusProker.BERJALAN -> "Berjalan"
    StatusProker.SELESAI -> "Selesai"
    StatusProker.BATAL -> "Batal"
}

@Composable
fun ProkerScreen(viewModel: ProkerViewModel) {
    val daftar by viewModel.daftarProker.collectAsState()
    var dialogTerbuka by remember { mutableStateOf(false) }
    val selesai = daftar.count { it.status == StatusProker.SELESAI }
    val aktif = daftar.count { it.status != StatusProker.BATAL }
    val frac by animateFloatAsState(if (aktif == 0) 0f else selesai.toFloat() / aktif, tween(800), label = "proker")

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            KartuGelap(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 4.dp)) {
                Text("$selesai dari $aktif proker selesai", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                Spacer(Modifier.height(10.dp))
                BatangProgres(frac)
            }
            if (daftar.isEmpty()) {
                KosongState("Belum ada program kerja. Tekan + untuk menambahkan.")
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(18.dp, 10.dp, 18.dp, 110.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    itemsIndexed(daftar, key = { _, p -> p.id }) { i, p ->
                        KartuProker(
                            proker = p,
                            modifier = Modifier.munculBertahap(i),
                            onUbahStatus = { status -> viewModel.ubahStatus(p, status) },
                            onHapus = { viewModel.hapusProker(p) }
                        )
                    }
                }
            }
        }
        FabGradien(Icons.Default.Add, "Tambah proker", Modifier.align(Alignment.BottomEnd).padding(20.dp)) {
            dialogTerbuka = true
        }
    }

    if (dialogTerbuka) {
        DialogTambahProker(
            onTutup = { dialogTerbuka = false },
            onSimpan = { nama, divisi ->
                viewModel.tambahProker(nama, divisi)
                dialogTerbuka = false
            }
        )
    }
}

@Composable
private fun KartuProker(
    proker: ProkerEntry,
    modifier: Modifier,
    onUbahStatus: (StatusProker) -> Unit,
    onHapus: () -> Unit
) {
    var menuTerbuka by remember { mutableStateOf(false) }
    KartuGelap(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(proker.namaProker, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                if (proker.divisiPenanggungJawab.isNotBlank()) {
                    Text("PJ: ${proker.divisiPenanggungJawab}", color = TeksRedup, fontSize = 12.sp)
                }
            }
            IconButton(onClick = onHapus) { Icon(Icons.Default.Delete, "Hapus", tint = TeksRedup) }
        }
        Spacer(Modifier.height(10.dp))
        Box {
            TagKecil(labelStatus(proker.status), warnaStatus(proker.status), Modifier.tekanPegas { menuTerbuka = true })
            DropdownMenu(expanded = menuTerbuka, onDismissRequest = { menuTerbuka = false }) {
                StatusProker.entries.forEach { status ->
                    DropdownMenuItem(
                        text = { Text(labelStatus(status)) },
                        onClick = { onUbahStatus(status); menuTerbuka = false }
                    )
                }
            }
        }
    }
}

@Composable
private fun DialogTambahProker(onTutup: () -> Unit, onSimpan: (String, String) -> Unit) {
    var nama by remember { mutableStateOf("") }
    var divisi by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onTutup,
        title = { Text("Tambah Program Kerja") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = nama, onValueChange = { nama = it },
                    label = { Text("Nama proker") }, modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = divisi, onValueChange = { divisi = it },
                    label = { Text("Divisi/PJ (opsional)") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = { TextButton(onClick = { onSimpan(nama, divisi) }) { Text("Simpan") } },
        dismissButton = { TextButton(onClick = onTutup) { Text("Batal") } }
    )
}

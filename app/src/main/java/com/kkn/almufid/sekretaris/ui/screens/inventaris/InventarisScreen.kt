package com.kkn.almufid.sekretaris.ui.screens.inventaris

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
import com.kkn.almufid.sekretaris.data.entity.InventarisEntry
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

@Composable
fun InventarisScreen(viewModel: InventarisViewModel) {
    val daftar by viewModel.daftarBarang.collectAsState()
    var dialogTerbuka by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        if (daftar.isEmpty()) {
            KosongState("Belum ada barang inventaris posko. Tekan + untuk menambah.")
        } else {
            LazyColumn(
                contentPadding = PaddingValues(18.dp, 8.dp, 18.dp, 110.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                itemsIndexed(daftar, key = { _, b -> b.id }) { i, b ->
                    KartuBarang(b, Modifier.munculBertahap(i)) { viewModel.hapusBarang(b) }
                }
            }
        }
        FabGradien(Icons.Default.Add, "Tambah barang", Modifier.align(Alignment.BottomEnd).padding(20.dp)) {
            dialogTerbuka = true
        }
    }

    if (dialogTerbuka) {
        DialogTambahBarang(
            onTutup = { dialogTerbuka = false },
            onSimpan = { nama, jumlah, kondisi, ket ->
                viewModel.tambahBarang(nama, jumlah, kondisi, ket)
                dialogTerbuka = false
            }
        )
    }
}

@Composable
private fun KartuBarang(barang: InventarisEntry, modifier: Modifier, onHapus: () -> Unit) {
    KartuGelap(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(46.dp).clip(RoundedCornerShape(14.dp)).background(GradUtama),
                contentAlignment = Alignment.Center
            ) {
                Text("${barang.jumlah}", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(barang.namaBarang, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("Kondisi: ${barang.kondisi}", color = TeksSoft, fontSize = 13.sp)
                if (barang.keterangan.isNotBlank()) {
                    Text(barang.keterangan, color = TeksRedup, fontSize = 12.sp)
                }
            }
            IconButton(onClick = onHapus) { Icon(Icons.Default.Delete, "Hapus", tint = TeksRedup) }
        }
    }
}

@Composable
private fun DialogTambahBarang(
    onTutup: () -> Unit,
    onSimpan: (String, Int, String, String) -> Unit
) {
    var nama by remember { mutableStateOf("") }
    var jumlahTeks by remember { mutableStateOf("1") }
    var kondisi by remember { mutableStateOf("Baik") }
    var keterangan by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onTutup,
        title = { Text("Tambah Barang") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = nama, onValueChange = { nama = it },
                    label = { Text("Nama barang") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = jumlahTeks,
                    onValueChange = { t -> jumlahTeks = t.filter { it.isDigit() } },
                    label = { Text("Jumlah") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = kondisi, onValueChange = { kondisi = it },
                    label = { Text("Kondisi") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = keterangan, onValueChange = { keterangan = it },
                    label = { Text("Keterangan (opsional)") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(onClick = { onSimpan(nama, jumlahTeks.toIntOrNull() ?: 0, kondisi, keterangan) }) { Text("Simpan") }
        },
        dismissButton = { TextButton(onClick = onTutup) { Text("Batal") } }
    )
}

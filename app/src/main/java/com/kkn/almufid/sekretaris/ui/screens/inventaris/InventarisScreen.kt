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

@Composable
fun InventarisScreen(viewModel: InventarisViewModel) {
    val daftar by viewModel.daftarBarang.collectAsState()
    var dialogTerbuka by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { dialogTerbuka = true }) {
                Icon(Icons.Default.Add, contentDescription = "Tambah barang")
            }
        }
    ) { padding ->
        if (daftar.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Belum ada barang inventaris posko.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(daftar, key = { it.id }) { barang ->
                    KartuBarang(barang = barang, onHapus = { viewModel.hapusBarang(barang) })
                }
            }
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
private fun KartuBarang(barang: InventarisEntry, onHapus: () -> Unit) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(barang.namaBarang, style = MaterialTheme.typography.titleMedium)
                Text("Jumlah: ${barang.jumlah} • Kondisi: ${barang.kondisi}", style = MaterialTheme.typography.bodyMedium)
                if (barang.keterangan.isNotBlank()) {
                    Text(barang.keterangan, style = MaterialTheme.typography.labelSmall)
                }
            }
            IconButton(onClick = onHapus) {
                Icon(Icons.Default.Delete, contentDescription = "Hapus")
            }
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

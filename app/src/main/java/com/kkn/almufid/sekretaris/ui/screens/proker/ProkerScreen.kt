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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProkerScreen(viewModel: ProkerViewModel) {
    val daftar by viewModel.daftarProker.collectAsState()
    var dialogTerbuka by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { dialogTerbuka = true }) {
                Icon(Icons.Default.Add, contentDescription = "Tambah proker")
            }
        }
    ) { padding ->
        if (daftar.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Belum ada program kerja. Tekan + untuk menambahkan.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(daftar, key = { it.id }) { proker ->
                    KartuProker(
                        proker = proker,
                        onUbahStatus = { status -> viewModel.ubahStatus(proker, status) },
                        onHapus = { viewModel.hapusProker(proker) }
                    )
                }
            }
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

private fun warnaStatus(status: StatusProker): androidx.compose.ui.graphics.Color = when (status) {
    StatusProker.BELUM_MULAI -> androidx.compose.ui.graphics.Color(0xFF9E9E9E)
    StatusProker.BERJALAN -> androidx.compose.ui.graphics.Color(0xFFF9A825)
    StatusProker.SELESAI -> androidx.compose.ui.graphics.Color(0xFF2E7D32)
    StatusProker.BATAL -> androidx.compose.ui.graphics.Color(0xFFC62828)
}

private fun labelStatus(status: StatusProker): String = when (status) {
    StatusProker.BELUM_MULAI -> "Belum Mulai"
    StatusProker.BERJALAN -> "Berjalan"
    StatusProker.SELESAI -> "Selesai"
    StatusProker.BATAL -> "Batal"
}

@Composable
private fun KartuProker(
    proker: ProkerEntry,
    onUbahStatus: (StatusProker) -> Unit,
    onHapus: () -> Unit
) {
    var menuTerbuka by remember { mutableStateOf(false) }

    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(proker.namaProker, style = MaterialTheme.typography.titleMedium)
                    if (proker.divisiPenanggungJawab.isNotBlank()) {
                        Text("PJ: ${proker.divisiPenanggungJawab}", style = MaterialTheme.typography.labelSmall)
                    }
                }
                IconButton(onClick = onHapus) {
                    Icon(Icons.Default.Delete, contentDescription = "Hapus")
                }
            }
            Spacer(Modifier.height(8.dp))
            Box {
                AssistChip(
                    onClick = { menuTerbuka = true },
                    label = { Text(labelStatus(proker.status)) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = warnaStatus(proker.status).copy(alpha = 0.15f),
                        labelColor = warnaStatus(proker.status)
                    )
                )
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

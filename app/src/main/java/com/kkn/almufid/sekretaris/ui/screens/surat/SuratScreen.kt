package com.kkn.almufid.sekretaris.ui.screens.surat

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
import com.kkn.almufid.sekretaris.data.entity.JenisSurat
import com.kkn.almufid.sekretaris.data.entity.SuratEntry
import com.kkn.almufid.sekretaris.ui.formatTanggal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuratScreen(viewModel: SuratViewModel) {
    val daftar by viewModel.daftarSurat.collectAsState()
    var dialogTerbuka by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { dialogTerbuka = true }) {
                Icon(Icons.Default.Add, contentDescription = "Tambah surat")
            }
        }
    ) { padding ->
        if (daftar.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Belum ada surat tercatat.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(daftar, key = { it.id }) { surat ->
                    KartuSurat(surat = surat, onHapus = { viewModel.hapusSurat(surat) })
                }
            }
        }
    }

    if (dialogTerbuka) {
        DialogTambahSurat(
            onTutup = { dialogTerbuka = false },
            onSimpan = { jenis, nomor, perihal, tujuan ->
                viewModel.tambahSurat(jenis, nomor, perihal, tujuan)
                dialogTerbuka = false
            }
        )
    }
}

@Composable
private fun KartuSurat(surat: SuratEntry, onHapus: () -> Unit) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AssistChip(
                    onClick = {},
                    label = { Text(if (surat.jenis == JenisSurat.MASUK) "Surat Masuk" else "Surat Keluar") }
                )
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onHapus) {
                    Icon(Icons.Default.Delete, contentDescription = "Hapus")
                }
            }
            if (surat.nomorSurat.isNotBlank()) {
                Text("No: ${surat.nomorSurat}", style = MaterialTheme.typography.labelSmall)
            }
            Spacer(Modifier.height(4.dp))
            Text(surat.perihal, style = MaterialTheme.typography.titleMedium)
            Text(
                if (surat.jenis == JenisSurat.MASUK) "Dari: ${surat.tujuanAtauPengirim}" else "Kepada: ${surat.tujuanAtauPengirim}",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(6.dp))
            Text(
                formatTanggal(surat.tanggalMillis),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
private fun DialogTambahSurat(
    onTutup: () -> Unit,
    onSimpan: (JenisSurat, String, String, String) -> Unit
) {
    var jenis by remember { mutableStateOf(JenisSurat.KELUAR) }
    var nomor by remember { mutableStateOf("") }
    var perihal by remember { mutableStateOf("") }
    var tujuan by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onTutup,
        title = { Text("Tambah Surat") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row {
                    FilterChip(selected = jenis == JenisSurat.MASUK, onClick = { jenis = JenisSurat.MASUK }, label = { Text("Masuk") })
                    Spacer(Modifier.width(6.dp))
                    FilterChip(selected = jenis == JenisSurat.KELUAR, onClick = { jenis = JenisSurat.KELUAR }, label = { Text("Keluar") })
                }
                OutlinedTextField(
                    value = nomor, onValueChange = { nomor = it },
                    label = { Text("Nomor surat (opsional)") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = perihal, onValueChange = { perihal = it },
                    label = { Text("Perihal") }, modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = tujuan, onValueChange = { tujuan = it },
                    label = { Text(if (jenis == JenisSurat.MASUK) "Dari" else "Kepada") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSimpan(jenis, nomor, perihal, tujuan) }) { Text("Simpan") }
        },
        dismissButton = { TextButton(onClick = onTutup) { Text("Batal") } }
    )
}

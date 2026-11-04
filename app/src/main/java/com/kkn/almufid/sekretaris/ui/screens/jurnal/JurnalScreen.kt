package com.kkn.almufid.sekretaris.ui.screens.jurnal

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
import com.kkn.almufid.sekretaris.data.entity.JenisJurnal
import com.kkn.almufid.sekretaris.data.entity.JurnalEntry
import com.kkn.almufid.sekretaris.ui.formatTanggal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JurnalScreen(viewModel: JurnalViewModel) {
    val daftar by viewModel.daftarJurnal.collectAsState()
    var dialogTerbuka by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { dialogTerbuka = true }) {
                Icon(Icons.Default.Add, contentDescription = "Tambah jurnal")
            }
        }
    ) { padding ->
        if (daftar.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Belum ada jurnal. Tekan + untuk menambah catatan harian atau notulen rapat.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(daftar, key = { it.id }) { entry ->
                    KartuJurnal(entry = entry, onHapus = { viewModel.hapusJurnal(entry) })
                }
            }
        }
    }

    if (dialogTerbuka) {
        DialogTambahJurnal(
            onTutup = { dialogTerbuka = false },
            onSimpan = { jenis, judul, isi, peserta ->
                viewModel.tambahJurnal(jenis, judul, isi, peserta)
                dialogTerbuka = false
            }
        )
    }
}

@Composable
private fun KartuJurnal(entry: JurnalEntry, onHapus: () -> Unit) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AssistChip(
                    onClick = {},
                    label = {
                        Text(if (entry.jenis == JenisJurnal.HARIAN) "Jurnal Harian" else "Notulen Rapat")
                    }
                )
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onHapus) {
                    Icon(Icons.Default.Delete, contentDescription = "Hapus")
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(entry.judul, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(entry.isi, style = MaterialTheme.typography.bodyMedium)
            if (entry.pesertaRapat.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text("Peserta: ${entry.pesertaRapat}", style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(6.dp))
            Text(
                formatTanggal(entry.tanggalMillis),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DialogTambahJurnal(
    onTutup: () -> Unit,
    onSimpan: (JenisJurnal, String, String, String) -> Unit
) {
    var jenis by remember { mutableStateOf(JenisJurnal.HARIAN) }
    var judul by remember { mutableStateOf("") }
    var isi by remember { mutableStateOf("") }
    var peserta by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onTutup,
        title = { Text("Tambah Catatan") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SingleChoiceSegmented(
                    pilihan = listOf("Harian" to JenisJurnal.HARIAN, "Notulen Rapat" to JenisJurnal.NOTULEN_RAPAT),
                    terpilih = jenis,
                    onPilih = { jenis = it }
                )
                OutlinedTextField(
                    value = judul,
                    onValueChange = { judul = it },
                    label = { Text("Judul singkat") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = isi,
                    onValueChange = { isi = it },
                    label = { Text(if (jenis == JenisJurnal.HARIAN) "Isi kegiatan" else "Hasil rapat") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
                if (jenis == JenisJurnal.NOTULEN_RAPAT) {
                    OutlinedTextField(
                        value = peserta,
                        onValueChange = { peserta = it },
                        label = { Text("Peserta rapat (pisahkan dengan koma)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSimpan(jenis, judul, isi, peserta) }) { Text("Simpan") }
        },
        dismissButton = {
            TextButton(onClick = onTutup) { Text("Batal") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SingleChoiceSegmented(
    pilihan: List<Pair<String, JenisJurnal>>,
    terpilih: JenisJurnal,
    onPilih: (JenisJurnal) -> Unit
) {
    SingleChoiceSegmentedButtonRow {
        pilihan.forEachIndexed { index, (label, nilai) ->
            SegmentedButton(
                selected = terpilih == nilai,
                onClick = { onPilih(nilai) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = pilihan.size)
            ) {
                Text(label)
            }
        }
    }
}

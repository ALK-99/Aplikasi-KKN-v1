package com.kkn.almufid.sekretaris.ui.screens.arsip

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.kkn.almufid.sekretaris.data.entity.NotaEntry
import com.kkn.almufid.sekretaris.ui.formatRupiah
import com.kkn.almufid.sekretaris.ui.formatTanggal
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArsipScreen(viewModel: NotaViewModel) {
    val daftar by viewModel.daftarNota.collectAsState()
    val total by viewModel.totalPengeluaran.collectAsState()
    var dialogTerbuka by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { dialogTerbuka = true }) {
                Icon(Icons.Default.Add, contentDescription = "Tambah nota")
            }
        }
    ) { padding ->
        if (daftar.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Belum ada nota/kwitansi tersimpan.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text(
                        "Total pengeluaran: ${formatRupiah(total)}",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                items(daftar, key = { it.id }) { nota ->
                    KartuNota(nota = nota, onHapus = { viewModel.hapusNota(nota) })
                }
            }
        }
    }

    if (dialogTerbuka) {
        DialogTambahNota(
            onTutup = { dialogTerbuka = false },
            onSimpan = { keterangan, nominal, proker, foto ->
                viewModel.tambahNota(keterangan, nominal, proker, foto)
                dialogTerbuka = false
            }
        )
    }
}

@Composable
private fun KartuNota(nota: NotaEntry, onHapus: () -> Unit) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (nota.fotoPath != null && File(nota.fotoPath).exists()) {
                AsyncImage(
                    model = File(nota.fotoPath),
                    contentDescription = "Foto nota",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(56.dp)
                )
                Spacer(Modifier.width(12.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(nota.keterangan, style = MaterialTheme.typography.titleMedium)
                Text(formatRupiah(nota.nominal), style = MaterialTheme.typography.bodyMedium)
                if (nota.prokerTerkait.isNotBlank()) {
                    Text("Proker: ${nota.prokerTerkait}", style = MaterialTheme.typography.labelSmall)
                }
                Text(
                    formatTanggal(nota.tanggalMillis),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            IconButton(onClick = onHapus) {
                Icon(Icons.Default.Delete, contentDescription = "Hapus")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DialogTambahNota(
    onTutup: () -> Unit,
    onSimpan: (String, Long, String, Bitmap?) -> Unit
) {
    var keterangan by remember { mutableStateOf("") }
    var nominalTeks by remember { mutableStateOf("") }
    var proker by remember { mutableStateOf("") }
    var foto by remember { mutableStateOf<Bitmap?>(null) }

    val peluncurKamera = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap -> foto = bitmap }

    AlertDialog(
        onDismissRequest = onTutup,
        title = { Text("Tambah Nota/Kwitansi") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { peluncurKamera.launch() }) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(if (foto == null) "Ambil Foto Nota" else "Ganti Foto")
                }
                foto?.let {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = "Pratinjau foto nota",
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        contentScale = ContentScale.Crop
                    )
                }
                OutlinedTextField(
                    value = keterangan, onValueChange = { keterangan = it },
                    label = { Text("Keterangan belanja") }, modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = nominalTeks,
                    onValueChange = { teks -> nominalTeks = teks.filter { it.isDigit() } },
                    label = { Text("Nominal (Rp)") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = proker, onValueChange = { proker = it },
                    label = { Text("Proker terkait (opsional)") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSimpan(keterangan, nominalTeks.toLongOrNull() ?: 0L, proker, foto)
            }) { Text("Simpan") }
        },
        dismissButton = { TextButton(onClick = onTutup) { Text("Batal") } }
    )
}

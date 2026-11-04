package com.kkn.almufid.sekretaris.ui.screens.absensi

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kkn.almufid.sekretaris.ui.formatTanggal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AbsensiScreen(viewModel: AbsensiViewModel) {
    val anggota by viewModel.daftarAnggota.collectAsState()
    val absensi by viewModel.absensiHariIni.collectAsState()
    val tanggal by viewModel.tanggalDipilih.collectAsState()
    var dialogTambahAnggota by remember { mutableStateOf(false) }

    val statusPerAnggota = remember(absensi) { absensi.associateBy { it.anggotaId } }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { dialogTambahAnggota = true }) {
                Icon(Icons.Default.PersonAdd, contentDescription = "Tambah anggota")
            }
        }
    ) { padding ->
        if (anggota.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Belum ada anggota. Tekan tombol + untuk menambah anggota kelompok.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Text(
                        "Tanggal: ${formatTanggal(tanggal).substringBeforeLast(",")}",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                items(anggota, key = { it.id }) { orang ->
                    val statusSaatIni = statusPerAnggota[orang.id]
                    BarisAbsensi(
                        nama = orang.nama,
                        divisi = orang.divisi,
                        hadir = statusSaatIni?.hadir,
                        onUbah = { hadir -> viewModel.tandaiKehadiran(orang.id, hadir) }
                    )
                }
            }
        }
    }

    if (dialogTambahAnggota) {
        DialogTambahAnggota(
            onTutup = { dialogTambahAnggota = false },
            onSimpan = { nama, divisi ->
                viewModel.tambahAnggota(nama, divisi)
                dialogTambahAnggota = false
            }
        )
    }
}

@Composable
private fun BarisAbsensi(
    nama: String,
    divisi: String,
    hadir: Boolean?,
    onUbah: (Boolean) -> Unit
) {
    ElevatedCard {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(nama, style = MaterialTheme.typography.titleMedium)
                if (divisi.isNotBlank()) {
                    Text(divisi, style = MaterialTheme.typography.labelSmall)
                }
            }
            FilterChip(
                selected = hadir == true,
                onClick = { onUbah(true) },
                label = { Text("Hadir") }
            )
            Spacer(Modifier.width(6.dp))
            FilterChip(
                selected = hadir == false,
                onClick = { onUbah(false) },
                label = { Text("Absen") }
            )
        }
    }
}

@Composable
private fun DialogTambahAnggota(onTutup: () -> Unit, onSimpan: (String, String) -> Unit) {
    var nama by remember { mutableStateOf("") }
    var divisi by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onTutup,
        title = { Text("Tambah Anggota") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = nama,
                    onValueChange = { nama = it },
                    label = { Text("Nama") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = divisi,
                    onValueChange = { divisi = it },
                    label = { Text("Divisi (opsional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSimpan(nama, divisi) }) { Text("Simpan") }
        },
        dismissButton = {
            TextButton(onClick = onTutup) { Text("Batal") }
        }
    )
}

package com.kkn.almufid.sekretaris.ui.screens.inventaris

import com.kkn.almufid.sekretaris.ui.components.BarPenyaring
import com.kkn.almufid.sekretaris.ui.components.rememberPenyaring
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kkn.almufid.sekretaris.data.cloud.InventarisDesa
import com.kkn.almufid.sekretaris.ui.auth.DialogInfo
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
    val pesan by viewModel.pesan.collectAsState()
    var dialogTerbuka by remember { mutableStateOf(false) }
    var diubah by remember { mutableStateOf<InventarisDesa?>(null) }

    val pny = rememberPenyaring()
    val tampil = pny.terapkan(
        daftar, { "${it.namaBarang} ${it.kondisi} ${it.keterangan}" },
        listOf<Pair<String, Comparator<InventarisDesa>>>(
            "Nama" to compareBy { it.namaBarang.lowercase() },
            "Jumlah" to compareBy { it.jumlah },
            "Kondisi" to compareBy { it.kondisi.lowercase() },
        ),
        { it.kondisi }
    )
    Box(Modifier.fillMaxSize()) {
        if (daftar.isEmpty()) {
            KosongState("Belum ada barang inventaris posko. Tekan + untuk menambah.")
        } else {
            Column(Modifier.fillMaxSize()) {
            BarPenyaring(pny, listOf("Nama", "Jumlah", "Kondisi"), "kondisi", daftar.map { it.kondisi }.distinct().sorted(), tampil.size, daftar.size)
            LazyColumn(
                contentPadding = PaddingValues(18.dp, 8.dp, 18.dp, 110.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                itemsIndexed(tampil, key = { _, b -> b.id }) { i, b ->
                    KartuBarang(b, Modifier.munculBertahap(i), onUbah = { diubah = b }) { viewModel.hapusBarang(b) }
                }
            }
            }
        }
        FabGradien(Icons.Default.Add, "Tambah barang", Modifier.align(Alignment.BottomEnd).padding(20.dp)) {
            dialogTerbuka = true
        }
    }

    pesan?.let { DialogInfo(judul = "Info", pesan = it, onTutup = viewModel::tutupPesan) }

    diubah?.let { b ->
        DialogUbah(
            judul = "Ubah Barang",
            bidang = listOf(Bidang("Nama barang", b.namaBarang), Bidang("Jumlah", b.jumlah.toString(), angka = true), Bidang("Kondisi", b.kondisi), Bidang("Keterangan", b.keterangan)),
            onTutup = { diubah = null },
            onSimpan = { v -> viewModel.ubahBarang(b, v[0], v[1].toIntOrNull() ?: 0, v[2], v[3]); diubah = null }
        )
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
private fun KartuBarang(barang: InventarisDesa, modifier: Modifier, onUbah: () -> Unit, onHapus: () -> Unit) {
    KartuGelap(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(46.dp).clip(RoundedCornerShape(16.dp)).background(GradUtama),
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
                if (barang.jejak.diubahOlehNama.isNotBlank()) {
                    Text("Dicatat ${barang.jejak.dibuatOlehNama}", color = TeksRedup, fontSize = 10.sp)
                }
            }
            IconButton(onClick = onUbah) { Icon(Icons.Default.Edit, "Ubah", tint = TeksRedup) }
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

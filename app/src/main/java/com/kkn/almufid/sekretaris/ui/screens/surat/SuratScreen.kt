package com.kkn.almufid.sekretaris.ui.screens.surat

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
import com.kkn.almufid.sekretaris.data.entity.JenisSurat
import com.kkn.almufid.sekretaris.data.cloud.SuratDesa
import com.kkn.almufid.sekretaris.ui.auth.DialogInfo
import com.kkn.almufid.sekretaris.ui.formatTanggal
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
fun SuratScreen(viewModel: SuratViewModel) {
    val daftar by viewModel.daftarSurat.collectAsState()
    val pesan by viewModel.pesan.collectAsState()
    var dialogTerbuka by remember { mutableStateOf(false) }
    var diubah by remember { mutableStateOf<SuratDesa?>(null) }
    var filter by remember { mutableStateOf<JenisSurat?>(null) }
    val pny = rememberPenyaring()
    val awal = if (filter == null) daftar else daftar.filter { it.jenis == filter }
    val tampil = pny.terapkan(
        awal, { "${it.perihal} ${it.nomorSurat} ${it.tujuanAtauPengirim}" },
        listOf<Pair<String, Comparator<SuratDesa>>>(
            "Tanggal" to compareBy { it.tanggalMillis },
            "Perihal" to compareBy { it.perihal.lowercase() },
            "Nomor" to compareBy { it.nomorSurat.lowercase() },
        ),
        { it.tujuanAtauPengirim.ifBlank { "-" } }
    )

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.padding(horizontal = 18.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PilChip("Semua", filter == null, { filter = null })
                PilChip("Masuk", filter == JenisSurat.MASUK, { filter = JenisSurat.MASUK })
                PilChip("Keluar", filter == JenisSurat.KELUAR, { filter = JenisSurat.KELUAR })
            }
            BarPenyaring(pny, listOf("Tanggal", "Perihal", "Nomor"), "tujuan/pengirim",
                awal.map { it.tujuanAtauPengirim.ifBlank { "-" } }.distinct().sorted().take(30), tampil.size, awal.size)
            if (tampil.isEmpty()) {
                KosongState("Belum ada surat tercatat. Tekan + untuk menambah.")
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(18.dp, 8.dp, 18.dp, 110.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    itemsIndexed(tampil, key = { _, s -> s.id }) { i, s ->
                        KartuSurat(s, Modifier.munculBertahap(i), onUbah = { diubah = s }) { viewModel.hapusSurat(s) }
                    }
                }
            }
        }
        FabGradien(Icons.Default.Add, "Tambah surat", Modifier.align(Alignment.BottomEnd).padding(20.dp)) {
            dialogTerbuka = true
        }
    }

    pesan?.let { DialogInfo(judul = "Info", pesan = it, onTutup = viewModel::tutupPesan) }

    diubah?.let { s ->
        DialogUbah(
            judul = "Ubah Surat",
            bidang = listOf(Bidang("Nomor surat", s.nomorSurat), Bidang("Perihal", s.perihal), Bidang(if (s.jenis == JenisSurat.MASUK) "Dari" else "Kepada", s.tujuanAtauPengirim)),
            onTutup = { diubah = null },
            onSimpan = { v -> viewModel.ubahSurat(s, v[0], v[1], v[2]); diubah = null }
        )
    }

    if (dialogTerbuka) {
        DialogTambahSurat(
            saranNomor = { viewModel.nomorBerikutnya(it) },
            onTutup = { dialogTerbuka = false },
            onSimpan = { jenis, nomor, perihal, tujuan ->
                viewModel.tambahSurat(jenis, nomor, perihal, tujuan)
                dialogTerbuka = false
            }
        )
    }
}

@Composable
private fun KartuSurat(surat: SuratDesa, modifier: Modifier, onUbah: () -> Unit, onHapus: () -> Unit) {
    val masuk = surat.jenis == JenisSurat.MASUK
    KartuGelap(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TagKecil(if (masuk) "Surat masuk" else "Surat keluar", if (masuk) Mint else Gold)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onUbah) { Icon(Icons.Default.Edit, "Ubah", tint = TeksRedup) }
            IconButton(onClick = onHapus) { Icon(Icons.Default.Delete, "Hapus", tint = TeksRedup) }
        }
        if (surat.nomorSurat.isNotBlank()) {
            Text("No. ${surat.nomorSurat}", color = TeksRedup, fontSize = 11.sp)
        }
        Spacer(Modifier.height(4.dp))
        Text(surat.perihal, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text(
            if (masuk) "Dari: ${surat.tujuanAtauPengirim}" else "Kepada: ${surat.tujuanAtauPengirim}",
            color = TeksSoft, fontSize = 14.sp
        )
        Spacer(Modifier.height(6.dp))
        Text(
            formatTanggal(surat.tanggalMillis) +
                if (surat.jejak.dibuatOlehNama.isNotBlank()) " · dicatat ${surat.jejak.dibuatOlehNama}" else "",
            color = TeksRedup, fontSize = 11.sp
        )
    }
}

@Composable
private fun DialogTambahSurat(
    saranNomor: (JenisSurat) -> String,
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
                TextButton(onClick = { nomor = saranNomor(jenis) }) { Text("Isi nomor otomatis") }
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

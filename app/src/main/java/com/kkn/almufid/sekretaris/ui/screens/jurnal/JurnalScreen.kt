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
fun JurnalScreen(viewModel: JurnalViewModel) {
    val daftar by viewModel.daftarJurnal.collectAsState()
    var dialogTerbuka by remember { mutableStateOf(false) }
    var filter by remember { mutableStateOf<JenisJurnal?>(null) }
    val tampil = remember(daftar, filter) { if (filter == null) daftar else daftar.filter { it.jenis == filter } }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.padding(horizontal = 18.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PilChip("Semua", filter == null, { filter = null })
                PilChip("Harian", filter == JenisJurnal.HARIAN, { filter = JenisJurnal.HARIAN })
                PilChip("Notulen", filter == JenisJurnal.NOTULEN_RAPAT, { filter = JenisJurnal.NOTULEN_RAPAT })
            }
            if (tampil.isEmpty()) {
                KosongState("Belum ada catatan. Tekan + untuk menambah jurnal harian atau notulen rapat.")
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(18.dp, 8.dp, 18.dp, 110.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    itemsIndexed(tampil, key = { _, e -> e.id }) { i, e ->
                        PostJurnal(e, Modifier.munculBertahap(i)) { viewModel.hapusJurnal(e) }
                    }
                }
            }
        }
        FabGradien(Icons.Default.Add, "Tambah jurnal", Modifier.align(Alignment.BottomEnd).padding(20.dp)) {
            dialogTerbuka = true
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
private fun PostJurnal(entry: JurnalEntry, modifier: Modifier, onHapus: () -> Unit) {
    KartuGelap(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AvatarGradien("SK", 40.dp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("Sekretaris", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                Text(formatTanggal(entry.tanggalMillis), color = TeksRedup, fontSize = 11.sp)
            }
            TagKecil(if (entry.jenis == JenisJurnal.HARIAN) "Harian" else "Notulen")
            IconButton(onClick = onHapus) { Icon(Icons.Default.Delete, "Hapus", tint = TeksRedup) }
        }
        Spacer(Modifier.height(12.dp))
        Text(entry.judul, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(Modifier.height(4.dp))
        Text(entry.isi, color = TeksSoft, fontSize = 14.sp, lineHeight = 21.sp)
        if (entry.pesertaRapat.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text("Peserta: ${entry.pesertaRapat}", color = TeksRedup, fontSize = 12.sp)
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

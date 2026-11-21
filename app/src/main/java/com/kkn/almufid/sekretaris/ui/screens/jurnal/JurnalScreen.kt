package com.kkn.almufid.sekretaris.ui.screens.jurnal

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kkn.almufid.sekretaris.data.cloud.JurnalBersama
import com.kkn.almufid.sekretaris.data.entity.JenisJurnal
import com.kkn.almufid.sekretaris.data.entity.JurnalEntry
import com.kkn.almufid.sekretaris.ui.auth.DialogInfo
import com.kkn.almufid.sekretaris.ui.formatTanggal
import com.kkn.almufid.sekretaris.ui.components.*
import com.kkn.almufid.sekretaris.ui.theme.*

@Composable
fun JurnalScreen(viewModel: JurnalViewModel) {
    val pribadi by viewModel.daftarJurnal.collectAsState()
    val bersama by viewModel.daftarBersama.collectAsState()
    val pesan by viewModel.pesan.collectAsState()

    var dialogTerbuka by remember { mutableStateOf(false) }
    var sedangUbah by remember { mutableStateOf<JurnalBersama?>(null) }
    var mode by remember { mutableStateOf(0) } // 0 = Pribadi, 1 = Bersama
    var filter by remember { mutableStateOf<JenisJurnal?>(null) }
    val tampilPribadi = remember(pribadi, filter) { if (filter == null) pribadi else pribadi.filter { it.jenis == filter } }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.padding(horizontal = 18.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PilChip("Jurnal Pribadi", mode == 0, { mode = 0 })
                PilChip("Jurnal Bersama", mode == 1, { mode = 1 })
            }

            if (mode == 0) {
                Row(Modifier.padding(horizontal = 18.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PilChip("Semua", filter == null, { filter = null })
                    PilChip("Harian", filter == JenisJurnal.HARIAN, { filter = JenisJurnal.HARIAN })
                    PilChip("Notulen", filter == JenisJurnal.NOTULEN_RAPAT, { filter = JenisJurnal.NOTULEN_RAPAT })
                }
                if (tampilPribadi.isEmpty()) {
                    KosongState("Belum ada catatan pribadi. Catatan ini hanya tersimpan di HP ini. Tekan + untuk menambah.")
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(18.dp, 8.dp, 18.dp, 110.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        itemsIndexed(tampilPribadi, key = { _, e -> e.id }) { i, e ->
                            PostPribadi(e, Modifier.munculBertahap(i)) { viewModel.hapusJurnal(e) }
                        }
                    }
                }
            } else {
                if (bersama.isEmpty()) {
                    KosongState("Belum ada jurnal bersama. Jurnal yang dibagikan akan tampil di semua pengguna. Tekan + untuk membagikan.")
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(18.dp, 8.dp, 18.dp, 110.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        itemsIndexed(bersama) { i, e ->
                            val milikSaya = e.penulisUid == viewModel.uidSaya
                            PostBersama(
                                e, milikSaya, Modifier.munculBertahap(i),
                                onUbah = { sedangUbah = e },
                                onHapus = { viewModel.hapusBersama(e.id) }
                            )
                        }
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
            awalBersama = mode == 1,
            onTutup = { dialogTerbuka = false },
            onSimpan = { jenis, judul, isi, peserta, bagikan ->
                if (bagikan) viewModel.tambahBersama(jenis, judul, isi, peserta)
                else viewModel.tambahJurnal(jenis, judul, isi, peserta)
                mode = if (bagikan) 1 else 0
                dialogTerbuka = false
            }
        )
    }

    sedangUbah?.let { entri ->
        DialogUbahBersama(
            entri = entri,
            onTutup = { sedangUbah = null },
            onSimpan = { judul, isi, peserta ->
                viewModel.ubahBersama(entri.id, judul, isi, peserta)
                sedangUbah = null
            }
        )
    }

    pesan?.let { DialogInfo(judul = "Info", pesan = it, onTutup = viewModel::tutupPesan) }
}

@Composable
private fun PostPribadi(entry: JurnalEntry, modifier: Modifier, onHapus: () -> Unit) {
    KartuGelap(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(formatTanggal(entry.tanggalMillis), color = TeksRedup, fontSize = 11.sp)
            }
            TagKecil(if (entry.jenis == JenisJurnal.HARIAN) "Harian" else "Notulen")
            IconButton(onClick = onHapus) { Icon(Icons.Default.Delete, "Hapus", tint = TeksRedup) }
        }
        Spacer(Modifier.height(6.dp))
        Text(entry.judul, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(Modifier.height(4.dp))
        Text(entry.isi, color = TeksSoft, fontSize = 14.sp, lineHeight = 21.sp)
        if (entry.pesertaRapat.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text("Peserta: ${entry.pesertaRapat}", color = TeksRedup, fontSize = 12.sp)
        }
    }
}

/** Jurnal Bersama: semua orang bisa melihat; tombol ubah/hapus hanya muncul untuk penulisnya. */
@Composable
private fun PostBersama(
    entry: JurnalBersama, milikSaya: Boolean, modifier: Modifier,
    onUbah: () -> Unit, onHapus: () -> Unit,
) {
    KartuGelap(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AvatarGradien(inisialDari(entry.penulisNama), 40.dp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(entry.penulisNama.ifBlank { "Anggota" }, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                Text(
                    listOf(entry.desa, formatTanggal(entry.tanggalMillis)).filter { it.isNotBlank() }.joinToString(" · "),
                    color = TeksRedup, fontSize = 11.sp
                )
            }
            TagKecil(if (entry.jenis == JenisJurnal.HARIAN.name) "Harian" else "Notulen")
            if (milikSaya) {
                IconButton(onClick = onUbah) { Icon(Icons.Default.Edit, "Ubah", tint = TeksRedup) }
                IconButton(onClick = onHapus) { Icon(Icons.Default.Delete, "Hapus", tint = TeksRedup) }
            }
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
    awalBersama: Boolean,
    onTutup: () -> Unit,
    onSimpan: (JenisJurnal, String, String, String, Boolean) -> Unit
) {
    var jenis by remember { mutableStateOf(JenisJurnal.HARIAN) }
    var bagikan by remember { mutableStateOf(awalBersama) }
    var judul by remember { mutableStateOf("") }
    var isi by remember { mutableStateOf("") }
    var peserta by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onTutup,
        title = { Text("Tambah Catatan") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Simpan sebagai", fontSize = 12.sp, color = TeksRedup)
                PilihanSegmen(
                    pilihan = listOf("Pribadi" to false, "Bersama" to true),
                    terpilih = bagikan,
                    onPilih = { bagikan = it }
                )
                Text(
                    if (bagikan) "Tampil di semua pengguna. Hanya Anda yang bisa mengubahnya."
                    else "Hanya tersimpan di HP ini.",
                    fontSize = 11.sp, color = TeksRedup
                )
                PilihanSegmen(
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
            TextButton(onClick = { onSimpan(jenis, judul, isi, peserta, bagikan) }) { Text("Simpan") }
        },
        dismissButton = {
            TextButton(onClick = onTutup) { Text("Batal") }
        }
    )
}

@Composable
private fun DialogUbahBersama(
    entri: JurnalBersama,
    onTutup: () -> Unit,
    onSimpan: (String, String, String) -> Unit
) {
    var judul by remember { mutableStateOf(entri.judul) }
    var isi by remember { mutableStateOf(entri.isi) }
    var peserta by remember { mutableStateOf(entri.pesertaRapat) }

    AlertDialog(
        onDismissRequest = onTutup,
        title = { Text("Ubah Jurnal Bersama") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = judul, onValueChange = { judul = it },
                    label = { Text("Judul singkat") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = isi, onValueChange = { isi = it },
                    label = { Text("Isi") }, modifier = Modifier.fillMaxWidth(), minLines = 3
                )
                if (entri.jenis == JenisJurnal.NOTULEN_RAPAT.name) {
                    OutlinedTextField(
                        value = peserta, onValueChange = { peserta = it },
                        label = { Text("Peserta rapat") }, modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = { onSimpan(judul, isi, peserta) }) { Text("Simpan") } },
        dismissButton = { TextButton(onClick = onTutup) { Text("Batal") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> PilihanSegmen(
    pilihan: List<Pair<String, T>>,
    terpilih: T,
    onPilih: (T) -> Unit
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

package com.kkn.almufid.sekretaris.ui.screens.koordinasi

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import com.kkn.almufid.sekretaris.data.auth.ProfilPengguna
import com.kkn.almufid.sekretaris.data.cloud.Kas
import com.kkn.almufid.sekretaris.data.cloud.Pengumuman
import com.kkn.almufid.sekretaris.data.cloud.Polling
import com.kkn.almufid.sekretaris.data.cloud.Tugas
import com.kkn.almufid.sekretaris.ui.auth.DialogInfo
import com.kkn.almufid.sekretaris.ui.components.*
import com.kkn.almufid.sekretaris.ui.formatRupiah
import com.kkn.almufid.sekretaris.ui.formatTanggal
import com.kkn.almufid.sekretaris.ui.theme.*

private const val HARI = 86_400_000L

@Composable
private fun PesanKoordinasi(vm: KoordinasiViewModel) {
    val pesan by vm.pesan.collectAsState()
    pesan?.let { DialogInfo(judul = "Info", pesan = it, onTutup = vm::tutupPesan) }
}

@Composable
private fun CatatanAdmin(teks: String) {
    Text(teks, fontSize = 11.sp, color = TeksRedup, modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp))
}

// ===================== PENGUMUMAN =====================

@Composable
fun PengumumanScreen(vm: KoordinasiViewModel) {
    val daftar by vm.pengumuman.collectAsState()
    val admin by vm.admin.collectAsState()
    var dialog by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            if (!admin) CatatanAdmin("Pengumuman dibuat oleh admin. Pengumuman yang tampil di layar otomatis ditandai sudah dibaca.")
            if (daftar.isEmpty()) {
                KosongState("Belum ada pengumuman.")
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(18.dp, 8.dp, 18.dp, 110.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    itemsIndexed(daftar) { i, p ->
                        KartuPengumuman(p, vm, admin, Modifier.munculBertahap(i))
                    }
                }
            }
        }
        if (admin) {
            FabGradien(Icons.Default.Add, "Tambah pengumuman", Modifier.align(Alignment.BottomEnd).padding(20.dp)) { dialog = true }
        }
    }
    if (dialog) {
        var judul by remember { mutableStateOf("") }
        var isi by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { dialog = false },
            title = { Text("Pengumuman Baru") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(judul, { judul = it }, label = { Text("Judul") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(isi, { isi = it }, label = { Text("Isi pengumuman") }, minLines = 3, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = { TextButton(onClick = { vm.tambahPengumuman(judul, isi); dialog = false }) { Text("Kirim") } },
            dismissButton = { TextButton(onClick = { dialog = false }) { Text("Batal") } }
        )
    }
    PesanKoordinasi(vm)
}

@Composable
private fun KartuPengumuman(p: Pengumuman, vm: KoordinasiViewModel, admin: Boolean, modifier: Modifier) {
    val sudahBaca = vm.uidSaya in p.dibacaOleh
    var ubah by remember { mutableStateOf(false) }
    if (ubah) DialogUbah("Ubah Pengumuman", listOf(Bidang("Judul", p.judul), Bidang("Isi", p.isi)), { ubah = false }) {
        vm.ubahPengumuman(p.id, it[0], it[1]); ubah = false
    }
    // Tampil di layar = dianggap dibaca.
    LaunchedEffect(p.id, sudahBaca) {
        if (!sudahBaca && vm.uidSaya.isNotEmpty()) vm.tandaiDibaca(p.id)
    }
    KartuGelap(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (!sudahBaca) {
                Box(Modifier.size(8.dp).background(Pink, CircleShape))
                Spacer(Modifier.width(8.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(p.judul, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                Text("${p.penulisNama} · ${formatTanggal(p.tanggalMillis)}", color = TeksRedup, fontSize = 11.sp)
            }
            if (admin) {
                IconButton(onClick = { ubah = true }) { Icon(Icons.Default.Edit, "Ubah", tint = TeksRedup) }
                IconButton(onClick = { vm.hapusPengumuman(p.id) }) { Icon(Icons.Default.Delete, "Hapus", tint = TeksRedup) }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(p.isi, color = TeksSoft, fontSize = 14.sp, lineHeight = 21.sp)
        if (admin) {
            Spacer(Modifier.height(8.dp))
            Text("Dibaca ${p.dibacaOleh.size} orang", color = Mint, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// ===================== POLLING =====================

@Composable
fun PollingScreen(vm: KoordinasiViewModel) {
    val daftar by vm.polling.collectAsState()
    val admin by vm.admin.collectAsState()
    var dialog by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            if (!admin) CatatanAdmin("Polling dibuat oleh admin. Ketuk pilihan untuk memberi suara (satu kali, tidak bisa diubah).")
            if (daftar.isEmpty()) {
                KosongState("Belum ada polling.")
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(18.dp, 8.dp, 18.dp, 110.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    itemsIndexed(daftar) { i, p -> KartuPolling(p, vm, admin, Modifier.munculBertahap(i)) }
                }
            }
        }
        if (admin) {
            FabGradien(Icons.Default.Add, "Buat polling", Modifier.align(Alignment.BottomEnd).padding(20.dp)) { dialog = true }
        }
    }
    if (dialog) {
        var tanya by remember { mutableStateOf("") }
        val opsi = remember { mutableStateListOf("", "", "", "") }
        AlertDialog(
            onDismissRequest = { dialog = false },
            title = { Text("Polling Baru") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(tanya, { tanya = it }, label = { Text("Pertanyaan") }, modifier = Modifier.fillMaxWidth())
                    opsi.forEachIndexed { i, nilai ->
                        OutlinedTextField(
                            nilai, { opsi[i] = it }, singleLine = true,
                            label = { Text("Pilihan ${i + 1}${if (i < 2) "" else " (opsional)"}") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = { TextButton(onClick = { vm.tambahPolling(tanya, opsi.toList()); dialog = false }) { Text("Buat") } },
            dismissButton = { TextButton(onClick = { dialog = false }) { Text("Batal") } }
        )
    }
    PesanKoordinasi(vm)
}

@Composable
private fun KartuPolling(p: Polling, vm: KoordinasiViewModel, admin: Boolean, modifier: Modifier) {
    val pilihanSaya = p.suara[vm.uidSaya]
    val total = p.suara.size
    var ubah by remember { mutableStateOf(false) }
    if (ubah) DialogUbah(
        "Ubah Polling",
        listOf(Bidang("Pertanyaan", p.pertanyaan)) + p.opsi.mapIndexed { i, o -> Bidang("Pilihan ${i + 1}", o) },
        { ubah = false }
    ) { vm.ubahPolling(p.id, it[0], it.drop(1)); ubah = false }
    KartuGelap(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(p.pertanyaan, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                Text(
                    "${p.penulisNama} · $total suara${if (p.ditutup) " · ditutup" else ""}",
                    color = TeksRedup, fontSize = 11.sp
                )
            }
            if (admin) {
                IconButton(onClick = { ubah = true }) { Icon(Icons.Default.Edit, "Ubah", tint = TeksRedup) }
                IconButton(onClick = { vm.hapusPolling(p.id) }) { Icon(Icons.Default.Delete, "Hapus", tint = TeksRedup) }
            }
        }
        Spacer(Modifier.height(10.dp))
        p.opsi.forEachIndexed { i, teks ->
            val jumlah = p.suara.values.count { it == i }
            val frac = if (total == 0) 0f else jumlah.toFloat() / total
            val bolehPilih = pilihanSaya == null && !p.ditutup
            Column(
                Modifier.fillMaxWidth().padding(vertical = 5.dp)
                    .then(if (bolehPilih) Modifier.tekanPegas { vm.beriSuara(p.id, i) } else Modifier)
            ) {
                Row {
                    Text(
                        (if (pilihanSaya == i) "✓ " else "") + teks,
                        fontSize = 14.sp, fontWeight = if (pilihanSaya == i) FontWeight.Bold else FontWeight.Normal,
                        color = if (pilihanSaya == i) Mint else TeksUtama, modifier = Modifier.weight(1f)
                    )
                    Text("$jumlah (${(frac * 100).toInt()}%)", fontSize = 12.sp, color = TeksRedup)
                }
                Spacer(Modifier.height(4.dp))
                BatangProgres(frac)
            }
        }
        if (admin && !p.ditutup) {
            Spacer(Modifier.height(6.dp))
            TextButton(onClick = { vm.tutupPolling(p.id) }) { Text("Tutup polling", color = Merah) }
        }
    }
}

// ===================== TUGAS & KALENDER =====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TugasScreen(vm: KoordinasiViewModel) {
    val daftar by vm.tugas.collectAsState()
    val admin by vm.admin.collectAsState()
    val anggota by vm.anggota.collectAsState()
    var dialog by remember { mutableStateOf(false) }
    var filter by remember { mutableStateOf(0) } // 0 belum, 1 selesai, 2 semua

    val tampil = remember(daftar, filter) {
        daftar.filter { filter == 2 || (filter == 0 && !it.selesai) || (filter == 1 && it.selesai) }
            .sortedBy { it.deadlineMillis }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.padding(horizontal = 18.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PilChip("Belum selesai", filter == 0, { filter = 0 })
                PilChip("Selesai", filter == 1, { filter = 1 })
                PilChip("Semua", filter == 2, { filter = 2 })
            }
            if (!admin) CatatanAdmin("Tugas diberikan oleh admin. Centang tugas milikmu setelah selesai.")
            if (tampil.isEmpty()) {
                KosongState("Tidak ada tugas di daftar ini.")
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(18.dp, 8.dp, 18.dp, 110.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    var tanggalTerakhir = ""
                    tampil.forEachIndexed { i, t ->
                        val tanggal = formatTanggal(t.deadlineMillis).substringBefore(",")
                        val tampilkanHeader = tanggal != tanggalTerakhir
                        tanggalTerakhir = tanggal
                        item(key = t.id) {
                            Column {
                                if (tampilkanHeader) {
                                    Text(
                                        tanggal, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold,
                                        color = VioletTerang, modifier = Modifier.padding(bottom = 6.dp, top = 4.dp)
                                    )
                                }
                                KartuTugas(t, vm, admin, Modifier.munculBertahap(i))
                            }
                        }
                    }
                }
            }
        }
        if (admin) {
            FabGradien(Icons.Default.Add, "Tambah tugas", Modifier.align(Alignment.BottomEnd).padding(20.dp)) { dialog = true }
        }
    }

    if (dialog) {
        var judul by remember { mutableStateOf("") }
        var deskripsi by remember { mutableStateOf("") }
        var untuk by remember { mutableStateOf<ProfilPengguna?>(null) }
        var deadline by remember { mutableStateOf<Long?>(null) }
        var tampilTanggal by remember { mutableStateOf(false) }
        val stateTanggal = rememberDatePickerState()

        AlertDialog(
            onDismissRequest = { dialog = false },
            title = { Text("Tugas Baru") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(judul, { judul = it }, label = { Text("Judul tugas") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(deskripsi, { deskripsi = it }, label = { Text("Keterangan (opsional)") }, modifier = Modifier.fillMaxWidth())
                    Text("Untuk siapa", fontSize = 12.sp, color = TeksRedup)
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        anggota.forEach { a ->
                            PilChip(a.namaLengkap.ifBlank { a.nim }, untuk?.uid == a.uid, { untuk = a })
                        }
                    }
                    TextButton(onClick = { tampilTanggal = true }) {
                        Text(deadline?.let { "Tenggat: ${formatTanggal(it).substringBefore(",")}" } ?: "Pilih tanggal tenggat")
                    }
                }
            },
            confirmButton = { TextButton(onClick = { vm.tambahTugas(judul, deskripsi, untuk, deadline); dialog = false }) { Text("Simpan") } },
            dismissButton = { TextButton(onClick = { dialog = false }) { Text("Batal") } }
        )
        if (tampilTanggal) {
            DatePickerDialog(
                onDismissRequest = { tampilTanggal = false },
                confirmButton = {
                    TextButton(onClick = { deadline = stateTanggal.selectedDateMillis; tampilTanggal = false }) { Text("OK") }
                },
                dismissButton = { TextButton(onClick = { tampilTanggal = false }) { Text("Batal") } }
            ) { DatePicker(state = stateTanggal) }
        }
    }
    PesanKoordinasi(vm)
}

@Composable
private fun KartuTugas(t: Tugas, vm: KoordinasiViewModel, admin: Boolean, modifier: Modifier) {
    val sekarang = System.currentTimeMillis()
    // Tenggat berlaku sampai akhir hari yang dipilih.
    val sisaHari = Math.floorDiv(t.deadlineMillis + HARI - sekarang, HARI)
    val (label, warna) = when {
        t.selesai -> "Selesai" to Mint
        sisaHari < 0L -> "Terlambat" to Merah
        sisaHari == 0L -> "Hari ini" to Gold
        else -> "$sisaHari hari lagi" to TeksRedup
    }
    val bolehCentang = admin || t.untukUid == vm.uidSaya
    var ubah by remember { mutableStateOf(false) }
    if (ubah) DialogUbah("Ubah Tugas", listOf(Bidang("Judul", t.judul), Bidang("Keterangan", t.deskripsi)), { ubah = false }) {
        vm.ubahTugas(t.id, it[0], it[1]); ubah = false
    }
    KartuGelap(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = t.selesai,
                onCheckedChange = { vm.tandaiSelesai(t.id, it) },
                enabled = bolehCentang
            )
            Column(Modifier.weight(1f)) {
                Text(t.judul, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text("Untuk ${t.untukNama}", fontSize = 11.sp, color = TeksRedup)
                if (t.deskripsi.isNotBlank()) Text(t.deskripsi, fontSize = 12.sp, color = TeksSoft)
            }
            Text(label, fontSize = 11.sp, color = warna, fontWeight = FontWeight.Bold)
            if (admin) {
                IconButton(onClick = { ubah = true }) { Icon(Icons.Default.Edit, "Ubah", tint = TeksRedup) }
                IconButton(onClick = { vm.hapusTugas(t.id) }) { Icon(Icons.Default.Delete, "Hapus", tint = TeksRedup) }
            }
        }
    }
}

// ===================== KAS =====================

@Composable
fun KasScreen(vm: KoordinasiViewModel) {
    val daftar by vm.kas.collectAsState()
    val admin by vm.admin.collectAsState()
    var dialog by remember { mutableStateOf(false) }
    var ubahKas by remember { mutableStateOf<Kas?>(null) }
    ubahKas?.let { k ->
        DialogUbah(
            "Ubah Catatan Kas",
            listOf(Bidang("Jumlah (Rp)", k.jumlah.toString(), angka = true), Bidang("Kategori", k.kategori), Bidang("Keterangan", k.keterangan)),
            { ubahKas = null }
        ) { vm.ubahKas(k.id, it[0], it[1], it[2]); ubahKas = null }
    }

    val masuk = daftar.filter { it.masuk }.sumOf { it.jumlah }
    val keluar = daftar.filter { !it.masuk }.sumOf { it.jumlah }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(18.dp, 8.dp, 18.dp, 110.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                KartuGelap(Modifier.fillMaxWidth()) {
                    Text("Saldo kas", fontSize = 12.sp, color = TeksRedup)
                    Text(formatRupiah(masuk - keluar), fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                    Spacer(Modifier.height(8.dp))
                    Row {
                        Column(Modifier.weight(1f)) {
                            Text("Pemasukan", fontSize = 11.sp, color = TeksRedup)
                            Text(formatRupiah(masuk), color = Mint, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Column(Modifier.weight(1f)) {
                            Text("Pengeluaran", fontSize = 11.sp, color = TeksRedup)
                            Text(formatRupiah(keluar), color = Merah, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }
            if (!admin) item { CatatanAdmin("Kas dicatat oleh admin (bendahara). Semua anggota bisa melihat agar transparan.") }
            if (daftar.isEmpty()) item { KosongState("Belum ada catatan kas.") }
            itemsIndexed(daftar) { i, k ->
                KartuGelap(Modifier.fillMaxWidth().munculBertahap(i)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(k.kategori.ifBlank { if (k.masuk) "Pemasukan" else "Pengeluaran" }, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            if (k.keterangan.isNotBlank()) Text(k.keterangan, fontSize = 12.sp, color = TeksSoft)
                            Text("${k.penulisNama} · ${formatTanggal(k.tanggalMillis)}", fontSize = 11.sp, color = TeksRedup)
                        }
                        Text(
                            (if (k.masuk) "+" else "-") + formatRupiah(k.jumlah),
                            color = if (k.masuk) Mint else Merah, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp
                        )
                        if (admin) {
                            IconButton(onClick = { ubahKas = k }) { Icon(Icons.Default.Edit, "Ubah", tint = TeksRedup) }
                            IconButton(onClick = { vm.hapusKas(k.id) }) { Icon(Icons.Default.Delete, "Hapus", tint = TeksRedup) }
                        }
                    }
                }
            }
        }
        if (admin) {
            FabGradien(Icons.Default.Add, "Catat kas", Modifier.align(Alignment.BottomEnd).padding(20.dp)) { dialog = true }
        }
    }
    if (dialog) {
        var masukBaru by remember { mutableStateOf(true) }
        var jumlah by remember { mutableStateOf("") }
        var kategori by remember { mutableStateOf("") }
        var ket by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { dialog = false },
            title = { Text("Catat Kas") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PilChip("Pemasukan", masukBaru, { masukBaru = true })
                        PilChip("Pengeluaran", !masukBaru, { masukBaru = false })
                    }
                    OutlinedTextField(
                        jumlah, { jumlah = it }, label = { Text("Jumlah (Rp)") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(kategori, { kategori = it }, label = { Text("Kategori (mis. Iuran, Konsumsi)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(ket, { ket = it }, label = { Text("Keterangan") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = { TextButton(onClick = { vm.tambahKas(masukBaru, jumlah, kategori, ket); dialog = false }) { Text("Simpan") } },
            dismissButton = { TextButton(onClick = { dialog = false }) { Text("Batal") } }
        )
    }
    PesanKoordinasi(vm)
}

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
import com.kkn.almufid.sekretaris.data.cloud.AnggotaDesa
import com.kkn.almufid.sekretaris.ui.auth.DialogInfo
import com.kkn.almufid.sekretaris.ui.formatTanggal
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.kkn.almufid.sekretaris.ui.components.*
import com.kkn.almufid.sekretaris.ui.theme.*
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AbsensiScreen(viewModel: AbsensiViewModel) {
    val anggota by viewModel.daftarAnggota.collectAsState()
    val absensi by viewModel.absensiHariIni.collectAsState()
    val tanggal by viewModel.tanggalDipilih.collectAsState()
    val semua by viewModel.semuaAnggota.collectAsState()
    val pesan by viewModel.pesan.collectAsState()
    var dialogTambahAnggota by remember { mutableStateOf(false) }
    var anggotaDiubah by remember { mutableStateOf<AnggotaDesa?>(null) }
    var konfirmasiNonaktif by remember { mutableStateOf<AnggotaDesa?>(null) }
    var lihatNonaktif by remember { mutableStateOf(false) }
    val nonaktifLain = remember(semua, anggota) { semua.filter { s -> !s.aktif && anggota.none { it.id == s.id } } }

    val statusPerAnggota = remember(absensi) { absensi.associateBy { it.anggotaId } }
    val hadir = absensi.count { it.hadir }
    val persen = if (anggota.isEmpty()) 0f else (hadir.toFloat() / anggota.size).coerceIn(0f, 1f)
    val hariIni = remember { awalHari(System.currentTimeMillis()) }
    val daftarHari = remember { (6 downTo 0).map { hariIni - it * 86_400_000L } }
    val fmtHari = remember { SimpleDateFormat("EEE", Locale("id", "ID")) }
    val fmtAngka = remember { SimpleDateFormat("d", Locale("id", "ID")) }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                daftarHari.forEach { hari ->
                    val dipilih = hari == tanggal
                    Column(
                        Modifier.weight(1f).tekanPegas { viewModel.pilihTanggal(hari) }
                            .height(64.dp).clip(RoundedCornerShape(3.dp))
                            .then(if (dipilih) Modifier.background(GradUtama) else Modifier.background(Surface1)),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            fmtHari.format(Date(hari)), fontSize = 11.sp, fontWeight = FontWeight.Bold,
                            color = if (dipilih) Color.White else TeksRedup
                        )
                        Text(
                            fmtAngka.format(Date(hari)), fontSize = 16.sp, fontWeight = FontWeight.ExtraBold,
                            color = if (dipilih) Color.White else TeksUtama
                        )
                    }
                }
            }
            KartuGelap(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CincinPersen(persen)
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text("$hadir dari ${anggota.size} hadir", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                        Text(formatTanggal(tanggal).substringBeforeLast(","), color = TeksRedup, fontSize = 12.sp)
                    }
                }
            }
            if (anggota.isEmpty()) {
                KosongState("Belum ada anggota. Tekan tombol di kanan bawah untuk menambah anggota kelompok.")
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(18.dp, 0.dp, 18.dp, 110.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(anggota, key = { _, a -> a.id }) { i, orang ->
                        BarisAbsensi(
                            nama = orang.nama, divisi = orang.divisi,
                            status = statusPerAnggota[orang.id]?.status,
                            aktif = orang.aktif,
                            dicatatOleh = statusPerAnggota[orang.id]?.jejak?.diubahOlehNama?.ifBlank { null },
                            modifier = Modifier.munculBertahap(i),
                            onUbah = { status -> viewModel.tandaiKehadiran(orang, status) },
                            onUbahData = { anggotaDiubah = orang },
                            onAturAktif = {
                                if (orang.aktif) konfirmasiNonaktif = orang else viewModel.aturAktif(orang, true)
                            }
                        )
                    }
                    if (nonaktifLain.isNotEmpty()) {
                        item {
                            Text(
                                (if (lihatNonaktif) "Sembunyikan" else "Lihat") + " anggota nonaktif (${nonaktifLain.size})",
                                color = VioletTerang, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                                modifier = Modifier.tekanPegas { lihatNonaktif = !lihatNonaktif }.padding(vertical = 6.dp)
                            )
                        }
                        if (lihatNonaktif) {
                            items(nonaktifLain) { orang ->
                                BarisAbsensi(
                                    nama = orang.nama, divisi = orang.divisi, status = null,
                                    aktif = false, dicatatOleh = null, modifier = Modifier,
                                    onUbah = {}, onAturAktif = { viewModel.aturAktif(orang, true) }
                                )
                            }
                        }
                    }
                }
            }
        }
        FabGradien(Icons.Default.PersonAdd, "Tambah anggota", Modifier.align(Alignment.BottomEnd).padding(20.dp)) {
            dialogTambahAnggota = true
        }
    }

    konfirmasiNonaktif?.let { orang ->
        AlertDialog(
            onDismissRequest = { konfirmasiNonaktif = null },
            title = { Text("Nonaktifkan ${orang.nama}?") },
            text = {
                Text(
                    "Anggota tidak muncul lagi di daftar harian, tetapi seluruh riwayat absensinya tetap tersimpan " +
                        "dan bisa diaktifkan kembali kapan saja."
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.aturAktif(orang, false); konfirmasiNonaktif = null }) { Text("Nonaktifkan") }
            },
            dismissButton = { TextButton(onClick = { konfirmasiNonaktif = null }) { Text("Batal") } }
        )
    }
    pesan?.let { DialogInfo(judul = "Info", pesan = it, onTutup = viewModel::tutupPesan) }

    anggotaDiubah?.let { a ->
        com.kkn.almufid.sekretaris.ui.components.DialogUbah(
            judul = "Ubah Anggota",
            bidang = listOf(com.kkn.almufid.sekretaris.ui.components.Bidang("Nama", a.nama), com.kkn.almufid.sekretaris.ui.components.Bidang("Divisi", a.divisi)),
            onTutup = { anggotaDiubah = null },
            onSimpan = { v -> viewModel.ubahAnggota(a, v[0], v[1]); anggotaDiubah = null }
        )
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
private fun CincinPersen(persen: Float) {
    val sapuan by animateFloatAsState(persen * 360f, tween(800), label = "sapuan")
    Box(Modifier.size(68.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val lebar = 9.dp.toPx()
            val ukuran = Size(size.width - lebar, size.height - lebar)
            val pojok = Offset(lebar / 2, lebar / 2)
            drawArc(Surface2, 0f, 360f, false, pojok, ukuran, style = Stroke(lebar, cap = StrokeCap.Round))
            drawArc(Mint, -90f, sapuan, false, pojok, ukuran, style = Stroke(lebar, cap = StrokeCap.Round))
        }
        Text("${(persen * 100).toInt()}%", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
    }
}

@Composable
private fun BarisAbsensi(
    nama: String,
    divisi: String,
    status: String?,
    aktif: Boolean,
    dicatatOleh: String?,
    modifier: Modifier,
    onUbah: (String) -> Unit,
    onUbahData: () -> Unit = {},
    onAturAktif: () -> Unit
) {
    Column(modifier.fillMaxWidth().clip(RoundedCornerShape(3.dp)).background(Surface1).padding(12.dp)) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        AvatarGradien(inisialDari(nama), 42.dp)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(nama, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
            if (divisi.isNotBlank()) Text(divisi, color = TeksRedup, fontSize = 11.sp)
            if (!aktif) Text("Nonaktif", color = Gold, fontSize = 11.sp)
            if (dicatatOleh != null) Text("dicatat oleh $dicatatOleh", color = TeksRedup, fontSize = 10.sp)
            Text(
                if (aktif) "Nonaktifkan" else "Aktifkan kembali", color = VioletTerang, fontSize = 10.sp,
                fontWeight = FontWeight.Bold, modifier = Modifier.tekanPegas(onAturAktif).padding(top = 2.dp)
            )
            Text(
                "Ubah nama / divisi", color = VioletTerang, fontSize = 10.sp,
                fontWeight = FontWeight.Bold, modifier = Modifier.tekanPegas(onUbahData).padding(top = 2.dp)
            )
        }
    }
    if (aktif || status != null) {
        Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            PilKecil("Hadir", status == "HADIR", HijauTua) { onUbah("HADIR") }
            PilKecil("Izin", status == "IZIN", Gold) { onUbah("IZIN") }
            PilKecil("Sakit", status == "SAKIT", VioletTerang) { onUbah("SAKIT") }
            PilKecil("Alpa", status == "ALPA", Pink) { onUbah("ALPA") }
        }
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

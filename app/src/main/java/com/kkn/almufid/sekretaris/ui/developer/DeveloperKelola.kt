package com.kkn.almufid.sekretaris.ui.developer

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kkn.almufid.sekretaris.data.auth.Peran
import com.kkn.almufid.sekretaris.data.cloud.JurnalBersama
import com.kkn.almufid.sekretaris.ui.components.AvatarGradien
import com.kkn.almufid.sekretaris.ui.components.KartuGelap
import com.kkn.almufid.sekretaris.ui.components.KosongState
import com.kkn.almufid.sekretaris.ui.components.PilChip
import com.kkn.almufid.sekretaris.ui.components.inisialDari
import com.kkn.almufid.sekretaris.ui.formatTanggal
import com.kkn.almufid.sekretaris.ui.screens.laporan.PembuatLaporan
import com.kkn.almufid.sekretaris.ui.theme.*

private val ISI = PaddingValues(start = 18.dp, end = 18.dp, top = 6.dp, bottom = 32.dp)

// ------------------------------------------------------------------- Akun

@Composable
fun AkunScreen(vm: DeveloperViewModel, data: DataDeveloper, status: StatusDeveloper) {
    var cari by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(0) } // 0 semua, 1 aktif, 2 diblokir
    val sekarang = remember(data) { System.currentTimeMillis() }
    val tampil = data.profil.filter {
        val q = cari.trim().lowercase()
        (filter == 0 || (filter == 1 && !it.akunDiblokir) || (filter == 2 && it.akunDiblokir)) &&
            (q.isEmpty() || listOf(it.namaLengkap, it.nim, it.desa, it.divisi).any { f -> f.lowercase().contains(q) })
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = ISI, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            OutlinedTextField(
                value = cari, onValueChange = { cari = it }, singleLine = true,
                modifier = Modifier.fillMaxWidth(), placeholder = { Text("Cari nama, NIM, desa, divisi") }
            )
        }
        item {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Semua", "Aktif", "Diblokir").forEachIndexed { i, t -> PilChip(t, filter == i, { filter = i }) }
            }
        }
        if (status.sedangMuat) item {
            Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = VioletTerang)
            }
        }
        if (!status.sedangMuat && tampil.isEmpty()) item { KosongState("Tidak ada akun yang cocok.") }
        items(tampil) { p ->
            KartuGelap(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AvatarGradien(inisialDari(p.namaLengkap), 42.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            p.namaLengkap.ifBlank { "(tanpa nama)" }, fontWeight = FontWeight.Bold, fontSize = 14.sp,
                            maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            listOf(p.nim, p.divisi, p.desa).filter { it.isNotBlank() }.joinToString(" · "),
                            fontSize = 11.sp, color = TeksRedup, maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                        val peran = if (Peran.dariString(p.peran) == Peran.DEVELOPER) "Developer" else "Peserta"
                        Text(
                            (if (p.akunDiblokir) "$peran · Diblokir" else peran) + " · aktif ${lalu(p.terakhirAktifMillis, sekarang)}",
                            fontSize = 11.sp, color = if (p.akunDiblokir) Merah else Mint
                        )
                    }
                }
                if (status.admin) {
                    Spacer(Modifier.height(10.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (p.uid != vm.uidSaya) {
                            Button(
                                onClick = { vm.aturBlokir(p.uid, !p.akunDiblokir) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = if (p.akunDiblokir) HijauTua else Merah)
                            ) { Text(if (p.akunDiblokir) "Buka blokir" else "Blokir", fontSize = 12.sp) }
                        }
                        Button(
                            onClick = { vm.resetSandi(p.email) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Surface2)
                        ) { Text("Reset sandi", fontSize = 12.sp) }
                    }
                }
            }
        }
    }
}

// --------------------------------------------------------------- Moderasi

@Composable
fun ModerasiScreen(vm: DeveloperViewModel, data: DataDeveloper, admin: Boolean) {
    var cari by remember { mutableStateOf("") }
    var target by remember { mutableStateOf<JurnalBersama?>(null) }
    val tampil = data.jurnal.filter {
        val q = cari.trim().lowercase()
        q.isEmpty() || (it.judul + " " + it.isi + " " + it.penulisNama).lowercase().contains(q)
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = ISI, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text(
                "Jurnal yang disembunyikan hilang dari semua pengguna, tetapi tidak dihapus permanen dari database.",
                fontSize = 11.sp, color = TeksRedup, lineHeight = 15.sp
            )
        }
        item {
            OutlinedTextField(
                value = cari, onValueChange = { cari = it }, singleLine = true,
                modifier = Modifier.fillMaxWidth(), placeholder = { Text("Cari judul, isi, atau penulis") }
            )
        }
        if (tampil.isEmpty()) item { KosongState("Tidak ada jurnal bersama.") }
        items(tampil) { j ->
            KartuGelap(Modifier.fillMaxWidth()) {
                Text(j.judul.ifBlank { "(tanpa judul)" }, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(
                    "${j.penulisNama}${if (j.desa.isNotBlank()) " · ${j.desa}" else ""} · ${formatTanggal(j.tanggalMillis)}",
                    fontSize = 11.sp, color = TeksRedup
                )
                if (j.isi.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(j.isi, fontSize = 12.sp, color = TeksSoft, maxLines = 3, overflow = TextOverflow.Ellipsis)
                }
                if (admin) {
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { target = j },
                        colors = ButtonDefaults.buttonColors(containerColor = Merah)
                    ) { Text("Sembunyikan", fontSize = 12.sp) }
                }
            }
        }
    }
    target?.let { j ->
        AlertDialog(
            onDismissRequest = { target = null },
            title = { Text("Sembunyikan jurnal ini?") },
            text = { Text("\"${j.judul.ifBlank { "(tanpa judul)" }}\" milik ${j.penulisNama} tidak akan terlihat oleh siapa pun lagi.") },
            confirmButton = {
                TextButton(onClick = { vm.sembunyikanJurnal(j.id); target = null }) { Text("Sembunyikan", color = Merah) }
            },
            dismissButton = { TextButton(onClick = { target = null }) { Text("Batal") } }
        )
    }
}

// ------------------------------------------------------------ Kotak masuk

@Composable
fun KotakMasukScreen(data: DataDeveloper) {
    val nama = data.profil.associate { it.uid to it.namaLengkap }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = ISI, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text(
                "Saran yang dikirim peserta dari menu Pengaturan. Hanya saran yang terkirim ke server yang muncul di sini.",
                fontSize = 11.sp, color = TeksRedup, lineHeight = 15.sp
            )
        }
        if (data.saran.isEmpty()) item {
            KosongState("Belum ada saran masuk. Kalau seharusnya ada, pastikan aturan Firestore terbaru sudah dipasang.")
        }
        items(data.saran) { s ->
            KartuGelap(Modifier.fillMaxWidth()) {
                Text(
                    "${nama[s.uid]?.ifBlank { null } ?: "Peserta"} · ${formatTanggal(s.dikirimMillis)}",
                    fontSize = 11.sp, color = TeksRedup
                )
                Spacer(Modifier.height(4.dp))
                Text(s.isi, fontSize = 13.sp, lineHeight = 18.sp)
            }
        }
    }
}

// ----------------------------------------------------------------- Ekspor

@Composable
fun EksporScreen(vm: DeveloperViewModel) {
    val context = LocalContext.current
    val sibuk by vm.sedangEkspor.collectAsState()
    LazyColumn(Modifier.fillMaxSize(), contentPadding = ISI, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text(
                "Berkas dibuat dari data yang sedang tampil, lalu dibagikan lewat menu Android (simpan ke File, kirim lewat WhatsApp, dan sebagainya).",
                fontSize = 11.sp, color = TeksRedup, lineHeight = 15.sp
            )
        }
        items(JenisEkspor.entries.toList()) { jenis ->
            KartuGelap(Modifier.fillMaxWidth()) {
                Text(jenis.judul, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(jenis.keterangan, fontSize = 11.sp, color = TeksRedup)
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { vm.ekspor(context, jenis) { file, mime -> PembuatLaporan.bagikan(context, file, mime) } },
                    enabled = !sibuk,
                    colors = ButtonDefaults.buttonColors(containerColor = Violet)
                ) { Text(if (sibuk) "Memproses..." else "Buat dan bagikan", fontSize = 12.sp) }
            }
        }
    }
}

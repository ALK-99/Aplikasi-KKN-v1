package com.kkn.almufid.sekretaris.ui.screens.proker

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kkn.almufid.sekretaris.data.cloud.BuktiDesa
import com.kkn.almufid.sekretaris.data.cloud.ProkerDesa
import com.kkn.almufid.sekretaris.ui.FotoUtil
import com.kkn.almufid.sekretaris.ui.permissions.rememberPemicuGaleri
import com.kkn.almufid.sekretaris.ui.permissions.rememberPemicuKamera
import com.kkn.almufid.sekretaris.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun format() = SimpleDateFormat("dd/MM/yyyy", Locale("id", "ID")).apply { isLenient = false }

internal fun ringkasTanggal(millis: Long): String = if (millis <= 0L) "?" else format().format(Date(millis))

private fun tulisTanggal(millis: Long): String = if (millis <= 0L) "" else format().format(Date(millis))

/** Kosong dianggap belum diisi (0). Null berarti format salah. */
private fun bacaTanggal(teks: String): Long? =
    if (teks.isBlank()) 0L else try { format().parse(teks.trim())?.time } catch (e: Exception) { null }

/** Rencana, anggaran, PIC, realisasi, evaluasi, dan bukti foto untuk satu proker. */
@Composable
fun DialogKelolaProker(proker: ProkerDesa, viewModel: ProkerViewModel, onTutup: () -> Unit) {
    var target by remember { mutableStateOf(proker.target) }
    var mulai by remember { mutableStateOf(tulisTanggal(proker.mulaiMillis)) }
    var selesai by remember { mutableStateOf(tulisTanggal(proker.selesaiMillis)) }
    var anggaran by remember { mutableStateOf(if (proker.anggaran > 0L) proker.anggaran.toString() else "") }
    var pic by remember { mutableStateOf(proker.picNama) }
    var realisasi by remember { mutableStateOf(if (proker.realisasi > 0L) proker.realisasi.toString() else "") }
    var evaluasi by remember { mutableStateOf(proker.catatanEvaluasi) }
    var catatanBukti by remember { mutableStateOf("") }
    var galat by remember { mutableStateOf<String?>(null) }

    val bukti by remember(proker.id) { viewModel.buktiUntuk(proker.id) }.collectAsState(initial = emptyList<BuktiDesa>())

    val context = LocalContext.current
    val peluncurKamera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bm ->
        if (bm != null) { viewModel.tambahBukti(proker, catatanBukti, bm); catatanBukti = "" }
    }
    val peluncurGaleri = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        val bm = uri?.let { FotoUtil.bacaDariUri(context, it) }
        if (bm != null) { viewModel.tambahBukti(proker, catatanBukti, bm); catatanBukti = "" }
    }
    val bukaKamera = rememberPemicuKamera(
        onDiizinkan = { peluncurKamera.launch(null) },
        onDitolak = { galat = "Izin kamera ditolak. Aktifkan lewat Pengaturan HP > Aplikasi > KKN Al-Mushlih 2027 > Izin." }
    )
    val bukaGaleri = rememberPemicuGaleri(
        onDiizinkan = { peluncurGaleri.launch("image/*") },
        onDitolak = { galat = "Izin galeri ditolak. Aktifkan lewat Pengaturan HP > Aplikasi > KKN Al-Mushlih 2027 > Izin." }
    )

    AlertDialog(
        onDismissRequest = onTutup,
        containerColor = Surface1,
        titleContentColor = TeksUtama,
        textContentColor = TeksSoft,
        title = { Text(proker.namaProker, fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(
                Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(target, { target = it }, label = { Text("Target / capaian") }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        mulai, { mulai = it }, label = { Text("Mulai") }, placeholder = { Text("dd/mm/yyyy") },
                        singleLine = true, modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        selesai, { selesai = it }, label = { Text("Selesai") }, placeholder = { Text("dd/mm/yyyy") },
                        singleLine = true, modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(pic, { pic = it }, label = { Text("PIC (penanggung jawab)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        anggaran, { anggaran = it.filter(Char::isDigit) }, label = { Text("Anggaran (Rp)") },
                        singleLine = true, modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        realisasi, { realisasi = it.filter(Char::isDigit) }, label = { Text("Realisasi (Rp)") },
                        singleLine = true, modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(evaluasi, { evaluasi = it }, label = { Text("Evaluasi / hasil") }, modifier = Modifier.fillMaxWidth())

                Text("Bukti kegiatan (${bukti.size})", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TeksUtama)
                if (bukti.isNotEmpty()) {
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        bukti.forEach { b -> BuktiKecil(b, onHapus = { viewModel.hapusBukti(b, proker.namaProker) }) }
                    }
                }
                OutlinedTextField(
                    catatanBukti, { catatanBukti = it }, label = { Text("Catatan foto berikutnya (opsional)") },
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = bukaKamera, modifier = Modifier.weight(1f)) { Text("Kamera") }
                    OutlinedButton(onClick = bukaGaleri, modifier = Modifier.weight(1f)) { Text("Galeri") }
                }
                Text(
                    "Foto dikompres kecil dan disimpan di cloud desa, jadi terlihat di semua HP satu desa.",
                    fontSize = 11.sp, color = TeksRedup
                )
                galat?.let { Text(it, color = Merah, fontSize = 12.sp) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val m = bacaTanggal(mulai)
                val s = bacaTanggal(selesai)
                when {
                    m == null || s == null -> galat = "Tanggal harus berformat dd/mm/yyyy, contoh 17/01/2027."
                    m > 0L && s > 0L && s < m -> galat = "Tanggal selesai tidak boleh sebelum tanggal mulai."
                    else -> {
                        viewModel.ubahRencana(
                            proker, target, m, s, anggaran.toLongOrNull() ?: 0L, pic,
                            realisasi.toLongOrNull() ?: 0L, evaluasi
                        )
                        onTutup()
                    }
                }
            }) { Text("Simpan", color = VioletTerang, fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onTutup) { Text("Tutup", color = TeksRedup) } }
    )
}

@Composable
private fun BuktiKecil(b: BuktiDesa, onHapus: () -> Unit) {
    val gambar = remember(b.id, b.fotoBase64) { FotoUtil.dekode(b.fotoBase64) }
    Column(Modifier.width(84.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        if (gambar != null) {
            Image(
                bitmap = gambar.asImageBitmap(), contentDescription = "Bukti kegiatan",
                modifier = Modifier.size(84.dp).clip(RoundedCornerShape(16.dp)), contentScale = ContentScale.Crop
            )
        } else {
            Box(Modifier.size(84.dp).clip(RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                Text("Tanpa foto", fontSize = 10.sp, color = TeksRedup)
            }
        }
        if (b.catatan.isNotBlank()) Text(b.catatan, fontSize = 10.sp, maxLines = 1, color = TeksSoft)
        TextButton(onClick = onHapus, contentPadding = PaddingValues(0.dp)) { Text("Hapus", fontSize = 11.sp, color = Merah) }
    }
}

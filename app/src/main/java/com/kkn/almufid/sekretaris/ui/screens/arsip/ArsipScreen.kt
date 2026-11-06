package com.kkn.almufid.sekretaris.ui.screens.arsip

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image as GambarPratinjau
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import com.kkn.almufid.sekretaris.ui.permissions.rememberPemicuGaleri
import com.kkn.almufid.sekretaris.ui.permissions.rememberPemicuKamera
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
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ArsipScreen(viewModel: NotaViewModel) {
    val daftar by viewModel.daftarNota.collectAsState()
    val total by viewModel.totalPengeluaran.collectAsState()
    var dialogTerbuka by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Fixed(2),
            contentPadding = PaddingValues(18.dp, 4.dp, 18.dp, 110.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalItemSpacing = 12.dp
        ) {
            item(span = StaggeredGridItemSpan.FullLine) { KartuTotal(total, daftar.size) }
            if (daftar.isEmpty()) {
                item(span = StaggeredGridItemSpan.FullLine) {
                    KosongState("Belum ada nota tersimpan. Tekan + lalu ambil foto nota.")
                }
            }
            itemsIndexed(daftar, key = { _, n -> n.id }) { i, nota ->
                TileNota(nota, if (i % 3 == 0) 0.8f else 1.1f, Modifier.munculBertahap(i)) {
                    viewModel.hapusNota(nota)
                }
            }
        }
        FabGradien(Icons.Default.Add, "Tambah nota", Modifier.align(Alignment.BottomEnd).padding(20.dp)) {
            dialogTerbuka = true
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
private fun KartuTotal(total: Long, jumlah: Int) {
    val bentuk = RoundedCornerShape(26.dp)
    Box(
        Modifier.fillMaxWidth().shadow(20.dp, bentuk, ambientColor = Pink, spotColor = Pink)
            .clip(bentuk).background(GradUtama).padding(20.dp)
    ) {
        Box(
            Modifier.align(Alignment.TopEnd).offset(40.dp, (-40).dp).size(120.dp)
                .clip(CircleShape).background(Color.White.copy(alpha = 0.15f))
        )
        Column {
            Text("Total pengeluaran", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text(formatRupiah(total), fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            Text("$jumlah nota tersimpan", fontSize = 12.sp, color = Color.White.copy(alpha = 0.9f))
        }
    }
}

@Composable
private fun TileNota(nota: NotaEntry, rasio: Float, modifier: Modifier, onHapus: () -> Unit) {
    val bentuk = RoundedCornerShape(20.dp)
    Box(
        modifier.fillMaxWidth().aspectRatio(rasio).shadow(10.dp, bentuk).clip(bentuk)
            .background(Brush.linearGradient(listOf(Surface2, Violet)))
    ) {
        if (nota.fotoPath != null && File(nota.fotoPath).exists()) {
            AsyncImage(
                model = File(nota.fotoPath), contentDescription = "Foto nota",
                contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()
            )
        } else {
            Text("Tanpa foto", Modifier.align(Alignment.Center), color = TeksSoft, fontSize = 12.sp)
        }
        Column(
            Modifier.align(Alignment.BottomStart).padding(10.dp).clip(RoundedCornerShape(12.dp))
                .background(Ground.copy(alpha = 0.82f)).padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text(formatRupiah(nota.nominal), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
            Text(nota.keterangan, color = TeksSoft, fontSize = 11.sp, maxLines = 1)
        }
        IconButton(onClick = onHapus, modifier = Modifier.align(Alignment.TopEnd)) {
            Icon(Icons.Default.Delete, "Hapus", tint = Color.White)
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
    var izinDitolakPesan by remember { mutableStateOf<String?>(null) }

    val context = androidx.compose.ui.platform.LocalContext.current
    val peluncurKamera = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap -> foto = bitmap }
    val peluncurGaleri = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            foto = try {
                context.contentResolver.openInputStream(it)?.use { input ->
                    android.graphics.BitmapFactory.decodeStream(input)
                }
            } catch (e: Exception) {
                null
            }
        }
    }
    val bukaKamera = rememberPemicuKamera(
        onDiizinkan = { peluncurKamera.launch(null) },
        onDitolak = { izinDitolakPesan = "Izin kamera ditolak. Aktifkan lewat Pengaturan HP > Aplikasi > Sekretaris KKN > Izin." }
    )
    val bukaGaleri = rememberPemicuGaleri(
        onDiizinkan = { peluncurGaleri.launch("image/*") },
        onDitolak = { izinDitolakPesan = "Izin galeri ditolak. Aktifkan lewat Pengaturan HP > Aplikasi > Sekretaris KKN > Izin." }
    )

    AlertDialog(
        onDismissRequest = onTutup,
        title = { Text("Tambah Nota/Kwitansi") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = bukaKamera, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(if (foto == null) "Kamera" else "Ganti")
                    }
                    OutlinedButton(onClick = bukaGaleri, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Image, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Galeri")
                    }
                }
                izinDitolakPesan?.let {
                    Text(it, color = Merah, fontSize = 11.sp)
                }
                foto?.let {
                    GambarPratinjau(
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

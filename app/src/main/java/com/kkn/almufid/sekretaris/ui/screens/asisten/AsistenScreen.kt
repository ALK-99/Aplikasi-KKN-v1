package com.kkn.almufid.sekretaris.ui.screens.asisten

import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kkn.almufid.sekretaris.data.auth.ProfilPengguna
import com.kkn.almufid.sekretaris.data.cloud.DesaRepository
import com.kkn.almufid.sekretaris.data.cloud.PremiumRepository
import com.kkn.almufid.sekretaris.data.cloud.StatusPremium
import com.kkn.almufid.sekretaris.ui.screens.premium.KartuPremium
import com.kkn.almufid.sekretaris.data.entity.JenisSurat
import com.kkn.almufid.sekretaris.ui.auth.DialogInfo
import com.kkn.almufid.sekretaris.ui.components.KartuGelap
import com.kkn.almufid.sekretaris.ui.components.PilChip
import com.kkn.almufid.sekretaris.ui.screens.koordinasi.KoordinasiViewModel
import com.kkn.almufid.sekretaris.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

private class ChatItem(val saya: Boolean, val teks: String, val aksi: List<AksiAsisten> = emptyList()) {
    /** Per aksi: 0 = menunggu konfirmasi, 1 = tersimpan, 2 = dibatalkan */
    val status = mutableStateListOf<Int>().apply { repeat(aksi.size) { add(0) } }
}

private val CONTOH = listOf(
    "Catat kas masuk 50 ribu iuran anggota", "Tugas buat poster untuk Fahmi besok",
    "Berapa saldo kas?", "Tugas apa yang belum selesai?", "Ringkasan hari ini"
)

private val TIPE_FILE = arrayOf(
    "application/pdf",
    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
    "text/csv", "text/plain", "application/octet-stream"
)

/** Bola asisten yang melayang di semua halaman. Bisa digeser, menempel ke tepi, dan bisa dikecilkan. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AsistenOverlay(vm: KoordinasiViewModel, desa: DesaRepository) {
    val context = LocalContext.current
    var buka by remember { mutableStateOf(false) }
    var mini by remember { mutableStateOf(AsistenStore.mini(context)) }
    var kiri by remember { mutableStateOf(AsistenStore.kiri(context)) }
    var yFrac by remember { mutableFloatStateOf(AsistenStore.yFrac(context)) }
    var seretX by remember { mutableFloatStateOf(Float.NaN) }
    var seretY by remember { mutableFloatStateOf(Float.NaN) }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val rapat = with(LocalDensity.current) { 6.dp.toPx() }
        val lebarBola = with(LocalDensity.current) { (if (mini) 16.dp else 50.dp).toPx() }
        val tinggiBola = with(LocalDensity.current) { (if (mini) 46.dp else 50.dp).toPx() }
        val lebar = constraints.maxWidth.toFloat()
        val tinggi = constraints.maxHeight.toFloat()
        val batasAtas = with(LocalDensity.current) { 90.dp.toPx() }
        val batasBawah = tinggi - with(LocalDensity.current) { 190.dp.toPx() }
        fun xDiam() = if (kiri) rapat else lebar - lebarBola - rapat
        fun yDiam() = (yFrac * tinggi).coerceIn(batasAtas, batasBawah.coerceAtLeast(batasAtas))
        val x = if (seretX.isNaN()) xDiam() else seretX
        val y = if (seretY.isNaN()) yDiam() else seretY

        if (!buka) {
            Box(
                Modifier
                    .offset { IntOffset(x.roundToInt(), y.roundToInt()) }
                    .size(with(LocalDensity.current) { lebarBola.toDp() }, with(LocalDensity.current) { tinggiBola.toDp() })
                    .alpha(if (mini) 0.55f else 0.9f)
                    .clip(if (mini) RoundedCornerShape(8.dp) else CircleShape)
                    .background(GradUtama)
                    .pointerInput(mini) {
                        detectTapGestures(onTap = { if (mini) { mini = false; AsistenStore.simpanMini(context, false) } else buka = true })
                    }
                    .pointerInput(mini) {
                        detectDragGestures(
                            onDragStart = { seretX = xDiam(); seretY = yDiam() },
                            onDrag = { change, d: Offset -> change.consume(); seretX += d.x; seretY += d.y },
                            onDragEnd = {
                                kiri = seretX + lebarBola / 2 < lebar / 2
                                yFrac = (seretY / tinggi).coerceIn(0.05f, 0.95f)
                                AsistenStore.simpanBola(context, kiri, yFrac)
                                seretX = Float.NaN; seretY = Float.NaN
                            },
                            onDragCancel = { seretX = Float.NaN; seretY = Float.NaN }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                if (!mini) Icon(Icons.Default.SmartToy, "Asisten KKN", tint = Color.White, modifier = Modifier.size(26.dp))
            }
        }
    }

    if (buka) {
        ModalBottomSheet(
            onDismissRequest = { buka = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Ground
        ) {
            AsistenPanel(vm, desa, onKecil = { mini = true; AsistenStore.simpanMini(context, true); buka = false })
        }
    }
}

@Composable
private fun AsistenPanel(vm: KoordinasiViewModel, desa: DesaRepository, onKecil: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val kas by vm.kas.collectAsState()
    val tugas by vm.tugas.collectAsState()
    val admin by vm.admin.collectAsState()
    val akun by vm.anggota.collectAsState()
    val pesanVm by vm.pesan.collectAsState()
    val proker by remember { desa.alirProker() }.collectAsState(emptyList())
    val nota by remember { desa.alirNota() }.collectAsState(emptyList())
    val surat by remember { desa.alirSurat() }.collectAsState(emptyList())
    val absensi by remember { desa.alirAbsensi() }.collectAsState(emptyList())
    val anggota by remember { desa.alirAnggota() }.collectAsState(emptyList())

    val chat = remember {
        mutableStateListOf<ChatItem>().apply {
            add(ChatItem(false, "Halo, saya asisten KKN. Saya bisa mencatat data, menjawab soal data desa, membaca PDF/Word/Excel, dan mengingat kesulitanmu. Setiap perubahan minta konfirmasi dulu."))
            AsistenStore.riwayat(context).forEach { add(ChatItem(it.saya, it.teks)) }
        }
    }
    var teks by remember { mutableStateOf("") }
    var sibuk by remember { mutableStateOf(false) }
    var bagian by remember { mutableIntStateOf(0) } // 0 tutup, 1 ingatan, 2 pengaturan
    var kunci by remember { mutableStateOf("") }
    var model by remember { mutableStateOf(MODEL_BAWAAN) }
    var kunciOr by remember { mutableStateOf("") }
    var premium by remember { mutableStateOf<StatusPremium?>(null) }
    var muatUlang by remember { mutableIntStateOf(0) }
    LaunchedEffect(muatUlang) {
        val repoP = PremiumRepository()
        val st = repoP.status(); premium = st
        if (st.aktif) repoP.ambilKunci()?.let { kunci = it.gemini; kunciOr = it.openrouter; if (it.model.isNotBlank()) model = it.model }
        else { kunci = ""; kunciOr = "" }
    }
    var ingatan by remember { mutableStateOf(AsistenStore.ingatan(context)) }
    var fileUri by remember { mutableStateOf<Uri?>(null) }
    var fileNama by remember { mutableStateOf("") }
    val daftar = rememberLazyListState()

    val pilihFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            fileUri = uri
            fileNama = context.contentResolver.query(uri, null, null, null, null)?.use { c ->
                val i = c.getColumnIndex(OpenableColumns.DISPLAY_NAME); if (c.moveToFirst() && i >= 0) c.getString(i) else null
            } ?: "file"
        }
    }

    pesanVm?.let { DialogInfo(judul = "Info", pesan = it, onTutup = vm::tutupPesan) }
    LaunchedEffect(chat.size) { daftar.animateScrollToItem((chat.size - 1).coerceAtLeast(0)) }

    fun konteks() = KonteksAsisten(kas, tugas, proker, nota, surat, absensi, anggota, akun.map { it.namaLengkap }, "", AsistenStore.ingatan(context))

    fun kirim(t0: String) {
        val uri = fileUri
        if ((t0.isBlank() && uri == null) || sibuk) return
        val q = t0.trim().ifEmpty { "Screening file ini: ringkas isinya, tandai kejanggalan, dan usulkan data yang bisa dimasukkan." }
        val nama = fileNama
        teks = ""; fileUri = null; fileNama = ""
        val tampil = (if (uri != null) "📎 $nama\n" else "") + q
        chat.add(ChatItem(true, tampil)); AsistenStore.tambahRiwayat(context, true, tampil); sibuk = true
        scope.launch {
            val k = konteks()
            val riw = AsistenStore.riwayat(context).dropLast(1)
            val hasil: HasilAsisten = try {
                var lamp: Lampiran? = null
                if (uri != null) {
                    val bytes = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: throw IllegalStateException("File tidak bisa dibaca")
                    }
                    if (bytes.size > 12_000_000) throw IllegalStateException("File terlalu besar (maks 12 MB)")
                    if (nama.lowercase().endsWith(".pdf")) {
                        if (kunci.isBlank()) throw IllegalStateException("Membaca PDF butuh Premium. Tukar kode lewat tombol pengaturan.")
                        lamp = Lampiran(nama, null, Base64.encodeToString(bytes, Base64.NO_WRAP))
                    } else {
                        if (kunci.isBlank() && kunciOr.isBlank()) throw IllegalStateException("Membaca file butuh Premium. Tukar kode lewat tombol pengaturan.")
                        lamp = Lampiran(nama, withContext(Dispatchers.Default) { BacaFile.teks(nama, bytes) }, null)
                    }
                }
                if (kunci.isNotBlank() || kunciOr.isNotBlank()) {
                    try { withContext(Dispatchers.IO) { AsistenParser.tanyaAI(q, kunci, model, kunciOr, k, riw, lamp) } }
                    catch (e: Exception) {
                        if (lamp != null) throw e
                        val h = AsistenParser.pahami(q, k)
                        HasilAsisten("(Mode pintar gagal: ${e.message?.take(120)}. Saya jawab dengan cara biasa.)\n" + h.balasan, h.aksi, h.ingat)
                    }
                } else AsistenParser.pahami(q, k)
            } catch (e: Exception) {
                HasilAsisten("Maaf, ${e.message ?: "terjadi kesalahan"}")
            }
            if (hasil.ingat.isNotEmpty()) { AsistenStore.tambahIngatan(context, hasil.ingat); ingatan = AsistenStore.ingatan(context) }
            chat.add(ChatItem(false, hasil.balasan, hasil.aksi))
            AsistenStore.tambahRiwayat(context, false, hasil.balasan + if (hasil.aksi.isNotEmpty()) "\n[usulan: " + hasil.aksi.joinToString("; ") { AsistenParser.ringkas(it) } + "]" else "")
            sibuk = false
        }
    }

    fun jalankan(a: AksiAsisten): Boolean {
        when (a) {
            is AksiAsisten.CatatKas -> vm.tambahKas(a.masuk, a.jumlah.toString(), a.kategori, a.keterangan)
            is AksiAsisten.BuatTugas -> {
                val cocok = akun.firstOrNull { a.untuk.isNotBlank() && it.namaLengkap.lowercase().contains(a.untuk.lowercase().split(" ")[0]) }
                val untuk = cocok ?: ProfilPengguna(uid = vm.uidSaya, namaLengkap = a.untuk.ifBlank { "Saya" })
                vm.tambahTugas(a.judul, a.deskripsi, untuk, if (a.tenggat > 0) a.tenggat else AsistenParser.tenggatDari("besok"))
            }
            is AksiAsisten.BuatPengumuman -> {
                if (!admin) { chat.add(ChatItem(false, "Pengumuman hanya bisa dibuat admin.")); return false }
                vm.tambahPengumuman(a.judul, a.isi)
            }
            is AksiAsisten.TambahBarang -> scope.launch { desa.tambahBarang(a.nama, a.jumlah, a.kondisi, "") }
            is AksiAsisten.CatatSurat -> scope.launch { desa.tambahSurat(if (a.masuk) JenisSurat.MASUK else JenisSurat.KELUAR, a.nomor, a.perihal, "") }
        }
        return true
    }

    Column(Modifier.fillMaxWidth().fillMaxHeight(0.88f).imePadding().padding(horizontal = 14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Asisten KKN", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, modifier = Modifier.weight(1f))
            IconButton(onClick = { bagian = if (bagian == 1) 0 else 1; ingatan = AsistenStore.ingatan(context) }) { Icon(Icons.Default.Psychology, "Ingatan") }
            IconButton(onClick = { bagian = if (bagian == 2) 0 else 2 }) { Icon(Icons.Default.Settings, "Pengaturan") }
            IconButton(onClick = onKecil) { Icon(Icons.Default.VisibilityOff, "Kecilkan bola") }
        }
        if (bagian == 1) {
            KartuGelap(Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Ingatan asisten", fontWeight = FontWeight.Bold)
                Text("Hal yang saya ingat tentangmu supaya bantuan makin pas. Disimpan hanya di HP ini.", fontSize = 12.sp, color = TeksRedup)
                if (ingatan.isEmpty()) Text("Belum ada.", fontSize = 13.sp)
                ingatan.forEachIndexed { i, t ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("• $t", fontSize = 13.sp, modifier = Modifier.weight(1f))
                        IconButton(onClick = { AsistenStore.hapusIngatan(context, i); ingatan = AsistenStore.ingatan(context) }) { Icon(Icons.Default.Close, "Hapus") }
                    }
                }
                OutlinedButton(onClick = {
                    AsistenStore.hapusRiwayat(context); chat.clear()
                    chat.add(ChatItem(false, "Riwayat obrolan dihapus."))
                }) { Text("Hapus riwayat obrolan") }
            }
        }
        if (bagian == 2) {
            KartuPremium(Modifier.padding(bottom = 8.dp), onBerubah = { muatUlang++ })
        }
        LazyColumn(Modifier.weight(1f), state = daftar, verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(vertical = 8.dp)) {
            items(chat) { c ->
                Box(Modifier.fillMaxWidth(), contentAlignment = if (c.saya) Alignment.CenterEnd else Alignment.CenterStart) {
                    Column(
                        Modifier.widthIn(max = 320.dp).clip(RoundedCornerShape(16.dp))
                            .background(if (c.saya) Violet else Surface1).padding(12.dp)
                    ) {
                        Text(c.teks, color = if (c.saya) Color.White else TeksUtama, fontSize = 14.sp)
                        c.aksi.forEachIndexed { i, a ->
                            Spacer(Modifier.height(8.dp))
                            Text(AsistenParser.ringkas(a), fontWeight = FontWeight.Bold, color = TeksUtama, fontSize = 14.sp)
                            when (c.status[i]) {
                                1 -> Text("Tersimpan.", color = TeksRedup, fontSize = 13.sp)
                                2 -> Text("Dibatalkan.", color = TeksRedup, fontSize = 13.sp)
                                else -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(onClick = { c.status[i] = if (jalankan(a)) 1 else 2 }) { Text("Simpan") }
                                    OutlinedButton(onClick = { c.status[i] = 2 }) { Text("Batal") }
                                }
                            }
                        }
                        if (c.aksi.size > 1 && c.status.any { it == 0 }) {
                            Spacer(Modifier.height(8.dp))
                            Button(onClick = { c.aksi.forEachIndexed { i, a -> if (c.status[i] == 0) c.status[i] = if (jalankan(a)) 1 else 2 } }) { Text("Simpan semua (${c.status.count { it == 0 }})") }
                        }
                    }
                }
            }
            if (sibuk) item { Text("Berpikir…", color = TeksRedup, fontSize = 13.sp) }
        }
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CONTOH.forEach { PilChip(it, false, { kirim(it) }) }
        }
        if (fileNama.isNotEmpty()) Text("📎 $fileNama (ketuk kirim)", fontSize = 12.sp, color = TeksRedup, modifier = Modifier.padding(bottom = 4.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(bottom = 10.dp)) {
            IconButton(onClick = { pilihFile.launch(TIPE_FILE) }) { Icon(Icons.Default.AttachFile, "Unggah PDF, Word, Excel") }
            OutlinedTextField(teks, { teks = it }, placeholder = { Text("Tulis atau tanya apa saja…") }, modifier = Modifier.weight(1f), maxLines = 3)
            FilledIconButton(onClick = { kirim(teks) }, enabled = !sibuk) { Icon(Icons.AutoMirrored.Filled.Send, "Kirim") }
        }
    }
}

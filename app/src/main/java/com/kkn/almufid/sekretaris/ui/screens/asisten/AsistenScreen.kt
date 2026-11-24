package com.kkn.almufid.sekretaris.ui.screens.asisten

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kkn.almufid.sekretaris.data.auth.ProfilPengguna
import com.kkn.almufid.sekretaris.data.cloud.DesaRepository
import com.kkn.almufid.sekretaris.data.entity.JenisSurat
import com.kkn.almufid.sekretaris.ui.auth.DialogInfo
import com.kkn.almufid.sekretaris.ui.components.KartuGelap
import com.kkn.almufid.sekretaris.ui.components.PilChip
import com.kkn.almufid.sekretaris.ui.screens.koordinasi.KoordinasiViewModel
import com.kkn.almufid.sekretaris.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private class ChatItem(val saya: Boolean, val teks: String, val aksi: AksiAsisten? = null) {
    /** 0 = menunggu konfirmasi, 1 = tersimpan, 2 = dibatalkan */
    var status by mutableStateOf(0)
}

private val CONTOH = listOf(
    "Catat kas masuk 50 ribu iuran anggota", "Kas keluar 25rb beli spidol", "Tugas buat poster untuk Fahmi besok",
    "Berapa saldo kas?", "Tugas apa yang belum selesai?", "Ringkasan hari ini"
)

private fun prefs(c: Context) = c.getSharedPreferences("asisten_prefs", Context.MODE_PRIVATE)

@Composable
fun AsistenScreen(vm: KoordinasiViewModel, desa: DesaRepository) {
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

    val chat = remember { mutableStateListOf(ChatItem(false, "Halo, saya asisten KKN. Saya bisa mencatat kas, membuat tugas dan pengumuman, menambah inventaris, mencatat surat, serta menjawab soal saldo, tugas, proker, dan absensi. Semua perubahan minta konfirmasi dulu.")) }
    var teks by remember { mutableStateOf("") }
    var sibuk by remember { mutableStateOf(false) }
    var pengaturan by remember { mutableStateOf(false) }
    var kunci by remember { mutableStateOf(prefs(context).getString("gemini", "") ?: "") }
    var model by remember { mutableStateOf(prefs(context).getString("model", "gemini-flash-latest") ?: "gemini-flash-latest") }
    val daftar = rememberLazyListState()

    pesanVm?.let { DialogInfo(judul = "Info", pesan = it, onTutup = vm::tutupPesan) }
    LaunchedEffect(chat.size) { daftar.animateScrollToItem((chat.size - 1).coerceAtLeast(0)) }

    fun konteks() = KonteksAsisten(kas, tugas, proker, nota, surat, absensi, anggota, akun.map { it.namaLengkap }, "")

    fun kirim(t: String) {
        val q = t.trim(); if (q.isEmpty() || sibuk) return
        teks = ""; chat.add(ChatItem(true, q)); sibuk = true
        scope.launch {
            val k = konteks()
            val h = if (kunci.isNotBlank()) {
                try { withContext(Dispatchers.IO) { AsistenParser.tanyaGemini(q, kunci, model, k) } }
                catch (e: Exception) {
                    chat.add(ChatItem(false, "Mode pintar gagal (${e.message?.take(120)}). Saya coba cara biasa."))
                    AsistenParser.pahami(q, k)
                }
            } else AsistenParser.pahami(q, k)
            chat.add(ChatItem(false, h.balasan, h.aksi)); sibuk = false
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

    Column(Modifier.fillMaxSize().imePadding().padding(horizontal = 14.dp)) {
        LazyColumn(Modifier.weight(1f), state = daftar, verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(vertical = 8.dp)) {
            items(chat) { c ->
                Box(Modifier.fillMaxWidth(), contentAlignment = if (c.saya) Alignment.CenterEnd else Alignment.CenterStart) {
                    Column(
                        Modifier.widthIn(max = 320.dp).clip(RoundedCornerShape(16.dp))
                            .background(if (c.saya) Violet else Surface1).padding(12.dp)
                    ) {
                        Text(c.teks, color = if (c.saya) Color.White else TeksUtama, fontSize = 14.sp)
                        val a = c.aksi
                        if (a != null) {
                            Spacer(Modifier.height(8.dp))
                            Text(AsistenParser.ringkas(a), fontWeight = FontWeight.Bold, color = TeksUtama, fontSize = 14.sp)
                            Spacer(Modifier.height(8.dp))
                            when (c.status) {
                                1 -> Text("Tersimpan.", color = TeksRedup, fontSize = 13.sp)
                                2 -> Text("Dibatalkan.", color = TeksRedup, fontSize = 13.sp)
                                else -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(onClick = { c.status = if (jalankan(a)) 1 else 2 }) { Text("Simpan") }
                                    OutlinedButton(onClick = { c.status = 2 }) { Text("Batal") }
                                }
                            }
                        }
                    }
                }
            }
            if (sibuk) item { Text("Berpikir…", color = TeksRedup, fontSize = 13.sp) }
        }
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CONTOH.forEach { PilChip(it, false, { kirim(it) }) }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(teks, { teks = it }, placeholder = { Text("Tulis perintah…") }, modifier = Modifier.weight(1f), maxLines = 3)
            FilledIconButton(onClick = { kirim(teks) }, enabled = !sibuk) { Icon(Icons.AutoMirrored.Filled.Send, "Kirim") }
        }
        TextButton(onClick = { pengaturan = !pengaturan }) {
            Text(if (kunci.isNotBlank()) "Mode pintar (gratis): aktif" else "Mode pintar (gratis): belum aktif")
        }
        if (pengaturan) {
            KartuGelap(Modifier.fillMaxWidth().padding(bottom = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Tanpa ini asisten tetap memahami perintah umum, offline dan gratis. Mode pintar memakai Google Gemini dengan kunci API gratis milikmu: buka aistudio.google.com/apikey, masuk akun Google, ketuk Create API key, salin, lalu tempel di bawah. Kunci hanya disimpan di HP ini. Saat dipakai, pertanyaan dan ringkasan data desa (saldo, tugas, proker, nama anggota) dikirim ke Google. Tidak ada tarif, tapi ada batas pemakaian harian.",
                    fontSize = 12.sp, color = TeksRedup
                )
                OutlinedTextField(kunci, { kunci = it.trim() }, label = { Text("Kunci API Gemini") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(model, { model = it.trim() }, label = { Text("Nama model") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Button(onClick = {
                    prefs(context).edit().putString("gemini", kunci).putString("model", model.ifBlank { "gemini-flash-latest" }).apply()
                    pengaturan = false
                }) { Text("Simpan") }
            }
        }
    }
}

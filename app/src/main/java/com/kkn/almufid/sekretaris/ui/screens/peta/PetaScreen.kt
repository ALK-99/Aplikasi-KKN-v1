package com.kkn.almufid.sekretaris.ui.screens.peta

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.kkn.almufid.sekretaris.ui.components.KartuGelap
import com.kkn.almufid.sekretaris.ui.components.PilChip
import com.kkn.almufid.sekretaris.ui.theme.*
import kotlinx.coroutines.launch
import java.util.Locale

private val PENCARIAN_CEPAT = listOf(
    "Masjid", "Puskesmas", "Rumah sakit", "Apotek",
    "SPBU", "ATM", "Warung makan", "Minimarket",
)

private fun bukaIntent(context: Context, uri: String, web: String) {
    val geo = Intent(Intent.ACTION_VIEW, Uri.parse(uri)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try { context.startActivity(geo) } catch (e: Exception) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(web)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: Exception) { /* tidak ada aplikasi yang bisa membuka peta */ }
    }
}

/** Cari teks bebas di peta (riwayat ikut tercatat). */
private fun bukaPeta(context: Context, kueri: String) {
    val q = kueri.trim()
    if (q.isEmpty()) return
    LokasiUtil.tambahRiwayat(context, q)
    bukaIntent(context, "geo:0,0?q=" + Uri.encode(q), "https://www.google.com/maps/search/?api=1&query=" + Uri.encode(q))
}

/**
 * Cari kategori di sekitar sebuah titik. Bila koordinat posko diketahui, peta DIPUSATKAN di koordinat itu
 * (bukan di GPS HP). Bila belum, memakai nama posko sebagai teks pencarian.
 */
private fun bukaSekitar(context: Context, kategori: String, titik: TitikPosko?, lat: Double?, lng: Double?) {
    val la = lat ?: titik?.lat
    val lo = lng ?: titik?.lng
    if (la != null && lo != null) {
        bukaIntent(
            context, "geo:$la,$lo?q=" + Uri.encode(kategori),
            "https://www.google.com/maps/search/" + Uri.encode(kategori) + "/@$la,$lo,15z"
        )
    } else if (titik != null && titik.nama.isNotBlank()) {
        val q = "$kategori dekat ${titik.nama}"
        bukaIntent(context, "geo:0,0?q=" + Uri.encode(q), "https://www.google.com/maps/search/?api=1&query=" + Uri.encode(q))
    } else bukaPeta(context, kategori)
}

private fun salin(context: Context, teks: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("lokasi", teks))
}

@Composable
fun PetaScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var cari by remember { mutableStateOf("") }
    var riwayat by remember { mutableStateOf(LokasiUtil.bacaRiwayat(context)) }
    var posko by remember { mutableStateOf(LokasiUtil.bacaPosko(context)) }
    var teksPosko by remember { mutableStateOf(posko.nama) }
    var infoPosko by remember { mutableStateOf("") }
    var sibuk by remember { mutableStateOf(false) }
    var pakaiSaya by remember { mutableStateOf(false) }

    // "Di mana saya sekarang"
    var posisi by remember { mutableStateOf<Location?>(null) }
    var alamat by remember { mutableStateOf<String?>(null) }
    var statusPosisi by remember { mutableStateOf("") }
    var mencari by remember { mutableStateOf(false) }

    fun cariPosisi(kunci: Boolean) {
        scope.launch {
            mencari = true; statusPosisi = "Mencari sinyal GPS… (di tempat terbuka lebih cepat)"
            alamat = null
            val l = LokasiUtil.posisiSekarang(context)
            mencari = false
            if (l == null) {
                posisi = null
                statusPosisi = "Belum dapat posisi. Nyalakan GPS/Lokasi di pengaturan HP, lalu coba di tempat terbuka."
                return@launch
            }
            posisi = l
            statusPosisi = ""
            val a = LokasiUtil.cariAlamat(context, l.latitude, l.longitude)
            if (a != null) { alamat = a; LokasiUtil.simpanAlamatTerakhir(context, l.latitude, l.longitude, a) }
            if (kunci) {
                val nama = a ?: "Titik GPS ${String.format(Locale.US, "%.5f, %.5f", l.latitude, l.longitude)}"
                val t = TitikPosko(nama, l.latitude, l.longitude)
                LokasiUtil.simpanPosko(context, t); posko = t; teksPosko = nama
                infoPosko = "Posko dikunci ke posisi GPS saat ini."
            }
        }
    }

    // Hasil "Cari di sekitar" (diurutkan menurut jarak)
    var hasil by remember { mutableStateOf<List<Tempat>>(emptyList()) }
    var judulHasil by remember { mutableStateOf("") }
    var infoHasil by remember { mutableStateOf("") }
    var memuat by remember { mutableStateOf(false) }

    fun cariDekat(kategori: String, pakaiGps: Boolean) {
        scope.launch {
            memuat = true; hasil = emptyList(); judulHasil = ""; infoHasil = "Mencari $kategori terdekat…"
            var la: Double? = null; var lo: Double? = null; var asal = ""
            if (pakaiGps) {
                val l = LokasiUtil.posisiSekarang(context)
                if (l != null) { la = l.latitude; lo = l.longitude; asal = "posisi GPS kamu" }
            } else {
                la = posko.lat; lo = posko.lng; asal = "posko (${posko.nama})"
                if (la == null && posko.nama.isNotBlank()) {
                    val k = LokasiUtil.cariKoordinat(context, posko.nama)
                    if (k != null) { la = k.first; lo = k.second; posko = TitikPosko(posko.nama, k.first, k.second); LokasiUtil.simpanPosko(context, posko) }
                }
            }
            if (la == null || lo == null) {
                infoHasil = if (pakaiGps) "Posisi GPS belum didapat. Izinkan Lokasi lewat tombol \"Tunjukkan posisi saya\" di atas, lalu coba lagi."
                else "Titik posko belum ada. Simpan & kunci lokasi posko dulu (butuh internet sekali)."
                memuat = false; return@launch
            }
            val d = LokasiUtil.cariTerdekat(kategori, la, lo)
            memuat = false
            when {
                d == null -> infoHasil = "Tidak bisa mengambil data. Periksa internet lalu coba lagi."
                d.isEmpty() -> infoHasil = "Tidak ada $kategori dalam 30 km dari $asal."
                else -> { hasil = d; judulHasil = "$kategori terdekat dari $asal"; infoHasil = "" }
            }
        }
    }

    var kunciSetelahIzin by remember { mutableStateOf(false) }
    val peminta = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { hasil ->
        if (hasil.values.any { it }) cariPosisi(kunciSetelahIzin)
        else statusPosisi = "Izin lokasi ditolak. Izinkan lewat Pengaturan HP > Aplikasi > izin Lokasi."
    }
    fun mintaPosisi(kunci: Boolean) {
        kunciSetelahIzin = kunci
        val sudah = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (sudah) cariPosisi(kunci)
        else peminta.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(start = 18.dp, end = 18.dp, top = 6.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ---------- Di mana saya sekarang ----------
        KartuGelap(Modifier.fillMaxWidth()) {
            Text("Di mana saya sekarang?", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
            Text("Memakai GPS HP, jadi tetap bisa tanpa sinyal internet. Cocok untuk keadaan darurat.", fontSize = 11.sp, color = TeksRedup)
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = { mintaPosisi(false) }, enabled = !mencari, modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Violet)
            ) { Text(if (mencari) "Mencari…" else "Tunjukkan posisi saya") }
            if (statusPosisi.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(statusPosisi, fontSize = 12.sp, color = TeksRedup)
            }
            posisi?.let { l ->
                val lat = l.latitude; val lng = l.longitude
                val koord = String.format(Locale.US, "%.6f, %.6f", lat, lng)
                val umur = (System.currentTimeMillis() - l.time) / 60000
                val patokan = buildList {
                    if (posko.lat != null && posko.lng != null) {
                        val m = LokasiUtil.jarakMeter(lat, lng, posko.lat!!, posko.lng!!)
                        add("${LokasiUtil.teksJarak(m)} arah ${LokasiUtil.arah(posko.lat!!, posko.lng!!, lat, lng)} dari posko (${posko.nama})")
                    }
                    LokasiUtil.bacaAlamatTerakhir(context)?.let { (a, b, n) ->
                        if (alamat == null) {
                            val m = LokasiUtil.jarakMeter(lat, lng, a, b)
                            add("${LokasiUtil.teksJarak(m)} arah ${LokasiUtil.arah(a, b, lat, lng)} dari tempat terakhir yang dikenali: $n")
                        }
                    }
                }
                val ringkas = buildString {
                    appendLine("Saya berada di:")
                    appendLine(alamat ?: "(alamat tidak tersedia, tidak ada internet)")
                    patokan.forEach { appendLine("Patokan: $it") }
                    appendLine("Koordinat: $koord")
                    appendLine("Kode Plus: ${LokasiUtil.kodePlus(lat, lng)}")
                    append("Peta: https://maps.google.com/?q=$lat,$lng")
                }
                Spacer(Modifier.height(12.dp))
                Text("Anda berada di", fontSize = 11.sp, color = TeksRedup)
                Text(alamat ?: "Alamat belum tersedia (butuh internet)", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                if (patokan.isNotEmpty()) {
                    Text("Patokan", fontSize = 11.sp, color = TeksRedup)
                    patokan.forEach { Text("• $it", fontSize = 13.sp) }
                    Spacer(Modifier.height(8.dp))
                } else if (alamat == null) {
                    Text("Patokan: simpan lokasi posko saat ada internet supaya jaraknya bisa ditampilkan di sini.", fontSize = 12.sp, color = TeksRedup)
                    Spacer(Modifier.height(8.dp))
                }
                Text("Titik koordinat", fontSize = 11.sp, color = TeksRedup)
                Text(koord, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text(
                    "${LokasiUtil.derajat(lat, true)}  ${LokasiUtil.derajat(lng, false)}",
                    fontSize = 12.sp, color = TeksRedup
                )
                Text("Kode Plus: ${LokasiUtil.kodePlus(lat, lng)}", fontSize = 13.sp)
                Text(
                    "Ketelitian ±${l.accuracy.toInt()} m" + (if (l.hasAltitude()) " · ketinggian ${l.altitude.toInt()} m" else "") +
                        (if (umur > 2) " · data ${umur} menit lalu" else ""),
                    fontSize = 11.sp, color = TeksRedup
                )
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = { salin(context, ringkas); statusPosisi = "Tersalin." }, modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Surface2)
                    ) { Text("Salin") }
                    Button(
                        onClick = {
                            val kirim = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, ringkas)
                            context.startActivity(Intent.createChooser(kirim, "Bagikan lokasi").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        }, modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Violet)
                    ) { Text("Bagikan") }
                }
            }
        }

        // ---------- Cari tempat ----------
        KartuGelap(Modifier.fillMaxWidth()) {
            Text("Cari tempat", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = cari, onValueChange = { cari = it },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                placeholder = { Text("mis. Balai Desa Wonosalam") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { bukaPeta(context, cari); riwayat = LokasiUtil.bacaRiwayat(context) })
            )
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = { bukaPeta(context, cari); riwayat = LokasiUtil.bacaRiwayat(context) },
                enabled = cari.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Violet)
            ) { Text("Buka di peta") }
            if (riwayat.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text("Pencarian terakhir", fontSize = 11.sp, color = TeksRedup)
                Spacer(Modifier.height(6.dp))
                riwayat.chunked(2).forEach { pasang ->
                    Row(Modifier.fillMaxWidth().padding(bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        pasang.forEach { q ->
                            PilChip(q, false, { cari = q; bukaPeta(context, q); riwayat = LokasiUtil.bacaRiwayat(context) }, Modifier.weight(1f))
                        }
                        if (pasang.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
                Button(
                    onClick = { LokasiUtil.hapusRiwayat(context); riwayat = emptyList() },
                    colors = ButtonDefaults.buttonColors(containerColor = Surface2)
                ) { Text("Hapus riwayat", fontSize = 12.sp) }
            }
        }

        // ---------- Cari di sekitar ----------
        KartuGelap(Modifier.fillMaxWidth()) {
            Text("Cari di sekitar", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
            val terkunci = posko.lat != null && posko.lng != null
            Text(
                when {
                    pakaiSaya -> "Dicari dari posisi GPS kamu sekarang, diurutkan dari yang paling dekat."
                    terkunci -> "Dicari dari lokasi posko yang terkunci (${posko.nama}), diurutkan dari yang paling dekat."
                    posko.nama.isNotBlank() -> "Dicari di sekitar \"${posko.nama}\" (belum terkunci ke koordinat, simpan ulang saat online)."
                    else -> "Simpan lokasi posko di bawah supaya pencarian mengikuti posko, bukan GPS."
                },
                fontSize = 11.sp, color = TeksRedup
            )
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PilChip("Sekitar posko", !pakaiSaya, { pakaiSaya = false }, Modifier.weight(1f))
                PilChip("Sekitar saya", pakaiSaya, { pakaiSaya = true }, Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
            PENCARIAN_CEPAT.chunked(2).forEach { pasang ->
                Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    pasang.forEach { nama ->
                        PilChip(nama, false, {
                            cariDekat(nama, pakaiSaya)
                        }, Modifier.weight(1f))
                    }
                    if (pasang.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }

        // ---------- Hasil terdekat ----------
        if (memuat || infoHasil.isNotBlank() || hasil.isNotEmpty()) {
            KartuGelap(Modifier.fillMaxWidth()) {
                if (judulHasil.isNotBlank()) Text(judulHasil, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                if (infoHasil.isNotBlank()) Text(infoHasil, fontSize = 12.sp, color = TeksRedup)
                hasil.forEachIndexed { i, t ->
                    Spacer(Modifier.height(8.dp))
                    Column(Modifier.fillMaxWidth().clickable {
                        bukaIntent(
                            context, "geo:${t.lat},${t.lng}?q=${t.lat},${t.lng}(" + Uri.encode(t.nama) + ")",
                            "https://www.google.com/maps/search/?api=1&query=${t.lat},${t.lng}"
                        )
                    }) {
                        Text("${i + 1}. ${t.nama}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("${LokasiUtil.teksJarak(t.jarak)} ke arah ${t.arah} · ketuk untuk navigasi", fontSize = 11.sp, color = TeksRedup)
                    }
                }
                if (hasil.isNotEmpty()) Text("Data: OpenStreetMap. Tempat yang belum dipetakan di sana tidak akan muncul.", fontSize = 10.sp, color = TeksRedup, modifier = Modifier.padding(top = 10.dp))
            }
        }

        // ---------- Lokasi posko ----------
        KartuGelap(Modifier.fillMaxWidth()) {
            Text("Lokasi posko", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
            Text("Simpan alamat posko. Aplikasi mengunci ke koordinatnya (butuh internet sekali), lalu pencarian sekitar selalu mengikuti titik ini.", fontSize = 11.sp, color = TeksRedup)
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = teksPosko, onValueChange = { teksPosko = it; infoPosko = "" },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("mis. Desa Putat, Gunungkidul") }
            )
            if (posko.lat != null && posko.lng != null) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "Terkunci: ${String.format(Locale.US, "%.5f, %.5f", posko.lat, posko.lng)}",
                    fontSize = 12.sp, color = TeksRedup
                )
            }
            if (infoPosko.isNotBlank()) { Spacer(Modifier.height(6.dp)); Text(infoPosko, fontSize = 12.sp) }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = {
                        val nama = teksPosko.trim()
                        if (nama.isEmpty()) { infoPosko = "Isi alamat posko dulu."; return@Button }
                        scope.launch {
                            sibuk = true; infoPosko = "Mengunci lokasi…"
                            val k = LokasiUtil.cariKoordinat(context, nama)
                            val t = TitikPosko(nama, k?.first, k?.second)
                            LokasiUtil.simpanPosko(context, t); posko = t
                            infoPosko = if (k != null) "Tersimpan dan terkunci ke koordinat."
                            else "Tersimpan, tapi koordinat belum ditemukan (periksa internet atau tulis alamat lebih lengkap). Pencarian memakai nama."
                            sibuk = false
                        }
                    },
                    enabled = !sibuk, modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Surface2)
                ) { Text(if (sibuk) "Mengunci…" else "Simpan & kunci") }
                Button(
                    onClick = { bukaSekitar(context, posko.nama, posko, null, null) },
                    enabled = posko.nama.isNotBlank(),
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Violet)
                ) { Text("Buka posko") }
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { mintaPosisi(true) }, enabled = !mencari, modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Surface2)
            ) { Text("Kunci ke posisi GPS saya sekarang") }
        }

        Text(
            "Peta dibuka lewat aplikasi peta di HP kamu (mis. Google Maps). Gratis, tanpa kunci API.",
            fontSize = 11.sp, color = TeksRedup
        )
    }
}

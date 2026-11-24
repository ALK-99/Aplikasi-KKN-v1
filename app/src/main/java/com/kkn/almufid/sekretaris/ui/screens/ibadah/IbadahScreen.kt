package com.kkn.almufid.sekretaris.ui.screens.ibadah

import android.content.Context
import android.icu.util.IslamicCalendar
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kkn.almufid.sekretaris.ui.components.KartuGelap
import com.kkn.almufid.sekretaris.ui.components.PilChip
import com.kkn.almufid.sekretaris.ui.theme.*
import kotlinx.coroutines.delay
import java.util.Calendar
import java.util.TimeZone

private val NAMA_BULAN_HIJRIAH = listOf(
    "Muharram", "Safar", "Rabiul Awal", "Rabiul Akhir", "Jumadil Awal", "Jumadil Akhir",
    "Rajab", "Syaban", "Ramadhan", "Syawal", "Dzulqaidah", "Dzulhijjah"
)

private fun bacaKota(context: Context): Int = KotaPilihan.indeks(context)

private fun simpanKota(context: Context, indeks: Int) {
    KotaPilihan.simpan(context, indeks)
    com.kkn.almufid.sekretaris.pengingat.PengingatSholat.jadwalkan(context)
}

@Composable
fun IbadahScreen() {
    val context = LocalContext.current
    var kotaIdx by remember { mutableStateOf(bacaKota(context)) }
    val kota = DAFTAR_KOTA[kotaIdx]

    // Perbarui "sekarang" tiap 30 detik supaya sholat berikutnya & hitung mundur ikut bergerak.
    var sekarang by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) { delay(30_000); sekarang = System.currentTimeMillis() }
    }

    val kal = Calendar.getInstance().apply { timeInMillis = sekarang }
    val zona = TimeZone.getDefault().getOffset(sekarang) / 3_600_000.0
    val jadwal = remember(kota, kal.get(Calendar.DAY_OF_YEAR), zona) {
        WaktuSholat.hitung(
            kota.lintang, kota.bujur, zona,
            kal.get(Calendar.YEAR), kal.get(Calendar.MONTH) + 1, kal.get(Calendar.DAY_OF_MONTH)
        )
    }
    val daftar = listOf(
        "Subuh" to jadwal.subuh, "Terbit" to jadwal.terbit, "Dzuhur" to jadwal.dzuhur,
        "Ashar" to jadwal.ashar, "Maghrib" to jadwal.maghrib, "Isya" to jadwal.isya,
    )
    val jamSekarang = kal.get(Calendar.HOUR_OF_DAY) + kal.get(Calendar.MINUTE) / 60.0
    val berikutnya = daftar.firstOrNull { it.second > jamSekarang }
    val kiblat = WaktuSholat.arahKiblat(kota.lintang, kota.bujur)

    val hijriah = try {
        val h = IslamicCalendar()
        "${h.get(android.icu.util.Calendar.DAY_OF_MONTH)} ${NAMA_BULAN_HIJRIAH[h.get(android.icu.util.Calendar.MONTH).coerceIn(0, 11)]} ${h.get(android.icu.util.Calendar.YEAR)} H"
    } catch (e: Exception) { "" }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp).padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DAFTAR_KOTA.forEachIndexed { i, k ->
                PilChip(k.nama, i == kotaIdx, { kotaIdx = i; simpanKota(context, i) })
            }
        }
        var ingat by remember { mutableStateOf(com.kkn.almufid.sekretaris.pengingat.PengingatSholat.aktif(context)) }
        KartuGelap(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Ingatkan 10 menit sebelum adzan", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text("Notifikasi dengan getar dan pesan islami, tanpa internet.", fontSize = 11.sp, color = TeksRedup)
                }
                androidx.compose.material3.Switch(checked = ingat, onCheckedChange = {
                    ingat = it; com.kkn.almufid.sekretaris.pengingat.PengingatSholat.atur(context, it)
                })
            }
        }
        KartuGelap(Modifier.fillMaxWidth()) {
            if (hijriah.isNotBlank()) Text(hijriah, fontSize = 13.sp, color = TeksRedup)
            Spacer(Modifier.height(4.dp))
            if (berikutnya != null) {
                val sisaMenit = Math.round((berikutnya.second - jamSekarang) * 60.0).toInt()
                Text("Menuju ${berikutnya.first}", fontSize = 12.sp, color = TeksRedup)
                Text(
                    WaktuSholat.format(berikutnya.second), fontSize = 34.sp, fontWeight = FontWeight.ExtraBold
                )
                Text(
                    "${sisaMenit / 60} jam ${sisaMenit % 60} menit lagi", fontSize = 12.sp, color = Mint,
                    fontWeight = FontWeight.Bold
                )
            } else {
                Text("Semua waktu sholat hari ini sudah lewat", fontSize = 13.sp, color = TeksSoft)
                Text("Subuh besok sekitar ${WaktuSholat.format(jadwal.subuh)}", fontSize = 12.sp, color = TeksRedup)
            }
        }
        KartuGelap(Modifier.fillMaxWidth()) {
            daftar.forEach { (nama, jam) ->
                val aktif = berikutnya?.first == nama
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    Text(
                        nama, modifier = Modifier.weight(1f), fontSize = 15.sp,
                        fontWeight = if (aktif) FontWeight.ExtraBold else FontWeight.Normal,
                        color = if (aktif) Mint else TeksUtama
                    )
                    Text(
                        WaktuSholat.format(jam), fontSize = 15.sp,
                        fontWeight = if (aktif) FontWeight.ExtraBold else FontWeight.SemiBold,
                        color = if (aktif) Mint else TeksUtama
                    )
                }
            }
        }
        KartuGelap(Modifier.fillMaxWidth()) {
            Text("Arah kiblat", fontSize = 12.sp, color = TeksRedup)
            Text(
                "${kiblat.toInt()}° ${WaktuSholat.namaArah(kiblat)}",
                fontSize = 22.sp, fontWeight = FontWeight.ExtraBold
            )
            Text(
                "Diukur searah jarum jam dari arah utara. Gunakan kompas HP, dan jauhkan dari benda logam.",
                fontSize = 11.sp, color = TeksRedup
            )
        }
        Text(
            "Jadwal dihitung di HP (tanpa internet) dengan parameter Kemenag RI; selisih 1-3 menit dari jadwal resmi " +
                "setempat wajar. Tanggal Hijriah bisa berbeda satu hari dari penetapan pemerintah.",
            fontSize = 11.sp, color = TeksRedup
        )
    }
}

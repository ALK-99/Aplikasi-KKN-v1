package com.kkn.almufid.sekretaris.ui.screens.rekonsiliasi

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kkn.almufid.sekretaris.data.cloud.DesaRepository
import com.kkn.almufid.sekretaris.data.cloud.KoordinasiRepository
import com.kkn.almufid.sekretaris.ui.components.KartuGelap
import com.kkn.almufid.sekretaris.ui.formatRupiah
import com.kkn.almufid.sekretaris.ui.formatTanggal
import com.kkn.almufid.sekretaris.ui.theme.*
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class RekonsiliasiViewModel(desa: DesaRepository, koordinasi: KoordinasiRepository) : ViewModel() {
    val hasil: StateFlow<HasilRekon?> = combine(
        koordinasi.alirKas(), desa.alirNota(), desa.alirProker()
    ) { kas, nota, proker -> Rekonsiliasi.hitung(kas, nota, proker) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
}

@Composable
fun RekonsiliasiScreen(vm: RekonsiliasiViewModel) {
    val h by vm.hasil.collectAsState()
    val r = h
    if (r == null) {
        Text("Memuat data kas dan nota...", Modifier.padding(18.dp), color = TeksRedup, fontSize = 13.sp)
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(18.dp, 4.dp, 18.dp, 40.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            KartuGelap(Modifier.fillMaxWidth()) {
                Text("Ringkasan", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                Spacer(Modifier.height(8.dp))
                Baris("Kas masuk", formatRupiah(r.kasMasuk))
                Baris("Kas keluar", formatRupiah(r.kasKeluar))
                Baris("Saldo kas", formatRupiah(r.saldo))
                Baris("Total nota & kwitansi", formatRupiah(r.totalNota))
                Spacer(Modifier.height(6.dp))
                val warna = if (r.selisih == 0L) Mint else Gold
                Text(
                    if (r.selisih == 0L) "Cocok: pengeluaran kas sepenuhnya punya nota"
                    else "Selisih kas keluar dan nota: ${formatRupiah(kotlin.math.abs(r.selisih))} " +
                        if (r.selisih > 0) "(kas keluar lebih besar)" else "(nota lebih besar)",
                    color = warna, fontWeight = FontWeight.Bold, fontSize = 13.sp
                )
                Text(
                    "${r.cocok.size} pengeluaran cocok dengan nota · ${r.notaTanpaFoto} nota tanpa foto",
                    color = TeksRedup, fontSize = 12.sp
                )
            }
        }
        item {
            Text(
                "Kas Kelompok mencakup semua desa, sedangkan nota hanya milik desa kamu. Pengeluaran desa lain " +
                    "akan tampil sebagai \"belum ada nota\". Pencocokan memakai nominal yang sama dan tanggal " +
                    "berselisih maksimal 14 hari.",
                color = TeksRedup, fontSize = 11.sp, lineHeight = 15.sp
            )
        }
        if (r.kasTanpaNota.isNotEmpty()) {
            item { Judul("Kas keluar belum ada nota (${r.kasTanpaNota.size})") }
            items(r.kasTanpaNota.size) { i ->
                val k = r.kasTanpaNota[i]
                KartuGelap(Modifier.fillMaxWidth()) {
                    Text("${formatRupiah(k.jumlah)} · ${k.kategori.ifBlank { "Tanpa kategori" }}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("${k.keterangan} · ${formatTanggal(k.tanggalMillis)}", color = TeksRedup, fontSize = 12.sp)
                }
            }
        }
        if (r.notaTanpaKas.isNotEmpty()) {
            item { Judul("Nota belum tercatat di kas (${r.notaTanpaKas.size})") }
            items(r.notaTanpaKas.size) { i ->
                val n = r.notaTanpaKas[i]
                KartuGelap(Modifier.fillMaxWidth()) {
                    Text("${formatRupiah(n.nominal)} · ${n.jenis.lowercase()}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("${n.keterangan} · ${formatTanggal(n.tanggalMillis)}", color = TeksRedup, fontSize = 12.sp)
                }
            }
        }
        if (r.perProker.isNotEmpty()) {
            item { Judul("Per proker: anggaran, realisasi, nota") }
            items(r.perProker.size) { i ->
                val p = r.perProker[i]
                KartuGelap(Modifier.fillMaxWidth()) {
                    Text(p.nama, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Baris("Anggaran", formatRupiah(p.anggaran))
                    Baris("Realisasi", formatRupiah(p.realisasi))
                    Baris("Total nota proker ini", formatRupiah(p.totalNota))
                    if (p.realisasi > 0L && p.totalNota != p.realisasi) {
                        Text("Realisasi dan nota belum sama", color = Gold, fontSize = 11.sp)
                    }
                    if (p.anggaran > 0L && maxOf(p.realisasi, p.totalNota) > p.anggaran) {
                        Text("Melebihi anggaran", color = Merah, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun Judul(teks: String) = Text(teks, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)

@Composable
private fun Baris(label: String, nilai: String, warna: Color = TeksUtama) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = TeksRedup, fontSize = 13.sp)
        Text(nilai, color = warna, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

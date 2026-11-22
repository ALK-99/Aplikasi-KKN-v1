package com.kkn.almufid.sekretaris.ui.screens.riwayat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kkn.almufid.sekretaris.data.cloud.DesaRepository
import com.kkn.almufid.sekretaris.data.cloud.RiwayatDesa
import com.kkn.almufid.sekretaris.ui.components.KartuGelap
import com.kkn.almufid.sekretaris.ui.components.KosongState
import com.kkn.almufid.sekretaris.ui.formatTanggal
import com.kkn.almufid.sekretaris.ui.theme.*
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class RiwayatViewModel(desa: DesaRepository) : ViewModel() {
    val riwayat: StateFlow<List<RiwayatDesa>> = desa.alirRiwayat()
        .map { daftar -> daftar.sortedByDescending { it.waktu } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}

private fun namaTabel(kode: String) = when (kode) {
    "anggota" -> "Anggota"
    "absensi" -> "Absensi"
    "proker" -> "Proker"
    "surat" -> "Surat"
    "inventaris" -> "Inventaris"
    "nota" -> "Nota"
    "bukti" -> "Bukti proker"
    "impor" -> "Impor"
    "cadangan" -> "Cadangan"
    else -> kode.replaceFirstChar { it.uppercase() }
}

/** Siapa mengubah apa dan kapan di desa ini. Catatan tidak bisa diubah atau dihapus oleh siapa pun. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun RiwayatScreen(vm: RiwayatViewModel) {
    val semua by vm.riwayat.collectAsState()
    var pilihan by remember { mutableStateOf<String?>(null) }
    val tabel = semua.map { it.tabel }.distinct().sorted()
    val tampil = if (pilihan == null) semua else semua.filter { it.tabel == pilihan }

    Column(Modifier.fillMaxSize()) {
        Text(
            "Catatan 300 perubahan terakhir di desa kamu. Tidak bisa diubah atau dihapus.",
            Modifier.padding(horizontal = 18.dp), fontSize = 12.sp, color = TeksRedup
        )
        LazyRow(
            contentPadding = PaddingValues(18.dp, 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item { FilterChip(selected = pilihan == null, onClick = { pilihan = null }, label = { Text("Semua") }) }
            items(tabel) { t ->
                FilterChip(selected = pilihan == t, onClick = { pilihan = t }, label = { Text(namaTabel(t)) })
            }
        }
        if (tampil.isEmpty()) {
            KosongState("Belum ada catatan perubahan.")
        } else {
            LazyColumn(
                contentPadding = PaddingValues(18.dp, 0.dp, 18.dp, 40.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(tampil, key = { it.id }) { r ->
                    KartuGelap(Modifier.fillMaxWidth()) {
                        Text(
                            "${namaTabel(r.tabel)} · ${r.aksi}",
                            fontWeight = FontWeight.Bold, fontSize = 14.sp, color = VioletTerang
                        )
                        if (r.ringkas.isNotBlank()) Text(r.ringkas, fontSize = 13.sp)
                        Text("${r.olehNama} · ${formatTanggal(r.waktu)}", fontSize = 11.sp, color = TeksRedup)
                    }
                }
            }
        }
    }
}

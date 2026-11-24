package com.kkn.almufid.sekretaris.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kkn.almufid.sekretaris.ui.theme.TeksRedup

/** Keadaan saring + urutkan sebuah daftar. Cukup satu per layar. */
class PenyaringState {
    var cari by mutableStateOf("")
    var urut by mutableIntStateOf(-1)
    var naik by mutableStateOf(true)
    var filter by mutableStateOf<String?>(null)
    var terbuka by mutableStateOf(false)

    /**
     * @param teks teks yang dicocokkan dengan kotak cari
     * @param urutan pilihan "Urutkan berdasarkan" (nama + pembanding)
     * @param nilaiFilter nilai yang dipakai untuk "Filter berdasarkan" (null = tanpa filter)
     */
    fun <T> terapkan(
        daftar: List<T>,
        teks: (T) -> String,
        urutan: List<Pair<String, Comparator<T>>>,
        nilaiFilter: ((T) -> String)? = null,
    ): List<T> {
        var hasil = daftar
        if (cari.isNotBlank()) hasil = hasil.filter { teks(it).contains(cari.trim(), ignoreCase = true) }
        if (nilaiFilter != null && filter != null) hasil = hasil.filter { nilaiFilter(it) == filter }
        if (urut in urutan.indices) {
            val p = urutan[urut].second
            hasil = hasil.sortedWith(if (naik) p else p.reversed())
        }
        return hasil
    }
}

@Composable
fun rememberPenyaring() = remember { PenyaringState() }

/** Baris "Saring & urutkan" yang bisa dibuka-tutup: kotak cari, urutkan berdasarkan, filter berdasarkan. */
@Composable
fun BarPenyaring(
    s: PenyaringState,
    namaUrut: List<String>,
    labelFilter: String? = null,
    nilaiFilter: List<String> = emptyList(),
    jumlah: Int,
    total: Int,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 4.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PilChip(if (s.terbuka) "Tutup saring" else "Saring & urutkan", s.terbuka, { s.terbuka = !s.terbuka })
            if (s.cari.isNotBlank() || s.filter != null || s.urut >= 0) {
                PilChip("Reset", false, { s.cari = ""; s.filter = null; s.urut = -1; s.naik = true })
                Text("$jumlah dari $total", fontSize = 12.sp, color = TeksRedup, modifier = Modifier.align(androidx.compose.ui.Alignment.CenterVertically))
            }
        }
        if (s.terbuka) {
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = s.cari, onValueChange = { s.cari = it }, singleLine = true,
                modifier = Modifier.fillMaxWidth(), placeholder = { Text("Cari kata…") }
            )
            Spacer(Modifier.height(8.dp))
            Text("Urutkan berdasarkan", fontSize = 11.sp, color = TeksRedup)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                namaUrut.forEachIndexed { i, n ->
                    PilChip(n + if (s.urut == i) (if (s.naik) " ↑" else " ↓") else "", s.urut == i, {
                        if (s.urut == i) s.naik = !s.naik else { s.urut = i; s.naik = true }
                    })
                }
            }
            if (labelFilter != null && nilaiFilter.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text("Filter berdasarkan $labelFilter", fontSize = 11.sp, color = TeksRedup)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PilChip("Semua", s.filter == null, { s.filter = null })
                    nilaiFilter.forEach { n -> PilChip(n, s.filter == n, { s.filter = n }) }
                }
            }
        }
    }
}

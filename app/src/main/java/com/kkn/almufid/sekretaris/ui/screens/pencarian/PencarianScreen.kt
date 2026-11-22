package com.kkn.almufid.sekretaris.ui.screens.pencarian

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kkn.almufid.sekretaris.data.cloud.DesaRepository
import com.kkn.almufid.sekretaris.data.cloud.JurnalBersamaRepository
import com.kkn.almufid.sekretaris.ui.components.KartuGelap
import com.kkn.almufid.sekretaris.ui.components.KosongState
import com.kkn.almufid.sekretaris.ui.components.tekanPegas
import com.kkn.almufid.sekretaris.ui.formatRupiah
import com.kkn.almufid.sekretaris.ui.navigation.Tujuan
import com.kkn.almufid.sekretaris.ui.theme.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class HasilCari(val judul: String, val keterangan: String, val jenis: String, val tujuan: Tujuan)

private class Basis(
    val anggota: List<com.kkn.almufid.sekretaris.data.cloud.AnggotaDesa>,
    val proker: List<com.kkn.almufid.sekretaris.data.cloud.ProkerDesa>,
    val surat: List<com.kkn.almufid.sekretaris.data.cloud.SuratDesa>,
    val barang: List<com.kkn.almufid.sekretaris.data.cloud.InventarisDesa>,
    val nota: List<com.kkn.almufid.sekretaris.data.cloud.NotaDesa>,
)

/** Mencari di seluruh data desa, jurnal bersama, dan daftar menu. Tanpa internet tetap jalan dari salinan di HP. */
class PencarianViewModel(desa: DesaRepository, bersama: JurnalBersamaRepository) : ViewModel() {
    val kueri = MutableStateFlow("")

    private val basis = combine(
        desa.alirAnggota(), desa.alirProker(), desa.alirSurat(), desa.alirInventaris(), desa.alirNota()
    ) { a, p, s, b, n -> Basis(a, p, s, b, n) }

    val hasil: StateFlow<List<HasilCari>> = combine(kueri, basis, bersama.alirSemua()) { q, d, jurnal ->
        val kata = q.trim().lowercase()
        if (kata.length < 2) return@combine emptyList()
        fun cocok(vararg teks: String) = teks.any { it.lowercase().contains(kata) }
        buildList {
            Tujuan.entries.filter { it != Tujuan.PENCARIAN && cocok(it.judul) }
                .forEach { add(HasilCari(it.judul, "Buka halaman", "Menu", it)) }
            d.anggota.filter { cocok(it.nama, it.divisi) }
                .forEach { add(HasilCari(it.nama, "Divisi ${it.divisi.ifBlank { "-" }}" + if (it.aktif) "" else " · nonaktif", "Anggota", Tujuan.ABSENSI)) }
            d.proker.filter { cocok(it.namaProker, it.picNama, it.target, it.divisiPenanggungJawab) }
                .forEach { add(HasilCari(it.namaProker, "PIC ${it.picNama.ifBlank { "-" }} · ${it.status.name.replace('_', ' ').lowercase()}", "Proker", Tujuan.PROKER)) }
            d.surat.filter { cocok(it.perihal, it.nomorSurat, it.tujuanAtauPengirim) }
                .forEach { add(HasilCari(it.perihal.ifBlank { "(tanpa perihal)" }, "${it.nomorSurat.ifBlank { "tanpa nomor" }} · ${it.tujuanAtauPengirim}", "Surat", Tujuan.SURAT)) }
            d.barang.filter { cocok(it.namaBarang, it.keterangan) }
                .forEach { add(HasilCari(it.namaBarang, "${it.jumlah} unit · ${it.kondisi}", "Inventaris", Tujuan.INVENTARIS)) }
            d.nota.filter { cocok(it.keterangan, it.prokerTerkait) }
                .forEach { add(HasilCari(it.keterangan, "${formatRupiah(it.nominal)} · ${it.jenis.lowercase()}", "Nota", Tujuan.ARSIP)) }
            jurnal.filter { cocok(it.judul, it.isi, it.penulisNama) }
                .forEach { add(HasilCari(it.judul.ifBlank { "(tanpa judul)" }, "Oleh ${it.penulisNama}", "Jurnal bersama", Tujuan.JURNAL)) }
        }.take(60)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}

@Composable
fun PencarianScreen(vm: PencarianViewModel, onBuka: (Tujuan) -> Unit) {
    val kueri by vm.kueri.collectAsState()
    val hasil by vm.hasil.collectAsState()
    val fokus = remember { FocusRequester() }
    LaunchedEffect(Unit) { try { fokus.requestFocus() } catch (e: Exception) { } }

    Column(Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
        OutlinedTextField(
            value = kueri, onValueChange = { vm.kueri.value = it },
            modifier = Modifier.fillMaxWidth().focusRequester(fokus),
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Search, null) },
            placeholder = { Text("Cari anggota, proker, surat, barang, nota, jurnal") },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        )
        Spacer(Modifier.height(12.dp))
        when {
            kueri.trim().length < 2 -> KosongState("Ketik minimal 2 huruf untuk mulai mencari.")
            hasil.isEmpty() -> KosongState("Tidak ada hasil untuk \"${kueri.trim()}\".")
            else -> LazyColumn(
                contentPadding = PaddingValues(bottom = 40.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(hasil) { h ->
                    KartuGelap(Modifier.fillMaxWidth().tekanPegas { onBuka(h.tujuan) }) {
                        Text(h.jenis, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = VioletTerang)
                        Text(h.judul, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(h.keterangan, fontSize = 12.sp, color = TeksRedup, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

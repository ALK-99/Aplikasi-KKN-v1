package com.kkn.almufid.sekretaris.ui.screens.ajar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kkn.almufid.sekretaris.data.auth.HasilAuth
import com.kkn.almufid.sekretaris.data.cloud.AjarDesa
import com.kkn.almufid.sekretaris.data.cloud.DesaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

val NAMA_HARI = listOf("Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu", "Minggu")

/** Mengubah teks "senin", "Jum'at", dst. menjadi 1..7, atau 0 kalau tidak dikenali. */
fun hariDariTeks(teks: String): Int {
    val t = teks.trim().lowercase().replace("'", "").replace("’", "")
    return NAMA_HARI.indexOfFirst { it.lowercase() == t } + 1
}

class AjarViewModel(private val desa: DesaRepository) : ViewModel() {

    val daftar: StateFlow<List<AjarDesa>> = desa.alirAjar()
        .map { l -> l.sortedWith(compareBy({ it.hari }, { it.jam })) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _pesan = MutableStateFlow<String?>(null)
    val pesan: StateFlow<String?> = _pesan.asStateFlow()

    private fun lapor(hasil: HasilAuth<Unit>) {
        if (hasil is HasilAuth.Gagal) _pesan.value = hasil.pesan
    }

    /** [entry] null = baru. Mengembalikan true kalau isian valid. */
    fun simpan(entry: AjarDesa?, hariTeks: String, jam: String, lembaga: String, materi: String, pengajar: String, catatan: String): Boolean {
        val hari = hariDariTeks(hariTeks)
        if (hari == 0) { _pesan.value = "Hari harus salah satu dari: ${NAMA_HARI.joinToString(", ")}."; return false }
        if (materi.isBlank() && lembaga.isBlank()) { _pesan.value = "Isi minimal lembaga/kelas atau materi."; return false }
        viewModelScope.launch {
            lapor(
                if (entry == null) desa.tambahAjar(hari, jam, lembaga, materi, pengajar, catatan)
                else desa.ubahAjar(entry, hari, jam, lembaga, materi, pengajar, catatan)
            )
        }
        return true
    }

    fun hapus(entry: AjarDesa) { viewModelScope.launch { lapor(desa.hapusAjar(entry)) } }

    fun tutupPesan() { _pesan.value = null }
}

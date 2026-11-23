package com.kkn.almufid.sekretaris.ui.screens.surat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kkn.almufid.sekretaris.data.auth.HasilAuth
import com.kkn.almufid.sekretaris.data.cloud.DesaRepository
import com.kkn.almufid.sekretaris.data.cloud.SuratDesa
import com.kkn.almufid.sekretaris.data.entity.JenisSurat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SuratViewModel(private val desa: DesaRepository) : ViewModel() {

    val daftarSurat: StateFlow<List<SuratDesa>> = desa.alirSurat()
        .map { daftar -> daftar.sortedByDescending { it.tanggalMillis } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _pesan = MutableStateFlow<String?>(null)
    val pesan: StateFlow<String?> = _pesan.asStateFlow()

    private fun lapor(hasil: HasilAuth<Unit>) {
        if (hasil is HasilAuth.Gagal) _pesan.value = hasil.pesan
    }

    fun ubahSurat(surat: SuratDesa, nomor: String, perihal: String, tujuan: String) {
        if (perihal.isBlank()) return
        viewModelScope.launch { lapor(desa.ubahSurat(surat, nomor, perihal, tujuan)) }
    }

    fun tambahSurat(jenis: JenisSurat, nomor: String, perihal: String, tujuan: String) {
        if (perihal.isBlank()) return
        viewModelScope.launch { lapor(desa.tambahSurat(jenis, nomor, perihal, tujuan)) }
    }

    /** Saran nomor berurutan per jenis, per tahun, dihitung dari surat seluruh desa: mis. 003/KKN-AM/XI/2026. */
    fun nomorBerikutnya(jenis: JenisSurat): String {
        val kal = java.util.Calendar.getInstance()
        val tahun = kal.get(java.util.Calendar.YEAR)
        val romawi = listOf("I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X", "XI", "XII")[kal.get(java.util.Calendar.MONTH)]
        val urut = daftarSurat.value.count {
            it.jenis == jenis && java.util.Calendar.getInstance().apply { timeInMillis = it.tanggalMillis }.get(java.util.Calendar.YEAR) == tahun
        } + 1
        val kode = if (jenis == JenisSurat.KELUAR) "KKN-AM" else "MSK"
        return "%03d/%s/%s/%d".format(urut, kode, romawi, tahun)
    }

    fun hapusSurat(entry: SuratDesa) {
        viewModelScope.launch { lapor(desa.hapusSurat(entry)) }
    }

    fun tutupPesan() { _pesan.value = null }
}

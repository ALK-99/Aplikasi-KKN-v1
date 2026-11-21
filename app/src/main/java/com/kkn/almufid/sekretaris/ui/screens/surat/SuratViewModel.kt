package com.kkn.almufid.sekretaris.ui.screens.surat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kkn.almufid.sekretaris.data.dao.SuratDao
import com.kkn.almufid.sekretaris.data.entity.JenisSurat
import com.kkn.almufid.sekretaris.data.entity.SuratEntry
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SuratViewModel(private val dao: SuratDao) : ViewModel() {

    val daftarSurat: StateFlow<List<SuratEntry>> = dao.semuaSurat()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun tambahSurat(jenis: JenisSurat, nomor: String, perihal: String, tujuan: String) {
        if (perihal.isBlank()) return
        viewModelScope.launch {
            dao.tambah(
                SuratEntry(
                    tanggalMillis = System.currentTimeMillis(),
                    jenis = jenis,
                    nomorSurat = nomor.trim(),
                    perihal = perihal.trim(),
                    tujuanAtauPengirim = tujuan.trim()
                )
            )
        }
    }

    /** Saran nomor berurutan per jenis, per tahun: mis. 003/KKN-AM/XI/2026. */
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

    fun hapusSurat(entry: SuratEntry) {
        viewModelScope.launch { dao.hapus(entry) }
    }
}

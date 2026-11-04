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

    fun hapusSurat(entry: SuratEntry) {
        viewModelScope.launch { dao.hapus(entry) }
    }
}

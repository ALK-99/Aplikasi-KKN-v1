package com.kkn.almufid.sekretaris.ui.screens.jurnal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kkn.almufid.sekretaris.data.dao.JurnalDao
import com.kkn.almufid.sekretaris.data.entity.JenisJurnal
import com.kkn.almufid.sekretaris.data.entity.JurnalEntry
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class JurnalViewModel(private val dao: JurnalDao) : ViewModel() {

    val daftarJurnal: StateFlow<List<JurnalEntry>> = dao.semuaJurnal()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun tambahJurnal(jenis: JenisJurnal, judul: String, isi: String, peserta: String = "") {
        if (judul.isBlank() || isi.isBlank()) return
        viewModelScope.launch {
            dao.tambah(
                JurnalEntry(
                    tanggalMillis = System.currentTimeMillis(),
                    jenis = jenis,
                    judul = judul.trim(),
                    isi = isi.trim(),
                    pesertaRapat = peserta.trim()
                )
            )
        }
    }

    fun hapusJurnal(entry: JurnalEntry) {
        viewModelScope.launch { dao.hapus(entry) }
    }
}

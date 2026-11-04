package com.kkn.almufid.sekretaris.ui.screens.proker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kkn.almufid.sekretaris.data.dao.ProkerDao
import com.kkn.almufid.sekretaris.data.entity.ProkerEntry
import com.kkn.almufid.sekretaris.data.entity.StatusProker
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProkerViewModel(private val dao: ProkerDao) : ViewModel() {

    val daftarProker: StateFlow<List<ProkerEntry>> = dao.semuaProker()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun tambahProker(nama: String, divisi: String) {
        if (nama.isBlank()) return
        viewModelScope.launch {
            dao.tambah(ProkerEntry(namaProker = nama.trim(), divisiPenanggungJawab = divisi.trim()))
        }
    }

    fun ubahStatus(entry: ProkerEntry, status: StatusProker) {
        viewModelScope.launch { dao.ubah(entry.copy(status = status)) }
    }

    fun hapusProker(entry: ProkerEntry) {
        viewModelScope.launch { dao.hapus(entry) }
    }
}

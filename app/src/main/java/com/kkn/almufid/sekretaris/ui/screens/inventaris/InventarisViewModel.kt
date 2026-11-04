package com.kkn.almufid.sekretaris.ui.screens.inventaris

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kkn.almufid.sekretaris.data.dao.InventarisDao
import com.kkn.almufid.sekretaris.data.entity.InventarisEntry
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class InventarisViewModel(private val dao: InventarisDao) : ViewModel() {

    val daftarBarang: StateFlow<List<InventarisEntry>> = dao.semuaInventaris()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun tambahBarang(nama: String, jumlah: Int, kondisi: String, keterangan: String) {
        if (nama.isBlank() || jumlah <= 0) return
        viewModelScope.launch {
            dao.tambah(
                InventarisEntry(
                    namaBarang = nama.trim(),
                    jumlah = jumlah,
                    kondisi = kondisi.trim().ifBlank { "Baik" },
                    keterangan = keterangan.trim()
                )
            )
        }
    }

    fun hapusBarang(entry: InventarisEntry) {
        viewModelScope.launch { dao.hapus(entry) }
    }
}

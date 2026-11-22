package com.kkn.almufid.sekretaris.ui.screens.inventaris

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kkn.almufid.sekretaris.data.auth.HasilAuth
import com.kkn.almufid.sekretaris.data.cloud.DesaRepository
import com.kkn.almufid.sekretaris.data.cloud.InventarisDesa
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class InventarisViewModel(private val desa: DesaRepository) : ViewModel() {

    val daftarBarang: StateFlow<List<InventarisDesa>> = desa.alirInventaris()
        .map { daftar -> daftar.sortedBy { it.namaBarang.lowercase() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _pesan = MutableStateFlow<String?>(null)
    val pesan: StateFlow<String?> = _pesan.asStateFlow()

    private fun lapor(hasil: HasilAuth<Unit>) {
        if (hasil is HasilAuth.Gagal) _pesan.value = hasil.pesan
    }

    fun tambahBarang(nama: String, jumlah: Int, kondisi: String, keterangan: String) {
        if (nama.isBlank() || jumlah <= 0) return
        viewModelScope.launch { lapor(desa.tambahBarang(nama, jumlah, kondisi, keterangan)) }
    }

    fun hapusBarang(entry: InventarisDesa) {
        viewModelScope.launch { lapor(desa.hapusBarang(entry)) }
    }

    fun tutupPesan() { _pesan.value = null }
}

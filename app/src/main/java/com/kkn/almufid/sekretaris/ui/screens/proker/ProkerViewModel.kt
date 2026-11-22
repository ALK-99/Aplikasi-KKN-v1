package com.kkn.almufid.sekretaris.ui.screens.proker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kkn.almufid.sekretaris.data.auth.HasilAuth
import com.kkn.almufid.sekretaris.data.cloud.DesaRepository
import com.kkn.almufid.sekretaris.data.cloud.ProkerDesa
import com.kkn.almufid.sekretaris.data.entity.StatusProker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProkerViewModel(private val desa: DesaRepository) : ViewModel() {

    val daftarProker: StateFlow<List<ProkerDesa>> = desa.alirProker()
        .map { daftar -> daftar.sortedBy { it.jejak.dibuatPada } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _pesan = MutableStateFlow<String?>(null)
    val pesan: StateFlow<String?> = _pesan.asStateFlow()

    private fun lapor(hasil: HasilAuth<Unit>) {
        if (hasil is HasilAuth.Gagal) _pesan.value = hasil.pesan
    }

    fun tambahProker(nama: String, divisi: String) {
        if (nama.isBlank()) return
        viewModelScope.launch { lapor(desa.tambahProker(nama, divisi)) }
    }

    fun ubahStatus(entry: ProkerDesa, status: StatusProker) {
        viewModelScope.launch { lapor(desa.ubahStatusProker(entry, status)) }
    }

    fun hapusProker(entry: ProkerDesa) {
        viewModelScope.launch { lapor(desa.hapusProker(entry)) }
    }

    fun tutupPesan() { _pesan.value = null }
}

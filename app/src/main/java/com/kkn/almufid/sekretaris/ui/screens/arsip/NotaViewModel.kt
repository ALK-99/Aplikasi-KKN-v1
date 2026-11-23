package com.kkn.almufid.sekretaris.ui.screens.arsip

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kkn.almufid.sekretaris.data.auth.HasilAuth
import com.kkn.almufid.sekretaris.data.cloud.DesaRepository
import com.kkn.almufid.sekretaris.data.cloud.NotaDesa
import com.kkn.almufid.sekretaris.ui.FotoUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Nota dan kwitansi desa. Foto dikompres kecil dan disimpan di dokumen Firestore (tanpa Storage). */
class NotaViewModel(private val desa: DesaRepository) : ViewModel() {

    val daftarNota: StateFlow<List<NotaDesa>> = desa.alirNota()
        .map { daftar -> daftar.sortedByDescending { it.tanggalMillis } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalPengeluaran: StateFlow<Long> = daftarNota
        .map { daftar -> daftar.sumOf { it.nominal } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    private val _pesan = MutableStateFlow<String?>(null)
    val pesan: StateFlow<String?> = _pesan.asStateFlow()

    fun tambahNota(keterangan: String, nominal: Long, prokerTerkait: String, jenis: String, foto: Bitmap?) {
        if (keterangan.isBlank() || nominal <= 0) return
        viewModelScope.launch {
            val b64 = foto?.let { withContext(Dispatchers.Default) { FotoUtil.kompresKeBase64(it) } } ?: ""
            val hasil = desa.tambahNota(keterangan, nominal, prokerTerkait, jenis, b64)
            if (hasil is HasilAuth.Gagal) _pesan.value = hasil.pesan
        }
    }

    fun ubahNota(entry: NotaDesa, keterangan: String, nominal: Long, proker: String) {
        if (keterangan.isBlank() || nominal <= 0) return
        viewModelScope.launch {
            val hasil = desa.ubahNota(entry, keterangan, nominal, proker)
            if (hasil is HasilAuth.Gagal) _pesan.value = hasil.pesan
        }
    }

    fun hapusNota(entry: NotaDesa) {
        viewModelScope.launch {
            val hasil = desa.hapusNota(entry)
            if (hasil is HasilAuth.Gagal) _pesan.value = hasil.pesan
        }
    }

    fun tutupPesan() { _pesan.value = null }
}

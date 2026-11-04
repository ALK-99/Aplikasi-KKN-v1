package com.kkn.almufid.sekretaris.ui.screens.arsip

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kkn.almufid.sekretaris.data.dao.NotaDao
import com.kkn.almufid.sekretaris.data.entity.NotaEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class NotaViewModel(
    private val dao: NotaDao,
    private val appContext: Context
) : ViewModel() {

    val daftarNota: StateFlow<List<NotaEntry>> = dao.semuaNota()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalPengeluaran: StateFlow<Long> = dao.totalPengeluaran()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    fun tambahNota(keterangan: String, nominal: Long, prokerTerkait: String, fotoBitmap: Bitmap?) {
        if (keterangan.isBlank() || nominal <= 0) return
        viewModelScope.launch {
            val fotoPath = fotoBitmap?.let { simpanFotoKeInternal(it) }
            dao.tambah(
                NotaEntry(
                    tanggalMillis = System.currentTimeMillis(),
                    keterangan = keterangan.trim(),
                    nominal = nominal,
                    prokerTerkait = prokerTerkait.trim(),
                    fotoPath = fotoPath
                )
            )
        }
    }

    fun hapusNota(entry: NotaEntry) {
        viewModelScope.launch {
            entry.fotoPath?.let { path -> withContext(Dispatchers.IO) { File(path).delete() } }
            dao.hapus(entry)
        }
    }

    /**
     * Disimpan di folder internal (files/nota_photos/), BUKAN di galeri
     * publik — supaya foto nota tidak bercampur dengan foto pribadi
     * pengguna dan otomatis ikut terhapus kalau aplikasi di-uninstall
     * (sesuai sifat data kesekretariatan yang privat untuk kelompok).
     */
    private suspend fun simpanFotoKeInternal(bitmap: Bitmap): String = withContext(Dispatchers.IO) {
        val folder = File(appContext.filesDir, "nota_photos").apply { mkdirs() }
        val file = File(folder, "nota_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { stream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream)
        }
        file.absolutePath
    }
}

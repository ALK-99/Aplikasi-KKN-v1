package com.kkn.almufid.sekretaris.update

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File

sealed interface StatusUpdate {
    data object Tidak : StatusUpdate
    data class Tersedia(val info: InfoUpdate) : StatusUpdate
    data class Mengunduh(val persen: Int) : StatusUpdate
    data class SiapPasang(val apk: File) : StatusUpdate
    data class Gagal(val pesan: String) : StatusUpdate
}

class UpdateViewModel(private val appContext: Context) : ViewModel() {

    private val _status = MutableStateFlow<StatusUpdate>(StatusUpdate.Tidak)
    val status: StateFlow<StatusUpdate> = _status

    private var sudahCek = false

    /** Dipanggil sekali saat app dibuka. Aman tanpa internet (diam-diam gagal). */
    fun cekSekaliSaatBuka() {
        if (sudahCek) return
        sudahCek = true
        viewModelScope.launch {
            val info = UpdateChecker.cek()
            if (info == null) {
                PrefUpdate.setMenunggu(appContext, false)
                return@launch
            }
            if (PrefUpdate.menunggu(appContext)) {
                // Pemasangan senyap tidak berhasil (mis. pertama kali / HP menolak):
                // tampilkan pop-up manual sebagai cadangan.
                _status.value = StatusUpdate.Tersedia(info)
            } else {
                // Normal: unduh & pasang diam-diam di latar belakang, tanpa pop-up.
                UpdateWorker.jalankanSekarang(appContext)
            }
        }
    }

    fun mulaiUnduh(info: InfoUpdate) {
        _status.value = StatusUpdate.Mengunduh(0)
        viewModelScope.launch {
            try {
                val apk = UpdateChecker.unduh(appContext, info.urlApk) { persen ->
                    _status.value = StatusUpdate.Mengunduh(persen)
                }
                _status.value = StatusUpdate.SiapPasang(apk)
            } catch (e: Exception) {
                _status.value = StatusUpdate.Gagal("Unduhan gagal. Periksa koneksi internet lalu coba lagi.")
            }
        }
    }

    fun gagalIzin() {
        _status.value = StatusUpdate.Gagal(
            "Aktifkan izin \"Pasang aplikasi tidak dikenal\" untuk KKN Al-Mushlih 2027 di halaman pengaturan yang baru terbuka, " +
                "lalu buka ulang aplikasi — pop-up update akan muncul lagi."
        )
    }

    fun tutup() {
        _status.value = StatusUpdate.Tidak
    }
}

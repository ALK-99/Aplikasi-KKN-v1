package com.kkn.almufid.sekretaris.ui.screens.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kkn.almufid.sekretaris.data.AppDatabase
import com.kkn.almufid.sekretaris.data.auth.AuthRepository
import com.kkn.almufid.sekretaris.data.auth.HasilAuth
import com.kkn.almufid.sekretaris.data.cloud.DesaRepository
import com.kkn.almufid.sekretaris.data.cloud.JumlahDataLama
import com.kkn.almufid.sekretaris.data.cloud.KoordinasiRepository
import com.kkn.almufid.sekretaris.data.cloud.Pengumuman
import com.kkn.almufid.sekretaris.data.entity.StatusProker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class RingkasanHome(
    val prokerSelesai: Int = 0,
    val prokerTotal: Int = 0,
)

class HomeViewModel(
    appContext: Context,
    private val db: AppDatabase,
    private val desa: DesaRepository,
    koordinasi: KoordinasiRepository,
    private val auth: AuthRepository,
) : ViewModel() {
    private val prefs = appContext.getSharedPreferences("sinkron_prefs", Context.MODE_PRIVATE)

    val ringkasan: StateFlow<RingkasanHome> = desa.alirProker()
        .map { proker ->
            RingkasanHome(
                prokerSelesai = proker.count { it.status == StatusProker.SELESAI },
                prokerTotal = proker.count { it.status != StatusProker.BATAL },
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RingkasanHome())

    val uidSaya: String get() = auth.uidSaatIni ?: ""

    /** Pengumuman terbaru (daftar sudah diurutkan terbaru dulu oleh repository). */
    val pengumumanTerbaru: StateFlow<Pengumuman?> = koordinasi.alirPengumuman()
        .map { it.firstOrNull() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // ---- Pemindahan data lama di HP ke ruang desa ----
    private val _dataLama = MutableStateFlow<JumlahDataLama?>(null)
    val dataLama: StateFlow<JumlahDataLama?> = _dataLama.asStateFlow()

    private val _sedangUnggah = MutableStateFlow(false)
    val sedangUnggah: StateFlow<Boolean> = _sedangUnggah.asStateFlow()

    private val _pesan = MutableStateFlow<String?>(null)
    val pesan: StateFlow<String?> = _pesan.asStateFlow()

    private fun kunci() = "impor_selesai_${auth.uidSaatIni ?: ""}"

    init { periksaDataLama() }

    private fun periksaDataLama() {
        viewModelScope.launch {
            if (auth.uidSaatIni == null || prefs.getBoolean(kunci(), false)) return@launch
            val jumlah = withContext(Dispatchers.IO) {
                JumlahDataLama(
                    anggota = db.anggotaDao().semuaAnggota().first().size,
                    absensi = db.absensiDao().semuaAbsensi().first().size,
                    proker = db.prokerDao().semuaProker().first().size,
                    surat = db.suratDao().semuaSurat().first().size,
                    inventaris = db.inventarisDao().semuaInventaris().first().size,
                )
            }
            _dataLama.value = if (jumlah.total > 0) jumlah else null
        }
    }

    fun unggahDataLama() {
        if (_sedangUnggah.value) return
        _sedangUnggah.value = true
        viewModelScope.launch {
            try {
                val hasil = withContext(Dispatchers.IO) {
                    desa.imporDataLama(
                        anggota = db.anggotaDao().semuaAnggota().first(),
                        absensi = db.absensiDao().semuaAbsensi().first(),
                        proker = db.prokerDao().semuaProker().first(),
                        surat = db.suratDao().semuaSurat().first(),
                        inventaris = db.inventarisDao().semuaInventaris().first(),
                    )
                }
                when (hasil) {
                    is HasilAuth.Sukses -> {
                        prefs.edit().putBoolean(kunci(), true).apply()
                        _dataLama.value = null
                        _pesan.value = "${hasil.data} data dari HP ini sudah masuk ke desa kamu. Data asli di HP tidak dihapus."
                    }
                    is HasilAuth.Gagal -> _pesan.value = hasil.pesan
                }
            } finally {
                _sedangUnggah.value = false
            }
        }
    }

    fun lewatiDataLama() {
        prefs.edit().putBoolean(kunci(), true).apply()
        _dataLama.value = null
    }

    fun tutupPesan() { _pesan.value = null }
}

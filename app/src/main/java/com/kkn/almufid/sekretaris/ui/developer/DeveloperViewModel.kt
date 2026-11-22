package com.kkn.almufid.sekretaris.ui.developer

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kkn.almufid.sekretaris.data.auth.AuthRepository
import com.kkn.almufid.sekretaris.data.auth.HasilAuth
import com.kkn.almufid.sekretaris.data.auth.ProfilPengguna
import com.kkn.almufid.sekretaris.data.cloud.JurnalBersamaRepository
import com.kkn.almufid.sekretaris.data.cloud.KoordinasiRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class StatusDeveloper(
    val sedangMuat: Boolean = true,
    val admin: Boolean = false,
    val daftar: List<ProfilPengguna> = emptyList(),
    val totalPendaftaran: Long = 0L,
)

class DeveloperViewModel(
    private val repo: AuthRepository,
    koordinasi: KoordinasiRepository,
    private val bersama: JurnalBersamaRepository,
) : ViewModel() {

    private val _status = MutableStateFlow(StatusDeveloper())
    val status: StateFlow<StatusDeveloper> = _status.asStateFlow()

    // Tiap aliran diberi nilai awal kosong (onStart) supaya satu koleksi yang
    // ditolak server / belum punya aturan tidak membuat seluruh data macet.
    private val bagianA = combine(
        bersama.alirSemua().onStart { emit(emptyList()) },
        koordinasi.alirPengumuman().onStart { emit(emptyList()) },
        koordinasi.alirPolling().onStart { emit(emptyList()) },
    ) { j, p, l -> Triple(j, p, l) }

    private val bagianB = combine(
        koordinasi.alirTugas().onStart { emit(emptyList()) },
        koordinasi.alirKas().onStart { emit(emptyList()) },
        koordinasi.alirSaran().onStart { emit(emptyList()) },
    ) { t, k, s -> Triple(t, k, s) }

    /** Seluruh data cloud (real-time) + daftar profil, dipakai One Eye, ekspor, dan beranda developer. */
    val data: StateFlow<DataDeveloper> = combine(bagianA, bagianB, _status) { a, b, st ->
        DataDeveloper(st.daftar, a.first, a.second, a.third, b.first, b.second, b.third)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DataDeveloper())

    private val _pesan = MutableStateFlow<String?>(null)
    val pesan: StateFlow<String?> = _pesan.asStateFlow()

    private val _sedangEkspor = MutableStateFlow(false)
    val sedangEkspor: StateFlow<Boolean> = _sedangEkspor.asStateFlow()

    val uidSaya: String? get() = repo.uidSaatIni

    init { muat() }

    fun muat() {
        val uid = repo.uidSaatIni
        _status.value = _status.value.copy(sedangMuat = true)
        viewModelScope.launch {
            val admin = uid != null && repo.apakahAdmin(uid)
            val total = repo.ambilTotalPendaftaran()
            val daftar = when (val h = repo.ambilSemuaProfil()) {
                is HasilAuth.Sukses -> h.data
                is HasilAuth.Gagal -> { _pesan.value = h.pesan; emptyList() }
            }
            _status.value = StatusDeveloper(false, admin, daftar, total)
        }
    }

    fun aturBlokir(uid: String, blokir: Boolean) {
        viewModelScope.launch {
            when (val h = repo.aturBlokir(uid, blokir)) {
                is HasilAuth.Sukses -> muat()
                is HasilAuth.Gagal -> _pesan.value = h.pesan
            }
        }
    }

    fun resetSandi(email: String) {
        viewModelScope.launch {
            when (val h = repo.kirimResetSandiKe(email)) {
                is HasilAuth.Sukses -> _pesan.value = "Email untuk membuat sandi baru sudah dikirim ke ${h.data}."
                is HasilAuth.Gagal -> _pesan.value = h.pesan
            }
        }
    }

    /** Moderasi: menyembunyikan jurnal bersama milik siapa pun (ditandai dihapus, bukan dihapus permanen). */
    fun sembunyikanJurnal(id: String) {
        viewModelScope.launch {
            val h = bersama.hapus(id)
            if (h is HasilAuth.Gagal) {
                _pesan.value = "Gagal menyembunyikan jurnal. Pastikan aturan Firestore terbaru sudah dipasang dan akun ini terdaftar sebagai admin."
            }
        }
    }

    fun ekspor(context: Context, jenis: JenisEkspor, onSiap: (File, String) -> Unit) {
        if (_sedangEkspor.value) return
        _sedangEkspor.value = true
        val appContext = context.applicationContext
        val paket = data.value
        viewModelScope.launch {
            try {
                val file = withContext(Dispatchers.IO) { buatBerkasEkspor(appContext, jenis, paket) }
                onSiap(file, jenis.mime)
            } catch (e: Exception) {
                _pesan.value = "Gagal membuat berkas: ${e.message ?: "kesalahan tidak diketahui"}"
            } finally {
                _sedangEkspor.value = false
            }
        }
    }

    fun tutupPesan() { _pesan.value = null }
}

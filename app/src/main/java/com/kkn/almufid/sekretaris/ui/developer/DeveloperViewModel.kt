package com.kkn.almufid.sekretaris.ui.developer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kkn.almufid.sekretaris.data.auth.AuthRepository
import com.kkn.almufid.sekretaris.data.auth.HasilAuth
import com.kkn.almufid.sekretaris.data.auth.ProfilPengguna
import com.kkn.almufid.sekretaris.data.cloud.JurnalBersama
import com.kkn.almufid.sekretaris.data.cloud.JurnalBersamaRepository
import com.kkn.almufid.sekretaris.data.cloud.Kas
import com.kkn.almufid.sekretaris.data.cloud.KoordinasiRepository
import com.kkn.almufid.sekretaris.data.cloud.Tugas
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class StatusDeveloper(
    val sedangMuat: Boolean = true,
    val admin: Boolean = false,
    val daftar: List<ProfilPengguna> = emptyList(),
    val totalPendaftaran: Long = 0L,
)

class DeveloperViewModel(
    private val repo: AuthRepository,
    koordinasi: KoordinasiRepository,
    bersama: JurnalBersamaRepository,
) : ViewModel() {

    // Ringkasan dashboard (real-time dari Firestore).
    val jurnalBersama: StateFlow<List<JurnalBersama>> = bersama.alirSemua()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val tugas: StateFlow<List<Tugas>> = koordinasi.alirTugas()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val kas: StateFlow<List<Kas>> = koordinasi.alirKas()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())


    private val _status = MutableStateFlow(StatusDeveloper())
    val status: StateFlow<StatusDeveloper> = _status.asStateFlow()

    private val _pesan = MutableStateFlow<String?>(null)
    val pesan: StateFlow<String?> = _pesan.asStateFlow()

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

    fun tutupPesan() { _pesan.value = null }
}

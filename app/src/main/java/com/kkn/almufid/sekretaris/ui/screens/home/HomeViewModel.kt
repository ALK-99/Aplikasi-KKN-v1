package com.kkn.almufid.sekretaris.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kkn.almufid.sekretaris.data.auth.AuthRepository
import com.kkn.almufid.sekretaris.data.cloud.KoordinasiRepository
import com.kkn.almufid.sekretaris.data.cloud.Pengumuman
import com.kkn.almufid.sekretaris.data.dao.AnggotaDao
import com.kkn.almufid.sekretaris.data.dao.ProkerDao
import com.kkn.almufid.sekretaris.data.entity.Anggota
import com.kkn.almufid.sekretaris.data.entity.StatusProker
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class RingkasanHome(
    val prokerSelesai: Int = 0,
    val prokerTotal: Int = 0,
    val anggota: List<Anggota> = emptyList()
)

class HomeViewModel(
    prokerDao: ProkerDao,
    anggotaDao: AnggotaDao,
    koordinasi: KoordinasiRepository,
    private val auth: AuthRepository,
) : ViewModel() {
    val ringkasan: StateFlow<RingkasanHome> = combine(
        prokerDao.semuaProker(), anggotaDao.semuaAnggota()
    ) { proker, anggota ->
        RingkasanHome(
            prokerSelesai = proker.count { it.status == StatusProker.SELESAI },
            prokerTotal = proker.count { it.status != StatusProker.BATAL },
            anggota = anggota
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RingkasanHome())

    val uidSaya: String get() = auth.uidSaatIni ?: ""

    /** Pengumuman terbaru (daftar sudah diurutkan terbaru dulu oleh repository). */
    val pengumumanTerbaru: StateFlow<Pengumuman?> = koordinasi.alirPengumuman()
        .map { it.firstOrNull() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
}

package com.kkn.almufid.sekretaris.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kkn.almufid.sekretaris.data.dao.AnggotaDao
import com.kkn.almufid.sekretaris.data.dao.NotaDao
import com.kkn.almufid.sekretaris.data.dao.ProkerDao
import com.kkn.almufid.sekretaris.data.entity.Anggota
import com.kkn.almufid.sekretaris.data.entity.StatusProker
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class RingkasanHome(
    val prokerSelesai: Int = 0,
    val prokerTotal: Int = 0,
    val anggota: List<Anggota> = emptyList()
)

class HomeViewModel(prokerDao: ProkerDao, anggotaDao: AnggotaDao) : ViewModel() {
    val ringkasan: StateFlow<RingkasanHome> = combine(
        prokerDao.semuaProker(), anggotaDao.semuaAnggota()
    ) { proker, anggota ->
        RingkasanHome(
            prokerSelesai = proker.count { it.status == StatusProker.SELESAI },
            prokerTotal = proker.count { it.status != StatusProker.BATAL },
            anggota = anggota
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RingkasanHome())
}

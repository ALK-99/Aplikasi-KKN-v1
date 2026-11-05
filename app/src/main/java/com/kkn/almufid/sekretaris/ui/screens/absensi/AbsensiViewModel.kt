package com.kkn.almufid.sekretaris.ui.screens.absensi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kkn.almufid.sekretaris.data.dao.AbsensiDao
import com.kkn.almufid.sekretaris.data.dao.AnggotaDao
import com.kkn.almufid.sekretaris.data.entity.AbsensiEntry
import com.kkn.almufid.sekretaris.data.entity.Anggota
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

/** Membulatkan sebuah waktu ke jam 00:00 di hari yang sama, dipakai
 * sebagai kunci tanggal yang konsisten untuk tabel absensi. */
fun awalHari(millis: Long): Long {
    val kalender = Calendar.getInstance()
    kalender.timeInMillis = millis
    kalender.set(Calendar.HOUR_OF_DAY, 0)
    kalender.set(Calendar.MINUTE, 0)
    kalender.set(Calendar.SECOND, 0)
    kalender.set(Calendar.MILLISECOND, 0)
    return kalender.timeInMillis
}

class AbsensiViewModel(
    private val anggotaDao: AnggotaDao,
    private val absensiDao: AbsensiDao
) : ViewModel() {

    private val _tanggalDipilih = MutableStateFlow(awalHari(System.currentTimeMillis()))
    val tanggalDipilih: StateFlow<Long> = _tanggalDipilih

    val daftarAnggota: StateFlow<List<Anggota>> = anggotaDao.semuaAnggota()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val absensiHariIni: StateFlow<List<AbsensiEntry>> = _tanggalDipilih
        .flatMapLatest { tanggal -> absensiDao.absensiPadaTanggal(tanggal) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun pilihTanggal(millis: Long) {
        _tanggalDipilih.value = awalHari(millis)
    }

    fun tambahAnggota(nama: String, divisi: String) {
        if (nama.isBlank()) return
        viewModelScope.launch { anggotaDao.tambah(Anggota(nama = nama.trim(), divisi = divisi.trim())) }
    }

    fun tandaiKehadiran(anggotaId: Long, hadir: Boolean, keterangan: String = "") {
        viewModelScope.launch {
            absensiDao.simpan(
                AbsensiEntry(
                    anggotaId = anggotaId,
                    tanggalMillis = _tanggalDipilih.value,
                    hadir = hadir,
                    keterangan = keterangan
                )
            )
        }
    }
}

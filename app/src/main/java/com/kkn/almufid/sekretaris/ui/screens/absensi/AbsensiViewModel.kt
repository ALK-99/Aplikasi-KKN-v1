package com.kkn.almufid.sekretaris.ui.screens.absensi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kkn.almufid.sekretaris.data.auth.HasilAuth
import com.kkn.almufid.sekretaris.data.cloud.AbsensiDesa
import com.kkn.almufid.sekretaris.data.cloud.AnggotaDesa
import com.kkn.almufid.sekretaris.data.cloud.DesaRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

/** Membulatkan sebuah waktu ke jam 00:00 di hari yang sama, dipakai
 * sebagai kunci tanggal yang konsisten untuk data absensi. */
fun awalHari(millis: Long): Long {
    val kalender = Calendar.getInstance()
    kalender.timeInMillis = millis
    kalender.set(Calendar.HOUR_OF_DAY, 0)
    kalender.set(Calendar.MINUTE, 0)
    kalender.set(Calendar.SECOND, 0)
    kalender.set(Calendar.MILLISECOND, 0)
    return kalender.timeInMillis
}

class AbsensiViewModel(private val desa: DesaRepository) : ViewModel() {

    private val _tanggalDipilih = MutableStateFlow(awalHari(System.currentTimeMillis()))
    val tanggalDipilih: StateFlow<Long> = _tanggalDipilih

    private val _pesan = MutableStateFlow<String?>(null)
    val pesan: StateFlow<String?> = _pesan.asStateFlow()

    /** Semua anggota desa, aktif maupun nonaktif. */
    val semuaAnggota: StateFlow<List<AnggotaDesa>> = desa.alirAnggota()
        .map { daftar -> daftar.sortedBy { it.nama.lowercase() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val absensiHariIni: StateFlow<List<AbsensiDesa>> = _tanggalDipilih
        .flatMapLatest { tanggal -> desa.alirAbsensi(tanggal) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Anggota yang tampil pada tanggal terpilih: semua yang aktif, ditambah anggota
     * nonaktif yang punya catatan di tanggal itu (supaya riwayat lama tidak hilang).
     */
    val daftarAnggota: StateFlow<List<AnggotaDesa>> = combine(semuaAnggota, absensiHariIni) { anggota, absensi ->
        anggota.filter { a -> a.aktif || absensi.any { it.anggotaId == a.id } }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun pilihTanggal(millis: Long) {
        _tanggalDipilih.value = awalHari(millis)
    }

    private fun lapor(hasil: HasilAuth<Unit>) {
        if (hasil is HasilAuth.Gagal) _pesan.value = hasil.pesan
    }

    fun tambahAnggota(nama: String, divisi: String) {
        if (nama.isBlank()) return
        viewModelScope.launch { lapor(desa.tambahAnggota(nama, divisi)) }
    }

    fun ubahAnggota(anggota: AnggotaDesa, nama: String, divisi: String) {
        if (nama.isBlank()) return
        viewModelScope.launch { lapor(desa.ubahAnggota(anggota, nama, divisi)) }
    }

    fun aturAktif(anggota: AnggotaDesa, aktif: Boolean) {
        viewModelScope.launch { lapor(desa.aturAnggotaAktif(anggota, aktif)) }
    }

    fun tandaiKehadiran(anggota: AnggotaDesa, hadir: Boolean, keterangan: String = "") {
        val tanggal = _tanggalDipilih.value
        val sudahAda = absensiHariIni.value.any { it.anggotaId == anggota.id }
        viewModelScope.launch { lapor(desa.simpanAbsensi(anggota, tanggal, hadir, keterangan, sudahAda)) }
    }

    fun tutupPesan() { _pesan.value = null }
}

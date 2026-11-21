package com.kkn.almufid.sekretaris.ui.screens.jurnal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kkn.almufid.sekretaris.data.auth.AuthRepository
import com.kkn.almufid.sekretaris.data.auth.HasilAuth
import com.kkn.almufid.sekretaris.data.auth.ProfilPengguna
import com.kkn.almufid.sekretaris.data.cloud.JurnalBersama
import com.kkn.almufid.sekretaris.data.cloud.JurnalBersamaRepository
import com.kkn.almufid.sekretaris.data.dao.JurnalDao
import com.kkn.almufid.sekretaris.data.entity.JenisJurnal
import com.kkn.almufid.sekretaris.data.entity.JurnalEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class JurnalViewModel(
    private val dao: JurnalDao,
    private val bersama: JurnalBersamaRepository,
    private val auth: AuthRepository,
) : ViewModel() {

    /** Jurnal Pribadi: hanya di HP ini (Room). */
    val daftarJurnal: StateFlow<List<JurnalEntry>> = dao.semuaJurnal()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Jurnal Bersama: real-time dari Firestore, tampil di semua pengguna. */
    val daftarBersama: StateFlow<List<JurnalBersama>> = bersama.alirSemua()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val uidSaya: String? get() = auth.uidSaatIni

    private var profil: ProfilPengguna? = null

    private val _pesan = MutableStateFlow<String?>(null)
    val pesan: StateFlow<String?> = _pesan.asStateFlow()

    init {
        val uid = auth.uidSaatIni
        if (uid != null) viewModelScope.launch { profil = auth.ambilProfil(uid) }
    }

    fun tutupPesan() { _pesan.value = null }

    fun tambahJurnal(jenis: JenisJurnal, judul: String, isi: String, peserta: String = "") {
        if (judul.isBlank() || isi.isBlank()) return
        viewModelScope.launch {
            dao.tambah(
                JurnalEntry(
                    tanggalMillis = System.currentTimeMillis(),
                    jenis = jenis,
                    judul = judul.trim(),
                    isi = isi.trim(),
                    pesertaRapat = peserta.trim()
                )
            )
        }
    }

    fun hapusJurnal(entry: JurnalEntry) {
        viewModelScope.launch { dao.hapus(entry) }
    }

    fun tambahBersama(jenis: JenisJurnal, judul: String, isi: String, peserta: String = "") {
        if (judul.isBlank() || isi.isBlank()) return
        val uid = auth.uidSaatIni ?: run { _pesan.value = "Silakan login ulang dulu"; return }
        viewModelScope.launch {
            val p = profil ?: auth.ambilProfil(uid).also { profil = it }
            val hasil = bersama.tambah(
                uid = uid,
                nama = p?.namaLengkap ?: "Anggota",
                desa = p?.desa ?: "",
                jenis = jenis.name,
                judul = judul.trim(),
                isi = isi.trim(),
                peserta = peserta.trim(),
            )
            if (hasil is HasilAuth.Gagal) _pesan.value = hasil.pesan
        }
    }

    fun ubahBersama(id: String, judul: String, isi: String, peserta: String) {
        if (judul.isBlank() || isi.isBlank()) return
        viewModelScope.launch {
            val hasil = bersama.ubah(id, judul.trim(), isi.trim(), peserta.trim())
            if (hasil is HasilAuth.Gagal) _pesan.value = hasil.pesan
        }
    }

    fun hapusBersama(id: String) {
        viewModelScope.launch {
            val hasil = bersama.hapus(id)
            if (hasil is HasilAuth.Gagal) _pesan.value = hasil.pesan
        }
    }
}

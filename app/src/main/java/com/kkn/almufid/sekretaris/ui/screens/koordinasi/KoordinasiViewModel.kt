package com.kkn.almufid.sekretaris.ui.screens.koordinasi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kkn.almufid.sekretaris.data.auth.AuthRepository
import com.kkn.almufid.sekretaris.data.auth.HasilAuth
import com.kkn.almufid.sekretaris.data.auth.ProfilPengguna
import com.kkn.almufid.sekretaris.data.cloud.Kas
import com.kkn.almufid.sekretaris.data.cloud.KoordinasiRepository
import com.kkn.almufid.sekretaris.data.cloud.Pengumuman
import com.kkn.almufid.sekretaris.data.cloud.Polling
import com.kkn.almufid.sekretaris.data.cloud.Tugas
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Satu ViewModel dipakai empat layar (Pengumuman, Polling, Tugas, Kas); tiap layar memakai bagiannya. */
class KoordinasiViewModel(
    private val repo: KoordinasiRepository,
    private val auth: AuthRepository,
) : ViewModel() {

    val pengumuman: StateFlow<List<Pengumuman>> = repo.alirPengumuman()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val polling: StateFlow<List<Polling>> = repo.alirPolling()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val tugas: StateFlow<List<Tugas>> = repo.alirTugas()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val kas: StateFlow<List<Kas>> = repo.alirKas()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _admin = MutableStateFlow(false)
    val admin: StateFlow<Boolean> = _admin.asStateFlow()

    private val _anggota = MutableStateFlow<List<ProfilPengguna>>(emptyList())
    val anggota: StateFlow<List<ProfilPengguna>> = _anggota.asStateFlow()

    private val _pesan = MutableStateFlow<String?>(null)
    val pesan: StateFlow<String?> = _pesan.asStateFlow()

    val uidSaya: String get() = auth.uidSaatIni ?: ""
    private var namaSaya = "Admin"

    init {
        val uid = auth.uidSaatIni
        if (uid != null) viewModelScope.launch {
            // MODE DEMO: semua anggota boleh membuat pengumuman, polling, tugas, dan kas (sesuai aturan Firestore demo).
            _admin.value = MODE_DEMO || auth.apakahAdmin(uid)
            auth.ambilProfil(uid)?.let { namaSaya = it.namaLengkap }
            if (_admin.value) {
                val h = auth.ambilSemuaProfil()
                if (h is HasilAuth.Sukses) _anggota.value = h.data.filter { !it.akunDiblokir }
            }
        }
    }

    fun tutupPesan() { _pesan.value = null }

    private fun kerjakan(aksi: suspend () -> HasilAuth<Unit>) {
        viewModelScope.launch {
            val h = aksi()
            if (h is HasilAuth.Gagal) _pesan.value = h.pesan
        }
    }

    fun tambahPengumuman(judul: String, isi: String) {
        if (judul.isBlank() || isi.isBlank()) return
        kerjakan { repo.tambahPengumuman(namaSaya, judul.trim(), isi.trim()) }
    }
    fun tandaiDibaca(id: String) { kerjakan { repo.tandaiDibaca(id, uidSaya) } }
    fun hapusPengumuman(id: String) { kerjakan { repo.hapusPengumuman(id) } }

    fun tambahPolling(pertanyaan: String, opsi: List<String>) {
        val bersih = opsi.map { it.trim() }.filter { it.isNotEmpty() }
        if (pertanyaan.isBlank() || bersih.size < 2) { _pesan.value = "Isi pertanyaan dan minimal 2 pilihan"; return }
        kerjakan { repo.tambahPolling(namaSaya, pertanyaan.trim(), bersih) }
    }
    fun beriSuara(id: String, indeks: Int) { kerjakan { repo.beriSuara(id, uidSaya, indeks) } }
    fun tutupPolling(id: String) { kerjakan { repo.tutupPolling(id) } }
    fun hapusPolling(id: String) { kerjakan { repo.hapusPolling(id) } }

    fun tambahTugas(judul: String, deskripsi: String, untuk: ProfilPengguna?, deadline: Long?) {
        if (judul.isBlank() || untuk == null || deadline == null) {
            _pesan.value = "Isi judul, pilih anggota, dan pilih tanggal tenggat"
            return
        }
        kerjakan { repo.tambahTugas(judul.trim(), deskripsi.trim(), untuk.uid, untuk.namaLengkap, deadline) }
    }
    fun tandaiSelesai(id: String, selesai: Boolean) { kerjakan { repo.tandaiSelesai(id, selesai) } }
    fun hapusTugas(id: String) { kerjakan { repo.hapusTugas(id) } }

    fun tambahKas(masuk: Boolean, jumlahTeks: String, kategori: String, keterangan: String) {
        val jumlah = jumlahTeks.filter { it.isDigit() }.toLongOrNull()
        if (jumlah == null || jumlah <= 0L) { _pesan.value = "Isi jumlah dengan angka lebih dari 0"; return }
        kerjakan { repo.tambahKas(namaSaya, masuk, jumlah, kategori.trim(), keterangan.trim()) }
    }
    fun hapusKas(id: String) { kerjakan { repo.hapusKas(id) } }
    fun ubahPengumuman(id: String, judul: String, isi: String) {
        if (judul.isBlank() || isi.isBlank()) { _pesan.value = "Judul dan isi tidak boleh kosong"; return }
        kerjakan { repo.ubahPengumuman(id, judul.trim(), isi.trim()) }
    }
    fun ubahPolling(id: String, pertanyaan: String, opsi: List<String>) {
        if (pertanyaan.isBlank() || opsi.any { it.isBlank() }) { _pesan.value = "Pertanyaan dan semua pilihan harus terisi"; return }
        kerjakan { repo.ubahPolling(id, pertanyaan.trim(), opsi.map { it.trim() }) }
    }
    fun ubahTugas(id: String, judul: String, deskripsi: String) {
        if (judul.isBlank()) { _pesan.value = "Judul tugas tidak boleh kosong"; return }
        kerjakan { repo.ubahTugas(id, judul.trim(), deskripsi.trim()) }
    }
    fun ubahKas(id: String, jumlahTeks: String, kategori: String, keterangan: String) {
        val jumlah = jumlahTeks.filter { it.isDigit() }.toLongOrNull()
        if (jumlah == null || jumlah <= 0L) { _pesan.value = "Isi jumlah dengan angka lebih dari 0"; return }
        kerjakan { repo.ubahKas(id, jumlah, kategori.trim(), keterangan.trim()) }
    }
}

/** Ubah ke false (dan kembalikan aturan Firestore) saat aplikasi dipakai sungguhan. */
const val MODE_DEMO = true

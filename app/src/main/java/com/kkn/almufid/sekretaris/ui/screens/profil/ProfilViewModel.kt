package com.kkn.almufid.sekretaris.ui.screens.profil

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kkn.almufid.sekretaris.data.auth.AuthRepository
import com.kkn.almufid.sekretaris.data.auth.HasilAuth
import com.kkn.almufid.sekretaris.data.auth.ProfilPengguna
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProfilViewModel(private val repo: AuthRepository) : ViewModel() {

    private val _profil = MutableStateFlow<ProfilPengguna?>(null)
    val profil: StateFlow<ProfilPengguna?> = _profil.asStateFlow()

    private val _sedangProses = MutableStateFlow(false)
    val sedangProses: StateFlow<Boolean> = _sedangProses.asStateFlow()

    private val _pesan = MutableStateFlow<String?>(null)
    val pesan: StateFlow<String?> = _pesan.asStateFlow()

    init { muat() }

    fun muat() {
        val uid = repo.uidSaatIni ?: return
        viewModelScope.launch { _profil.value = repo.ambilProfil(uid) }
    }

    fun simpan(nama: String, desa: String, divisi: String) {
        val uid = repo.uidSaatIni ?: return
        if (nama.isBlank()) {
            _pesan.value = "Nama lengkap wajib diisi"
            return
        }
        _sedangProses.value = true
        viewModelScope.launch {
            when (val hasil = repo.perbaruiProfil(uid, nama, desa, divisi)) {
                is HasilAuth.Sukses -> {
                    _pesan.value = "Profil berhasil disimpan"
                    muat()
                }
                is HasilAuth.Gagal -> _pesan.value = hasil.pesan
            }
            _sedangProses.value = false
        }
    }

    fun kirimGantiSandi() {
        _sedangProses.value = true
        viewModelScope.launch {
            when (val hasil = repo.kirimResetSandiAkunIni()) {
                is HasilAuth.Sukses -> _pesan.value =
                    "Tautan untuk membuat sandi baru sudah dikirim ke ${hasil.data}. Cek juga folder Spam."
                is HasilAuth.Gagal -> _pesan.value = hasil.pesan
            }
            _sedangProses.value = false
        }
    }

    fun tutupPesan() { _pesan.value = null }
}

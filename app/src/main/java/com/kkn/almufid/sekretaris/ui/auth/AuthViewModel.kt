package com.kkn.almufid.sekretaris.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kkn.almufid.sekretaris.data.auth.AuthRepository
import com.kkn.almufid.sekretaris.data.auth.HasilAuth
import com.kkn.almufid.sekretaris.data.auth.Peran
import com.kkn.almufid.sekretaris.data.local.BATAS_GAGAL_LOGIN
import com.kkn.almufid.sekretaris.data.local.SesiLokal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class TahapAuth { DAFTAR, VERIFIKASI, LOGIN }

/** Data profil yang dikumpulkan dari form Daftar, dibawa ke tahap Verifikasi. */
data class DataPendaftaran(
    val namaLengkap: String = "",
    val desa: String = "",
    val divisi: String = "",
    val emailGoogle: String = "",
    val nim: String = "",
    val peran: Peran = Peran.PESERTA,
)

class AuthViewModel(
    private val repo: AuthRepository,
    private val sesi: SesiLokal,
) : ViewModel() {

    private val _tahap = MutableStateFlow(TahapAuth.LOGIN)
    val tahap: StateFlow<TahapAuth> = _tahap.asStateFlow()

    private val _sedangProses = MutableStateFlow(false)
    val sedangProses: StateFlow<Boolean> = _sedangProses.asStateFlow()

    private val _pesanError = MutableStateFlow<String?>(null)
    val pesanError: StateFlow<String?> = _pesanError.asStateFlow()

    private val _popupSukses = MutableStateFlow(false)
    val popupSukses: StateFlow<Boolean> = _popupSukses.asStateFlow()

    private val _popupGagalLogin = MutableStateFlow(false)
    val popupGagalLogin: StateFlow<Boolean> = _popupGagalLogin.asStateFlow()

    private val _popupLimitLogin = MutableStateFlow(false)
    val popupLimitLogin: StateFlow<Boolean> = _popupLimitLogin.asStateFlow()

    private val _pesanInfo = MutableStateFlow<String?>(null)
    val pesanInfo: StateFlow<String?> = _pesanInfo.asStateFlow()

    var dataPendaftaran: DataPendaftaran = DataPendaftaran()
        private set

    val nimDiingat: String? get() = sesi.nimDiingat
    val ingatSayaDisimpan: Boolean get() = sesi.ingatSaya

    fun pindahTahap(t: TahapAuth) {
        _pesanError.value = null
        _tahap.value = t
    }

    fun tutupError() { _pesanError.value = null }
    fun tutupInfo() { _pesanInfo.value = null }

    /** "Lupa sandi?": kirim email reset ke alamat yang terdaftar untuk NIM ini. */
    fun kirimResetSandi(nim: String) {
        if (nim.isBlank()) {
            _pesanError.value = "Isi NIM dulu untuk reset sandi"
            return
        }
        _sedangProses.value = true
        viewModelScope.launch {
            val hasil = repo.kirimResetSandi(nim)
            _sedangProses.value = false
            when (hasil) {
                is HasilAuth.Sukses -> _pesanInfo.value =
                    "Tautan untuk membuat sandi baru sudah dikirim ke ${hasil.data}. " +
                        "Buka email itu (cek juga folder Spam), klik tautannya, lalu masuk dengan sandi baru."
                is HasilAuth.Gagal -> _pesanError.value = hasil.pesan
            }
        }
    }
    fun tutupPopupSukses() { _popupSukses.value = false }
    fun tutupPopupGagalLogin() { _popupGagalLogin.value = false }
    fun tutupPopupLimitLogin() { _popupLimitLogin.value = false }

    fun daftar(
        namaLengkap: String,
        desa: String,
        divisi: String,
        emailGoogle: String,
        nim: String,
        password: String,
        konfirmasiPassword: String,
        peran: Peran,
    ) {
        val validasi = validasiPendaftaran(namaLengkap, emailGoogle, nim, password, konfirmasiPassword)
        if (validasi != null) { _pesanError.value = validasi; return }

        _sedangProses.value = true
        viewModelScope.launch {
            val hasil = repo.daftar(namaLengkap, desa, divisi, emailGoogle, nim, password, peran)
            _sedangProses.value = false
            when (hasil) {
                is HasilAuth.Sukses -> {
                    dataPendaftaran = DataPendaftaran(namaLengkap, desa, divisi, emailGoogle, nim, peran)
                    _tahap.value = TahapAuth.VERIFIKASI
                }
                is HasilAuth.Gagal -> _pesanError.value = hasil.pesan
            }
        }
    }

    private fun validasiPendaftaran(
        nama: String, email: String, nim: String, password: String, konfirmasi: String,
    ): String? = when {
        nama.isBlank() -> "Nama lengkap wajib diisi"
        nim.isBlank() -> "NIM wajib diisi"
        !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> "Email Google tidak valid"
        password.length < 6 -> "Sandi minimal 6 karakter"
        password != konfirmasi -> "Konfirmasi sandi tidak cocok"
        else -> null
    }

    fun kirimUlangVerifikasi() {
        _sedangProses.value = true
        viewModelScope.launch {
            val hasil = repo.kirimUlangVerifikasi()
            _sedangProses.value = false
            if (hasil is HasilAuth.Gagal) _pesanError.value = hasil.pesan
        }
    }

    fun cekVerifikasi() {
        _sedangProses.value = true
        viewModelScope.launch {
            val terverifikasi = repo.cekSudahVerifikasi()
            _sedangProses.value = false
            if (terverifikasi) {
                repo.logout() // kembali bersih ke halaman Login, sesuai alur yang diminta
                _popupSukses.value = true
            } else {
                _pesanError.value = "Email belum diverifikasi. Buka email Anda dan klik tautan verifikasi dulu."
            }
        }
    }

    fun lanjutKeLoginSetelahSukses() {
        _popupSukses.value = false
        _tahap.value = TahapAuth.LOGIN
    }

    fun login(nim: String, password: String, ingatSaya: Boolean, onSukses: (Peran) -> Unit) {
        if (nim.isBlank() || password.isBlank()) {
            _pesanError.value = "NIM dan sandi wajib diisi"
            return
        }
        _sedangProses.value = true
        viewModelScope.launch {
            val hasil = repo.login(nim.trim(), password)
            _sedangProses.value = false
            when (hasil) {
                is HasilAuth.Sukses -> {
                    sesi.resetGagalLogin()
                    sesi.ingatSaya = ingatSaya
                    sesi.nimDiingat = if (ingatSaya) nim.trim() else null
                    onSukses(hasil.data)
                }
                is HasilAuth.Gagal -> {
                    val jumlahGagal = sesi.catatGagalLogin(nim.trim())
                    if (jumlahGagal >= BATAS_GAGAL_LOGIN) {
                        _popupLimitLogin.value = true
                    } else {
                        _popupGagalLogin.value = true
                    }
                }
            }
        }
    }
}

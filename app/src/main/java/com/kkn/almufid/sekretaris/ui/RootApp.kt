package com.kkn.almufid.sekretaris.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kkn.almufid.sekretaris.data.auth.AuthRepository
import com.kkn.almufid.sekretaris.data.auth.Peran
import com.kkn.almufid.sekretaris.data.local.SesiLokal
import com.kkn.almufid.sekretaris.ui.auth.AuthFlow
import com.kkn.almufid.sekretaris.ui.auth.AuthViewModel
import com.kkn.almufid.sekretaris.ui.developer.DeveloperLobbyScreen
import com.kkn.almufid.sekretaris.ui.developer.DeveloperViewModel
import com.kkn.almufid.sekretaris.ui.navigation.AppNav
import com.kkn.almufid.sekretaris.ui.theme.Ground
import com.kkn.almufid.sekretaris.ui.theme.TeksUtama
import com.kkn.almufid.sekretaris.ui.theme.VioletTerang

private sealed class StatusRoot {
    data object Memuat : StatusRoot()
    data object PerluAuth : StatusRoot()
    data class MasukSebagai(val peran: Peran) : StatusRoot()
}

/**
 * Gerbang paling atas aplikasi: memutuskan apakah pengguna melihat alur
 * Daftar/Verifikasi/Login, lobi peserta (fitur KKN seperti biasa), atau
 * lobi developer (masih coming soon), berdasarkan status Firebase Auth
 * + flag "ingat saya" yang tersimpan lokal.
 */
@Composable
fun RootApp() {
    val context = LocalContext.current
    val authRepo = remember { AuthRepository() }
    val sesi = remember { SesiLokal(context) }
    var status by remember { mutableStateOf<StatusRoot>(StatusRoot.Memuat) }

    LaunchedEffect(Unit) {
        status = tentukanStatusAwal(authRepo, sesi)
    }

    Surface(Modifier.fillMaxSize(), color = Ground, contentColor = TeksUtama) {
        when (val s = status) {
            StatusRoot.Memuat -> LayarMemuat()
            StatusRoot.PerluAuth -> {
                val authVm: AuthViewModel = viewModel(
                    factory = GenericViewModelFactory { AuthViewModel(authRepo, sesi) }
                )
                AuthFlow(authVm) { peran -> status = StatusRoot.MasukSebagai(peran) }
            }
            is StatusRoot.MasukSebagai -> when (s.peran) {
                Peran.PESERTA -> AppNav(onLogout = {
                    authRepo.logout()
                    status = StatusRoot.PerluAuth
                })
                Peran.DEVELOPER -> {
                    val devVm: DeveloperViewModel = viewModel(
                        factory = GenericViewModelFactory { DeveloperViewModel(authRepo) }
                    )
                    DeveloperLobbyScreen(devVm, onLogout = {
                        authRepo.logout()
                        status = StatusRoot.PerluAuth
                    })
                }
            }
        }
    }
}

private suspend fun tentukanStatusAwal(authRepo: AuthRepository, sesi: SesiLokal): StatusRoot {
    // "Ingat saya" tidak dicentang saat login terakhir -> jangan auto-login
    // walau Firebase Auth SDK sendiri masih menyimpan sesinya.
    if (authRepo.sudahLogin && !sesi.ingatSaya) {
        authRepo.logout()
    }
    if (!authRepo.sudahLogin) return StatusRoot.PerluAuth

    // Sudah login tapi email belum pernah diverifikasi (mis. app ditutup
    // di tengah alur daftar) -> paksa ulang lewat Login biasa daripada
    // menampilkan layar verifikasi tanpa konteks nama/desa yang tadi diisi.
    if (!authRepo.emailTerverifikasi) {
        authRepo.logout()
        return StatusRoot.PerluAuth
    }

    val uid = authRepo.uidSaatIni ?: return StatusRoot.PerluAuth
    val profil = authRepo.ambilProfil(uid)
    if (profil == null || profil.akunDiblokir) {
        authRepo.logout()
        return StatusRoot.PerluAuth
    }
    return StatusRoot.MasukSebagai(Peran.dariString(profil.peran))
}

@Composable
private fun LayarMemuat() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = VioletTerang)
    }
}

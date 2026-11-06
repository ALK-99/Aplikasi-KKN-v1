package com.kkn.almufid.sekretaris.ui.auth

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.kkn.almufid.sekretaris.data.auth.Peran

/** Mengatur tiga sub-halaman akun (Daftar, Verifikasi, Login) dalam satu alur. */
@Composable
fun AuthFlow(vm: AuthViewModel, onMasuk: (Peran) -> Unit) {
    val tahap by vm.tahap.collectAsState()
    AnimatedContent(
        targetState = tahap,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "auth"
    ) { t ->
        when (t) {
            TahapAuth.DAFTAR -> DaftarScreen(vm)
            TahapAuth.VERIFIKASI -> VerifikasiScreen(vm)
            TahapAuth.LOGIN -> LoginScreen(vm, onMasuk)
        }
    }
}

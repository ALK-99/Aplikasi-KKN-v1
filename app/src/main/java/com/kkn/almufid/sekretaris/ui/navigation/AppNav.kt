package com.kkn.almufid.sekretaris.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.kkn.almufid.sekretaris.SekretarisApp
import com.kkn.almufid.sekretaris.ui.GenericViewModelFactory
import com.kkn.almufid.sekretaris.ui.screens.absensi.AbsensiScreen
import com.kkn.almufid.sekretaris.ui.screens.absensi.AbsensiViewModel
import com.kkn.almufid.sekretaris.ui.screens.arsip.ArsipScreen
import com.kkn.almufid.sekretaris.ui.screens.arsip.NotaViewModel
import com.kkn.almufid.sekretaris.ui.screens.home.HomeScreen
import com.kkn.almufid.sekretaris.ui.screens.inventaris.InventarisScreen
import com.kkn.almufid.sekretaris.ui.screens.inventaris.InventarisViewModel
import com.kkn.almufid.sekretaris.ui.screens.jurnal.JurnalScreen
import com.kkn.almufid.sekretaris.ui.screens.jurnal.JurnalViewModel
import com.kkn.almufid.sekretaris.ui.screens.pengaturan.PengaturanScreen
import com.kkn.almufid.sekretaris.ui.screens.proker.ProkerScreen
import com.kkn.almufid.sekretaris.ui.screens.proker.ProkerViewModel
import com.kkn.almufid.sekretaris.ui.screens.surat.SuratScreen
import com.kkn.almufid.sekretaris.ui.screens.surat.SuratViewModel
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kkn.almufid.sekretaris.ui.components.tekanPegas
import com.kkn.almufid.sekretaris.ui.screens.home.HomeViewModel
import com.kkn.almufid.sekretaris.ui.theme.*

@Composable
fun AppNav(onLogout: () -> Unit = {}) {
    val navController = rememberNavController()
    val app = LocalContext.current.applicationContext as SekretarisApp
    val db = app.database

    val posisi by navController.currentBackStackEntryAsState()
    val rute = posisi?.destination?.route ?: Tujuan.RUTE_HOME
    val judul = Tujuan.entries.firstOrNull { it.rute == rute }?.judul

    Surface(Modifier.fillMaxSize(), color = Ground, contentColor = TeksUtama) {
        Column(Modifier.fillMaxSize()) {
            if (judul != null) HeaderHalaman(judul) { navController.popBackStack() }
            NavHost(
                navController = navController,
                startDestination = Tujuan.RUTE_HOME,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                enterTransition = { slideInHorizontally(tween(300)) { it / 6 } + fadeIn(tween(300)) },
                exitTransition = { fadeOut(tween(200)) },
                popEnterTransition = { fadeIn(tween(250)) },
                popExitTransition = { slideOutHorizontally(tween(250)) { it / 6 } + fadeOut(tween(250)) }
            ) {
                composable(Tujuan.RUTE_HOME) {
                    val vm: HomeViewModel = viewModel(factory = GenericViewModelFactory { HomeViewModel(db.prokerDao(), db.anggotaDao()) })
                    HomeScreen(vm, onBuka = { navController.navigate(it.rute) { launchSingleTop = true } })
                }
                composable(Tujuan.JURNAL.rute) {
                    val vm: JurnalViewModel = viewModel(factory = GenericViewModelFactory { JurnalViewModel(db.jurnalDao()) })
                    JurnalScreen(vm)
                }
                composable(Tujuan.ABSENSI.rute) {
                    val vm: AbsensiViewModel = viewModel(factory = GenericViewModelFactory { AbsensiViewModel(db.anggotaDao(), db.absensiDao()) })
                    AbsensiScreen(vm)
                }
                composable(Tujuan.SURAT.rute) {
                    val vm: SuratViewModel = viewModel(factory = GenericViewModelFactory { SuratViewModel(db.suratDao()) })
                    SuratScreen(vm)
                }
                composable(Tujuan.PROKER.rute) {
                    val vm: ProkerViewModel = viewModel(factory = GenericViewModelFactory { ProkerViewModel(db.prokerDao()) })
                    ProkerScreen(vm)
                }
                composable(Tujuan.ARSIP.rute) {
                    val vm: NotaViewModel = viewModel(factory = GenericViewModelFactory { NotaViewModel(db.notaDao(), app) })
                    ArsipScreen(vm)
                }
                composable(Tujuan.INVENTARIS.rute) {
                    val vm: InventarisViewModel = viewModel(factory = GenericViewModelFactory { InventarisViewModel(db.inventarisDao()) })
                    InventarisScreen(vm)
                }
                composable(Tujuan.PENGATURAN.rute) {
                    PengaturanScreen(onLogout = onLogout)
                }
            }
        }
    }
}

@Composable
private fun HeaderHalaman(judul: String, onKembali: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.tekanPegas(onKembali).size(44.dp).clip(CircleShape).background(Surface1),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali", tint = TeksUtama)
        }
        Spacer(Modifier.width(14.dp))
        Text(judul, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
    }
}

package com.kkn.almufid.sekretaris.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
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
import com.kkn.almufid.sekretaris.ui.screens.proker.ProkerScreen
import com.kkn.almufid.sekretaris.ui.screens.proker.ProkerViewModel
import com.kkn.almufid.sekretaris.ui.screens.surat.SuratScreen
import com.kkn.almufid.sekretaris.ui.screens.surat.SuratViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNav() {
    val navController = rememberNavController()
    val app = LocalContext.current.applicationContext as SekretarisApp
    val db = app.database

    val rutePosisi by navController.currentBackStackEntryAsState()
    val ruteSekarang = rutePosisi?.destination?.route ?: Tujuan.RUTE_HOME
    val judul = Tujuan.entries.firstOrNull { it.rute == ruteSekarang }?.judul ?: "Sekretaris KKN"
    val diHome = ruteSekarang == Tujuan.RUTE_HOME

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(judul) },
                navigationIcon = {
                    if (!diHome) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Tujuan.RUTE_HOME,
            modifier = Modifier.padding(padding)
        ) {
            composable(Tujuan.RUTE_HOME) {
                HomeScreen(onBuka = { navController.navigate(it.rute) { launchSingleTop = true } })
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
        }
    }
}

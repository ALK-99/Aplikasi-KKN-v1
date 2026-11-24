package com.kkn.almufid.sekretaris.ui.screens.dokumen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.kkn.almufid.sekretaris.data.cloud.DesaRepository
import com.kkn.almufid.sekretaris.ui.screens.laporan.LaporanScreen
import com.kkn.almufid.sekretaris.ui.screens.laporan.LaporanViewModel
import com.kkn.almufid.sekretaris.ui.screens.templat.TemplatSuratScreen

/** Satu tempat untuk semua pembuatan PDF: templat surat, laporan, rekap absensi, dan cadangan. */
@Composable
fun DokumenScreen(vm: LaporanViewModel, desa: DesaRepository) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = tab) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Templat Surat") })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Laporan & Cadangan") })
        }
        if (tab == 0) TemplatSuratScreen(desa) else LaporanScreen(vm)
    }
}

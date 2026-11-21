package com.kkn.almufid.sekretaris.ui.screens.laporan

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kkn.almufid.sekretaris.ui.auth.DialogInfo
import com.kkn.almufid.sekretaris.ui.components.KartuGelap
import com.kkn.almufid.sekretaris.ui.components.tekanPegas
import com.kkn.almufid.sekretaris.ui.theme.*

@Composable
fun LaporanScreen(vm: LaporanViewModel) {
    val context = LocalContext.current
    val sedangProses by vm.sedangProses.collectAsState()
    val pesan by vm.pesan.collectAsState()

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp).padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            "Pilih laporan, lalu bagikan ke WhatsApp, email, atau simpan ke Drive/penyimpanan HP.",
            fontSize = 12.sp, color = TeksRedup
        )
        if (sedangProses) Text("Menyiapkan berkas...", fontSize = 12.sp, color = Gold, fontWeight = FontWeight.Bold)
        JenisLaporan.entries.forEach { jenis ->
            KartuGelap(Modifier.fillMaxWidth().tekanPegas {
                if (!sedangProses) vm.buat(jenis) { file, mime -> PembuatLaporan.bagikan(context, file, mime) }
            }) {
                Text(jenis.judul, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(jenis.keterangan, fontSize = 12.sp, color = TeksRedup)
            }
        }
    }
    pesan?.let { DialogInfo(judul = "Info", pesan = it, onTutup = vm::tutupPesan) }
}

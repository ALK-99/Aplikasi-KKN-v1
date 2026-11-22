package com.kkn.almufid.sekretaris.ui.screens.laporan

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.kkn.almufid.sekretaris.ui.formatTanggal
import com.kkn.almufid.sekretaris.ui.theme.*

@Composable
fun LaporanScreen(vm: LaporanViewModel) {
    val context = LocalContext.current
    val sedangProses by vm.sedangProses.collectAsState()
    val pesan by vm.pesan.collectAsState()
    val pratinjau by vm.pratinjau.collectAsState()
    val terakhir by vm.cadanganTerakhir.collectAsState()
    val pilihBerkas = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.bacaCadangan(uri)
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp).padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            "Pilih laporan, lalu bagikan ke WhatsApp, email, atau simpan ke Drive/penyimpanan HP.",
            fontSize = 12.sp, color = TeksRedup
        )
        if (sedangProses) Text("Menyiapkan berkas...", fontSize = 12.sp, color = Gold, fontWeight = FontWeight.Bold)

        Text("Cadangan & Pemulihan", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
        KartuGelap(Modifier.fillMaxWidth().tekanPegas {
            if (!sedangProses) vm.buat(JenisLaporan.BACKUP_ZIP) { file, mime -> PembuatLaporan.bagikan(context, file, mime) }
        }) {
            Text(JenisLaporan.BACKUP_ZIP.judul, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(JenisLaporan.BACKUP_ZIP.keterangan, fontSize = 12.sp, color = TeksRedup)
            Text(
                if (terakhir > 0L) "Terakhir dibuat: ${formatTanggal(terakhir)}" else "Belum pernah dibuat di HP ini",
                fontSize = 12.sp, color = if (terakhir > 0L) Gold else TeksRedup
            )
        }
        KartuGelap(Modifier.fillMaxWidth().tekanPegas {
            if (!sedangProses) pilihBerkas.launch(arrayOf("application/zip", "application/octet-stream", "*/*"))
        }) {
            Text("Pulihkan dari Cadangan", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text("Pilih berkas ZIP cadangan. Kamu akan melihat ringkasannya dulu sebelum apa pun diubah", fontSize = 12.sp, color = TeksRedup)
        }

        Text("Laporan", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
        JenisLaporan.entries.filter { it != JenisLaporan.BACKUP_ZIP }.forEach { jenis ->
            KartuGelap(Modifier.fillMaxWidth().tekanPegas {
                if (!sedangProses) vm.buat(jenis) { file, mime -> PembuatLaporan.bagikan(context, file, mime) }
            }) {
                Text(jenis.judul, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(jenis.keterangan, fontSize = 12.sp, color = TeksRedup)
            }
        }
    }
    pratinjau?.let { p ->
        AlertDialog(
            onDismissRequest = vm::batalPulih,
            containerColor = Surface1,
            titleContentColor = TeksUtama,
            textContentColor = TeksSoft,
            title = { Text("Pulihkan cadangan ini?", fontWeight = FontWeight.ExtraBold) },
            text = {
                Text(
                    "Dibuat: ${formatTanggal(p.dibuat)}\n" +
                        (if (p.desaNama.isNotBlank()) "Desa: ${p.desaNama}\n" else "") +
                        "Jurnal pribadi: ${p.jurnal}\nNota: ${p.nota} (foto ${p.foto})\nData desa: ${p.dataDesa}\n\n" +
                        "Data di HP yang sudah ada dilewati, jadi tidak ganda. Data desa dengan ID yang sama " +
                        "dikembalikan ke isi cadangan (perubahan sesudah cadangan dibuat pada data itu tertimpa). " +
                        "Data yang dibuat setelah cadangan tidak dihapus."
                )
            },
            confirmButton = {
                TextButton(onClick = vm::pulihkan) { Text("Pulihkan", color = VioletTerang, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = vm::batalPulih) { Text("Batal", color = TeksRedup) } }
        )
    }
    pesan?.let { DialogInfo(judul = "Info", pesan = it, onTutup = vm::tutupPesan) }
}

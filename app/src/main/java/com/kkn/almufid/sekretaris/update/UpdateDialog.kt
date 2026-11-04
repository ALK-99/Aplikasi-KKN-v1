package com.kkn.almufid.sekretaris.update

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/** Pop-up update. Dipasang sekali di MainActivity; muncul sendiri saat ada versi baru. */
@Composable
fun UpdateDialog(viewModel: UpdateViewModel) {
    val status by viewModel.status.collectAsState()
    val context = LocalContext.current

    when (val s = status) {
        StatusUpdate.Tidak -> Unit

        is StatusUpdate.Tersedia -> AlertDialog(
            onDismissRequest = viewModel::tutup,
            title = { Text("Update tersedia: v${s.info.versiBaru}") },
            text = { Text(s.info.catatan.ifBlank { "Perbaikan dan peningkatan aplikasi." }) },
            confirmButton = { TextButton(onClick = { viewModel.mulaiUnduh(s.info) }) { Text("Unduh & Pasang") } },
            dismissButton = { TextButton(onClick = viewModel::tutup) { Text("Nanti") } }
        )

        is StatusUpdate.Mengunduh -> AlertDialog(
            onDismissRequest = {},
            title = { Text("Mengunduh update") },
            text = {
                Column {
                    if (s.persen in 0..100) {
                        LinearProgressIndicator(progress = { s.persen / 100f }, modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(8.dp))
                        Text("${s.persen}%")
                    } else {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }
            },
            confirmButton = {}
        )

        is StatusUpdate.SiapPasang -> {
            // Satu kali: buka installer lalu tutup dialog. Data lokal tidak tersentuh.
            LaunchedEffect(s.apk) {
                val berhasil = UpdateChecker.pasang(context, s.apk)
                if (berhasil) viewModel.tutup() else viewModel.gagalIzin()
            }
        }

        is StatusUpdate.Gagal -> AlertDialog(
            onDismissRequest = viewModel::tutup,
            title = { Text("Update gagal") },
            text = { Text(s.pesan) },
            confirmButton = { TextButton(onClick = viewModel::tutup) { Text("Tutup") } }
        )
    }
}

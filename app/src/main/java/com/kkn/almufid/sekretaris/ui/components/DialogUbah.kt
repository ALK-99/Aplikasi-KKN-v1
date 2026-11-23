package com.kkn.almufid.sekretaris.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

/** Satu kolom isian pada [DialogUbah]. [angka] = hanya digit. */
class Bidang(val label: String, val awal: String, val angka: Boolean = false)

/** Dialog sederhana untuk memperbaiki data yang sudah ada. Nilai dikembalikan sesuai urutan [bidang]. */
@Composable
fun DialogUbah(judul: String, bidang: List<Bidang>, onTutup: () -> Unit, onSimpan: (List<String>) -> Unit) {
    val nilai = remember { mutableStateListOf<String>().apply { addAll(bidang.map { it.awal }) } }
    AlertDialog(
        onDismissRequest = onTutup,
        title = { Text(judul) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                bidang.forEachIndexed { i, b ->
                    OutlinedTextField(
                        value = nilai[i],
                        onValueChange = { t -> nilai[i] = if (b.angka) t.filter { it.isDigit() } else t },
                        label = { Text(b.label) },
                        singleLine = true,
                        keyboardOptions = if (b.angka) KeyboardOptions(keyboardType = KeyboardType.Number) else KeyboardOptions.Default,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = { onSimpan(nilai.toList()) }) { Text("Simpan") } },
        dismissButton = { TextButton(onClick = onTutup) { Text("Batal") } }
    )
}

package com.kkn.almufid.sekretaris.ui.screens.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kkn.almufid.sekretaris.ui.navigation.Tujuan

@Composable
fun HomeScreen(onBuka: (Tujuan) -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Menu Sekretaris", style = MaterialTheme.typography.titleLarge)
        Text(
            "Semua data tersimpan di HP ini dan bisa dipakai tanpa internet.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline
        )
        Spacer(Modifier.height(16.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(Tujuan.menuUtama, key = { it.rute }) { tujuan ->
                KartuMenu(judul = tujuan.judul, ikon = tujuan.ikon, onClick = { onBuka(tujuan) })
            }
        }
    }
}

@Composable
private fun KartuMenu(judul: String, ikon: ImageVector, onClick: () -> Unit) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth().height(110.dp).clickable(onClick = onClick)
    ) {
        Column(
            Modifier.fillMaxSize().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(ikon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
            Spacer(Modifier.height(8.dp))
            Text(judul, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        }
    }
}

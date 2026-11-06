package com.kkn.almufid.sekretaris.ui.developer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kkn.almufid.sekretaris.ui.components.tekanPegas
import com.kkn.almufid.sekretaris.ui.theme.*

/**
 * Lobi developer sengaja masih placeholder ("Masih coming soon" sesuai
 * permintaan — fokus pembaruan ini ada di alur daftar/login umum).
 * Begitu menu developer dirancang, tinggal ganti isi Column ini.
 */
@Composable
fun DeveloperLobbyScreen(onLogout: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(30.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            Modifier.size(90.dp).background(Surface2, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Construction, null, tint = Gold, modifier = Modifier.size(42.dp))
        }
        Spacer(Modifier.height(22.dp))
        Text("Lobi Developer", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(10.dp))
        Text(
            "Menu khusus developer (lebih kompleks & canggih) masih dalam " +
                "pengembangan. Login Anda sebagai developer sudah berhasil dan " +
                "tersimpan — menu ini akan diisi menyusul.",
            fontSize = 13.sp, color = TeksRedup, textAlign = TextAlign.Center, lineHeight = 19.sp
        )
        Spacer(Modifier.height(30.dp))
        Row(
            Modifier.tekanPegas(onLogout)
                .background(Surface1, RoundedCornerShape(14.dp))
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Logout, null, tint = Merah, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Keluar", color = Merah, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    }
}

package com.kkn.almufid.sekretaris.ui.developer

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.kkn.almufid.sekretaris.ui.components.KartuGelap
import com.kkn.almufid.sekretaris.ui.theme.TeksRedup

@Composable
internal fun KartuAngka(angka: String, label: String, modifier: Modifier = Modifier) {
    KartuGelap(modifier) {
        Text(angka, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(label, fontSize = 11.sp, color = TeksRedup, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
internal fun JudulBagian(teks: String, keterangan: String? = null) {
    Column {
        Text(teks, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
        if (keterangan != null) Text(keterangan, fontSize = 11.sp, color = TeksRedup)
    }
}

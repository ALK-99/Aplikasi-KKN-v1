package com.kkn.almufid.sekretaris.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kkn.almufid.sekretaris.ui.theme.*
import kotlinx.coroutines.delay

/** Klik dengan efek pegas: mengecil saat ditekan lalu memantul kembali. */
@Composable
fun Modifier.tekanPegas(onClick: () -> Unit): Modifier {
    val sumber = remember { MutableInteractionSource() }
    val ditekan by sumber.collectIsPressedAsState()
    val skala by animateFloatAsState(
        targetValue = if (ditekan) 0.95f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "tekan"
    )
    return this
        .graphicsLayer { scaleX = skala; scaleY = skala }
        .clickable(interactionSource = sumber, indication = null, onClick = onClick)
}

/** Muncul bertahap (naik + memudar masuk), jeda mengikuti urutan item. */
@Composable
fun Modifier.munculBertahap(indeks: Int): Modifier {
    val progres = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(minOf(indeks, 8) * 45L)
        progres.animateTo(1f, tween(380, easing = FastOutSlowInEasing))
    }
    return this.graphicsLayer {
        alpha = progres.value
        translationY = (1f - progres.value) * 40f
    }
}

fun inisialDari(nama: String): String =
    nama.trim().split(" ").filter { it.isNotEmpty() }.take(2)
        .joinToString("") { it.first().toString() }.ifEmpty { "?" }

@Composable
fun AvatarGradien(inisial: String, ukuran: Dp = 42.dp, modifier: Modifier = Modifier) {
    Box(
        modifier.size(ukuran).clip(CircleShape).background(Stabilo),
        contentAlignment = Alignment.Center
    ) {
        Text(
            inisial.take(2).uppercase(), color = TintaStabilo,
            fontWeight = FontWeight.ExtraBold, fontSize = (ukuran.value * 0.32f).sp
        )
    }
}

@Composable
fun KartuGelap(
    modifier: Modifier = Modifier,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable ColumnScope.() -> Unit
) {
    val bentuk = RoundedCornerShape(22.dp)
    Column(
        modifier
            .clip(bentuk).background(Surface1)
            .border(1.dp, Garis, bentuk)
            .padding(16.dp),
        verticalArrangement = verticalArrangement,
        content = content
    )
}

@Composable
fun FabGradien(ikon: ImageVector, deskripsi: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier.tekanPegas(onClick)
            .size(56.dp)
            .clip(CircleShape).background(Stabilo),
        contentAlignment = Alignment.Center
    ) {
        Icon(ikon, deskripsi, tint = TintaStabilo, modifier = Modifier.size(26.dp))
    }
}

@Composable
fun PilChip(teks: String, aktif: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val bentuk = RoundedCornerShape(50)
    val latar = if (aktif) Modifier.background(Stabilo)
        else Modifier.background(Surface1).border(1.dp, Garis, bentuk)
    Box(
        modifier.tekanPegas(onClick).clip(bentuk).then(latar)
            .padding(horizontal = 16.dp, vertical = 9.dp)
    ) {
        Text(
            teks, fontSize = 13.sp, fontWeight = FontWeight.Bold,
            color = if (aktif) TintaStabilo else TeksSoft
        )
    }
}

@Composable
fun PilKecil(teks: String, aktif: Boolean, warnaAktif: Color, onClick: () -> Unit) {
    Box(
        Modifier.tekanPegas(onClick).clip(RoundedCornerShape(50))
            .background(if (aktif) warnaAktif else Surface2)
            .padding(horizontal = 12.dp, vertical = 9.dp)
    ) {
        Text(
            teks, fontSize = 12.sp, fontWeight = FontWeight.Bold,
            color = if (aktif) Color.White else TeksSoft
        )
    }
}

@Composable
fun TagKecil(teks: String, warna: Color = VioletTerang, modifier: Modifier = Modifier) {
    Box(
        modifier.clip(RoundedCornerShape(50)).background(warna.copy(alpha = 0.18f))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(teks, color = warna, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun BatangProgres(
    fraksi: Float,
    modifier: Modifier = Modifier,
    latar: Color = Surface2,
    isi: Brush = GradUtama
) {
    Box(modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(50)).background(latar)) {
        Box(
            Modifier.fillMaxWidth(fraksi.coerceIn(0f, 1f)).fillMaxHeight()
                .clip(RoundedCornerShape(50)).background(isi)
        )
    }
}

@Composable
fun KosongState(teks: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(teks, color = TeksRedup, fontSize = 14.sp, textAlign = TextAlign.Center)
    }
}

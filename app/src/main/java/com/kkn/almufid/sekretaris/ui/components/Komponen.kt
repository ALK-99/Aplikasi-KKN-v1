package com.kkn.almufid.sekretaris.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.shadow
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
        modifier.size(ukuran).clip(CircleShape).background(GradStory),
        contentAlignment = Alignment.Center
    ) {
        Text(
            inisial.take(2).uppercase(), color = Color.White,
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
            .shadow(10.dp, bentuk, ambientColor = Color.Black, spotColor = Color.Black)
            .clip(bentuk).background(Surface1).padding(16.dp),
        verticalArrangement = verticalArrangement,
        content = content
    )
}

@Composable
fun FabGradien(ikon: ImageVector, deskripsi: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val transisi = rememberInfiniteTransition(label = "fab")
    val denyut by transisi.animateFloat(
        initialValue = 1f, targetValue = 1.07f,
        animationSpec = infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "denyut"
    )
    Box(
        modifier.tekanPegas(onClick)
            .graphicsLayer { scaleX = denyut; scaleY = denyut }
            .size(60.dp)
            .shadow(14.dp, CircleShape, ambientColor = Pink, spotColor = Pink)
            .clip(CircleShape).background(GradUtama),
        contentAlignment = Alignment.Center
    ) {
        Icon(ikon, deskripsi, tint = Color.White, modifier = Modifier.size(26.dp))
    }
}

@Composable
fun PilChip(teks: String, aktif: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val latar = if (aktif) Modifier.background(GradUtama) else Modifier.background(Surface1)
    Box(
        modifier.tekanPegas(onClick).clip(RoundedCornerShape(20.dp)).then(latar)
            .padding(horizontal = 16.dp, vertical = 9.dp)
    ) {
        Text(
            teks, fontSize = 13.sp, fontWeight = FontWeight.Bold,
            color = if (aktif) Color.White else TeksSoft
        )
    }
}

@Composable
fun PilKecil(teks: String, aktif: Boolean, warnaAktif: Color, onClick: () -> Unit) {
    Box(
        Modifier.tekanPegas(onClick).clip(RoundedCornerShape(14.dp))
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
        modifier.clip(RoundedCornerShape(10.dp)).background(warna.copy(alpha = 0.18f))
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
    Box(modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(latar)) {
        Box(
            Modifier.fillMaxWidth(fraksi.coerceIn(0f, 1f)).fillMaxHeight()
                .clip(RoundedCornerShape(4.dp)).background(isi)
        )
    }
}

@Composable
fun KosongState(teks: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(teks, color = TeksRedup, fontSize = 14.sp, textAlign = TextAlign.Center)
    }
}

package com.kkn.almufid.sekretaris.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.kkn.almufid.sekretaris.R

/** Satu keluarga huruf untuk seluruh app: Bricolage Grotesque (tiga bobot). */
val Bricolage = FontFamily(
    Font(R.font.bricolage_reguler, FontWeight.Normal),
    Font(R.font.bricolage_reguler, FontWeight.Medium),
    Font(R.font.bricolage_tebal, FontWeight.SemiBold),
    Font(R.font.bricolage_tebal, FontWeight.Bold),
    Font(R.font.bricolage_berat, FontWeight.ExtraBold),
    Font(R.font.bricolage_berat, FontWeight.Black),
)

private fun TextStyle.b() = copy(fontFamily = Bricolage)

val Typography = Typography().let { t ->
    t.copy(
        displayLarge = t.displayLarge.b(), displayMedium = t.displayMedium.b(), displaySmall = t.displaySmall.b(),
        headlineLarge = t.headlineLarge.b().copy(fontWeight = FontWeight.ExtraBold),
        headlineMedium = t.headlineMedium.b().copy(fontWeight = FontWeight.ExtraBold),
        headlineSmall = t.headlineSmall.b().copy(fontWeight = FontWeight.ExtraBold),
        titleLarge = t.titleLarge.b().copy(fontWeight = FontWeight.ExtraBold),
        titleMedium = t.titleMedium.b().copy(fontWeight = FontWeight.Bold),
        titleSmall = t.titleSmall.b().copy(fontWeight = FontWeight.Bold),
        bodyLarge = t.bodyLarge.b(), bodyMedium = t.bodyMedium.b(), bodySmall = t.bodySmall.b(),
        labelLarge = t.labelLarge.b().copy(fontWeight = FontWeight.Bold),
        labelMedium = t.labelMedium.b(), labelSmall = t.labelSmall.b(),
    )
}

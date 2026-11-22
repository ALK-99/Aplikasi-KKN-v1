package com.kkn.almufid.sekretaris.ui.theme

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Pengatur mode tampilan. [mode]: 0 = ikuti sistem, 1 = terang, 2 = gelap.
 * Warna di bawah dibaca lewat getter, jadi semua layar otomatis berganti
 * begitu mode berubah, tanpa perlu mengubah kode di tiap layar.
 */
object Palet {
    const val SISTEM = 0
    const val TERANG = 1
    const val GELAP = 2

    var mode by mutableIntStateOf(SISTEM)
    var sistemGelap by mutableStateOf(true)

    val gelap: Boolean get() = when (mode) {
        TERANG -> false
        GELAP -> true
        else -> sistemGelap
    }

    private const val PREF = "pengaturan_tampilan"

    fun muat(context: Context) {
        mode = try { context.getSharedPreferences(PREF, Context.MODE_PRIVATE).getInt("mode", SISTEM) } catch (e: Exception) { SISTEM }
    }

    fun atur(context: Context, baru: Int) {
        mode = baru
        try { context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putInt("mode", baru).apply() } catch (e: Exception) { }
    }
}

val Ground: Color get() = if (Palet.gelap) Color(0xFF0E1020) else Color(0xFFF2F3FA)
val Surface1: Color get() = if (Palet.gelap) Color(0xFF181B30) else Color(0xFFFFFFFF)
val Surface2: Color get() = if (Palet.gelap) Color(0xFF232846) else Color(0xFFE6E8F4)
val Violet = Color(0xFF5B4BDB)
val VioletTerang: Color get() = if (Palet.gelap) Color(0xFF8B7BFF) else Color(0xFF4A3BC4)
val Pink = Color(0xFFD92B6B)
val Gold: Color get() = if (Palet.gelap) Color(0xFFFFB84D) else Color(0xFF9A5B00)
val Mint: Color get() = if (Palet.gelap) Color(0xFF2BD4A0) else Color(0xFF0B8A66)
val HijauTua = Color(0xFF0B7A5C)
val Merah: Color get() = if (Palet.gelap) Color(0xFFFF6B6B) else Color(0xFFD13434)
val TeksUtama: Color get() = if (Palet.gelap) Color(0xFFF4F5FB) else Color(0xFF14172B)
val TeksSoft: Color get() = if (Palet.gelap) Color(0xFFC3C8E3) else Color(0xFF3D4260)
val TeksRedup: Color get() = if (Palet.gelap) Color(0xFF9AA0BF) else Color(0xFF5F6586)

val GradUtama = Brush.linearGradient(listOf(Violet, Pink))
val GradStory = Brush.linearGradient(listOf(Color(0xFFFFB84D), Pink, Violet))

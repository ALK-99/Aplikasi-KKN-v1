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

val Ground: Color get() = if (Palet.gelap) Color(0xFF0A1512) else Color(0xFFEEF3EF)
val Surface1: Color get() = if (Palet.gelap) Color(0xFF12211D) else Color(0xFFFFFFFF)
val Surface2: Color get() = if (Palet.gelap) Color(0xFF1B2F29) else Color(0xFFE4ECE6)
val Violet: Color get() = if (Palet.gelap) Color(0xFF2F7A60) else Color(0xFF0F5A47)
val VioletTerang: Color get() = if (Palet.gelap) Color(0xFF9BE7C4) else Color(0xFF0F5A47)
val Pink: Color get() = if (Palet.gelap) Color(0xFFFF8D72) else Color(0xFFE5553A)
val Gold: Color get() = if (Palet.gelap) Color(0xFFF0C75A) else Color(0xFF946000)
val Mint: Color get() = if (Palet.gelap) Color(0xFF6FDBA8) else Color(0xFF0F7A55)
val HijauTua = Color(0xFF0B7A5C)
val Merah: Color get() = if (Palet.gelap) Color(0xFFFF8D84) else Color(0xFFC9372C)
val TeksUtama: Color get() = if (Palet.gelap) Color(0xFFEAF4EE) else Color(0xFF10201C)
val TeksSoft: Color get() = if (Palet.gelap) Color(0xFFBDD0C6) else Color(0xFF3C4E47)
val TeksRedup: Color get() = if (Palet.gelap) Color(0xFF8FA89C) else Color(0xFF5A6C65)
/** Lime: warna sorot untuk status aktif dan tombol utama kecil. */
val Stabilo = Color(0xFFC6F24E)
val TintaStabilo = Color(0xFF0D2B26)
val Hutan: Color get() = if (Palet.gelap) Color(0xFF173128) else Color(0xFF0D2B26)
val BiruLangit = Color(0xFFCFE6FF)
val Kuning = Color(0xFFFFD66B)
val Garis: Color get() = if (Palet.gelap) Color(0xFF24382F) else Color(0xFFD6E1D9)

// Gradasi tidak dipakai lagi: nama dipertahankan agar semua layar tetap jalan, isinya warna rata.
val GradUtama: Brush get() = Brush.linearGradient(listOf(Violet, Violet))
val GradStory: Brush get() = Brush.linearGradient(listOf(Stabilo, Stabilo))

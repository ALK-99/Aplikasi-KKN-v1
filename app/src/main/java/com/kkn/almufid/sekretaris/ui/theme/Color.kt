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

val Ground: Color get() = if (Palet.gelap) Color(0xFF16211F) else Color(0xFFFBFCFF)
val Surface1: Color get() = if (Palet.gelap) Color(0xFF1B2825) else Color(0xFFFBFCFF)
val Surface2: Color get() = if (Palet.gelap) Color(0xFF1F2D2A) else Color(0xFFEEF2FB)
val Violet: Color get() = if (Palet.gelap) Color(0xFF3F63CC) else Color(0xFF1D3F9C)
val VioletTerang: Color get() = if (Palet.gelap) Color(0xFF8FB0FF) else Color(0xFF1D3F9C)
val Pink: Color get() = if (Palet.gelap) Color(0xFFFF8A7A) else Color(0xFFD9443B)
val Gold: Color get() = if (Palet.gelap) Color(0xFFF0C75A) else Color(0xFF8A5A00)
val Mint: Color get() = if (Palet.gelap) Color(0xFF6FDBA8) else Color(0xFF0B7A4E)
val HijauTua = Color(0xFF0B7A5C)
val Merah: Color get() = if (Palet.gelap) Color(0xFFFF8D84) else Color(0xFFC42F2F)
val TeksUtama: Color get() = if (Palet.gelap) Color(0xFFEDF3EE) else Color(0xFF1B2240)
val TeksSoft: Color get() = if (Palet.gelap) Color(0xFFC5D2C9) else Color(0xFF464F70)
val TeksRedup: Color get() = if (Palet.gelap) Color(0xFF9FB0A6) else Color(0xFF646E8C)
/** Stabilo kuning: satu-satunya warna mencolok, dipakai untuk status aktif. */
val Stabilo: Color get() = if (Palet.gelap) Color(0xFFE9D56A) else Color(0xFFFFE66B)
val TintaStabilo = Color(0xFF1B2240)
/** Garis tipis ala buku bergaris. */
val Garis: Color get() = if (Palet.gelap) Color(0xFF2F413D) else Color(0xFFD3DCF0)

// Gradasi dihapus: nama dipertahankan agar semua layar tetap jalan, isinya warna rata.
val GradUtama: Brush get() = Brush.linearGradient(listOf(Violet, Violet))
val GradStory: Brush get() = Brush.linearGradient(listOf(Stabilo, Stabilo))

package com.kkn.almufid.sekretaris.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream
import kotlin.math.max

/**
 * Foto nota/bukti disimpan sebagai JPEG kecil (base64) langsung di dokumen Firestore,
 * karena Firebase Storage butuh paket berbayar. Batas ~180 KB per foto, jauh di bawah
 * batas 1 MB per dokumen, dan cukup jelas untuk membaca nominal di nota.
 */
object FotoUtil {
    private const val SISI_MAKS = 1024
    private const val BATAS_BYTE = 180_000

    fun kompresKeBase64(sumber: Bitmap): String {
        val skala = SISI_MAKS.toFloat() / max(sumber.width, sumber.height)
        val bitmap = if (skala < 1f) {
            Bitmap.createScaledBitmap(
                sumber, (sumber.width * skala).toInt().coerceAtLeast(1),
                (sumber.height * skala).toInt().coerceAtLeast(1), true
            )
        } else sumber
        var mutu = 75
        var bytes: ByteArray
        do {
            val keluar = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, mutu, keluar)
            bytes = keluar.toByteArray()
            mutu -= 10
        } while (bytes.size > BATAS_BYTE && mutu >= 25)
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    fun dekode(base64: String): Bitmap? {
        if (base64.isBlank()) return null
        return try {
            val bytes = Base64.decode(base64, Base64.NO_WRAP)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (e: Exception) {
            null
        }
    }

    fun bacaDariUri(context: Context, uri: Uri): Bitmap? = try {
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
    } catch (e: Exception) {
        null
    }
}

package com.kkn.almufid.sekretaris.ui.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

private fun sudahDiizinkan(context: Context, izin: String): Boolean =
    ContextCompat.checkSelfPermission(context, izin) == PackageManager.PERMISSION_GRANTED

/**
 * Izin kamera untuk memotret dokumen/nota langsung dari dalam aplikasi
 * (poin B.1). Mengembalikan fungsi: panggil untuk membuka kamera — kalau
 * izin belum ada, dialog izin sistem muncul dulu; begitu pengguna
 * menyetujui, [onDiizinkan] langsung dipanggil otomatis.
 */
@Composable
fun rememberPemicuKamera(onDiizinkan: () -> Unit, onDitolak: () -> Unit = {}): () -> Unit {
    val context = LocalContext.current
    val peluncurIzin = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { diizinkan -> if (diizinkan) onDiizinkan() else onDitolak() }

    return {
        if (sudahDiizinkan(context, Manifest.permission.CAMERA)) {
            onDiizinkan()
        } else {
            peluncurIzin.launch(Manifest.permission.CAMERA)
        }
    }
}

/**
 * Izin galeri/folder foto (poin B.1). Pada Android 13+ sebenarnya Photo
 * Picker (PickVisualMedia) tidak wajib izin runtime, tapi kita tetap
 * memintanya secara eksplisit di sini supaya pengalaman izin konsisten
 * di semua versi Android dan sesuai permintaan: otorisasi akses galeri
 * diminta jelas ke pengguna sebelum dipakai.
 */
@Composable
fun rememberPemicuGaleri(onDiizinkan: () -> Unit, onDitolak: () -> Unit = {}): () -> Unit {
    val context = LocalContext.current
    val izin = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    val peluncurIzin = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { diizinkan -> if (diizinkan) onDiizinkan() else onDitolak() }

    return {
        if (sudahDiizinkan(context, izin)) {
            onDiizinkan()
        } else {
            peluncurIzin.launch(izin)
        }
    }
}

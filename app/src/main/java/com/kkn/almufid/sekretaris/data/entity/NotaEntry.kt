package com.kkn.almufid.sekretaris.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "nota_entries")
data class NotaEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tanggalMillis: Long,
    val keterangan: String,
    val nominal: Long,
    val prokerTerkait: String = "",
    // Path absolut ke file foto di internal storage (folder nota_photos/),
    // bukan isi gambar langsung. Simpan path, bukan Bitmap, agar
    // database tetap ringan dan cepat.
    val fotoPath: String? = null
)

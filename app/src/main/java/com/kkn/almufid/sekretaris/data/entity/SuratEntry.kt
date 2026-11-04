package com.kkn.almufid.sekretaris.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class JenisSurat { MASUK, KELUAR }

@Entity(tableName = "surat_entries")
data class SuratEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tanggalMillis: Long,
    val jenis: JenisSurat,
    val nomorSurat: String,
    val perihal: String,
    val tujuanAtauPengirim: String
)

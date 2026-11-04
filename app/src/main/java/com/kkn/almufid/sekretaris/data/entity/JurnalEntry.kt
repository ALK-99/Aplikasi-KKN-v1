package com.kkn.almufid.sekretaris.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class JenisJurnal { HARIAN, NOTULEN_RAPAT }

/**
 * Satu baris untuk jurnal kegiatan harian ATAU notulen rapat internal.
 * Dibedakan lewat [jenis] supaya keduanya bisa tampil terpisah di UI
 * tanpa perlu dua tabel berbeda (strukturnya memang mirip: tanggal,
 * judul singkat, isi, siapa yang hadir/terlibat).
 */
@Entity(tableName = "jurnal_entries")
data class JurnalEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tanggalMillis: Long,
    val jenis: JenisJurnal,
    val judul: String,
    val isi: String,
    val pesertaRapat: String = "" // kosong jika jenis = HARIAN
)

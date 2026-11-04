package com.kkn.almufid.sekretaris.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Satu baris = status kehadiran satu [anggotaId] pada satu [tanggalMillis].
 * Primary key GABUNGAN (anggotaId + tanggalMillis), sengaja TANPA id
 * autoGenerate terpisah. Alasannya: menandai ulang kehadiran anggota
 * yang sama di tanggal yang sama (misal user mengubah pikiran dari
 * "hadir" ke "izin") harus menimpa baris lama lewat
 * OnConflictStrategy.REPLACE di AbsensiDao.simpan(), bukan menambah
 * baris baru yang menyebabkan data ganda per anggota per hari.
 * ForeignKey CASCADE: kalau anggota dihapus, riwayat absensinya ikut hilang.
 */
@Entity(
    tableName = "absensi_entries",
    primaryKeys = ["anggotaId", "tanggalMillis"],
    foreignKeys = [
        ForeignKey(
            entity = Anggota::class,
            parentColumns = ["id"],
            childColumns = ["anggotaId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("anggotaId")]
)
data class AbsensiEntry(
    val anggotaId: Long,
    val tanggalMillis: Long,
    val hadir: Boolean,
    val keterangan: String = ""
)

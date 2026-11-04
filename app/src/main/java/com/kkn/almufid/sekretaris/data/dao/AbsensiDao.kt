package com.kkn.almufid.sekretaris.data.dao

import androidx.room.*
import com.kkn.almufid.sekretaris.data.entity.AbsensiEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface AbsensiDao {
    @Query("SELECT * FROM absensi_entries WHERE tanggalMillis = :tanggalMillis")
    fun absensiPadaTanggal(tanggalMillis: Long): Flow<List<AbsensiEntry>>

    @Query("SELECT * FROM absensi_entries ORDER BY tanggalMillis DESC")
    fun semuaAbsensi(): Flow<List<AbsensiEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun simpan(entry: AbsensiEntry): Long

    @Delete
    suspend fun hapus(entry: AbsensiEntry)
}

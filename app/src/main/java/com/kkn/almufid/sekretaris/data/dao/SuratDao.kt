package com.kkn.almufid.sekretaris.data.dao

import androidx.room.*
import com.kkn.almufid.sekretaris.data.entity.SuratEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface SuratDao {
    @Query("SELECT * FROM surat_entries ORDER BY tanggalMillis DESC")
    fun semuaSurat(): Flow<List<SuratEntry>>

    @Insert
    suspend fun tambah(entry: SuratEntry): Long

    @Delete
    suspend fun hapus(entry: SuratEntry)
}

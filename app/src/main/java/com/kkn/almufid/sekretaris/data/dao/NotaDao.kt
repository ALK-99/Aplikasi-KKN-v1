package com.kkn.almufid.sekretaris.data.dao

import androidx.room.*
import com.kkn.almufid.sekretaris.data.entity.NotaEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface NotaDao {
    @Query("SELECT * FROM nota_entries ORDER BY tanggalMillis DESC")
    fun semuaNota(): Flow<List<NotaEntry>>

    @Query("SELECT COALESCE(SUM(nominal), 0) FROM nota_entries")
    fun totalPengeluaran(): Flow<Long>

    @Insert
    suspend fun tambah(entry: NotaEntry): Long

    @Delete
    suspend fun hapus(entry: NotaEntry)
}

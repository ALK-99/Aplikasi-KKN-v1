package com.kkn.almufid.sekretaris.data.dao

import androidx.room.*
import com.kkn.almufid.sekretaris.data.entity.JurnalEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface JurnalDao {
    @Query("SELECT * FROM jurnal_entries ORDER BY tanggalMillis DESC")
    fun semuaJurnal(): Flow<List<JurnalEntry>>

    @Insert
    suspend fun tambah(entry: JurnalEntry): Long

    @Update
    suspend fun ubah(entry: JurnalEntry)

    @Delete
    suspend fun hapus(entry: JurnalEntry)
}

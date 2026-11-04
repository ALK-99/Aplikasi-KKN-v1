package com.kkn.almufid.sekretaris.data.dao

import androidx.room.*
import com.kkn.almufid.sekretaris.data.entity.ProkerEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface ProkerDao {
    @Query("SELECT * FROM proker_entries ORDER BY id DESC")
    fun semuaProker(): Flow<List<ProkerEntry>>

    @Insert
    suspend fun tambah(entry: ProkerEntry): Long

    @Update
    suspend fun ubah(entry: ProkerEntry)

    @Delete
    suspend fun hapus(entry: ProkerEntry)
}

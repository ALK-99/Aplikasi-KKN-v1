package com.kkn.almufid.sekretaris.data.dao

import androidx.room.*
import com.kkn.almufid.sekretaris.data.entity.InventarisEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface InventarisDao {
    @Query("SELECT * FROM inventaris_entries ORDER BY namaBarang ASC")
    fun semuaInventaris(): Flow<List<InventarisEntry>>

    @Insert
    suspend fun tambah(entry: InventarisEntry): Long

    @Update
    suspend fun ubah(entry: InventarisEntry)

    @Delete
    suspend fun hapus(entry: InventarisEntry)
}

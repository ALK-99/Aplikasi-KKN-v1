package com.kkn.almufid.sekretaris.data.dao

import androidx.room.*
import com.kkn.almufid.sekretaris.data.entity.Anggota
import kotlinx.coroutines.flow.Flow

@Dao
interface AnggotaDao {
    @Query("SELECT * FROM anggota ORDER BY nama ASC")
    fun semuaAnggota(): Flow<List<Anggota>>

    @Insert
    suspend fun tambah(anggota: Anggota): Long

    @Delete
    suspend fun hapus(anggota: Anggota)
}

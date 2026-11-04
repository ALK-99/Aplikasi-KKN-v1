package com.kkn.almufid.sekretaris.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "inventaris_entries")
data class InventarisEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val namaBarang: String,
    val jumlah: Int,
    val kondisi: String = "Baik",
    val keterangan: String = ""
)

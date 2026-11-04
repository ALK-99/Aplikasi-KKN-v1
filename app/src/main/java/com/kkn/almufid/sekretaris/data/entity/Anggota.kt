package com.kkn.almufid.sekretaris.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "anggota")
data class Anggota(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nama: String,
    val divisi: String = ""
)

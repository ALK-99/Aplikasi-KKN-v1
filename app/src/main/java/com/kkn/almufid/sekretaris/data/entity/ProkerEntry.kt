package com.kkn.almufid.sekretaris.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class StatusProker { BELUM_MULAI, BERJALAN, SELESAI, BATAL }

@Entity(tableName = "proker_entries")
data class ProkerEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val namaProker: String,
    val divisiPenanggungJawab: String,
    val status: StatusProker = StatusProker.BELUM_MULAI,
    val catatanEvaluasi: String = ""
)

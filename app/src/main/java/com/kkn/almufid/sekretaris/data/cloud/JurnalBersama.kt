package com.kkn.almufid.sekretaris.data.cloud

/** Satu entri Jurnal Bersama di Firestore (`jurnal_bersama/{id}`). Dibaca semua pengguna, hanya penulisnya yang boleh mengubah. */
data class JurnalBersama(
    val id: String = "",
    val penulisUid: String = "",
    val penulisNama: String = "",
    val desa: String = "",
    val jenis: String = "HARIAN",
    val judul: String = "",
    val isi: String = "",
    val pesertaRapat: String = "",
    val tanggalMillis: Long = 0L,
)

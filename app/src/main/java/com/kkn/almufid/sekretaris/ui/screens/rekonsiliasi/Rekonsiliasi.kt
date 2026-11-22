package com.kkn.almufid.sekretaris.ui.screens.rekonsiliasi

import com.kkn.almufid.sekretaris.data.cloud.Kas
import com.kkn.almufid.sekretaris.data.cloud.NotaDesa
import com.kkn.almufid.sekretaris.data.cloud.ProkerDesa
import com.kkn.almufid.sekretaris.data.entity.StatusProker
import kotlin.math.abs

class BarisProker(val nama: String, val anggaran: Long, val realisasi: Long, val totalNota: Long)

class HasilRekon(
    val kasMasuk: Long,
    val kasKeluar: Long,
    val totalNota: Long,
    val cocok: List<Pair<Kas, NotaDesa>>,
    val kasTanpaNota: List<Kas>,
    val notaTanpaKas: List<NotaDesa>,
    val notaTanpaFoto: Int,
    val perProker: List<BarisProker>,
) {
    val saldo: Long get() = kasMasuk - kasKeluar
    /** Kas keluar dikurangi total nota. Nol berarti pengeluaran kas sepenuhnya didukung nota. */
    val selisih: Long get() = kasKeluar - totalNota
}

/**
 * Mencocokkan pengeluaran di Kas Kelompok dengan nota/kwitansi desa:
 * nominal sama dan tanggal berselisih paling banyak 14 hari. Satu nota hanya dipakai sekali.
 */
object Rekonsiliasi {
    private const val JENDELA = 14L * 24 * 60 * 60 * 1000

    fun hitung(kas: List<Kas>, nota: List<NotaDesa>, proker: List<ProkerDesa>): HasilRekon {
        val keluar = kas.filter { !it.masuk }.sortedBy { it.tanggalMillis }
        val sisa = nota.sortedBy { it.tanggalMillis }.toMutableList()
        val cocok = mutableListOf<Pair<Kas, NotaDesa>>()
        val tanpaNota = mutableListOf<Kas>()
        for (k in keluar) {
            val calon = sisa.filter {
                it.nominal == k.jumlah && (k.tanggalMillis == 0L || abs(it.tanggalMillis - k.tanggalMillis) <= JENDELA)
            }.minByOrNull { abs(it.tanggalMillis - k.tanggalMillis) }
            if (calon != null) { sisa.remove(calon); cocok += k to calon } else tanpaNota += k
        }
        val baris = proker.filter { it.status != StatusProker.BATAL }.map { p ->
            BarisProker(
                nama = p.namaProker, anggaran = p.anggaran, realisasi = p.realisasi,
                totalNota = nota.filter { it.prokerTerkait.trim().equals(p.namaProker.trim(), ignoreCase = true) }.sumOf { it.nominal },
            )
        }
        return HasilRekon(
            kasMasuk = kas.filter { it.masuk }.sumOf { it.jumlah },
            kasKeluar = keluar.sumOf { it.jumlah },
            totalNota = nota.sumOf { it.nominal },
            cocok = cocok, kasTanpaNota = tanpaNota, notaTanpaKas = sisa,
            notaTanpaFoto = nota.count { it.fotoBase64.isBlank() },
            perProker = baris,
        )
    }
}

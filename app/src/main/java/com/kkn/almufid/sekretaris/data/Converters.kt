package com.kkn.almufid.sekretaris.data

import androidx.room.TypeConverter
import com.kkn.almufid.sekretaris.data.entity.JenisJurnal
import com.kkn.almufid.sekretaris.data.entity.JenisSurat
import com.kkn.almufid.sekretaris.data.entity.StatusProker

/**
 * Room tidak bisa menyimpan enum langsung — semua dikonversi ke/dari
 * String memakai nama konstanta enum-nya sendiri (name/valueOf), bukan
 * ordinal, supaya urutan deklarasi enum di kode tidak memengaruhi data
 * yang sudah tersimpan di database lama.
 */
class Converters {
    @TypeConverter
    fun jenisJurnalKeString(nilai: JenisJurnal): String = nilai.name

    @TypeConverter
    fun stringKeJenisJurnal(nilai: String): JenisJurnal = JenisJurnal.valueOf(nilai)

    @TypeConverter
    fun jenisSuratKeString(nilai: JenisSurat): String = nilai.name

    @TypeConverter
    fun stringKeJenisSurat(nilai: String): JenisSurat = JenisSurat.valueOf(nilai)

    @TypeConverter
    fun statusProkerKeString(nilai: StatusProker): String = nilai.name

    @TypeConverter
    fun stringKeStatusProker(nilai: String): StatusProker = StatusProker.valueOf(nilai)
}

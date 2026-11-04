package com.kkn.almufid.sekretaris.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.kkn.almufid.sekretaris.data.dao.*
import com.kkn.almufid.sekretaris.data.entity.*

@Database(
    entities = [
        JurnalEntry::class,
        Anggota::class,
        AbsensiEntry::class,
        SuratEntry::class,
        ProkerEntry::class,
        NotaEntry::class,
        InventarisEntry::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun jurnalDao(): JurnalDao
    abstract fun anggotaDao(): AnggotaDao
    abstract fun absensiDao(): AbsensiDao
    abstract fun suratDao(): SuratDao
    abstract fun prokerDao(): ProkerDao
    abstract fun notaDao(): NotaDao
    abstract fun inventarisDao(): InventarisDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // Double-checked locking: mencegah dua Activity/proses membuat
        // instance Room terpisah secara bersamaan (penyebab klasik bug
        // "database is locked" yang sulit dilacak kalau tidak dicegah
        // dari awal).
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "sekretaris_kkn.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}

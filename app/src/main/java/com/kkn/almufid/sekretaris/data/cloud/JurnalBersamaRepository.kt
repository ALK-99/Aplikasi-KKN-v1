package com.kkn.almufid.sekretaris.data.cloud

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.kkn.almufid.sekretaris.data.auth.HasilAuth
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Jurnal Bersama: tersimpan di Firestore dan tampil real-time di semua
 * pengguna. Pembacaan dipakai bersama, tapi ubah/hapus hanya diizinkan
 * untuk penulisnya (dipaksa oleh firestore.rules, bukan hanya tampilan).
 * Penghapusan = menandai `dihapus: true` (bukan hapus permanen).
 */
class JurnalBersamaRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    private val koleksi get() = db.collection("jurnal_bersama")

    fun alirSemua(): Flow<List<JurnalBersama>> = callbackFlow {
        val pendaftaran = koleksi
            .orderBy("tanggalMillis", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, galat ->
                if (galat != null || snap == null) return@addSnapshotListener
                trySend(snap.documents.mapNotNull { it.keJurnal() })
            }
        awaitClose { pendaftaran.remove() }
    }

    suspend fun tambah(
        uid: String, nama: String, desa: String,
        jenis: String, judul: String, isi: String, peserta: String,
    ): HasilAuth<Unit> = try {
        koleksi.add(
            mapOf(
                "penulisUid" to uid,
                "penulisNama" to nama,
                "desa" to desa,
                "jenis" to jenis,
                "judul" to judul,
                "isi" to isi,
                "pesertaRapat" to peserta,
                "tanggalMillis" to System.currentTimeMillis(),
                "dihapus" to false,
            )
        ).await()
        HasilAuth.Sukses(Unit)
    } catch (e: Exception) {
        HasilAuth.Gagal("Gagal membagikan jurnal: ${e.message ?: "kesalahan tidak diketahui"}")
    }

    suspend fun ubah(id: String, judul: String, isi: String, peserta: String): HasilAuth<Unit> = try {
        koleksi.document(id).update(
            mapOf("judul" to judul, "isi" to isi, "pesertaRapat" to peserta)
        ).await()
        HasilAuth.Sukses(Unit)
    } catch (e: Exception) {
        HasilAuth.Gagal("Gagal menyimpan perubahan: ${e.message ?: "kesalahan tidak diketahui"}")
    }

    suspend fun hapus(id: String): HasilAuth<Unit> = try {
        koleksi.document(id).update("dihapus", true).await()
        HasilAuth.Sukses(Unit)
    } catch (e: Exception) {
        HasilAuth.Gagal("Gagal menghapus: ${e.message ?: "kesalahan tidak diketahui"}")
    }

    private fun DocumentSnapshot.keJurnal(): JurnalBersama? {
        if (getBoolean("dihapus") == true) return null
        return JurnalBersama(
            id = id,
            penulisUid = getString("penulisUid") ?: "",
            penulisNama = getString("penulisNama") ?: "",
            desa = getString("desa") ?: "",
            jenis = getString("jenis") ?: "HARIAN",
            judul = getString("judul") ?: "",
            isi = getString("isi") ?: "",
            pesertaRapat = getString("pesertaRapat") ?: "",
            tanggalMillis = getLong("tanggalMillis") ?: 0L,
        )
    }
}

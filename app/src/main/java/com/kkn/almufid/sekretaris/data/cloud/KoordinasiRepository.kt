package com.kkn.almufid.sekretaris.data.cloud

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.kkn.almufid.sekretaris.data.auth.HasilAuth
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

data class Pengumuman(
    val id: String = "",
    val judul: String = "",
    val isi: String = "",
    val penulisNama: String = "",
    val tanggalMillis: Long = 0L,
    val dibacaOleh: List<String> = emptyList(),
)

data class Polling(
    val id: String = "",
    val pertanyaan: String = "",
    val opsi: List<String> = emptyList(),
    val suara: Map<String, Int> = emptyMap(),
    val ditutup: Boolean = false,
    val penulisNama: String = "",
    val tanggalMillis: Long = 0L,
)

data class Tugas(
    val id: String = "",
    val judul: String = "",
    val deskripsi: String = "",
    val untukUid: String = "",
    val untukNama: String = "",
    val deadlineMillis: Long = 0L,
    val selesai: Boolean = false,
    val tanggalMillis: Long = 0L,
)

data class Kas(
    val id: String = "",
    val masuk: Boolean = true,
    val jumlah: Long = 0L,
    val kategori: String = "",
    val keterangan: String = "",
    val tanggalMillis: Long = 0L,
    val penulisNama: String = "",
)

/** Saran masuk dari menu Pengaturan (koleksi `feedback`), hanya dibaca developer. */
data class Saran(
    val id: String = "",
    val uid: String = "",
    val isi: String = "",
    val dikirimMillis: Long = 0L,
)

/**
 * Pengumuman, polling, tugas, dan kas kelompok. Semua pengguna yang login
 * bisa membaca; yang boleh MEMBUAT hanya admin (akun yang punya dokumen
 * admins/{uid}). Pengguna biasa hanya boleh: menandai pengumuman dibaca,
 * memberi suara, dan menandai tugas miliknya selesai (dipaksa firestore.rules).
 * Penghapusan = menandai `dihapus: true`.
 */
class KoordinasiRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    private fun alir(nama: String): Query = db.collection(nama).orderBy("tanggalMillis", Query.Direction.DESCENDING)

    private fun <T : Any> Query.pantau(peta: (DocumentSnapshot) -> T?): Flow<List<T>> = callbackFlow {
        val reg = addSnapshotListener { snap, galat ->
            if (galat != null || snap == null) return@addSnapshotListener
            trySend(snap.documents.mapNotNull(peta))
        }
        awaitClose { reg.remove() }
    }

    private suspend fun aman(aksi: suspend () -> Unit): HasilAuth<Unit> = try {
        aksi()
        HasilAuth.Sukses(Unit)
    } catch (e: Exception) {
        val ditolak = (e as? FirebaseFirestoreException)?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED
        HasilAuth.Gagal(
            if (ditolak) "Ditolak server. Pastikan aturan Firestore terbaru sudah dipublish dan akun ini terdaftar sebagai admin bila fitur ini khusus admin."
            else "Gagal: ${e.message ?: "kesalahan tidak diketahui"}"
        )
    }

    private fun DocumentSnapshot.hapus() = getBoolean("dihapus") == true

    // ---- Pengumuman ----
    fun alirPengumuman(): Flow<List<Pengumuman>> = alir("pengumuman").pantau { d ->
        if (d.hapus()) null else Pengumuman(
            id = d.id,
            judul = d.getString("judul") ?: "",
            isi = d.getString("isi") ?: "",
            penulisNama = d.getString("penulisNama") ?: "",
            tanggalMillis = d.getLong("tanggalMillis") ?: 0L,
            dibacaOleh = (d.get("dibacaOleh") as? List<*>)?.map { it.toString() } ?: emptyList(),
        )
    }

    suspend fun tambahPengumuman(nama: String, judul: String, isi: String) = aman {
        db.collection("pengumuman").add(
            mapOf(
                "judul" to judul, "isi" to isi, "penulisNama" to nama,
                "tanggalMillis" to System.currentTimeMillis(),
                "dibacaOleh" to emptyList<String>(), "dihapus" to false,
            )
        ).await()
    }

    suspend fun tandaiDibaca(id: String, uid: String) = aman {
        db.collection("pengumuman").document(id).update("dibacaOleh", FieldValue.arrayUnion(uid)).await()
    }

    suspend fun hapusPengumuman(id: String) = aman {
        db.collection("pengumuman").document(id).update("dihapus", true).await()
    }

    // ---- Saran masuk (khusus admin) ----
    fun alirSaran(): Flow<List<Saran>> =
        db.collection("feedback").orderBy("dikirimPadaMillis", Query.Direction.DESCENDING).pantau { d ->
            Saran(
                id = d.id,
                uid = d.getString("uid") ?: "",
                isi = d.getString("isi") ?: "",
                dikirimMillis = d.getLong("dikirimPadaMillis") ?: 0L,
            )
        }

    // ---- Polling ----
    fun alirPolling(): Flow<List<Polling>> = alir("polling").pantau { d ->
        if (d.hapus()) null else Polling(
            id = d.id,
            pertanyaan = d.getString("pertanyaan") ?: "",
            opsi = (d.get("opsi") as? List<*>)?.map { it.toString() } ?: emptyList(),
            suara = (d.get("suara") as? Map<*, *>)?.entries
                ?.associate { it.key.toString() to ((it.value as? Number)?.toInt() ?: -1) } ?: emptyMap(),
            ditutup = d.getBoolean("ditutup") == true,
            penulisNama = d.getString("penulisNama") ?: "",
            tanggalMillis = d.getLong("tanggalMillis") ?: 0L,
        )
    }

    suspend fun tambahPolling(nama: String, pertanyaan: String, opsi: List<String>) = aman {
        db.collection("polling").add(
            mapOf(
                "pertanyaan" to pertanyaan, "opsi" to opsi, "suara" to emptyMap<String, Int>(),
                "ditutup" to false, "penulisNama" to nama,
                "tanggalMillis" to System.currentTimeMillis(), "dihapus" to false,
            )
        ).await()
    }

    suspend fun beriSuara(id: String, uid: String, indeks: Int) = aman {
        db.collection("polling").document(id).update(FieldPath.of("suara", uid), indeks).await()
    }

    suspend fun tutupPolling(id: String) = aman {
        db.collection("polling").document(id).update("ditutup", true).await()
    }

    suspend fun hapusPolling(id: String) = aman {
        db.collection("polling").document(id).update("dihapus", true).await()
    }

    // ---- Tugas ----
    fun alirTugas(): Flow<List<Tugas>> = alir("tugas").pantau { d ->
        if (d.hapus()) null else Tugas(
            id = d.id,
            judul = d.getString("judul") ?: "",
            deskripsi = d.getString("deskripsi") ?: "",
            untukUid = d.getString("untukUid") ?: "",
            untukNama = d.getString("untukNama") ?: "",
            deadlineMillis = d.getLong("deadlineMillis") ?: 0L,
            selesai = d.getBoolean("selesai") == true,
            tanggalMillis = d.getLong("tanggalMillis") ?: 0L,
        )
    }

    suspend fun tambahTugas(judul: String, deskripsi: String, untukUid: String, untukNama: String, deadline: Long) = aman {
        db.collection("tugas").add(
            mapOf(
                "judul" to judul, "deskripsi" to deskripsi,
                "untukUid" to untukUid, "untukNama" to untukNama,
                "deadlineMillis" to deadline, "selesai" to false,
                "tanggalMillis" to System.currentTimeMillis(), "dihapus" to false,
            )
        ).await()
    }

    suspend fun tandaiSelesai(id: String, selesai: Boolean) = aman {
        db.collection("tugas").document(id).update("selesai", selesai).await()
    }

    suspend fun hapusTugas(id: String) = aman {
        db.collection("tugas").document(id).update("dihapus", true).await()
    }

    // ---- Kas ----
    fun alirKas(): Flow<List<Kas>> = alir("kas").pantau { d ->
        if (d.hapus()) null else Kas(
            id = d.id,
            masuk = d.getBoolean("masuk") != false,
            jumlah = d.getLong("jumlah") ?: 0L,
            kategori = d.getString("kategori") ?: "",
            keterangan = d.getString("keterangan") ?: "",
            tanggalMillis = d.getLong("tanggalMillis") ?: 0L,
            penulisNama = d.getString("penulisNama") ?: "",
        )
    }

    suspend fun tambahKas(nama: String, masuk: Boolean, jumlah: Long, kategori: String, keterangan: String) = aman {
        db.collection("kas").add(
            mapOf(
                "masuk" to masuk, "jumlah" to jumlah, "kategori" to kategori, "keterangan" to keterangan,
                "penulisNama" to nama, "tanggalMillis" to System.currentTimeMillis(), "dihapus" to false,
            )
        ).await()
    }

    suspend fun hapusKas(id: String) = aman {
        db.collection("kas").document(id).update("dihapus", true).await()
    }
}

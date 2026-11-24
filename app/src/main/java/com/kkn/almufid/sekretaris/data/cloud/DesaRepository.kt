package com.kkn.almufid.sekretaris.data.cloud

import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import com.kkn.almufid.sekretaris.data.auth.AuthRepository
import com.kkn.almufid.sekretaris.data.auth.HasilAuth
import com.kkn.almufid.sekretaris.data.auth.desaIdDari
import com.kkn.almufid.sekretaris.data.entity.AbsensiEntry
import com.kkn.almufid.sekretaris.data.entity.Anggota
import com.kkn.almufid.sekretaris.data.entity.InventarisEntry
import com.kkn.almufid.sekretaris.data.entity.JenisSurat
import com.kkn.almufid.sekretaris.data.entity.NotaEntry
import com.kkn.almufid.sekretaris.data.entity.ProkerEntry
import com.kkn.almufid.sekretaris.data.entity.StatusProker
import com.kkn.almufid.sekretaris.data.entity.SuratEntry
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

/** Siapa yang sedang bekerja dan di ruang desa mana. */
class KonteksDesa(val uid: String, val nama: String, val desaId: String, val desaNama: String)

/**
 * Data kerja satu desa yang dipakai bersama oleh semua peserta desa itu:
 * anggota, absensi, proker, surat, inventaris, ditambah catatan riwayat
 * perubahan. Lokasi: `desa/{desaId}/{koleksi}/{id}`.
 *
 * Firestore menyimpan salinan di HP, jadi tetap bisa dipakai tanpa internet
 * dan otomatis tersinkron begitu online. Tidak ada yang benar-benar dihapus:
 * anggota dinonaktifkan, data lain ditandai `dihapus`. Setiap perubahan
 * mencatat nama dan UID pelakunya (UID dicek aturan Firestore).
 */
class DesaRepository(
    private val auth: AuthRepository,
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    private var cache: KonteksDesa? = null

    /** Null kalau belum login, profil tidak terbaca, atau desa belum diisi. */
    suspend fun konteks(): KonteksDesa? {
        cache?.let { return it }
        val uid = auth.uidSaatIni ?: return null
        val profil = auth.ambilProfil(uid) ?: return null
        var id = profil.desaId
        if (id.isBlank()) {
            id = desaIdDari(profil.desa)
            if (id.isBlank()) return null
            auth.simpanDesaId(uid, id)
        }
        return KonteksDesa(uid, profil.namaLengkap.ifBlank { "Pengguna" }, id, profil.desa).also { cache = it }
    }

    private fun koleksi(k: KonteksDesa, nama: String): CollectionReference =
        db.collection("desa").document(k.desaId).collection(nama)

    // ------------------------------------------------------------ pembacaan

    private fun <T : Any> Query.pantauDaftar(peta: (DocumentSnapshot) -> T?): Flow<List<T>> = callbackFlow {
        val reg = addSnapshotListener { snap, galat ->
            if (galat != null || snap == null) return@addSnapshotListener
            trySend(snap.documents.mapNotNull(peta))
        }
        awaitClose { reg.remove() }
    }

    private fun <T : Any> alirKoleksi(
        nama: String,
        saring: (Query) -> Query = { it },
        peta: (DocumentSnapshot) -> T?,
    ): Flow<List<T>> = flow {
        val k = konteks()
        if (k == null) {
            emit(emptyList())
        } else {
            emitAll(saring(koleksi(k, nama)).pantauDaftar(peta))
        }
    }

    private fun DocumentSnapshot.dihapus() = getBoolean("dihapus") == true

    private fun DocumentSnapshot.jejak() = Jejak(
        dibuatOlehNama = getString("dibuatOlehNama") ?: "",
        dibuatPada = getLong("dibuatPada") ?: 0L,
        diubahOlehNama = getString("diubahOlehNama") ?: "",
        diubahPada = getLong("diubahPada") ?: 0L,
    )

    fun alirAnggota(): Flow<List<AnggotaDesa>> = alirKoleksi("anggota") { d ->
        if (d.dihapus()) null else AnggotaDesa(
            id = d.id,
            nama = d.getString("nama") ?: "",
            divisi = d.getString("divisi") ?: "",
            aktif = d.getBoolean("aktif") ?: true,
            jejak = d.jejak(),
        )
    }

    /** Semua absensi, atau hanya satu tanggal bila [tanggalMillis] diisi (lebih hemat bacaan). */
    fun alirAbsensi(tanggalMillis: Long? = null): Flow<List<AbsensiDesa>> = alirKoleksi(
        "absensi", { q -> if (tanggalMillis == null) q else q.whereEqualTo("tanggalMillis", tanggalMillis) }
    ) { d ->
        AbsensiDesa(
            id = d.id,
            anggotaId = d.getString("anggotaId") ?: "",
            anggotaNama = d.getString("anggotaNama") ?: "",
            tanggalMillis = d.getLong("tanggalMillis") ?: 0L,
            hadir = d.getBoolean("hadir") ?: false,
            keterangan = d.getString("keterangan") ?: "",
            jejak = d.jejak(),
            status = d.getString("status") ?: if (d.getBoolean("hadir") == true) "HADIR" else "ALPA",
        )
    }

    fun alirProker(): Flow<List<ProkerDesa>> = alirKoleksi("proker") { d ->
        if (d.dihapus()) null else ProkerDesa(
            id = d.id,
            namaProker = d.getString("namaProker") ?: "",
            divisiPenanggungJawab = d.getString("divisiPenanggungJawab") ?: "",
            status = try {
                StatusProker.valueOf(d.getString("status") ?: "BELUM_MULAI")
            } catch (e: Exception) {
                StatusProker.BELUM_MULAI
            },
            catatanEvaluasi = d.getString("catatanEvaluasi") ?: "",
            target = d.getString("target") ?: "",
            mulaiMillis = d.getLong("mulaiMillis") ?: 0L,
            selesaiMillis = d.getLong("selesaiMillis") ?: 0L,
            anggaran = d.getLong("anggaran") ?: 0L,
            picNama = d.getString("picNama") ?: "",
            realisasi = d.getLong("realisasi") ?: 0L,
            jejak = d.jejak(),
        )
    }

    fun alirBukti(prokerId: String): Flow<List<BuktiDesa>> = alirKoleksi("bukti", { it.whereEqualTo("prokerId", prokerId) }) { d ->
        if (d.dihapus()) null else BuktiDesa(
            id = d.id, prokerId = d.getString("prokerId") ?: "", catatan = d.getString("catatan") ?: "",
            fotoBase64 = d.getString("foto") ?: "", jejak = d.jejak(),
        )
    }

    fun alirNota(): Flow<List<NotaDesa>> = alirKoleksi("nota") { d ->
        if (d.dihapus()) null else NotaDesa(
            id = d.id, tanggalMillis = d.getLong("tanggalMillis") ?: 0L, keterangan = d.getString("keterangan") ?: "",
            nominal = d.getLong("nominal") ?: 0L, prokerTerkait = d.getString("prokerTerkait") ?: "",
            jenis = d.getString("jenis") ?: "NOTA", fotoBase64 = d.getString("foto") ?: "", jejak = d.jejak(),
        )
    }

    fun alirSurat(): Flow<List<SuratDesa>> = alirKoleksi("surat") { d ->
        if (d.dihapus()) null else SuratDesa(
            id = d.id,
            tanggalMillis = d.getLong("tanggalMillis") ?: 0L,
            jenis = try {
                JenisSurat.valueOf(d.getString("jenis") ?: "KELUAR")
            } catch (e: Exception) {
                JenisSurat.KELUAR
            },
            nomorSurat = d.getString("nomorSurat") ?: "",
            perihal = d.getString("perihal") ?: "",
            tujuanAtauPengirim = d.getString("tujuanAtauPengirim") ?: "",
            jejak = d.jejak(),
        )
    }

    fun alirInventaris(): Flow<List<InventarisDesa>> = alirKoleksi("inventaris") { d ->
        if (d.dihapus()) null else InventarisDesa(
            id = d.id,
            namaBarang = d.getString("namaBarang") ?: "",
            jumlah = (d.getLong("jumlah") ?: 0L).toInt(),
            kondisi = d.getString("kondisi") ?: "Baik",
            keterangan = d.getString("keterangan") ?: "",
            jejak = d.jejak(),
        )
    }

    fun alirAjar(): Flow<List<AjarDesa>> = alirKoleksi("ajar") { d ->
        if (d.dihapus()) null else AjarDesa(
            id = d.id, hari = (d.getLong("hari") ?: 1L).toInt(), jam = d.getString("jam") ?: "",
            lembaga = d.getString("lembaga") ?: "", materi = d.getString("materi") ?: "",
            pengajarNama = d.getString("pengajarNama") ?: "", catatan = d.getString("catatan") ?: "", jejak = d.jejak(),
        )
    }

    fun alirRiwayat(): Flow<List<RiwayatDesa>> = alirKoleksi(
        "riwayat", { it.orderBy("waktu", Query.Direction.DESCENDING).limit(300) }
    ) { d ->
        RiwayatDesa(
            id = d.id,
            tabel = d.getString("tabel") ?: "",
            aksi = d.getString("aksi") ?: "",
            ringkas = d.getString("ringkas") ?: "",
            olehNama = d.getString("olehNama") ?: "",
            waktu = d.getLong("waktu") ?: 0L,
        )
    }

    // ------------------------------------------------------------ penulisan

    private suspend fun aman(aksi: suspend (KonteksDesa) -> Unit): HasilAuth<Unit> {
        val k = konteks() ?: return HasilAuth.Gagal(
            "Desa akun ini belum diisi atau profil belum terbaca. Minta developer mengisi desa di akun kamu."
        )
        return try {
            aksi(k)
            HasilAuth.Sukses(Unit)
        } catch (e: Exception) {
            val ditolak = (e as? FirebaseFirestoreException)?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED
            HasilAuth.Gagal(
                if (ditolak) "Ditolak server. Pastikan aturan Firestore terbaru sudah dipublish dan akun ini tidak diblokir."
                else "Gagal: ${e.message ?: "kesalahan tidak diketahui"}"
            )
        }
    }

    private fun jejakBaru(k: KonteksDesa, sekarang: Long): Map<String, Any> = mapOf(
        "dibuatOleh" to k.uid, "dibuatOlehNama" to k.nama, "dibuatPada" to sekarang,
        "diubahOleh" to k.uid, "diubahOlehNama" to k.nama, "diubahPada" to sekarang,
    )

    private fun jejakUbah(k: KonteksDesa, sekarang: Long): Map<String, Any> = mapOf(
        "diubahOleh" to k.uid, "diubahOlehNama" to k.nama, "diubahPada" to sekarang,
    )

    private fun catatan(k: KonteksDesa, tabel: String, aksi: String, targetId: String, ringkas: String, sekarang: Long): Map<String, Any> = mapOf(
        "tabel" to tabel, "aksi" to aksi, "targetId" to targetId, "ringkas" to ringkas.take(160),
        "oleh" to k.uid, "olehNama" to k.nama, "waktu" to sekarang,
    )

    /** Satu perubahan data + satu baris riwayat ditulis bersamaan (atomik). */
    private suspend fun tulis(
        k: KonteksDesa, tabel: String, aksi: String, ringkas: String,
        ref: DocumentReference, data: Map<String, Any>, baru: Boolean,
    ) {
        val sekarang = System.currentTimeMillis()
        val batch = db.batch()
        if (baru) batch.set(ref, data) else batch.update(ref, data)
        batch.set(koleksi(k, "riwayat").document(), catatan(k, tabel, aksi, ref.id, ringkas, sekarang))
        batch.commit().await()
    }

    // -- anggota
    suspend fun tambahAnggota(nama: String, divisi: String): HasilAuth<Unit> = aman { k ->
        val ref = koleksi(k, "anggota").document()
        val t = System.currentTimeMillis()
        val data = mapOf<String, Any>("nama" to nama.trim(), "divisi" to divisi.trim(), "aktif" to true) + jejakBaru(k, t)
        tulis(k, "anggota", "tambah", nama.trim(), ref, data, true)
    }

    suspend fun aturAnggotaAktif(anggota: AnggotaDesa, aktif: Boolean): HasilAuth<Unit> = aman { k ->
        val ref = koleksi(k, "anggota").document(anggota.id)
        val data = mapOf<String, Any>("aktif" to aktif) + jejakUbah(k, System.currentTimeMillis())
        tulis(k, "anggota", if (aktif) "aktifkan" else "nonaktifkan", anggota.nama, ref, data, false)
    }

    suspend fun ubahAnggota(a: AnggotaDesa, nama: String, divisi: String): HasilAuth<Unit> = aman { k ->
        val data = mapOf<String, Any>("nama" to nama.trim(), "divisi" to divisi.trim()) + jejakUbah(k, System.currentTimeMillis())
        tulis(k, "anggota", "ubah", "${a.nama} → ${nama.trim()}", koleksi(k, "anggota").document(a.id), data, false)
    }

    suspend fun ubahSurat(s: SuratDesa, nomor: String, perihal: String, tujuan: String): HasilAuth<Unit> = aman { k ->
        val data = mapOf<String, Any>(
            "nomorSurat" to nomor.trim(), "perihal" to perihal.trim(), "tujuanAtauPengirim" to tujuan.trim(),
        ) + jejakUbah(k, System.currentTimeMillis())
        tulis(k, "surat", "ubah", perihal.trim(), koleksi(k, "surat").document(s.id), data, false)
    }

    suspend fun ubahBarang(b: InventarisDesa, nama: String, jumlah: Int, kondisi: String, keterangan: String): HasilAuth<Unit> = aman { k ->
        val data = mapOf<String, Any>(
            "namaBarang" to nama.trim(), "jumlah" to jumlah, "kondisi" to kondisi.trim().ifBlank { "Baik" },
            "keterangan" to keterangan.trim(),
        ) + jejakUbah(k, System.currentTimeMillis())
        tulis(k, "inventaris", "ubah", "${nama.trim()} ($jumlah)", koleksi(k, "inventaris").document(b.id), data, false)
    }

    suspend fun ubahNota(n: NotaDesa, keterangan: String, nominal: Long, proker: String): HasilAuth<Unit> = aman { k ->
        val data = mapOf<String, Any>(
            "keterangan" to keterangan.trim(), "nominal" to nominal, "prokerTerkait" to proker.trim(),
        ) + jejakUbah(k, System.currentTimeMillis())
        tulis(k, "nota", "ubah", "${n.jenis} · ${keterangan.trim()} · Rp$nominal", koleksi(k, "nota").document(n.id), data, false)
    }

    // -- absensi
    /** [sudahAda] true bila data absensi anggota itu pada tanggal tersebut sudah tercatat (menentukan buat baru atau ubah). */
    suspend fun simpanAbsensi(
        anggota: AnggotaDesa, tanggalMillis: Long, status: String, keterangan: String, sudahAda: Boolean,
    ): HasilAuth<Unit> = aman { k ->
        val hadir = status == "HADIR"
        val ref = koleksi(k, "absensi").document("${anggota.id}_$tanggalMillis")
        val t = System.currentTimeMillis()
        val isi = mapOf<String, Any>(
            "anggotaId" to anggota.id, "anggotaNama" to anggota.nama,
            "tanggalMillis" to tanggalMillis, "hadir" to hadir, "status" to status, "keterangan" to keterangan,
        )
        val ringkas = "${anggota.nama}: ${status.lowercase()}"
        if (sudahAda) tulis(k, "absensi", "ubah", ringkas, ref, isi + jejakUbah(k, t), false)
        else tulis(k, "absensi", "tambah", ringkas, ref, isi + jejakBaru(k, t), true)
    }

    // -- proker
    suspend fun tambahProker(nama: String, divisi: String): HasilAuth<Unit> = aman { k ->
        val ref = koleksi(k, "proker").document()
        val data = mapOf<String, Any>(
            "namaProker" to nama.trim(), "divisiPenanggungJawab" to divisi.trim(),
            "status" to StatusProker.BELUM_MULAI.name, "catatanEvaluasi" to "", "dihapus" to false,
        ) + jejakBaru(k, System.currentTimeMillis())
        tulis(k, "proker", "tambah", nama.trim(), ref, data, true)
    }

    suspend fun ubahStatusProker(p: ProkerDesa, status: StatusProker): HasilAuth<Unit> = aman { k ->
        val data = mapOf<String, Any>("status" to status.name) + jejakUbah(k, System.currentTimeMillis())
        tulis(k, "proker", "ubah status", "${p.namaProker} → ${status.name}", koleksi(k, "proker").document(p.id), data, false)
    }

    suspend fun ubahRencanaProker(
        p: ProkerDesa, target: String, mulai: Long, selesai: Long, anggaran: Long,
        pic: String, realisasi: Long, evaluasi: String,
    ): HasilAuth<Unit> = aman { k ->
        val data = mapOf<String, Any>(
            "target" to target.trim(), "mulaiMillis" to mulai, "selesaiMillis" to selesai,
            "anggaran" to anggaran, "picNama" to pic.trim(), "realisasi" to realisasi,
            "catatanEvaluasi" to evaluasi.trim(),
        ) + jejakUbah(k, System.currentTimeMillis())
        tulis(k, "proker", "ubah rencana", p.namaProker, koleksi(k, "proker").document(p.id), data, false)
    }

    suspend fun tambahBukti(p: ProkerDesa, catatan: String, fotoBase64: String): HasilAuth<Unit> = aman { k ->
        val data = mapOf<String, Any>(
            "prokerId" to p.id, "catatan" to catatan.trim(), "foto" to fotoBase64, "dihapus" to false,
        ) + jejakBaru(k, System.currentTimeMillis())
        tulis(k, "bukti", "tambah bukti", p.namaProker, koleksi(k, "bukti").document(), data, true)
    }

    suspend fun hapusBukti(b: BuktiDesa, namaProker: String): HasilAuth<Unit> = aman { k ->
        val data = mapOf<String, Any>("dihapus" to true) + jejakUbah(k, System.currentTimeMillis())
        tulis(k, "bukti", "hapus bukti", namaProker, koleksi(k, "bukti").document(b.id), data, false)
    }

    suspend fun tambahNota(keterangan: String, nominal: Long, proker: String, jenis: String, fotoBase64: String): HasilAuth<Unit> = aman { k ->
        val t = System.currentTimeMillis()
        val data = mapOf<String, Any>(
            "tanggalMillis" to t, "keterangan" to keterangan.trim(), "nominal" to nominal,
            "prokerTerkait" to proker.trim(), "jenis" to jenis, "foto" to fotoBase64, "dihapus" to false,
        ) + jejakBaru(k, t)
        tulis(k, "nota", "tambah", "$jenis · ${keterangan.trim()} · Rp$nominal", koleksi(k, "nota").document(), data, true)
    }

    suspend fun hapusNota(n: NotaDesa): HasilAuth<Unit> = aman { k ->
        val data = mapOf<String, Any>("dihapus" to true) + jejakUbah(k, System.currentTimeMillis())
        tulis(k, "nota", "hapus", n.keterangan, koleksi(k, "nota").document(n.id), data, false)
    }

    suspend fun hapusProker(p: ProkerDesa): HasilAuth<Unit> = aman { k ->
        val data = mapOf<String, Any>("dihapus" to true) + jejakUbah(k, System.currentTimeMillis())
        tulis(k, "proker", "hapus", p.namaProker, koleksi(k, "proker").document(p.id), data, false)
    }

    // -- surat
    suspend fun tambahSurat(jenis: JenisSurat, nomor: String, perihal: String, tujuan: String): HasilAuth<Unit> = aman { k ->
        val t = System.currentTimeMillis()
        val data = mapOf<String, Any>(
            "tanggalMillis" to t, "jenis" to jenis.name, "nomorSurat" to nomor.trim(),
            "perihal" to perihal.trim(), "tujuanAtauPengirim" to tujuan.trim(), "dihapus" to false,
        ) + jejakBaru(k, t)
        tulis(k, "surat", "tambah", "${jenis.name} · ${perihal.trim()}", koleksi(k, "surat").document(), data, true)
    }

    suspend fun hapusSurat(s: SuratDesa): HasilAuth<Unit> = aman { k ->
        val data = mapOf<String, Any>("dihapus" to true) + jejakUbah(k, System.currentTimeMillis())
        tulis(k, "surat", "hapus", s.perihal, koleksi(k, "surat").document(s.id), data, false)
    }

    // -- jadwal mengajar (divisi Pendidikan)
    private fun dataAjar(hari: Int, jam: String, lembaga: String, materi: String, pengajar: String, catatan: String) = mapOf<String, Any>(
        "hari" to hari, "jam" to jam.trim(), "lembaga" to lembaga.trim(), "materi" to materi.trim(),
        "pengajarNama" to pengajar.trim(), "catatan" to catatan.trim(),
    )

    suspend fun tambahAjar(hari: Int, jam: String, lembaga: String, materi: String, pengajar: String, catatan: String): HasilAuth<Unit> = aman { k ->
        val data = dataAjar(hari, jam, lembaga, materi, pengajar, catatan) + mapOf("dihapus" to false) + jejakBaru(k, System.currentTimeMillis())
        tulis(k, "ajar", "tambah", "${materi.trim().ifBlank { lembaga.trim() }} (hari $hari)", koleksi(k, "ajar").document(), data, true)
    }

    suspend fun ubahAjar(a: AjarDesa, hari: Int, jam: String, lembaga: String, materi: String, pengajar: String, catatan: String): HasilAuth<Unit> = aman { k ->
        val data = dataAjar(hari, jam, lembaga, materi, pengajar, catatan) + jejakUbah(k, System.currentTimeMillis())
        tulis(k, "ajar", "ubah", materi.trim().ifBlank { lembaga.trim() }, koleksi(k, "ajar").document(a.id), data, false)
    }

    suspend fun hapusAjar(a: AjarDesa): HasilAuth<Unit> = aman { k ->
        val data = mapOf<String, Any>("dihapus" to true) + jejakUbah(k, System.currentTimeMillis())
        tulis(k, "ajar", "hapus", a.materi.ifBlank { a.lembaga }, koleksi(k, "ajar").document(a.id), data, false)
    }

    // -- inventaris
    suspend fun tambahBarang(nama: String, jumlah: Int, kondisi: String, keterangan: String): HasilAuth<Unit> = aman { k ->
        val data = mapOf<String, Any>(
            "namaBarang" to nama.trim(), "jumlah" to jumlah, "kondisi" to kondisi.trim().ifBlank { "Baik" },
            "keterangan" to keterangan.trim(), "dihapus" to false,
        ) + jejakBaru(k, System.currentTimeMillis())
        tulis(k, "inventaris", "tambah", "${nama.trim()} ($jumlah)", koleksi(k, "inventaris").document(), data, true)
    }

    suspend fun hapusBarang(b: InventarisDesa): HasilAuth<Unit> = aman { k ->
        val data = mapOf<String, Any>("dihapus" to true) + jejakUbah(k, System.currentTimeMillis())
        tulis(k, "inventaris", "hapus", b.namaBarang, koleksi(k, "inventaris").document(b.id), data, false)
    }

    // ------------------------------------------------- pindah data lama di HP

    /**
     * Mengunggah data lama (Room) ke ruang desa. ID dokumen dibuat tetap
     * (berisi UID dan ID lama), jadi mengulang unggahan menimpa dokumen yang
     * sama dan tidak menggandakan data. Data di HP tidak dihapus.
     */
    suspend fun imporDataLama(
        anggota: List<Anggota>, absensi: List<AbsensiEntry>, proker: List<ProkerEntry>,
        surat: List<SuratEntry>, inventaris: List<InventarisEntry>,
        nota: List<Pair<NotaEntry, String>> = emptyList(),
    ): HasilAuth<Int> {
        val k = konteks() ?: return HasilAuth.Gagal("Desa akun ini belum diisi atau profil belum terbaca.")
        return try {
            val sekarang = System.currentTimeMillis()
            val jb = jejakBaru(k, sekarang)
            val kerja = mutableListOf<Pair<DocumentReference, Map<String, Any>>>()
            fun idAnggota(lama: Long) = "lama_${k.uid}_anggota_$lama"

            val namaAnggota = anggota.associate { it.id to it.nama }
            anggota.forEach {
                kerja += koleksi(k, "anggota").document(idAnggota(it.id)) to
                    (mapOf<String, Any>("nama" to it.nama, "divisi" to it.divisi, "aktif" to true) + jb)
            }
            absensi.forEach {
                val aid = idAnggota(it.anggotaId)
                kerja += koleksi(k, "absensi").document("${aid}_${it.tanggalMillis}") to
                    (mapOf<String, Any>(
                        "anggotaId" to aid, "anggotaNama" to (namaAnggota[it.anggotaId] ?: "(anggota lama)"),
                        "tanggalMillis" to it.tanggalMillis, "hadir" to it.hadir, "status" to it.status, "keterangan" to it.keterangan,
                    ) + jb)
            }
            proker.forEach {
                kerja += koleksi(k, "proker").document("lama_${k.uid}_proker_${it.id}") to
                    (mapOf<String, Any>(
                        "namaProker" to it.namaProker, "divisiPenanggungJawab" to it.divisiPenanggungJawab,
                        "status" to it.status.name, "catatanEvaluasi" to it.catatanEvaluasi, "dihapus" to false,
                    ) + jb)
            }
            surat.forEach {
                kerja += koleksi(k, "surat").document("lama_${k.uid}_surat_${it.id}") to
                    (mapOf<String, Any>(
                        "tanggalMillis" to it.tanggalMillis, "jenis" to it.jenis.name, "nomorSurat" to it.nomorSurat,
                        "perihal" to it.perihal, "tujuanAtauPengirim" to it.tujuanAtauPengirim, "dihapus" to false,
                    ) + jb)
            }
            inventaris.forEach {
                kerja += koleksi(k, "inventaris").document("lama_${k.uid}_inventaris_${it.id}") to
                    (mapOf<String, Any>(
                        "namaBarang" to it.namaBarang, "jumlah" to it.jumlah, "kondisi" to it.kondisi,
                        "keterangan" to it.keterangan, "dihapus" to false,
                    ) + jb)
            }
            nota.forEach { (n, foto) ->
                kerja += koleksi(k, "nota").document("lama_${k.uid}_nota_${n.id}") to
                    (mapOf<String, Any>(
                        "tanggalMillis" to n.tanggalMillis, "keterangan" to n.keterangan, "nominal" to n.nominal,
                        "prokerTerkait" to n.prokerTerkait, "jenis" to "NOTA", "foto" to foto, "dihapus" to false,
                    ) + jb)
            }
            kerja.chunked(100).forEach { bagian ->
                val batch = db.batch()
                bagian.forEach { (ref, data) -> batch.set(ref, data) }
                batch.commit().await()
            }
            koleksi(k, "riwayat").document()
                .set(catatan(k, "impor", "impor", "", "${kerja.size} data lama dari HP", sekarang)).await()
            HasilAuth.Sukses(kerja.size)
        } catch (e: Exception) {
            val ditolak = (e as? FirebaseFirestoreException)?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED
            HasilAuth.Gagal(
                if (ditolak) "Ditolak server. Pastikan aturan Firestore terbaru sudah dipublish."
                else "Gagal mengunggah: ${e.message ?: "kesalahan tidak diketahui"}"
            )
        }
    }

    // ------------------------------------------------------------ cadangan

    /** Koleksi data desa yang ikut dicadangkan dan dipulihkan (riwayat hanya dicadangkan sebagai arsip). */
    val koleksiCadangan = listOf("anggota", "absensi", "proker", "surat", "inventaris", "nota", "bukti", "ajar")

    /** Identitas desa untuk berkas cadangan, atau null kalau belum login / desa belum diisi. */
    suspend fun identitasDesa(): Pair<String, String>? = konteks()?.let { it.desaId to it.desaNama }

    /**
     * Membaca semua dokumen mentah (termasuk yang ditandai dihapus) satu koleksi.
     * Mencoba server dulu (maks 10 detik), lalu salinan di HP kalau offline.
     * Null kalau tidak bisa dibaca sama sekali.
     */
    suspend fun ambilMentah(nama: String): List<Pair<String, Map<String, Any?>>>? {
        val k = konteks() ?: return null
        val ref = koleksi(k, nama)
        val snap = try {
            withTimeoutOrNull(10_000) { ref.get().await() } ?: ref.get(Source.CACHE).await()
        } catch (e: Exception) {
            try { ref.get(Source.CACHE).await() } catch (e2: Exception) { return null }
        }
        return snap.documents.map { it.id to (it.data ?: emptyMap()) }
    }

    /**
     * Memulihkan dokumen dari cadangan. Dokumen dengan ID sama ditimpa isi cadangan,
     * dokumen yang dibuat setelah cadangan dibiarkan. Satu baris riwayat dicatat.
     * Ditolak kalau cadangan milik desa lain.
     */
    suspend fun pulihkanMentah(
        desaIdCadangan: String,
        data: Map<String, List<Pair<String, Map<String, Any?>>>>,
    ): HasilAuth<Int> {
        val k = konteks() ?: return HasilAuth.Gagal("Desa akun ini belum diisi atau profil belum terbaca.")
        if (k.desaId != desaIdCadangan) {
            return HasilAuth.Gagal("Cadangan ini milik desa lain ($desaIdCadangan), sedangkan akun ini di ${k.desaId}.")
        }
        return try {
            val sekarang = System.currentTimeMillis()
            val kerja = mutableListOf<Pair<DocumentReference, Map<String, Any>>>()
            for (nama in koleksiCadangan) {
                for ((id, isi) in data[nama].orEmpty()) {
                    val bersih = isi.filterValues { it != null }.mapValues { it.value as Any }.toMutableMap()
                    bersih["diubahOleh"] = k.uid
                    bersih["diubahOlehNama"] = k.nama
                    bersih["diubahPada"] = sekarang
                    kerja += koleksi(k, nama).document(id) to bersih
                }
            }
            kerja.chunked(100).forEach { bagian ->
                val batch = db.batch()
                bagian.forEach { (ref, isi) -> batch.set(ref, isi) }
                batch.commit().await()
            }
            koleksi(k, "riwayat").document()
                .set(catatan(k, "cadangan", "pulihkan", "", "${kerja.size} data dipulihkan dari cadangan", sekarang)).await()
            HasilAuth.Sukses(kerja.size)
        } catch (e: Exception) {
            val ditolak = (e as? FirebaseFirestoreException)?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED
            HasilAuth.Gagal(
                if (ditolak) "Ditolak server. Pastikan aturan Firestore terbaru sudah dipublish."
                else "Gagal memulihkan: ${e.message ?: "kesalahan tidak diketahui"}"
            )
        }
    }
}

package com.kkn.almufid.sekretaris.data.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/** Batas jumlah akun yang boleh terdaftar (poin B.10 permintaan pengguna). */
const val KUOTA_PENDAFTARAN = 20

private const val KOLEKSI_USERS = "users"
private const val KOLEKSI_NIM_INDEX = "nim_index"
private const val KOLEKSI_ADMINS = "admins"
private const val DOC_META_PENDAFTARAN = "meta/pendaftaran"

sealed class HasilAuth<out T> {
    data class Sukses<T>(val data: T) : HasilAuth<T>()
    data class Gagal(val pesan: String) : HasilAuth<Nothing>()
}

/**
 * Satu titik akses ke Firebase Auth + Firestore untuk akun & profil
 * pengguna. Login memakai NIM (bukan email) sebagai identitas yang
 * diketik pengguna; di baliknya tetap email Firebase Auth biasa, jadi
 * kita simpan pemetaan NIM -> email di koleksi `nim_index` (bisa dibaca
 * siapa saja tanpa login, supaya proses login bisa menemukan emailnya,
 * tapi hanya bisa ditulis sekali oleh pemilik akun — lihat firestore.rules).
 *
 * Verifikasi akun memakai TAUTAN yang dikirim Firebase ke email terdaftar
 * (bukan kode 6 digit), jadi tidak perlu Cloud Functions / server kirim
 * email kustom — pengguna tinggal klik link di email lalu kembali ke app
 * dan menekan "Saya sudah verifikasi".
 */
class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    val uidSaatIni: String? get() = auth.currentUser?.uid
    val sudahLogin: Boolean get() = auth.currentUser != null
    val emailTerverifikasi: Boolean get() = auth.currentUser?.isEmailVerified == true

    suspend fun daftar(
        namaLengkap: String,
        desa: String,
        divisi: String,
        emailGoogle: String,
        nim: String,
        password: String,
        peran: Peran,
    ): HasilAuth<Unit> {
        val nimBersih = nim.trim()
        if (nimBersih.isBlank()) return HasilAuth.Gagal("NIM wajib diisi")

        // 1) Buat akun Firebase Auth LEBIH DULU. Ini supaya langkah
        //    berikutnya (transaksi Firestore) sudah punya request.auth
        //    terisi -> sesuai firestore.rules yang mewajibkan login untuk
        //    menulis ke meta/pendaftaran & nim_index (mencegah orang luar
        //    nge-spam kuota pendaftaran tanpa pernah bikin akun beneran).
        val uid: String
        try {
            val hasil = auth.createUserWithEmailAndPassword(emailGoogle.trim(), password).await()
            uid = hasil.user?.uid ?: throw IllegalStateException("UID_KOSONG")
        } catch (e: Exception) {
            return HasilAuth.Gagal(pesanErrorAuth(e, "mendaftar"))
        }

        // 2) Jatah kuota + keunikan NIM dicek & "direservasi" dalam satu
        //    transaksi Firestore, supaya dua pendaftar tidak bisa lolos
        //    sekaligus memakai NIM yang sama atau menembus kuota 20 akun
        //    akibat race condition (dua orang daftar persis bersamaan).
        //    Kalau gagal di sini (NIM dipakai / kuota penuh), akun Auth
        //    yang baru dibuat di atas langsung dihapus lagi (rollback)
        //    supaya tidak nyangkut jadi akun yatim tanpa profil.
        try {
            db.runTransaction { tx ->
                val metaRef = db.document(DOC_META_PENDAFTARAN)
                val nimRef = db.collection(KOLEKSI_NIM_INDEX).document(nimBersih)

                val metaSnap = tx.get(metaRef)
                val nimSnap = tx.get(nimRef)

                if (nimSnap.exists()) {
                    throw IllegalStateException("NIM_DIPAKAI")
                }
                val total = metaSnap.getLong("total") ?: 0L
                if (total >= KUOTA_PENDAFTARAN) {
                    throw IllegalStateException("KUOTA_PENUH")
                }

                tx.set(metaRef, mapOf("total" to total + 1))
                tx.set(nimRef, mapOf("reserved" to true))
            }.await()
        } catch (e: Exception) {
            batalkanAkunBaru()
            return HasilAuth.Gagal(
                when (e.message) {
                    "NIM_DIPAKAI" -> "NIM ini sudah terdaftar. Gunakan NIM lain atau hubungi admin."
                    "KUOTA_PENUH" -> "Pendaftaran sudah mencapai batas $KUOTA_PENDAFTARAN akun. Hubungi admin untuk info lebih lanjut."
                    else -> "Gagal memeriksa kuota pendaftaran: ${e.message}"
                }
            )
        }

        // 3) Kirim email verifikasi (tautan klik, bukan kode).
        try {
            auth.currentUser?.sendEmailVerification()?.await()
        } catch (e: Exception) {
            // Akun sudah terlanjur dibuat; jangan gagalkan seluruh proses
            // hanya karena pengiriman email bermasalah — pengguna masih
            // bisa menekan "Kirim ulang" di halaman verifikasi nanti.
        }

        // 4) Simpan profil & finalisasi pemetaan NIM -> uid/email.
        val profil = ProfilPengguna(
            uid = uid,
            namaLengkap = namaLengkap.trim(),
            desa = desa.trim(),
            divisi = divisi.trim(),
            nim = nimBersih,
            email = emailGoogle.trim(),
            peran = peran.name,
            akunDiblokir = false,
            dibuatPadaMillis = System.currentTimeMillis(),
            desaId = desaIdDari(desa),
        )
        return try {
            db.collection(KOLEKSI_USERS).document(uid).set(profil.keMap()).await()
            db.collection(KOLEKSI_NIM_INDEX).document(nimBersih)
                .set(mapOf("uid" to uid, "email" to emailGoogle.trim())).await()
            HasilAuth.Sukses(Unit)
        } catch (e: Exception) {
            HasilAuth.Gagal("Akun dibuat, tapi menyimpan profil gagal: ${e.message}. Coba login ulang.")
        }
    }

    /**
     * Hapus akun Firebase Auth yang baru saja dibuat di langkah 1 [daftar],
     * dipanggil kalau transaksi kuota/NIM di langkah 2 gagal. Best-effort:
     * kalau penghapusan ini sendiri gagal (mis. koneksi putus di saat
     * yang sangat tidak beruntung), admin bisa menghapus akun yatim itu
     * manual lewat Firebase Console -> Authentication. Tidak melempar
     * error ke pengguna di sini karena error ASLI (kegagalan daftar)
     * sudah dilaporkan oleh pemanggil.
     */
    private suspend fun batalkanAkunBaru() {
        try {
            auth.currentUser?.delete()?.await()
        } catch (_: Exception) {
            try { auth.signOut() } catch (_: Exception) {}
        }
    }

    suspend fun kirimUlangVerifikasi(): HasilAuth<Unit> = try {
        auth.currentUser?.sendEmailVerification()?.await()
        HasilAuth.Sukses(Unit)
    } catch (e: Exception) {
        HasilAuth.Gagal(pesanErrorAuth(e, "mengirim ulang email verifikasi"))
    }

    /** Muat ulang status akun dari server lalu laporkan apakah email sudah diklik-verifikasi. */
    suspend fun cekSudahVerifikasi(): Boolean = try {
        auth.currentUser?.reload()?.await()
        auth.currentUser?.isEmailVerified == true
    } catch (e: Exception) {
        false
    }

    suspend fun login(nim: String, password: String): HasilAuth<Peran> {
        val nimBersih = nim.trim()
        val email = try {
            val snap = db.collection(KOLEKSI_NIM_INDEX).document(nimBersih).get().await()
            snap.getString("email")
        } catch (e: Exception) {
            return HasilAuth.Gagal("Tidak bisa menghubungi server. Periksa koneksi internet.")
        } ?: return HasilAuth.Gagal("NIM tidak ditemukan")

        return try {
            auth.signInWithEmailAndPassword(email, password).await()
            val profil = ambilProfil(auth.currentUser!!.uid)
            if (profil?.akunDiblokir == true) {
                auth.signOut()
                return HasilAuth.Gagal("Akun ini diblokir. Hubungi admin.")
            }
            HasilAuth.Sukses(Peran.dariString(profil?.peran))
        } catch (e: Exception) {
            HasilAuth.Gagal(pesanErrorAuth(e, "masuk"))
        }
    }

    suspend fun ambilProfil(uid: String): ProfilPengguna? = try {
        val snap = db.collection(KOLEKSI_USERS).document(uid).get().await()
        snap.toObject(ProfilPengguna::class.java)
    } catch (e: Exception) {
        null
    }

    fun logout() = auth.signOut()

    /**
     * Kirim email "buat sandi baru" ke email yang dipakai saat daftar.
     * Pengguna hanya mengetik NIM; emailnya dicari lewat `nim_index`.
     * Hasil sukses berisi email yang sudah disamarkan (a***@gmail.com)
     * supaya orang lain tidak bisa memakai fitur ini untuk membocorkan
     * alamat email pemilik NIM.
     */
    suspend fun kirimResetSandi(nim: String): HasilAuth<String> {
        val nimBersih = nim.trim()
        if (nimBersih.isBlank()) return HasilAuth.Gagal("NIM wajib diisi")
        val email = try {
            db.collection(KOLEKSI_NIM_INDEX).document(nimBersih).get().await().getString("email")
        } catch (e: Exception) {
            return HasilAuth.Gagal("Tidak bisa menghubungi server. Periksa koneksi internet.")
        } ?: return HasilAuth.Gagal("NIM tidak ditemukan")
        return try {
            auth.sendPasswordResetEmail(email).await()
            HasilAuth.Sukses(samarkanEmail(email))
        } catch (e: Exception) {
            HasilAuth.Gagal(pesanErrorAuth(e, "mengirim email reset sandi"))
        }
    }

    /** Sama seperti [kirimResetSandi], tapi untuk akun yang sedang login (dari halaman Profil). */
    suspend fun kirimResetSandiAkunIni(): HasilAuth<String> {
        val email = auth.currentUser?.email ?: return HasilAuth.Gagal("Akun tidak ditemukan. Coba login ulang.")
        return try {
            auth.sendPasswordResetEmail(email).await()
            HasilAuth.Sukses(samarkanEmail(email))
        } catch (e: Exception) {
            HasilAuth.Gagal(pesanErrorAuth(e, "mengirim email ganti sandi"))
        }
    }

    /** Developer mengirim email reset sandi ke email milik peserta lain. */
    suspend fun kirimResetSandiKe(email: String): HasilAuth<String> {
        if (email.isBlank()) return HasilAuth.Gagal("Email peserta ini kosong")
        return try {
            auth.sendPasswordResetEmail(email).await()
            HasilAuth.Sukses(samarkanEmail(email))
        } catch (e: Exception) {
            HasilAuth.Gagal(pesanErrorAuth(e, "mengirim email reset sandi"))
        }
    }

    /** Catat waktu terakhir akun ini membuka app (dilihat developer di One Eye). Gagal diam-diam. */
    suspend fun catatAktif(uid: String) {
        try {
            db.collection(KOLEKSI_USERS).document(uid)
                .update("terakhirAktifMillis", System.currentTimeMillis()).await()
        } catch (_: Exception) {
        }
    }

    /**
     * Mengisi kunci ruang desa untuk akun lama yang dibuat sebelum fitur sinkronisasi.
     * Aturan Firestore hanya mengizinkan pengisian SEKALI (saat masih kosong).
     */
    suspend fun simpanDesaId(uid: String, desaId: String): Boolean = try {
        db.collection(KOLEKSI_USERS).document(uid).update("desaId", desaId).await()
        true
    } catch (e: Exception) {
        false
    }

    /** Ubah data profil yang boleh diubah sendiri (peran & status blokir tidak termasuk). */
    suspend fun perbaruiProfil(uid: String, nama: String, divisi: String): HasilAuth<Unit> = try {
        db.collection(KOLEKSI_USERS).document(uid).update(
            mapOf(
                "namaLengkap" to nama.trim(),
                "divisi" to divisi.trim(),
            )
        ).await()
        HasilAuth.Sukses(Unit)
    } catch (e: Exception) {
        HasilAuth.Gagal("Gagal menyimpan profil: ${e.message ?: "kesalahan tidak diketahui"}")
    }

    // ---- Khusus lobi developer ----

    /** Semua profil akun terdaftar, diurutkan berdasarkan nama. */
    suspend fun ambilSemuaProfil(): HasilAuth<List<ProfilPengguna>> = try {
        val snap = db.collection(KOLEKSI_USERS).get().await()
        val daftar = snap.documents
            .mapNotNull { it.toObject(ProfilPengguna::class.java) }
            .sortedBy { it.namaLengkap.lowercase() }
        HasilAuth.Sukses(daftar)
    } catch (e: Exception) {
        HasilAuth.Gagal("Gagal memuat daftar anggota: ${e.message ?: "kesalahan tidak diketahui"}")
    }

    /** Angka penghitung kuota pendaftaran (dokumen meta/pendaftaran). */
    suspend fun ambilTotalPendaftaran(): Long = try {
        db.document(DOC_META_PENDAFTARAN).get().await().getLong("total") ?: 0L
    } catch (e: Exception) {
        0L
    }

    /**
     * Admin = punya dokumen `admins/{uid}` yang dibuat MANUAL lewat Firebase
     * Console (klien tidak boleh menulisnya, lihat firestore.rules). Dengan
     * begitu memilih "Developer" saat daftar tidak otomatis memberi hak
     * memblokir akun orang lain.
     */
    suspend fun apakahAdmin(uid: String): Boolean = try {
        db.collection(KOLEKSI_ADMINS).document(uid).get().await().exists()
    } catch (e: Exception) {
        false
    }

    suspend fun aturBlokir(uid: String, blokir: Boolean): HasilAuth<Unit> = try {
        db.collection(KOLEKSI_USERS).document(uid).update("akunDiblokir", blokir).await()
        HasilAuth.Sukses(Unit)
    } catch (e: Exception) {
        HasilAuth.Gagal("Gagal mengubah status blokir. Pastikan akun ini sudah terdaftar sebagai admin di Firebase Console.")
    }

    private fun samarkanEmail(email: String): String {
        val at = email.indexOf('@')
        if (at <= 1) return email
        val depan = email.substring(0, 1)
        val bintang = "*".repeat(at - 1)
        val belakang = email.substring(at)
        return depan + bintang + belakang
    }

    private fun pesanErrorAuth(e: Exception, konteks: String): String {
        val kode = (e as? FirebaseAuthException)?.errorCode
        return when (kode) {
            "ERROR_INVALID_EMAIL" -> "Format email Google tidak valid"
            "ERROR_EMAIL_ALREADY_IN_USE" -> "Email ini sudah terdaftar di akun lain"
            "ERROR_WEAK_PASSWORD" -> "Sandi terlalu lemah, minimal 6 karakter"
            "ERROR_INVALID_CREDENTIAL", "ERROR_WRONG_PASSWORD" -> "NIM atau sandi salah"
            "ERROR_USER_NOT_FOUND" -> "Akun tidak ditemukan"
            "ERROR_USER_DISABLED" -> "Akun ini dinonaktifkan. Hubungi admin."
            "ERROR_TOO_MANY_REQUESTS" -> "Terlalu banyak percobaan. Coba lagi beberapa saat lagi."
            "ERROR_NETWORK_REQUEST_FAILED" -> "Tidak ada koneksi internet"
            else -> "Gagal $konteks: ${e.message ?: "kesalahan tidak diketahui"}"
        }
    }
}

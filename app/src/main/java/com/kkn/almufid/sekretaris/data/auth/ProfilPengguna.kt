package com.kkn.almufid.sekretaris.data.auth

/**
 * Cerminan dokumen Firestore di `users/{uid}`. Field apa adanya (bukan
 * data class Room) karena ini murni objek transport ke/dari cloud.
 *
 * @JvmOverloads WAJIB ada: deserialisasi otomatis Firestore
 * (`snapshot.toObject(ProfilPengguna::class.java)`) butuh constructor
 * tanpa argumen lewat refleksi Java. Tanpa anotasi ini, Kotlin hanya
 * membuat SATU constructor (dengan semua parameter), bukan constructor
 * kosong, walau semua parameternya punya nilai default.
 */
data class ProfilPengguna @JvmOverloads constructor(
    val uid: String = "",
    val namaLengkap: String = "",
    val desa: String = "",
    val divisi: String = "",
    val nim: String = "",
    val email: String = "",
    val peran: String = Peran.PESERTA.name,
    val akunDiblokir: Boolean = false,
    val dibuatPadaMillis: Long = 0L,
) {
    /** Dipakai Firestore untuk serialisasi: butuh constructor tanpa argumen + getter standar,
     * data class Kotlin dengan nilai default di atas sudah memenuhi syarat itu. */
    fun keMap(): Map<String, Any?> = mapOf(
        "uid" to uid,
        "namaLengkap" to namaLengkap,
        "desa" to desa,
        "divisi" to divisi,
        "nim" to nim,
        "email" to email,
        "peran" to peran,
        "akunDiblokir" to akunDiblokir,
        "dibuatPadaMillis" to dibuatPadaMillis,
    )
}

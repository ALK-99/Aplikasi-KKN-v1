package com.kkn.almufid.sekretaris.data.auth

/** Peran akun. Alur daftar/login sama persis untuk keduanya — yang beda
 * hanya menu lobi setelah berhasil masuk (lihat DeveloperLobbyScreen). */
enum class Peran {
    PESERTA,
    DEVELOPER;

    companion object {
        fun dariString(nilai: String?): Peran =
            entries.firstOrNull { it.name == nilai } ?: PESERTA
    }
}

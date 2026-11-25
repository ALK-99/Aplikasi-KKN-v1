package com.kkn.almufid.sekretaris.data.cloud

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import com.kkn.almufid.sekretaris.data.auth.HasilAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.security.SecureRandom
import java.util.Date

/** Satu kode akses premium (koleksi `premium_kode`, ID dokumen = kodenya). */
data class KodePremium @JvmOverloads constructor(
    val kode: String = "",
    val hari: Long = 0L,
    val catatan: String = "",
    val dibuatMillis: Long = 0L,
    val dipakaiOleh: String = "",
    val dipakaiNama: String = "",
    val dipakaiMillis: Long = 0L,
    val dicabut: Boolean = false,
)

data class StatusPremium(val aktif: Boolean, val sampaiMillis: Long, val admin: Boolean)
data class KunciAi(val gemini: String, val openrouter: String, val model: String)

/**
 * Premium = kode akses dari developer + masa aktif. Semuanya di Firestore paket gratis (tanpa server):
 *  - premium_kode/{kode}: dibuat developer, dipakai sekali.
 *  - users/{uid}.premiumSampai: tanggal berakhir, hanya bisa terisi lewat penukaran kode yang valid (aturan Firestore).
 *  - config/ai_premium: kunci AI bersama, hanya bisa dibaca akun premium (aturan Firestore memakai jam server).
 */
class PremiumRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
) {
    private val hariMs = 86_400_000L

    /** Jam server (dari header HTTP Google); bila gagal memakai jam HP. */
    suspend fun waktuServer(): Long = withContext(Dispatchers.IO) {
        try {
            val c = URL("https://www.gstatic.com/generate_204").openConnection() as HttpURLConnection
            try {
                c.connectTimeout = 6000; c.readTimeout = 6000; c.requestMethod = "HEAD"
                c.responseCode
                val d = c.date
                if (d > 0) d else System.currentTimeMillis()
            } finally { c.disconnect() }
        } catch (e: Exception) { System.currentTimeMillis() }
    }

    suspend fun status(): StatusPremium {
        val uid = auth.currentUser?.uid ?: return StatusPremium(false, 0L, false)
        val admin = try { db.collection("admins").document(uid).get().await().exists() } catch (e: Exception) { false }
        val sampai = try { db.collection("users").document(uid).get().await().getTimestamp("premiumSampai")?.toDate()?.time ?: 0L } catch (e: Exception) { 0L }
        return StatusPremium(admin || sampai > waktuServer(), sampai, admin)
    }

    suspend fun tukar(kodeMentah: String, namaSaya: String): HasilAuth<Long> {
        val uid = auth.currentUser?.uid ?: return HasilAuth.Gagal("Belum login.")
        val kode = kodeMentah.trim().uppercase()
        if (kode.length < 6) return HasilAuth.Gagal("Isi kode dengan lengkap.")
        return try {
            val ref = db.collection("premium_kode").document(kode)
            val d = ref.get(Source.SERVER).await()
            if (!d.exists()) return HasilAuth.Gagal("Kode tidak ditemukan.")
            if (d.getBoolean("dicabut") == true) return HasilAuth.Gagal("Kode ini sudah dicabut.")
            if (!d.getString("dipakaiOleh").isNullOrEmpty()) return HasilAuth.Gagal("Kode ini sudah dipakai.")
            val hari = d.getLong("hari") ?: return HasilAuth.Gagal("Kode rusak.")
            val sekarang = waktuServer()
            val lama = db.collection("users").document(uid).get().await().getTimestamp("premiumSampai")?.toDate()?.time ?: 0L
            val sampai = maxOf(lama, sekarang) + hari * hariMs
            val batch = db.batch()
            batch.update(ref, mapOf("dipakaiOleh" to uid, "dipakaiNama" to namaSaya, "dipakaiMillis" to sekarang))
            batch.update(db.collection("users").document(uid), mapOf("premiumSampai" to Timestamp(Date(sampai)), "premiumKode" to kode))
            batch.commit().await()
            HasilAuth.Sukses(sampai)
        } catch (e: Exception) {
            HasilAuth.Gagal("Gagal menukar kode: " + (e.message ?: "periksa internet") + ". Pastikan aturan Firestore terbaru sudah dipublikasikan.")
        }
    }

    /** Developer: buat kode baru. */
    suspend fun buat(hari: Int, catatan: String): HasilAuth<String> = try {
        val abjad = "ABCDEFGHJKMNPQRSTUVWXYZ23456789"
        val r = SecureRandom()
        fun potong() = (1..4).map { abjad[r.nextInt(abjad.length)] }.joinToString("")
        val kode = "KKN-${potong()}-${potong()}"
        db.collection("premium_kode").document(kode).set(
            mapOf("hari" to hari.toLong(), "catatan" to catatan.trim(), "dibuatMillis" to System.currentTimeMillis(), "dipakaiOleh" to "", "dipakaiNama" to "", "dipakaiMillis" to 0L, "dicabut" to false)
        ).await()
        HasilAuth.Sukses(kode)
    } catch (e: Exception) { HasilAuth.Gagal("Gagal membuat kode: " + (e.message ?: "")) }

    suspend fun daftarKode(): List<KodePremium> = try {
        db.collection("premium_kode").orderBy("dibuatMillis", Query.Direction.DESCENDING).limit(100).get().await()
            .documents.map { d -> (d.toObject(KodePremium::class.java) ?: KodePremium()).copy(kode = d.id) }
    } catch (e: Exception) { emptyList() }

    suspend fun cabut(kode: String): Boolean = try { db.collection("premium_kode").document(kode).update("dicabut", true).await(); true } catch (e: Exception) { false }

    suspend fun ambilKunci(): KunciAi? = try {
        val d = db.collection("config").document("ai_premium").get(Source.SERVER).await()
        if (!d.exists()) null else KunciAi(d.getString("gemini").orEmpty(), d.getString("openrouter").orEmpty(), d.getString("model").orEmpty())
    } catch (e: Exception) { null }

    suspend fun simpanKunci(gemini: String, openrouter: String, model: String): Boolean = try {
        db.collection("config").document("ai_premium").set(mapOf("gemini" to gemini.trim(), "openrouter" to openrouter.trim(), "model" to model.trim())).await(); true
    } catch (e: Exception) { false }
}

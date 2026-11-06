# Sekretaris KKN

Aplikasi Android untuk tugas sekretaris & wakil kepala desa selama KKN:
Jurnal & Notulen, Absensi, Surat Masuk/Keluar, Program Kerja, Arsip Nota (foto), Inventaris Posko.
Data operasional (jurnal, absensi, surat, proker, nota, inventaris) tersimpan **offline** di HP.
Akun pengguna (daftar/login/verifikasi email) memakai **Firebase** dan butuh internet — lihat
bagian "Setup Firebase" di bawah sebelum aplikasi bisa dipakai.

## 1. Langkah awal (sekali saja)
1. Salin SEMUA isi folder ini ke dalam folder repo GitHub Anda yang kosong (termasuk folder tersembunyi `.github`).
2. Buka `app/build.gradle.kts`, ganti dua baris ini:
   - `GANTI_USERNAME_GITHUB` -> username GitHub Anda
   - `GANTI_NAMA_REPO` -> nama repo Anda
3. Push ke GitHub.

## Setup Firebase (wajib, sekali saja)
Tanpa langkah ini aplikasi tetap ter-build, tapi halaman Daftar/Login tidak akan
berfungsi (akan muncul error "tidak bisa menghubungi server").

1. Buka [Firebase Console](https://console.firebase.google.com) -> **Add project** ->
   beri nama bebas (mis. "Sekretaris KKN").
2. Di dalam project, klik ikon Android -> **Add app** -> isi
   **Android package name** persis: `com.kkn.almufid.sekretaris` -> Register app.
3. Unduh file `google-services.json` yang ditawarkan, taruh persis di folder `app/`
   (sejajar dengan `app/build.gradle.kts`). File ini **sengaja tidak ikut di-commit**
   ke GitHub (lihat `.gitignore`) karena isinya kunci khusus project Firebase Anda.
4. Di menu kiri Firebase Console:
   - **Authentication** -> tab Sign-in method -> aktifkan **Email/Password**.
   - **Authentication** -> Templates -> Email address verification: boleh dibiarkan
     bawaan, atau ganti bahasanya ke Indonesia/custom teks sesuai selera.
   - **Firestore Database** -> Create database -> pilih lokasi Asia (mis. `asia-southeast2`)
     -> mode Production.
   - Tempel isi file `firestore.rules` (di root repo ini) ke tab **Rules** Firestore
     Console, lalu **Publish**. Tanpa ini, semua akses database akan ditolak secara default.
   - **Storage** -> Get started -> pilih lokasi yang sama dengan Firestore (untuk
     penyimpanan foto/upload per-akun ke depannya).
5. Build ulang / jalankan aplikasi dari Android Studio seperti biasa.

### Build otomatis lewat GitHub Actions (opsional tapi disarankan)
Supaya APK hasil GitHub Actions (lihat bagian 3 di bawah) juga punya Firebase aktif:
1. Encode file `google-services.json` ke base64:
   `base64 -w0 app/google-services.json` (Linux/Mac) atau cara setara di Windows.
2. Di GitHub: repo Settings -> Secrets and variables -> Actions -> New repository secret
   -> nama `GOOGLE_SERVICES_JSON`, isi dengan hasil base64 tadi -> Add secret.
3. Workflow build-release.yml sudah otomatis membaca secret ini dan menaruhnya
   sebagai `app/google-services.json` sebelum build. Tanpa secret ini, build CI
   tetap berhasil tapi APK-nya tidak punya fitur akun (ada warning di log Actions).

## 2. Menjalankan di HP lewat Android Studio
1. Buka Android Studio -> **Open** -> pilih folder project ini. Tunggu "Gradle sync" selesai (pertama kali lama, butuh internet).
2. Di HP: aktifkan Opsi Pengembang -> **USB debugging**, sambungkan kabel.
3. Pilih HP di bar atas Android Studio, klik tombol hijau **Run**.
   (Matikan VM Kali Linux dulu supaya RAM laptop cukup.)

## 3. Merilis update (tanpa Android Studio)
1. Naikkan perubahan kode ke GitHub (commit & push).
2. Buat tag versi baru, misalnya:
   `git tag v1.0.1` lalu `git push origin v1.0.1`
3. GitHub Actions otomatis build APK dan memasangnya di halaman **Releases**
   (lihat tab Actions untuk progresnya, sekitar 5-8 menit).
4. Buka aplikasi di HP (ada internet) -> muncul pop-up **Update tersedia** -> Unduh & Pasang.
   Data lama TIDAK hilang.

## Catatan soal akun pengguna
- Login memakai **NIM**, bukan email, tapi di baliknya tetap akun Firebase biasa
  (email yang didaftarkan + sandi buatan sendiri).
- Verifikasi akun memakai **tautan di email** (klik link, bukan kode angka) — setelah
  klik, kembali ke app dan tekan "Saya Sudah Verifikasi".
- Pendaftaran dibatasi **50 akun**. Untuk menaikkan atau mereset batas ini, buka
  Firestore Console -> dokumen `meta/pendaftaran` -> ubah field `total` secara manual.
- "Lupa sandi" & limit 3x gagal login mengarahkan ke WhatsApp admin (081384725967),
  bukan reset sandi otomatis — sesuai permintaan awal. Admin bisa reset sandi pengguna
  manual lewat Firebase Console -> Authentication -> klik user -> Reset password.
- Untuk memblokir akun tertentu: Firestore Console -> `users/{uid}` -> ubah field
  `akunDiblokir` jadi `true`. Pengguna itu akan otomatis logout & ditolak login berikutnya.

## Catatan penting
- **Repo harus Publik** agar pengecekan update jalan tanpa token. Konsekuensi: file keystore
  (`app/sekretaris-kkn.jks`, password di `app/build.gradle.kts`) ikut terlihat publik. Untuk aplikasi
  pribadi ini risikonya kecil, tapi jangan pakai keystore ini untuk aplikasi lain.
- **Jangan hapus/ganti keystore.** Android hanya mau meng-update jika tanda tangan APK sama.
- Pertama kali memasang update, Android meminta izin "Pasang aplikasi tidak dikenal" - izinkan, lalu buka ulang app.
- Backup data: salin HP secara berkala atau tunggu fitur Google Drive/Telegram (ditunda).

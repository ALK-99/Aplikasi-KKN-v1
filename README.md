# Sekretaris KKN

Aplikasi Android untuk tugas sekretaris & wakil kepala desa selama KKN:
Jurnal & Notulen, Absensi, Surat Masuk/Keluar, Program Kerja, Arsip Nota (foto), Inventaris Posko.
Semua data tersimpan **offline** di HP. Internet hanya dipakai untuk cek update.

## 1. Langkah awal (sekali saja)
1. Salin SEMUA isi folder ini ke dalam folder repo GitHub Anda yang kosong (termasuk folder tersembunyi `.github`).
2. Buka `app/build.gradle.kts`, ganti dua baris ini:
   - `GANTI_USERNAME_GITHUB` -> username GitHub Anda
   - `GANTI_NAMA_REPO` -> nama repo Anda
3. Push ke GitHub.

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

## Catatan penting
- **Repo harus Publik** agar pengecekan update jalan tanpa token. Konsekuensi: file keystore
  (`app/sekretaris-kkn.jks`, password di `app/build.gradle.kts`) ikut terlihat publik. Untuk aplikasi
  pribadi ini risikonya kecil, tapi jangan pakai keystore ini untuk aplikasi lain.
- **Jangan hapus/ganti keystore.** Android hanya mau meng-update jika tanda tangan APK sama.
- Pertama kali memasang update, Android meminta izin "Pasang aplikasi tidak dikenal" - izinkan, lalu buka ulang app.
- Backup data: salin HP secara berkala atau tunggu fitur Google Drive/Telegram (ditunda).

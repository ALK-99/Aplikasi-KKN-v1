# KKN Al-Mushlih 2027

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
   beri nama bebas (mis. "KKN Al-Mushlih 2027").
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
- Pendaftaran dibatasi **20 akun**. Untuk menaikkan atau mereset batas ini, buka
  Firestore Console -> dokumen `meta/pendaftaran` -> ubah field `total` secara manual.
- "Lupa sandi" & limit 3x gagal login mengarahkan ke WhatsApp admin (081384725967),
  bukan reset sandi otomatis — sesuai permintaan awal. Admin bisa reset sandi pengguna
  manual lewat Firebase Console -> Authentication -> klik user -> Reset password.
- Untuk memblokir akun tertentu: Firestore Console -> `users/{uid}` -> ubah field
  `akunDiblokir` jadi `true`. Pengguna itu akan otomatis logout & ditolak login berikutnya.

## Data desa & audit hak akses (Tahap A)
- Anggota, absensi, proker, surat, dan inventaris sekarang tersimpan di Firestore per desa
  (`desa/{desaId}/...`) dan tampil sama di semua HP satu desa. Firestore menyimpan salinan
  offline, jadi tetap bisa dibuka tanpa sinyal dan otomatis sinkron saat online.
- Desa dikunci setelah daftar. Untuk memindahkan peserta ke desa lain, admin mengubah field
  `desa` dan `desaId` di `users/{uid}` lewat Firebase Console.
- Anggota tidak pernah dihapus, hanya **dinonaktifkan**; absensi menyimpan salinan nama, jadi
  laporan lama tetap utuh. Proker/surat/barang yang "dihapus" hanya ditandai `dihapus`.
- Setiap perubahan menulis satu baris `riwayat` (siapa, kapan, apa) yang tidak bisa diubah/dihapus.
- Aturan keamanan: hanya anggota desa yang sama (dan tidak diblokir) boleh membaca/menulis data
  desa; `uid/NIM/email/desa` profil tidak bisa diganti sendiri; `nim_index` tidak bisa di-list;
  `meta/pendaftaran` hanya boleh naik/turun satu; tidak ada hapus permanen dari aplikasi.
- Data lama di HP (sebelum v1.11) diunggah lewat kartu **Data lama ditemukan** di Beranda
  (sekali saja per akun, aman diulang karena ID tetap). **Cukup satu orang per desa** yang
  mengunggah data yang sama.
- Setelah memperbarui `firestore.rules`, tempel ulang di Console > Firestore > Rules > Publish.

## Cadangan & pemulihan (Tahap B)
- Menu **Laporan & Backup → Cadangan Lengkap + Foto (ZIP)** membuat satu berkas ZIP berisi: data desa
  (anggota, absensi, proker, surat, inventaris, riwayat), jurnal pribadi dan nota di HP, foto nota, dan
  ringkasan mudah dibaca (`baca-saja.json`, termasuk jurnal bersama, tugas, kas). Bagikan ke WhatsApp/Drive.
- **Pulihkan dari Cadangan**: pilih ZIP, lihat ringkasan, lalu konfirmasi. Data HP yang sudah ada dilewati
  (tidak ganda); data desa ber-ID sama dikembalikan ke isi cadangan; data baru tidak dihapus.
  Cadangan milik desa lain ditolak.
- Jurnal bersama, tugas, kas, pengumuman, dan polling hanya ikut sebagai arsip baca (tidak dipulihkan
  dari app karena aturan keamanan hanya mengizinkan penulis/admin).
- Semua gratis: tanpa Firebase Storage, foto hanya di dalam ZIP.

## Manajemen proker, nota, rekonsiliasi, riwayat (Tahap C)
- **Proker**: ketuk kartu untuk mengisi target, jadwal (dd/mm/yyyy), PIC, anggaran, realisasi, evaluasi,
  dan **bukti kegiatan** (foto dari kamera/galeri). Foto dikompres (maks ~180 KB) dan disimpan di Firestore
  desa, bukan Firebase Storage, jadi tetap gratis dan terlihat di semua HP satu desa.
- **Arsip Nota** sekarang per desa (cloud) dan mendukung jenis **Nota** atau **Kwitansi**. Nota lama di HP
  diunggah lewat kartu "Data lama ditemukan" di Beranda (aman diulang).
- **Rekonsiliasi Kas**: mencocokkan pengeluaran Kas Kelompok dengan nota (nominal sama, selisih tanggal
  maksimal 14 hari), menampilkan kas tanpa nota, nota tanpa kas, dan anggaran vs realisasi vs nota per proker.
- **Laporan baru**: Pertanggungjawaban Proker, Register Surat, Rekonsiliasi Kas (PDF).
- **Riwayat Perubahan** (menu Lainnya): siapa mengubah apa dan kapan, bisa disaring per jenis data.
- Aturan Firestore **tidak berubah** di tahap ini (koleksi `nota` dan `bukti` masuk aturan desa yang sama).

## Catatan penting
- **Repo harus Publik** agar pengecekan update jalan tanpa token. Konsekuensi: file keystore
  (`app/sekretaris-kkn.jks`, password di `app/build.gradle.kts`) ikut terlihat publik. Untuk aplikasi
  pribadi ini risikonya kecil, tapi jangan pakai keystore ini untuk aplikasi lain.
- **Jangan hapus/ganti keystore.** Android hanya mau meng-update jika tanda tangan APK sama.
- Pertama kali memasang update, Android meminta izin "Pasang aplikasi tidak dikenal" - izinkan, lalu buka ulang app.
- Backup data: salin HP secara berkala atau tunggu fitur Google Drive/Telegram (ditunda).

## Akun admin (developer)

Memilih peran "Developer" saat daftar hanya mengubah tampilan lobi. Supaya
akun developer boleh **memblokir/membuka blokir** akun lain dari dalam app:

1. Login sekali sebagai developer, lalu buka Firebase Console > Authentication
   dan salin **UID** akun itu.
2. Firestore Database > Start collection > ID koleksi `admins`.
3. Document ID = UID tadi, isi satu field apa saja (mis. `aktif` = true).
4. Tempel ulang isi `firestore.rules` ke tab Rules, lalu Publish.

Tanpa langkah ini lobi developer tetap bisa dibuka, tapi hanya mode lihat saja.

Akun developer punya menu sendiri (Pusat Kendali): One Eye (ringkasan, daftar
peserta + detail, linimasa aktivitas), Pengumuman, Polling, Tugas, Kas,
Kelola Akun (blokir, reset sandi), Moderasi Jurnal, Kotak Masuk (saran), dan
Ekspor Data (CSV/PDF/JSON). Semua gratis (Firestore Spark, tanpa server).
Setelah memperbarui app, tempel ulang `firestore.rules` terbaru: ada aturan
baru untuk moderasi jurnal dan pembacaan saran oleh admin.


## Tahap D: tampilan dan navigasi
- Mode terang/gelap/ikuti sistem (Pengaturan > Tampilan), tata letak adaptif HP dan tablet (lebar maksimal 720dp, kolom menu menyesuaikan).
- Menu Utama dibagi divisi: Sekretaris, Bendahara, Media (berisi Jurnal), Pendidikan (segera). Pengumuman ada di Umum.
- Pencarian global (ikon kaca pembesar di Beranda), panduan awal (onboarding), Cek Pembaruan di Pengaturan.

## Tahap E: pengingat, widget, templat surat
- Pengingat: worker 15 menit memeriksa tugas, jadwal proker, absensi hari ini (setelah 16.00), dan pengumuman belum dibaca. Notifikasi diulang dengan getar dan lampu sampai diketuk. Dapat dimatikan di Pengaturan > Pengingat.
- Widget layar utama: sholat berikutnya (Yogyakarta), progres proker, jumlah pengingat.
- Templat Surat PDF (divisi Sekretaris): Undangan, Permohonan, Pemberitahuan, Terima Kasih; opsional dicatat sebagai surat keluar.

## Tahap F: versi PC (web)
Folder `web/` adalah aplikasi web dan PC lengkap (baca dan tulis) dengan login NIM yang sama, tabel yang bisa dicari dan diurutkan, ekspor CSV, dan cetak. Dibungkus menjadi aplikasi Windows oleh `.github/workflows/pc-build.yml` (tag `pc-v...`).
Pengaturan satu kali: isi `web/firebase-config.js` dari Firebase Console (Web app), tambahkan domain `alk-99.github.io` di Authentication > Settings > Authorized domains, lalu aktifkan Pages (Settings > Pages > Source: GitHub Actions).

## Tahap G: mode demo dan paritas PC dan Android
- Mode demo: semua akun yang login boleh membuat tugas, kas, pengumuman, dan polling. Aturan ada di `firestore.rules` (publish ulang), flag `MODE_DEMO` di KoordinasiViewModel. Saat produksi, kembalikan ke admin.
- Android: ubah data anggota, surat, inventaris, nota. Pembaruan hanya membaca rilis APK (tag `v` + angka), rilis PC (`pc-v...`) otomatis pre-release.
- PC: profil, ubah data, laporan PDF, cadangan/pulihkan ZIP (sama dengan HP), peta, pengaturan (tema, pengingat, saran, nomor darurat), Pusat Kendali (admin), notifikasi berulang, cek pembaruan, panduan awal.

## Tahap H: divisi Pendidikan
Menu Jadwal Mengajar (hari, jam, lembaga/kelas, materi, pengajar, catatan) di Android dan PC, tersimpan di `desa/{desaId}/ajar`, ikut cadangan, pencarian, dan PDF di PC.

## Tahap I
- Menu **Dokumen & PDF** (Sekretaris): templat surat, laporan, rekap absensi bulanan, dan cadangan jadi satu tempat (Android dan PC).
- Edit isi tugas, kas, pengumuman, polling; absensi Hadir/Izin/Sakit/Alpa (`status`, `hadir` tetap sinkron).
- PC: ikon aplikasi, mode offline (cache Firestore + antrean tulis), pasang pembaruan otomatis dari GitHub, baki sistem (tombol X menyembunyikan), jalan otomatis saat Windows menyala.
- Pantauan Desa (admin baca semua desa) dan Jurnal Pribadi di PC. **Wajib publish ulang `firestore.rules`** (aturan baca admin + `users/{uid}/jurnal_pribadi`).

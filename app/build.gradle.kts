plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

// google-services.json BELUM ditrack di git (lihat .gitignore) karena isinya
// spesifik per-project Firebase dan harus didownload sendiri dari Firebase
// Console (Project settings > Your apps > google-services.json), lalu
// ditaruh di folder app/ ini. Plugin hanya diterapkan jika file itu ADA,
// supaya `gradle build` tidak langsung gagal total di mesin/CI yang belum
// punya file tersebut — build tetap jalan, hanya fitur Firebase yang akan
// crash saat runtime sampai file-nya ditambahkan.
val adaGoogleServices = file("google-services.json").exists()
if (adaGoogleServices) {
    apply(plugin = "com.google.gms.google-services")
} else {
    logger.warn(
        "PERINGATAN: app/google-services.json tidak ditemukan — fitur " +
            "Firebase (akun, database cloud) TIDAK akan berfungsi sampai " +
            "file itu ditambahkan. Lihat README untuk cara setup."
    )
}

android {
    namespace = "com.kkn.almufid.sekretaris"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.kkn.almufid.sekretaris"
        minSdk = 24
        targetSdk = 34
        // Dari GitHub Actions: versi diambil dari nama tag rilis (mis. v1.0.1)
        // sehingga versi di dalam APK SELALU sama dengan tag di GitHub.
        // Jika dibuild dari Android Studio, dipakai nilai default di bawah.
        versionCode = (project.findProperty("appVersionCode") as String?)?.toIntOrNull() ?: 1
        versionName = (project.findProperty("appVersionName") as String?) ?: "1.0.0"

        // Dipakai UpdateChecker untuk tahu repo GitHub mana yang dicek.
        // Ganti nilai ini sesuai repo Anda sendiri.
        buildConfigField("String", "GITHUB_OWNER", "\"ALK-99\"")
        buildConfigField("String", "GITHUB_REPO", "\"Download-Aplikasi-KKN\"")
    }

    // Keystore TETAP (sekretaris-kkn.jks). Penting: Android hanya mau
    // meng-update aplikasi jika tanda tangan APK baru sama dengan yang
    // lama. Tanpa keystore tetap, setiap build di GitHub Actions memakai
    // kunci acak berbeda sehingga update gagal ("aplikasi tidak
    // terpasang / konflik paket"). Jangan hapus atau ganti file ini.
    signingConfigs {
        create("tetap") {
            storeFile = file("sekretaris-kkn.jks")
            storePassword = "sekretaris123"
            keyAlias = "sekretaris"
            keyPassword = "sekretaris123"
        }
    }

    buildTypes {
        // Debug (dari Android Studio) juga memakai kunci yang sama, supaya
        // APK rilis dari GitHub bisa menimpa versi yang dipasang lewat kabel.
        debug {
            signingConfig = signingConfigs.getByName("tetap")
        }
        release {
            signingConfig = signingConfigs.getByName("tetap")
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.05.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.1")
    implementation("androidx.activity:activity-compose:1.9.0")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3:1.2.1")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // Room — database lokal offline-first
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // Coroutines untuk operasi database & jaringan asinkron
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    // Jembatan Task (Firebase/Google Play Services) <-> suspend fun (.await())
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")

    // Coil — memuat foto nota dari file lokal ke Composable secara efisien
    // (auto-caching & downsampling, menghindari OutOfMemoryError kalau
    // foto nota beresolusi besar).
    implementation("io.coil-kt:coil-compose:2.6.0")

    // Firebase — akun (Auth), database cloud per-akun (Firestore).
    // BoM mengunci semua versi library Firebase agar saling kompatibel.
    implementation(platform("com.google.firebase:firebase-bom:33.1.2"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-storage-ktx")

    // EncryptedSharedPreferences — penyimpanan lokal terenkripsi untuk
    // status "ingat saya" & penghitung percobaan login gagal.
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}

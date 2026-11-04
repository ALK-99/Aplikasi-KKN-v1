// Top-level build file. Versi plugin dikunci secara eksplisit agar build
// konsisten dan tidak terpengaruh update otomatis yang bisa memicu error
// ketidakcocokan versi (AGP/Kotlin/KSP harus saling kompatibel).
plugins {
    id("com.android.application") version "8.4.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.24" apply false
    id("com.google.devtools.ksp") version "1.9.24-1.0.20" apply false
}

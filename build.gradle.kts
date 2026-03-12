// Top-level build file for all modules
plugins {
    id("com.android.application") version "8.7.2" apply false
    id("org.jetbrains.kotlin.android") version "2.2.21" apply false
    // KSP version must match your Kotlin version
    id("com.google.devtools.ksp") version "2.2.21-2.0.5" apply false
    // Firebase (OTP)
    id("com.google.gms.google-services") version "4.4.2" apply false
}
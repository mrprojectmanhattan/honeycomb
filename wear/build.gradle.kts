plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    // Namespace differs from the phone's so the two R classes never collide;
    // applicationId is the SAME as the phone's on purpose: the Data Layer only
    // delivers between apps with the same package name and signing key.
    namespace = "com.mark.moodlogger.watch"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.mark.moodlogger"
        minSdk = 30
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(composeBom)

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.wear.compose:compose-material:1.4.0")
    implementation("androidx.wear.compose:compose-foundation:1.4.0")

    // Data Layer: a local link to the paired phone (Bluetooth / Wi-Fi Direct).
    // No INTERNET permission in this module either.
    implementation("com.google.android.gms:play-services-wearable:18.2.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")

    // BridgingManager/BridgingConfig: opt this watch app's notifications out of
    // phone->watch bridging (see BridgingSetup.kt for why).
    implementation("androidx.wear:wear-phone-interactions:1.1.0")
}

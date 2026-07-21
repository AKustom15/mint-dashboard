plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.parcelize)
    id("maven-publish")
}

android {
    namespace = "com.akustom15.mint.library"
    compileSdk = 36

    defaultConfig {
        minSdk = 29
        
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    api(libs.androidx.core.ktx)
    api(libs.androidx.appcompat)
    api(libs.material)
    
    // Compose
    api(platform(libs.androidx.compose.bom))
    api(libs.androidx.compose.ui)
    api(libs.androidx.compose.ui.graphics)
    api(libs.androidx.compose.ui.tooling.preview)
    api(libs.androidx.compose.material3)
    api(libs.androidx.compose.material.icons.extended)
    api(libs.androidx.activity.compose)
    api(libs.androidx.lifecycle.runtime.ktx)
    api(libs.androidx.lifecycle.viewmodel.compose)
    api(libs.androidx.navigation.compose)
    
    // Coil
    api(libs.coil.compose)
    api(libs.coil.gif)

    // Lottie
    api(libs.lottie.compose)

    // Real Blur — Haze (GPU-accelerated Compose frosted glass)
    api(libs.haze)
    api(libs.haze.materials)

    // Kustom Official API - Required for KWGT/KLWP pack discovery & preset loading
    api("org.bitbucket.frankmonza:kustomapi:21")

    // Network & Data
    api(libs.okhttp)
    api(libs.gson)

    // Firebase (transitive to app)
    api(platform(libs.firebase.bom))
    api(libs.firebase.firestore)
    api(libs.firebase.messaging)
    api(libs.firebase.analytics)
    api(libs.kotlinx.coroutines.play.services)

    // Coroutines
    api("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // Security & Anti-Piracy
    api("com.google.android.play:integrity:1.4.0")
    // Firebase App Check (Play Integrity provider) — protects Firestore/FCM backends
    api(libs.firebase.appcheck)
    // Firebase Functions — client calls the server-side license verifier
    api(libs.firebase.functions)
    // Encrypted storage for premium/entitlement state
    api(libs.androidx.security.crypto)

    // Google Play Billing (premium icon requests)
    api(libs.billing.ktx)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

publishing {
    publications {
        register<MavenPublication>("release") {
            groupId = "com.github.rs1525"
            artifactId = "mint-library"
            version = "1.0.0"

            afterEvaluate {
                from(components["release"])
            }
        }
    }
}

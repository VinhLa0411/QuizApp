plugins {
    alias(libs.plugins.android.application)   // dùng alias từ libs.versions.toml
    id("com.google.gms.google-services")      // KHÔNG ghi version ở đây
}

android {
    namespace = "com.example.quizapp"
    compileSdk = 35
    buildFeatures { viewBinding = true }
    defaultConfig {
        applicationId = "com.example.quizapp"
        minSdk = 24            // 24 là mức an toàn, bạn có thể để 26 nếu muốn
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Nếu bạn code Java thuần:
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    // Nếu có Kotlin module thì mới cần kotlinOptions
    // kotlinOptions { jvmTarget = "17" }
}

dependencies {
    // Google Sign-In
    implementation("com.google.android.gms:play-services-auth:21.2.0")

    // ---- Firebase: DÙNG 1 BoM DUY NHẤT ----
    implementation(platform("com.google.firebase:firebase-bom:33.3.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    // (Tuỳ chọn) Analytics nếu bạn cần
    // implementation("com.google.firebase:firebase-analytics")

    // AndroidX cơ bản
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
}

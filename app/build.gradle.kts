import java.util.Properties


// 2. local.properties 파일을 읽어오는 로직 추가
val properties = Properties().apply {
    val propertiesFile = project.rootProject.file("local.properties")
    if (propertiesFile.exists()) {
        load(propertiesFile.inputStream())
    }
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
   // id("com.google.gms.google-services") version "4.4.2"
}

android {
    namespace = "com.example.fillin"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.fillin"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        val apiKey = properties.getProperty("GEMINI_API_KEY") ?: ""
        buildConfigField("String", "GEMINI_API_KEY", "\"$apiKey\"")

        // local.properties에서 키 로드 (민감 정보 분리)
        val kakaoNativeKey = properties.getProperty("KAKAO_NATIVE_APP_KEY") ?: ""
        val kakaoRestKey = properties.getProperty("KAKAO_REST_API_KEY") ?: ""
        val naverMapClientId = properties.getProperty("NAVER_MAP_CLIENT_ID") ?: ""
        buildConfigField("String", "KAKAO_NATIVE_APP_KEY", "\"$kakaoNativeKey\"")
        buildConfigField("String", "KAKAO_REST_API_KEY", "\"$kakaoRestKey\"")
        buildConfigField("String", "NAVER_MAP_CLIENT_ID", "\"$naverMapClientId\"")
        manifestPlaceholders["KAKAO_NATIVE_APP_KEY"] = kakaoNativeKey
        manifestPlaceholders["NAVER_MAP_CLIENT_ID"] = naverMapClientId

      /*  // BuildConfig 상수로 Gemini API Key 노출
        val geminiKey: String = (project.findProperty("GEMINI_API_KEY") as String?)
            ?: ""
        buildConfigField("String", "GEMINI_API_KEY", "\"$geminiKey\"")*/
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.8.5")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    // Retrofit & Gson
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    // Google (Credential Manager)
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.0")

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-storage")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")

    // Location (FusedLocationProviderClient)
    implementation("com.google.android.gms:play-services-location:21.3.0")

    // Coil - Image loading
    implementation("io.coil-kt:coil-compose:2.7.0")

    // Naver Map SDK
    implementation("com.naver.maps:map-sdk:3.23.0")

    // CameraX
    implementation("androidx.camera:camera-core:1.4.0")
    implementation("androidx.camera:camera-camera2:1.4.0")
    implementation("androidx.camera:camera-lifecycle:1.4.0")
    implementation("androidx.camera:camera-view:1.4.0")

    // Guava (required by CameraX)
    implementation("com.google.guava:guava:33.3.1-android")

    // Kakao SDK
    implementation("com.kakao.sdk:v2-user:2.20.1")
    implementation(libs.androidx.compose.foundation.layout)

    // Image Cropper
    implementation("com.vanniktech:android-image-cropper:4.6.0")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
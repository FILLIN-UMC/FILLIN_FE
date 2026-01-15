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
}

android {
    namespace = "com.example.fillin2"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.fillin2"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        val apiKey = properties.getProperty("GEMINI_API_KEY") ?: ""
        buildConfigField("String", "GEMINI_API_KEY", "\"$apiKey\"")
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
        buildConfig = true // BuildConfig 클래스를 생성하도록 설정
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    // 네이버 지도 SDK
    implementation("com.naver.maps:map-sdk:3.23.0")
    implementation("androidx.compose.material:material-icons-extended:1.5.4") // 버전은 프로젝트 설정에 맞춰 자동 조정됨
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    // 네트워크 통신을 위한 Retrofit & Gson (JSON 해석기)
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    //위치 정보를 쉽게 가져오기 위한 Google 서비스를 추가합니다.
    implementation("com.google.android.gms:play-services-location:21.0.1")
    // 로그로 통신 내용을 확인하기 위한 OkHttp 인터셉터
    implementation("com.squareup.okhttp3:logging-interceptor:4.10.0")
    // CameraX 라이브러리를 추가
    val camerax_version = "1.3.0"
    implementation("androidx.camera:camera-core:${camerax_version}")
    implementation("androidx.camera:camera-camera2:${camerax_version}")
    implementation("androidx.camera:camera-lifecycle:${camerax_version}")
    implementation("androidx.camera:camera-view:${camerax_version}")
    // Coil (이미지 로딩 라이브러리) 추가
    implementation("io.coil-kt:coil-compose:2.5.0")
    // 1. Firebase BoM (Bill of Materials) 추가
    // 이 줄이 있으면 다른 Firebase 라이브러리의 버전을 일일이 적지 않아도 서로 호환되는 버전을 자동으로 맞춰줍니다.
    implementation(platform("com.google.firebase:firebase-bom:33.1.2"))

    // 2. [필수] 코루틴 Play Services 지원 라이브러리
    // 유저님이 짠 Repository 코드의 '.await()' 기능이 작동하려면 이 라이브러리가 반드시 필요합니다.
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")

    // 3. (선택) Firebase 분석 도구
    implementation("com.google.firebase:firebase-analytics-ktx")

    // 기존에 있던 것들 (버전 숫자를 지워도 BoM이 관리해줍니다)
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-storage-ktx")
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
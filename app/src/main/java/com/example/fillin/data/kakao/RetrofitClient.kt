package com.example.fillin.data.kakao

import com.example.fillin.data.ai.GeminiApiService
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private const val BASE_URL = "https://dapi.kakao.com/"
    private const val GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/"

    // by lazy를 사용했기 때문에, 카카오 기능만 쓸 때는 구글용 인스턴스를 만들지 않아 메모리를 효율적으로 사용합니다.
    val kakaoApi: KakaoApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create()) // JSON을 Kotlin 객체로 변환
            .build()
            .create(KakaoApiService::class.java)
    }
    // 2. [추가] Gemini API용 인스턴스
    val geminiApi: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(GEMINI_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GeminiApiService::class.java)
    }
}

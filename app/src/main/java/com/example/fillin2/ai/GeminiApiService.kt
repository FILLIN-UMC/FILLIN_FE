package com.example.fillin2.ai

// api/GeminiApiService.kt
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query

interface GeminiApiService {
    @POST("v1beta/models/gemini-1.5-flash:generateContent")
    suspend fun analyzeImage(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}

// 1. RetrofitClient: 서버와 통신할 엔진을 준비합니다.

// 2. GeminiApiService: 서버에 어떤 요청(POST 등)을 보낼지 명시합니다.

// 3. GeminiRepository: RetrofitClient를 사용해 서버에 데이터를 요청하고, 결과를 가공합니다.

// 4. GeminiViewModel: Repository에서 받은 결과를 화면에 보여주기 위해 보관합니다.
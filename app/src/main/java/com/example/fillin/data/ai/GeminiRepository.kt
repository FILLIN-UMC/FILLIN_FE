package com.example.fillin.data.ai

import android.content.Context
import android.net.Uri
import android.util.Base64
import com.example.fillin.data.ai.GeminiApiService

class GeminiRepository(private val apiService: GeminiApiService) {

    suspend fun fetchAiAnalysis(context: Context, imageUri: Uri, apiKey: String): String {

        // 1. 이미지를 바이트로 읽어 Base64로 변환 (기존 로직 유지)
        val inputStream = context.contentResolver.openInputStream(imageUri)
        val bytes = inputStream?.readBytes() ?: return "이미지 로드 실패"
        val base64Image = Base64.encodeToString(bytes, Base64.NO_WRAP)

        // 2. 프롬프트 수정: 카테고리 제한 없이 LLM이 스스로 판단하게 함
        val prompt = "사진 속 상황을 분석해서 이를 가장 잘 나타내는 짧은 명사(한 단어 혹은 두 단어)로 답변해줘. 부연 설명 없이 명사만 딱 하나 대답해줘."

        val request = GeminiRequest(
            contents = listOf(
                Content(
                    parts = listOf(
                        Part(text = prompt),
                        Part(inline_data = InlineData(data = base64Image))
                    )
                )
            )
        )

        // 2. API 호출 (try-catch 없이 바로 반환)
        val response = apiService.analyzeImage(apiKey, request)
        return response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim()
            ?: "분석 불가"

    }
}

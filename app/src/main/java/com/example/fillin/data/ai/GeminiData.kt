package com.example.fillin.data.ai

// 요청용 데이터 구조
data class GeminiRequest(val contents: List<Content>)

data class Content(val parts: List<Part>)

data class Part(
    val text: String? = null,
    val inline_data: InlineData? = null
)

data class InlineData(
    val mime_type: String = "image/jpeg",
    val data: String // Base64로 인코딩된 이미지 문자열
)

// 응답용 데이터 구조
data class GeminiResponse(val candidates: List<Candidate>)

data class Candidate(val content: Content)

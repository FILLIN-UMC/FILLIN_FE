package com.example.fillin.data.model.auth

/**
 * 로그인 API 요청 Body
 * API 명세서에 맞게 수정 필요
 */
data class LoginRequest(
    val email: String? = null,
    val password: String? = null,
    val socialType: String? = null,  // "KAKAO", "GOOGLE" 등
    val socialToken: String? = null
)

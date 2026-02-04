package com.example.fillin.data.model.auth

/**
 * 회원가입 API 요청 Body
 * API 명세서에 맞게 수정 필요
 */
data class SignupRequest(
    val email: String? = null,
    val password: String? = null,
    val nickname: String? = null,
    val socialType: String? = null,
    val socialToken: String? = null
)

package com.example.fillin.data.model.auth

/**
 * 회원가입 API 응답
 * API 명세서에 맞게 수정 필요
 */
data class SignupResponse(
    val status: String? = null,
    val code: String? = null,
    val message: String? = null,
    val data: SignupResponseData? = null
)

data class SignupResponseData(
    val userId: Long? = null,
    val accessToken: String? = null,
    val refreshToken: String? = null
)

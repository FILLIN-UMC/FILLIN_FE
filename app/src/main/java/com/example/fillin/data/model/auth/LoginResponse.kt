package com.example.fillin.data.model.auth

/**
 * 로그인 API 응답
 * API 명세서에 맞게 수정 필요
 */
data class LoginResponse(
    val status: String? = null,
    val code: String? = null,
    val message: String? = null,
    val data: LoginResponseData? = null
)

data class LoginResponseData(
    val accessToken: String? = null,
    val refreshToken: String? = null,
    val user: UserInfo? = null
)

data class UserInfo(
    val id: Long? = null,
    val email: String? = null,
    val nickname: String? = null,
    val profileImageUrl: String? = null
)

package com.example.fillin.data.model.auth

import com.google.gson.annotations.SerializedName

/**
 * 로그인/토큰 응답 공통 구조
 * - 테스트 로그인, 온보딩 완료, 토큰 재발급
 */
data class LoginResponse(
    @SerializedName("status") val status: String? = null,
    @SerializedName("code") val code: String? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("data") val data: TokenData? = null
)

data class TokenData(
    @SerializedName("accessToken") val accessToken: String? = null,
    @SerializedName("refreshToken") val refreshToken: String? = null
)

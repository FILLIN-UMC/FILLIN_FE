package com.example.fillin.data.model.auth

import com.google.gson.annotations.SerializedName

/**
 * 소셜 로그인 API 응답 (카카오, 구글)
 * - needOnboarding: true면 온보딩 화면으로 이동
 * - tempToken: 온보딩 API 호출 시 Authorization Bearer에 사용
 * - token: needOnboarding=false일 때만 존재
 */
data class SocialAuthResponse(
    @SerializedName("status") val status: String? = null,
    @SerializedName("code") val code: String? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("data") val data: SocialAuthData? = null
)

data class SocialAuthData(
    @SerializedName("needOnboarding") val needOnboarding: Boolean = false,
    @SerializedName("tempToken") val tempToken: String? = null,
    @SerializedName("token") val token: TokenData? = null
)

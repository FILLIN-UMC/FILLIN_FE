package com.example.fillin.data.model.auth

import com.google.gson.annotations.SerializedName

/**
 * 카카오 소셜 로그인 API 요청
 * POST /api/auth/kakao/login
 */
data class SocialAuthRequest(
    @SerializedName("socialType") val socialType: String, // "KAKAO"
    @SerializedName("accessToken") val accessToken: String
)

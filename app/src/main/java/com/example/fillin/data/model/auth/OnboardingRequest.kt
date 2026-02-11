package com.example.fillin.data.model.auth

import com.google.gson.annotations.SerializedName

/**
 * 온보딩 완료 API 요청
 * POST /api/auth/onboarding
 * - tempToken을 Authorization Bearer 헤더에 담아 전송
 */
data class OnboardingRequest(
    @SerializedName("nickname") val nickname: String,
    @SerializedName("email") val email: String,
    @SerializedName("agreedAgreementIds") val agreedAgreementIds: List<Int>
)

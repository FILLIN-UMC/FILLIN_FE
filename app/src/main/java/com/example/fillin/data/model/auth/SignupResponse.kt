package com.example.fillin.data.model.auth

import com.google.gson.annotations.SerializedName

/**
 * 테스트 회원가입 API 응답
 * POST /api/auth/test/signup
 */
data class SignupResponse(
    @SerializedName("status") val status: String? = null,
    @SerializedName("code") val code: String? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("data") val data: String? = null
)

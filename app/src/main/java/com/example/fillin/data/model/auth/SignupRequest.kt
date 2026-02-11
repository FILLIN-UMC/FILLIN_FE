package com.example.fillin.data.model.auth

import com.google.gson.annotations.SerializedName

/**
 * 테스트 회원가입 API 요청
 * POST /api/auth/test/signup
 */
data class SignupRequest(
    @SerializedName("nickname") val nickname: String,
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String,
    @SerializedName("confirmPassword") val confirmPassword: String
)

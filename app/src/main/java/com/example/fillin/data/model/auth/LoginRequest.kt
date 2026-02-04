package com.example.fillin.data.model.auth

import com.google.gson.annotations.SerializedName

/**
 * 테스트 로그인 API 요청
 * POST /api/auth/test/login
 */
data class LoginRequest(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String
)

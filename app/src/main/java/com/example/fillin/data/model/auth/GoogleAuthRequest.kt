package com.example.fillin.data.model.auth

import com.google.gson.annotations.SerializedName

/**
 * 구글 안드로이드 로그인 API 요청
 * POST /api/auth/google/android/login
 * - code: 구글에서 내려주는 인가코드 (또는 ID Token)
 */
data class GoogleAuthRequest(
    @SerializedName("code") val code: String
)

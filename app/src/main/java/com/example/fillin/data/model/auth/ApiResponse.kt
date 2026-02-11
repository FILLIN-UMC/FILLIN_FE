package com.example.fillin.data.model.auth

import com.google.gson.annotations.SerializedName

/**
 * 공통 API 응답 래퍼 (로그아웃 등)
 */
data class ApiResponse(
    @SerializedName("status") val status: String? = null,
    @SerializedName("code") val code: String? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("data") val data: String? = null
)

package com.example.fillin.data.model.auth

import com.google.gson.annotations.SerializedName

/**
 * 공통 API 응답 래퍼 (로그아웃 등)
 */
data class ApiResponse<T>( // 👈 <T> 추가
    @SerializedName("status") val status: String? = null,
    @SerializedName("code") val code: String? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("data") val data: T? = null // 👈 String? 대신 T? 사용
)

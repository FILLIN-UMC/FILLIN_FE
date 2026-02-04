package com.example.fillin.data.model.auth

import com.google.gson.annotations.SerializedName
import retrofit2.HttpException

/**
 * API 에러 응답 파싱
 * 4xx, 5xx 응답의 message 필드를 추출하여 사용자에게 표시
 */
data class ApiError(
    @SerializedName("status") val status: String? = null,
    @SerializedName("code") val code: String? = null,
    @SerializedName("message") val message: String? = null
)

object ApiErrorParser {
    private val gson = com.google.gson.Gson()

    /**
     * HttpException에서 사용자에게 보여줄 메시지 추출
     */
    fun getMessage(e: HttpException, defaultMessage: String): String {
        return try {
            val body = e.response()?.errorBody()?.string() ?: return defaultMessage
            val error = gson.fromJson(body, ApiError::class.java)
            error?.message?.takeIf { it.isNotBlank() } ?: defaultMessage
        } catch (_: Exception) {
            defaultMessage
        }
    }
}

package com.example.fillin.data.model.auth

import android.util.Log
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import retrofit2.HttpException

/**
 * API 에러 응답 파싱
 * 4xx, 5xx 응답의 message, error, error_description 등 추출하여 사용자에게 표시
 */
data class ApiError(
    @SerializedName("status") val status: String? = null,
    @SerializedName("code") val code: String? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("data") val data: String? = null, // 에러 상세 (예: SQL 메시지)
    @SerializedName("detail") val detail: String? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("error") val error: String? = null,
    @SerializedName("error_description") val errorDescription: String? = null,
    @SerializedName("errorMessage") val errorMessage: String? = null
)

object ApiErrorParser {
    private val gson = Gson()
    private const val TAG = "ApiErrorParser"

    /**
     * HttpException에서 사용자에게 보여줄 메시지 추출
     */
    fun getMessage(e: HttpException, defaultMessage: String): String {
        return try {
            val response = e.response()
            val code = response?.code() ?: 0
            val body = response?.errorBody()?.string()
            Log.d(TAG, "API error: HTTP $code, body length=${body?.length ?: 0}, body='$body'")
            if (body.isNullOrBlank()) return defaultMessage
            val error = gson.fromJson(body, ApiError::class.java)
            // 동일 이메일로 다른 소셜 가입 시 DB unique 제약 위반 (백엔드 미처리 시 400 + SQL 메시지)
            if (error?.data?.contains("Duplicate entry", ignoreCase = true) == true &&
                error.data?.contains("member", ignoreCase = true) == true
            ) {
                return "해당 이메일은 이미 다른 로그인 방식으로 가입되어 있습니다.\n기존 로그인 방식을 사용해주세요."
            }
            val msg = error?.message?.takeIf { it.isNotBlank() }
                ?: error?.detail?.takeIf { it.isNotBlank() }
                ?: error?.errorDescription?.takeIf { it.isNotBlank() }
                ?: error?.errorMessage?.takeIf { it.isNotBlank() }
                ?: error?.title?.takeIf { it.isNotBlank() }
                ?: error?.error?.takeIf { it.isNotBlank() }
            msg ?: defaultMessage
        } catch (ex: Exception) {
            Log.e(TAG, "Failed to parse API error", ex)
            defaultMessage
        }
    }
}

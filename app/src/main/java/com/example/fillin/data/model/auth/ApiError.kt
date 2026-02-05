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

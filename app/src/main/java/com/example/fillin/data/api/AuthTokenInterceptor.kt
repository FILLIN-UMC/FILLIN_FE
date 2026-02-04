package com.example.fillin.data.api

import android.content.Context
import okhttp3.Interceptor
import okhttp3.Response

/**
 * API 요청 시 Authorization 헤더에 토큰 자동 첨부
 * API 명세서의 인증 방식(Bearer, API Key 등)에 맞게 수정 필요
 */
class AuthTokenInterceptor(private val context: Context) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val token = TokenManager.getAccessToken(context)

        val request = if (token != null) {
            // Bearer 토큰 방식 (JWT 등)
            originalRequest.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            originalRequest
        }

        return chain.proceed(request)
    }
}

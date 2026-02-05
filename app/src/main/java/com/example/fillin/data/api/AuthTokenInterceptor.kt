package com.example.fillin.data.api

import android.content.Context
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

/**
 * API 요청 시 Authorization Bearer 헤더 자동 첨부
 * - 인증 불필요 엔드포인트(로그인, 회원가입, 소셜로그인, 토큰재발급)는 제외
 */
class AuthTokenInterceptor(private val context: Context) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val url = request.url.toString()

        // 인증 불필요한 엔드포인트
        if (isNoAuthEndpoint(url)) {
            return chain.proceed(request)
        }

        val token = TokenManager.getBearerToken(context)
        val newRequest = if (token != null) {
            request.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            request
        }

        return chain.proceed(newRequest)
    }

    private fun isNoAuthEndpoint(url: String): Boolean {
        return url.contains("/api/v1/auth/test/signup") ||
            url.contains("/api/v1/auth/test/login") ||
            url.contains("/api/auth/kakao/login") ||
            url.contains("/api/auth/google/login") ||
            url.contains("/api/auth/reissue")
    }
}

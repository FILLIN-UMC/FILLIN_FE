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

        // 온보딩은 tempToken으로만 호출 (백엔드 스펙)
        // 그 외 일반 API는 accessToken으로만 호출 (tempToken을 일반 API에 쓰면 403 가능)
        val token = if (url.contains("/api/auth/onboarding")) {
            TokenManager.getTempToken(context)
        } else {
            TokenManager.getAccessToken(context)
        }
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
        return url.contains("/api/auth/test/signup") ||
            url.contains("/api/auth/test/login") ||
            url.contains("/api/auth/kakao/login") ||
            url.contains("/api/auth/google/login") ||
            url.contains("/api/auth/reissue")
    }
}

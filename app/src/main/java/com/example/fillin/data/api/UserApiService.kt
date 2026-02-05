package com.example.fillin.data.api

import com.example.fillin.data.model.auth.ApiResponse
import com.example.fillin.data.model.auth.LoginRequest
import com.example.fillin.data.model.auth.LoginResponse
import com.example.fillin.data.model.auth.OnboardingRequest
import com.example.fillin.data.model.auth.SignupRequest
import com.example.fillin.data.model.auth.SignupResponse
import com.example.fillin.data.model.auth.SocialAuthRequest
import com.example.fillin.data.model.auth.SocialAuthResponse
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST

/**
 * FILLIN 로그인/회원가입 API
 * Base URL: https://api.fillin.site
 */
interface UserApiService {

    // ---- 테스트용 ----
    @POST("api/v1/auth/test/signup")
    suspend fun testSignup(@Body request: SignupRequest): SignupResponse

    @POST("api/v1/auth/test/login")
    suspend fun testLogin(@Body request: LoginRequest): LoginResponse

    // ---- 소셜 로그인 (백엔드 OAuthController: /api/auth) ----
    @POST("api/auth/kakao/login")
    suspend fun kakaoLogin(@Body request: SocialAuthRequest): SocialAuthResponse

    @POST("api/auth/google/login")
    suspend fun googleLogin(@Body request: SocialAuthRequest): SocialAuthResponse

    // ---- 온보딩 ----
    @POST("api/auth/onboarding")
    suspend fun completeOnboarding(@Body request: OnboardingRequest): LoginResponse

    // ---- 토큰 ----
    @POST("api/auth/reissue")
    suspend fun reissueToken(@Header("X-Refresh-Token") refreshToken: String): LoginResponse

    @PATCH("api/auth/logout")
    suspend fun logout(@Header("X-Refresh-Token") refreshToken: String): ApiResponse
}

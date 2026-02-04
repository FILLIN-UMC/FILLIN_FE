package com.example.fillin.data.repository

import android.content.Context
import com.example.fillin.data.api.RetrofitClient
import com.example.fillin.data.api.TokenManager
import com.example.fillin.data.model.auth.GoogleAuthRequest
import com.example.fillin.data.model.auth.LoginRequest
import com.example.fillin.data.model.auth.LoginResponse
import com.example.fillin.data.model.auth.OnboardingRequest
import com.example.fillin.data.model.auth.SignupRequest
import com.example.fillin.data.model.auth.SignupResponse
import com.example.fillin.data.model.auth.SocialAuthRequest
import com.example.fillin.data.model.auth.SocialAuthResponse

/**
 * 로그인/회원가입 API 연동 Repository
 */
class AuthRepository(private val context: Context) {

    private val userApi = RetrofitClient.getUserApi(context)

    // ---- 테스트용 ----
    suspend fun testSignup(request: SignupRequest): Result<SignupResponse> = runCatching {
        userApi.testSignup(request)
    }

    suspend fun testLogin(request: LoginRequest): Result<LoginResponse> = runCatching {
        userApi.testLogin(request)
    }.onSuccess { response ->
        response.data?.let { saveTokens(it) }
    }

    // ---- 소셜 로그인 ----
    suspend fun kakaoLogin(accessToken: String): Result<SocialAuthResponse> = runCatching {
        userApi.kakaoLogin(SocialAuthRequest(socialType = "KAKAO", accessToken = accessToken))
    }.onSuccess { response ->
        handleSocialAuthResponse(response)
    }

    suspend fun googleLogin(code: String): Result<SocialAuthResponse> = runCatching {
        userApi.googleLogin(GoogleAuthRequest(code = code))
    }.onSuccess { response ->
        handleSocialAuthResponse(response)
    }

    private fun handleSocialAuthResponse(response: SocialAuthResponse) {
        val data = response.data ?: return
        if (data.needOnboarding) {
            data.tempToken?.let { TokenManager.saveTempToken(context, it) }
        } else {
            data.token?.let { saveTokens(it) }
        }
    }

    // ---- 온보딩 ----
    suspend fun completeOnboarding(request: OnboardingRequest): Result<LoginResponse> = runCatching {
        userApi.completeOnboarding(request)
    }.onSuccess { response ->
        response.data?.let { saveTokens(it) }
        TokenManager.clearTempToken(context)
    }

    // ---- 토큰 ----
    suspend fun reissueToken(): Result<LoginResponse> = runCatching {
        val refreshToken = TokenManager.getRefreshToken(context)
            ?: throw IllegalStateException("Refresh token not found")
        userApi.reissueToken(refreshToken)
    }.onSuccess { response ->
        response.data?.let { saveTokens(it) }
    }

    suspend fun logout(): Result<Unit> = runCatching {
        val refreshToken = TokenManager.getRefreshToken(context)
        if (refreshToken != null) {
            userApi.logout(refreshToken)
        }
    }.also {
        // API 실패해도 로컬 토큰은 삭제
        TokenManager.clearTokens(context)
    }.map { }

    private fun saveTokens(data: com.example.fillin.data.model.auth.TokenData) {
        data.accessToken?.let { TokenManager.saveAccessToken(context, it) }
        data.refreshToken?.let { TokenManager.saveRefreshToken(context, it) }
    }

    fun clearTokensLocally() {
        TokenManager.clearTokens(context)
    }

    fun hasTempToken(): Boolean = TokenManager.getTempToken(context) != null
}

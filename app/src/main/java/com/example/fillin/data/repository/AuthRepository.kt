package com.example.fillin.data.repository

import android.content.Context
import com.example.fillin.data.api.RetrofitClient
import com.example.fillin.data.api.TokenManager
import com.example.fillin.data.model.auth.LoginRequest
import com.example.fillin.data.model.auth.LoginResponse
import com.example.fillin.data.model.auth.SignupRequest
import com.example.fillin.data.model.auth.SignupResponse

/**
 * 로그인/회원가입 API 호출을 담당하는 Repository
 * API 명세서 확정 후 구현
 */
class AuthRepository(private val context: Context) {

    private val userApi = RetrofitClient.getUserApi(context)

    suspend fun login(request: LoginRequest): Result<LoginResponse> {
        return try {
            val response = userApi.login(request)
            response.data?.let { data ->
                data.accessToken?.let { TokenManager.saveAccessToken(context, it) }
                data.refreshToken?.let { TokenManager.saveRefreshToken(context, it) }
            }
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signup(request: SignupRequest): Result<SignupResponse> {
        return try {
            val response = userApi.signup(request)
            response.data?.let { data ->
                data.accessToken?.let { TokenManager.saveAccessToken(context, it) }
                data.refreshToken?.let { TokenManager.saveRefreshToken(context, it) }
            }
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun logout() {
        TokenManager.clearTokens(context)
    }
}

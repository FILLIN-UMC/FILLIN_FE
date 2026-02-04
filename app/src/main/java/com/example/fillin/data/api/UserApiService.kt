package com.example.fillin.data.api

import com.example.fillin.data.model.auth.LoginRequest
import com.example.fillin.data.model.auth.LoginResponse
import com.example.fillin.data.model.auth.SignupRequest
import com.example.fillin.data.model.auth.SignupResponse
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * 로그인/회원가입 API 엔드포인트 정의
 * API 명세서에 맞게 수정 필요
 */
interface UserApiService {
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @POST("auth/signup")
    suspend fun signup(@Body request: SignupRequest): SignupResponse

    // API 명세서에 따라 추가 엔드포인트 정의
    // @GET("users/profile")
    // suspend fun getUserProfile(): UserProfileResponse
}

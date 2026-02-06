package com.example.fillin.data.api

import com.example.fillin.data.model.auth.ApiResponse
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * 회원 관련 API (FCM 토큰 등록 등)
 * Base URL: https://api.fillin.site
 */
interface MemberApiService {

    @POST("api/members/fcm-token")
    suspend fun registerFcmToken(@Body request: FcmTokenRequest): ApiResponse
}

data class FcmTokenRequest(
    val fcmToken: String
)

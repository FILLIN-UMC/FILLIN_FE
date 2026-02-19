package com.example.fillin.data.api

import com.example.fillin.data.model.auth.ApiResponse
import com.example.fillin.data.model.mypage.MemberProfileResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * 회원 관련 API (FCM 토큰 등록, 회원 프로필 조회 등)
 * Base URL: https://api.fillin.site
 */
interface MemberApiService {

    @POST("api/members/fcm-token")
    suspend fun registerFcmToken(@Body request: FcmTokenRequest): ApiResponse<Unit>

    @GET("api/members/{memberId}")
    suspend fun getMemberProfile(@Path("memberId") memberId: Long): MemberProfileResponse
}

data class FcmTokenRequest(
    val fcmToken: String
)

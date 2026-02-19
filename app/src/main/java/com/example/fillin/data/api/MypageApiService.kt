package com.example.fillin.data.api

import com.example.fillin.data.model.mypage.MissionStatusResponse
import com.example.fillin.data.model.mypage.MyReportCategoryResponse
import com.example.fillin.data.model.mypage.MyReportListResponse
import com.example.fillin.data.model.mypage.NicknameCheckResponse
import com.example.fillin.data.model.mypage.NotificationRequest
import com.example.fillin.data.model.mypage.NotificationResponse
import com.example.fillin.data.model.mypage.ProfileResponse
import com.example.fillin.data.model.mypage.RankResponse
import com.example.fillin.data.model.mypage.ReportCountResponse
import com.example.fillin.data.model.mypage.ReportExpireSoonDetailResponse
import com.example.fillin.data.model.mypage.ReportExpireSoonResponse
import com.example.fillin.data.model.mypage.WithdrawResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * 마이페이지 API
 * Base URL: https://api.fillin.site
 * 인증: Authorization Bearer (AuthTokenInterceptor에서 자동 첨부)
 */
interface MypageApiService {

    // ---- 프로필 ----
    @GET("api/mypage/profile")
    suspend fun getProfile(): ProfileResponse

    @Multipart
    @POST("api/mypage/profile/edit")
    suspend fun updateProfile(
        @Part("request") request: RequestBody,
        @Part image: MultipartBody.Part? = null
    ): ProfileResponse

    @GET("api/mypage/profile/check")
    suspend fun checkNickname(@Query("nickname") nickname: String): NicknameCheckResponse

    @GET("api/mypage/profile/ranks")
    suspend fun getRanks(): RankResponse

    @GET("api/mypage/missions")
    suspend fun getMissions(): MissionStatusResponse

    // ---- 알림 설정 ----
    @GET("api/mypage/profile/notiSet")
    suspend fun getNotificationSettings(): NotificationResponse

    @POST("api/mypage/profile/notiSet")
    suspend fun updateNotificationSettings(@retrofit2.http.Body request: NotificationRequest): NotificationResponse

    // ---- 나의 제보 ----
    @GET("api/mypage/reports/count")
    suspend fun getReportCount(): ReportCountResponse

    @GET("api/mypage/reports/category")
    suspend fun getReportCategory(): MyReportCategoryResponse

    @GET("api/mypage/reports/soon")
    suspend fun getReportExpireSoon(): ReportExpireSoonResponse

    @GET("api/mypage/reports/soon/detail")
    suspend fun getReportExpireSoonDetail(): ReportExpireSoonDetailResponse

    @GET("api/mypage/reports")
    suspend fun getMyReports(): MyReportListResponse

    @GET("api/mypage/reports/expired")
    suspend fun getMyReportsExpired(): MyReportListResponse

    @POST("api/mypage/reports/{reportId}/expired")
    suspend fun deleteReport(@Path("reportId") reportId: Long): com.example.fillin.data.model.auth.ApiResponse<Unit>

    @GET("api/mypage/reports/like")
    suspend fun getLikedReports(): MyReportListResponse

    // ---- 회원 탈퇴 ----
    @DELETE("api/mypage/withdraw")
    suspend fun withdraw(): WithdrawResponse
}

package com.example.fillin.data.api

import com.example.fillin.data.model.report.PopularReportListResponse
import com.example.fillin.data.model.report.ReportCreateResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * 제보 API
 * Base URL: https://api.fillin.site
 * 인증: Authorization Bearer (AuthTokenInterceptor에서 자동 첨부)
 */
interface ReportApiService {

    @Multipart
    @POST("api/reports")
    suspend fun createReport(
        @Part("request") request: RequestBody,
        @Part image: MultipartBody.Part? = null
    ): ReportCreateResponse

    @GET("api/reports/popular")
    suspend fun getPopularReports(): PopularReportListResponse

    @POST("api/reports/{reportId}/feedback")
    suspend fun createFeedback(
        @Path("reportId") reportId: Long,
        @Query("type") type: String
    ): com.example.fillin.data.model.report.ReportApiResponse

    @POST("api/reports/{reportId}/like")
    suspend fun likeToggle(@Path("reportId") reportId: Long): com.example.fillin.data.model.report.ReportApiResponse
}

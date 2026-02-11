package com.example.fillin.data.api

import com.example.fillin.data.model.report.PopularReportListResponse
import com.example.fillin.data.model.report.ReportAnalyzeResponse
import com.example.fillin.data.model.report.ReportCreateResponse
import com.example.fillin.data.model.report.ReportImageDetailResponse
import com.example.fillin.data.model.report.ReportImageProcessResponse
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

    @GET("api/reports/{reportId}/detail")
    suspend fun getReportDetail(@Path("reportId") reportId: Long): ReportImageDetailResponse

    @POST("api/reports/{reportId}/feedback")
    suspend fun createFeedback(
        @Path("reportId") reportId: Long,
        @Query("type") type: String
    ): com.example.fillin.data.model.report.ReportApiResponse

    @POST("api/reports/{reportId}/like")
    suspend fun likeToggle(@Path("reportId") reportId: Long): com.example.fillin.data.model.report.ReportApiResponse

    // 1. AI 분석 API (추천 제목/카테고리)
    @Multipart
    @POST("api/reports/analyze")
    suspend fun analyzeReportImage(
        @Part image: MultipartBody.Part
    ): ReportAnalyzeResponse

    // 2. 이미지 전처리 API (번호판 모자이크)
    @Multipart
    @POST("api/reports/image-process")
    suspend fun processReportImage(
        @Part image: MultipartBody.Part
    ): ReportImageProcessResponse
}

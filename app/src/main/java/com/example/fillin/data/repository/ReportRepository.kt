package com.example.fillin.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.fillin.data.api.ReportApiService
import com.example.fillin.data.api.RetrofitClient
import com.example.fillin.data.api.TokenManager
import com.example.fillin.data.db.FirestoreRepository
import com.example.fillin.data.db.UploadedReportResult
import com.example.fillin.data.model.report.PopularReportListResponse
import com.example.fillin.data.model.report.ReportApiResponse
import com.example.fillin.data.model.report.ReportImageDetailResponse
import com.example.fillin.data.model.report.ReportCategory
import com.example.fillin.data.model.report.ReportCreateRequest
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException
import java.io.File
import java.io.FileOutputStream

/**
 * 제보 등록 Repository
 * - 로그인 시: 백엔드 API (POST /api/reports) 사용
 * - 비로그인 또는 API 실패 시: Firestore fallback
 */
class ReportRepository(private val context: Context) {

    private val api: ReportApiService = RetrofitClient.getReportApi(context)
    private val firestoreRepository = FirestoreRepository()
    private val gson = Gson()

    suspend fun uploadReport(
        category: String,
        title: String,
        location: String,
        imageUri: Uri,
        latitude: Double = 0.0,
        longitude: Double = 0.0
    ): UploadedReportResult? {
        val hasToken = TokenManager.getBearerToken(context) != null
        Log.d("ReportRepository", "제보 등록 시도: hasToken=$hasToken")

        if (hasToken) {
            Log.d("ReportRepository", "API로 제보 등록 시도 중...")
            val apiResult = uploadReportViaApi(category, title, location, imageUri, latitude, longitude)
            if (apiResult != null) {
                Log.d("ReportRepository", "API 제보 등록 성공: reportId=${apiResult.documentId}")
                return apiResult
            }
            Log.w("ReportRepository", "API 제보 등록 실패, Firestore fallback")
        } else {
            Log.d("ReportRepository", "토큰 없음 → Firestore로 저장")
        }

        return firestoreRepository.uploadReport(
            category = category,
            title = title,
            location = location,
            imageUri = imageUri,
            latitude = latitude,
            longitude = longitude
        )
    }

    private suspend fun uploadReportViaApi(
        category: String,
        title: String,
        location: String,
        imageUri: Uri,
        latitude: Double,
        longitude: Double
    ): UploadedReportResult? = runCatching {
        val reportCategory = when (category) {
            "위험" -> ReportCategory.DANGER
            "불편" -> ReportCategory.INCONVENIENCE
            else -> ReportCategory.DISCOVERY
        }

        val request = ReportCreateRequest(
            title = title,
            latitude = latitude,
            longitude = longitude,
            category = reportCategory
        )
        val requestBody = gson.toJson(request).toRequestBody("application/json".toMediaTypeOrNull())
        val imagePart = uriToPart(imageUri)

        val response = api.createReport(request = requestBody, image = imagePart)
        val reportId = response.data
        Log.d("ReportRepository", "API 응답: reportId=$reportId, status=${response.status}")

        if (reportId != null) {
            UploadedReportResult(
                documentId = reportId.toString(),
                imageUrl = null,
                imageUri = imageUri,
                category = category,
                title = title,
                location = location
            )
        } else {
            null
        }
    }.getOrElse { e ->
        when (e) {
            is HttpException -> {
                val body = e.response()?.errorBody()?.string() ?: ""
                Log.e("ReportRepository", "API 오류: ${e.code()} ${e.message()}, body=$body")
                if (e.code() == 401 || e.code() == 403) {
                    Log.w("ReportRepository", "인증 실패 → Firestore fallback")
                }
            }
            else -> Log.e("ReportRepository", "제보 등록 실패", e)
        }
        null
    }

    private suspend fun uriToPart(uri: Uri): MultipartBody.Part = withContext(Dispatchers.IO) {
        val file = File(context.cacheDir, "report_${System.currentTimeMillis()}.jpg")
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(file).use { output ->
                input.copyTo(output)
            }
        }
        val requestBody = file.asRequestBody("image/*".toMediaTypeOrNull())
        MultipartBody.Part.createFormData("image", file.name, requestBody)
    }

    suspend fun getPopularReports(): Result<PopularReportListResponse> = runCatching {
        api.getPopularReports()
    }

    suspend fun getReportDetail(reportId: Long): Result<ReportImageDetailResponse> = runCatching {
        api.getReportDetail(reportId)
    }

    suspend fun createFeedback(reportId: Long, type: String): Result<ReportApiResponse> = runCatching {
        api.createFeedback(reportId, type)
    }

    suspend fun likeToggle(reportId: Long): Result<ReportApiResponse> = runCatching {
        api.likeToggle(reportId)
    }
}

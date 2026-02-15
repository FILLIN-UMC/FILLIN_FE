package com.example.fillin.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.fillin.data.api.ReportApiService
import com.example.fillin.data.api.RetrofitClient
import com.example.fillin.data.api.TokenManager
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
 * - 비로그인 또는 API 실패 시: null 반환 (백엔드 전용)
 */
class ReportRepository(private val context: Context) {

    private val api: ReportApiService = RetrofitClient.getReportApi(context)
    private val gson = Gson()

    suspend fun uploadReport(
        category: String,
        title: String,
        location: String,
        imageUri: Uri,
        latitude: Double = 0.0,
        longitude: Double = 0.0
    ): UploadedReportResult? {
        // accessToken 필요 (tempToken만 있으면 403 발생)
        val accessToken = TokenManager.getAccessToken(context)
        Log.d("ReportRepository", "제보 등록 시도: accessToken=${accessToken != null}")

        if (accessToken == null) {
            Log.d("ReportRepository", "accessToken 없음 → 제보 등록 불가")
            throw IllegalStateException(
                if (TokenManager.getTempToken(context) != null)
                    "온보딩을 완료한 후 제보를 등록할 수 있습니다."
                else
                    "로그인 후 제보를 등록할 수 있습니다."
            )
        }

        Log.d("ReportRepository", "API로 제보 등록 시도 중...")
        try {
            val apiResult = uploadReportViaApi(category, title, location, imageUri, latitude, longitude)
            return if (apiResult != null) {
                Log.d("ReportRepository", "API 제보 등록 성공: reportId=${apiResult.documentId}")
                apiResult
            } else {
                Log.w("ReportRepository", "API 제보 등록 실패")
                throw RuntimeException("서버에서 응답이 없습니다.")
            }
        } catch (e: Exception) {
            if (e is IllegalStateException) throw e
            Log.e("ReportRepository", "제보 등록 실패", e)
            val msg = when (e) {
                is HttpException -> {
                    val body = e.response()?.errorBody()?.string() ?: ""
                    val code = e.code()
                    Log.e("ReportRepository", "API 오류: $code, body=$body")
                    when (code) {
                        401 -> "로그인이 만료되었습니다. 다시 로그인해주세요."
                        403 -> "접근 권한이 없습니다. 온보딩을 완료했는지 확인해주세요."
                        400 -> "요청 형식이 올바르지 않습니다."
                        500 -> "서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요."
                        else -> "네트워크 연결을 확인해주세요. (코드: $code)"
                    }
                }
                else -> "등록에 실패했습니다: ${e.message ?: "알 수 없는 오류"}"
            }
            throw RuntimeException(msg)
        }
    }

    private suspend fun uploadReportViaApi(
        category: String,
        title: String,
        location: String,
        imageUri: Uri,
        latitude: Double,
        longitude: Double
    ): UploadedReportResult? {
        val reportCategory = when (category) {
            "위험" -> ReportCategory.DANGER
            "불편" -> ReportCategory.INCONVENIENCE
            else -> ReportCategory.DISCOVERY
        }

        val request = ReportCreateRequest(
            title = title,
            latitude = latitude,
            longitude = longitude,
            category = reportCategory,
            address = location
        )
        val requestBody = gson.toJson(request).toRequestBody("application/json".toMediaTypeOrNull())
        val imagePart = uriToPart(imageUri)

        val response = api.createReport(request = requestBody, image = imagePart)
        val reportId = response.data
        Log.d("ReportRepository", "API 응답: reportId=$reportId, status=${response.status}")

        if (reportId != null) {
            return UploadedReportResult(
                documentId = reportId.toString(),
                imageUrl = null,
                imageUri = imageUri,
                category = category,
                title = title,
                location = location
            )
        } else {
            return null
        }
    }

    private suspend fun uriToPart(uri: Uri): MultipartBody.Part = withContext(Dispatchers.IO) {
        val file = File(context.cacheDir, "report_${System.currentTimeMillis()}.jpg")
        val inputStream = context.contentResolver.openInputStream(uri)
        if (inputStream == null) {
            Log.e("ReportRepository", "이미지를 읽을 수 없습니다. openInputStream(uri) null")
            throw RuntimeException("이미지를 읽을 수 없습니다. 다시 촬영해주세요.")
        }
        inputStream.use { input ->
            FileOutputStream(file).use { output ->
                input.copyTo(output)
            }
        }
        if (file.length() == 0L) {
            Log.e("ReportRepository", "이미지 파일 크기가 0바이트입니다.")
            throw RuntimeException("이미지가 비어 있습니다. 다시 촬영해주세요.")
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

package com.example.fillin.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
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

    // 수정된 uploadReport (finalImageUrl 추가)
    suspend fun uploadReport(
        category: String,
        title: String,
        location: String,
        imageUri: Uri,
        finalImageUrl: String? = null, // 👈 추가된 파라미터
        latitude: Double = 0.0,
        longitude: Double = 0.0
    ): UploadedReportResult? {
        val hasToken = TokenManager.getBearerToken(context) != null

        if (hasToken) {
            // API 호출 시 finalImageUrl을 함께 전달합니다.
            val apiResult = uploadReportViaApi(category, title, location, imageUri, finalImageUrl, latitude, longitude)
            if (apiResult != null) return apiResult
        }

        // Firestore fallback (생략된 기존 로직 그대로 사용)
        return firestoreRepository.uploadReport(category, title, location, imageUri, latitude, longitude)
    }

    // 수정된 uploadReportViaApi
    private suspend fun uploadReportViaApi(
        category: String,
        title: String,
        location: String,
        imageUri: Uri,
        finalImageUrl: String?, // 👈 추가
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
            category = reportCategory,
            reportImageUrl = finalImageUrl // 👈 S3에 저장된 모자이크 URL 전달
        )
        val requestBody = gson.toJson(request).toRequestBody("application/json".toMediaTypeOrNull())

        // [중요 로직] 모자이크 이미지 URL이 있다면 이를 서버에 알리거나 처리하는 로직 필요
        // 현재 Swagger(image_f1f483)는 파일을 직접 받으므로, 여기서는 원본 imagePart를 보냅니다.
        // 만약 백엔드에서 finalImageUrl을 JSON(request)에 넣어달라고 하면 DTO 수정을 해야 합니다.
        // 💡 [핵심 수정] 모자이크 URL이 있으면 파일(image)은 null로 보냅니다.
        // 이렇게 해야 서버가 새로 보낸 원본 파일로 덮어쓰지 않고 URL을 사용합니다.
        val imagePart = if (finalImageUrl != null) {
            null
        } else {
            uriToPart(imageUri)
        }

        val response = api.createReport(request = requestBody, image = imagePart)
        val reportId = response.data

        if (reportId != null) {
            UploadedReportResult(
                documentId = reportId.toString(),
                imageUrl = finalImageUrl ?: imageUri.toString(), // 👈 모자이크 URL 우선 사용
                imageUri = imageUri,
                category = category,
                title = title,
                location = location
            )
        } else {
            null
        }
    }.getOrElse { e ->
        handleApiError(e)
        null
    }

    /** 📸 [핵심 수정] Uri를 서버 전송용 Part로 변환 (로그 및 타입 보강) */
    private suspend fun uriToPart(uri: Uri): MultipartBody.Part = withContext(Dispatchers.IO) {
        val file = File(context.cacheDir, "report_${System.currentTimeMillis()}.jpg")

        try {
            // 1. 원본 비트맵 로드
            val inputStream = context.contentResolver.openInputStream(uri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            // 2. EXIF에서 회전 정보 읽기
            val exifInputStream = context.contentResolver.openInputStream(uri)
            val exif = exifInputStream?.use { ExifInterface(it) }
            val orientation = exif?.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)

            // 3. 각도에 맞춰 비트맵 회전
            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            }

            val rotatedBitmap = Bitmap.createBitmap(
                originalBitmap, 0, 0, originalBitmap.width, originalBitmap.height, matrix, true
            )

            // 4. 회전된 비트맵을 파일로 저장 (서버가 EXIF를 몰라도 정방향으로 보이게 함)
            FileOutputStream(file).use { out ->
                rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out) // 품질 90%로 압축
            }

            Log.d("ReportDebug", "이미지 정방향 회전 완료: ${file.length()} bytes")

            // 메모리 해제
            if (originalBitmap != rotatedBitmap) originalBitmap.recycle()
            rotatedBitmap.recycle()

        } catch (e: Exception) {
            Log.e("ReportDebug", "이미지 회전 처리 중 오류", e)
        }

        val requestBody = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
        MultipartBody.Part.createFormData("image", file.name, requestBody)
    }

    /** 1. AI 분석 요청 */
    suspend fun analyzeImage(imageUri: Uri): com.example.fillin.data.model.report.ReportAnalyzeResponse? = withContext(Dispatchers.IO) {
        try {
            val imagePart = uriToPart(imageUri)
            api.analyzeReportImage(imagePart)
        } catch (e: Exception) {
            Log.e("ReportDebug", "AI 분석 API 실패", e)
            null
        }
    }

    /** 2. 이미지 전처리(번호판 모자이크) 요청 */
    suspend fun processImage(imageUri: Uri): com.example.fillin.data.model.report.ReportImageProcessResponse? = withContext(Dispatchers.IO) {
        try {
            val imagePart = uriToPart(imageUri)
            api.processReportImage(imagePart)
        } catch (e: Exception) {
            Log.e("ReportDebug", "이미지 전처리 API 실패", e)
            null
        }
    }

    private fun handleApiError(e: Throwable) {
        if (e is HttpException) {
            val body = e.response()?.errorBody()?.string() ?: ""
            Log.e("ReportDebug", "API 오류: ${e.code()} | body=$body")
        } else {
            Log.e("ReportDebug", "네트워크 오류", e)
        }
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

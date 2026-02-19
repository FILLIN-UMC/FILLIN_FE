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
import com.example.fillin.data.db.UploadedReportResult
import com.example.fillin.data.model.report.MapMarkerResponse
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
        finalImageUrl: String? = null, // 👈 추가된 파라미터
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
            val apiResult = uploadReportViaApi(category, title, location, imageUri, finalImageUrl, latitude, longitude)
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
        finalImageUrl: String?, // 👈 추가
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
            address = location,
            reportImageUrl = finalImageUrl // 👈 S3에 저장된 모자이크 URL 전달

        )
        val jsonRequest = gson.toJson(request)
        Log.d("ReportDebug", "최종 등록 요청 JSON: $jsonRequest")
        val requestBody = jsonRequest.toRequestBody("application/json".toMediaTypeOrNull())
        // 💡 [핵심 수정] 모자이크 URL이 있으면 파일 파라미터는 null로 보냅니다
        // 서버가 원본 파일로 덮어쓰지 않게 하기 위함입니다.
        val imagePart = if (finalImageUrl != null) null else uriToPart(imageUri)

        val response = api.createReport(request = requestBody, image = imagePart)
        val reportId = response.data
        Log.d("ReportRepository", "API 응답: reportId=$reportId, status=${response.status}")

        if (reportId != null) {
            return UploadedReportResult(
                documentId = reportId.toString(),
                // 💡 중요: finalImageUrl이 있으면 그걸 쓰고, 없으면 로컬 URI라도 써야 합니다.
                imageUrl = finalImageUrl ?: imageUri.toString(),
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
        // [기능 1] 임시 파일 생성 (파일명에 타임스탬프 포함하여 중복 방지)
        val file = File(context.cacheDir, "report_${System.currentTimeMillis()}.jpg")

        try {
            // [기능 2] openInputStream을 통한 이미지 읽기 및 null 체크 (예외 처리 포함)
            val inputStream = context.contentResolver.openInputStream(uri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream)

            // 팀원 코드의 'if (inputStream == null)' 기능을 아래 throw문이 대신 수행합니다.
            inputStream?.close() ?: throw RuntimeException("이미지를 읽을 수 없습니다.")

            // --- (추가된 기능: 이미지 회전 정보(EXIF) 확인 및 Matrix 회전 처리) ---
            val exifInputStream = context.contentResolver.openInputStream(uri)
            val exif = exifInputStream?.use { ExifInterface(it) }
            val orientation = exif?.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)

            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            }

            val rotatedBitmap = Bitmap.createBitmap(
                originalBitmap, 0, 0, originalBitmap.width, originalBitmap.height, matrix, true
            )
            // --------------------------------------------------------------------------

            // [기능 3] 파일에 데이터 쓰기 (FileOutputStream 사용)
            // 원본 코드는 'input.copyTo'를 썼지만, 여기서는 회전된 데이터를 'compress'로 저장합니다.
            FileOutputStream(file).use { out ->
                rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }

            // [기능 4] 파일 크기(length) 확인 및 로그 출력
            // 팀원 코드의 'if (file.length() == 0L)' 체크 기능을 로그와 try-catch가 함께 수행합니다.
            Log.d("ReportDebug", "이미지 회전 및 준비 완료: ${file.length()} bytes")

            if (originalBitmap != rotatedBitmap) originalBitmap.recycle()
            rotatedBitmap.recycle()

        } catch (e: Exception) {
            // 팀원 코드의 로그 기록 및 에러 던지기 기능과 동일합니다.
            Log.e("ReportDebug", "이미지 처리 중 오류", e)
            throw e
        }

        // [기능 5] MultipartBody.Part 생성 및 전송 (image/jpeg로 타입 구체화)
        val requestBody = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
        MultipartBody.Part.createFormData("image", file.name, requestBody)
    }

    /** 4. 예외 통합 처리 */
    private fun handleException(e: Exception) {
        val msg = when (e) {
            is HttpException -> {
                val body = e.response()?.errorBody()?.string() ?: ""
                Log.e("ReportDebug", "API 오류: ${e.code()}, body=$body")
                when (e.code()) {
                    401 -> "로그인이 만료되었습니다."
                    403 -> "권한이 없습니다."
                    else -> "서버 통신 실패 (코드: ${e.code()})"
                }
            }
            else -> "등록 실패: ${e.message}"
        }
        throw RuntimeException(msg)
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

    suspend fun getMapMarkers(
        minLat: Double, maxLat: Double,
        minLon: Double, maxLon: Double
    ): List<MapMarkerResponse>? = withContext(Dispatchers.IO) {
        try {
            val response = api.getMapMarkers(minLat, maxLat, minLon, maxLon)
            response.data // 👈 ApiResponse 객체에서 실제 리스트인 data만 추출
        } catch (e: Exception) {
            Log.e("ReportRepository", "마커 조회 실패", e)
            null
        }
    }
}

package com.example.fillin.feature.report

import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fillin.data.db.UploadedReportResult
import com.example.fillin.data.repository.ReportRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.async

/** 업로드 직후 API 덮어쓰기 방지 + 재진입 시에도 방금 올린 제보 보존용 스냅샷 */
data class LastUploadedSnapshot(
    val id: Long,
    val latitude: Double,
    val longitude: Double,
    val category: String,
    val title: String,
    val location: String,
    val documentId: String
)

class ReportViewModel(private val repository: ReportRepository) : ViewModel() {

    init {
        Log.d("ReportDebug", "ViewModel이 생성되었습니다!") // 앱 실행 시 이게 먼저 떠야 합니다.
    }
    var isUploading by mutableStateOf(false)
        private set
    /** null: 대기, true: 성공, false: 실패 */
    var uploadStatus by mutableStateOf<Boolean?>(null)
        private set
    /** 실패 시 표시할 오류 메시지 (null이면 기본 문구 사용) */
    var uploadErrorMessage by mutableStateOf<String?>(null)
        private set
    /** 업로드 성공 시 지도에 추가할 새 제보 데이터 (HomeScreen에서 소비 후 clear) */
    var lastUploadedReport by mutableStateOf<UploadedReportResult?>(null)
        private set

    /** 업로드 직후 5초 동안 API 목록 덮어쓰기 방지 (화면 재진입 시에도 유지) */
    var lastUploadTimeMillis by mutableStateOf(0L)
        private set
    /** 방금 올린 제보 ID (병합 시 누락 방지용) */
    var lastUploadedReportId by mutableStateOf<Long?>(null)
        private set
    /** 방금 올린 제보 스냅샷 (API 병합 후 목록에 없을 때 재추가용) */
    var lastUploadedReportSnapshot by mutableStateOf<LastUploadedSnapshot?>(null)
        private set

    /** 업로드 성공 시 HomeScreen에서 호출. 5초 가드 + 병합 시 보존용 스냅샷 설정 */
    fun setUploadGuard(
        reportId: Long,
        latitude: Double,
        longitude: Double,
        category: String,
        title: String,
        location: String,
        documentId: String
    ) {
        lastUploadTimeMillis = System.currentTimeMillis()
        lastUploadedReportId = reportId
        lastUploadedReportSnapshot = LastUploadedSnapshot(
            id = reportId,
            latitude = latitude,
            longitude = longitude,
            category = category,
            title = title,
            location = location,
            documentId = documentId
        )
    }

    /** 업로드 처리 완료 후 5초 뒤 가드 해제 (그동안 API 덮어쓰기 방지) */
    fun scheduleClearUploadGuard(delayMs: Long = 5000L) {
        viewModelScope.launch {
            delay(delayMs)
            clearUploadGuard()
        }
    }

    fun clearUploadGuard() {
        lastUploadTimeMillis = 0L
        lastUploadedReportId = null
        lastUploadedReportSnapshot = null
    }

    /** 서버에서 받은 모자이크 처리된 이미지 URL (null이면 원본 표시) */
    var processedImageUrl by mutableStateOf<String?>(null)
        private set

    var isProcessingImage by mutableStateOf(false) // 전처리 중인지 상태 추가
        private set

    // 사진이 준비되면 즉시 호출할 함수
    fun prepareImage(uri: Uri) {
        viewModelScope.launch {
            try {
                isProcessingImage = true
                processedImageUrl = null // 초기화

                Log.d("ReportDebug", "전처리 시작: $uri")
                val result = repository.processImage(uri)

                if (result?.data?.hasLicensePlate == true) {
                    processedImageUrl = result.data.processedImageUrl
                    Log.d("ReportDebug", "번호판 감지! 모자이크 URL로 변경됨: $processedImageUrl")
                } else {
                    Log.d("ReportDebug", "번호판 없음. 원본 사용")
                }
            } catch (e: Exception) {
                Log.e("ReportDebug", "전처리 에러", e)
            } finally {
                isProcessingImage = false
            }
        }
    }
    fun uploadReport(
        category: String,
        title: String,
        imageUri: Uri,
        location: String,
        latitude: Double,
        longitude: Double
    ) {
        viewModelScope.launch {
            try {
                isUploading = true
                uploadStatus = null
                uploadErrorMessage = null
                lastUploadedReport = null

                // 1. [병렬 호출] 분석과 전처리를 동시에 시작
                val analyzeDeferred = async { repository.analyzeImage(imageUri) }
                val processDeferred = async { repository.processImage(imageUri) }

                // 2. 결과 대기
                val analyzeResult = analyzeDeferred.await()
                val processResult = processDeferred.await()

                // [로그 추가] 서버 응답 결과 확인
                Log.d("ReportDebug", "--- 이미지 전처리 결과 확인 ---")
                if (processResult != null && processResult.status == "OK") { // status 체크 추가 권장
                    Log.d("ReportDebug", "감지된 번호판 유무: ${processResult.data?.hasLicensePlate}")
                    Log.d("ReportDebug", "처리된 이미지 URL: ${processResult.data?.processedImageUrl}")
                } else {
                    Log.e("ReportDebug", "이미지 전처리 API 호출 실패 또는 에러 응답: ${processResult?.message}")
                }
                Log.d("ReportDebug", "------------------------------")

                // 3. 번호판 유무에 따른 데이터 처리
                val hasPlate = processResult?.data?.hasLicensePlate == true
                val finalImageUrl = if (hasPlate) processResult?.data?.processedImageUrl else null

                // UI 업데이트를 위한 상태 저장
                processedImageUrl = finalImageUrl

                // 4. 최종 제보 등록 호출
                val result = repository.uploadReport(
                    category = analyzeResult?.data?.category ?: "DANGER",
                    title = analyzeResult?.data?.title ?: "새로운 제보",
                    location = location,
                    imageUri = imageUri,
                    finalImageUrl = finalImageUrl,
                    latitude = latitude,
                    longitude = longitude
                )

                if (result != null) {
                    uploadStatus = true
                    lastUploadedReport = result
                } else {
                    uploadStatus = false
                    uploadErrorMessage = "서버에서 응답이 없습니다."
                }
            } catch (e: Exception) {
                e.printStackTrace()
                uploadStatus = false
                uploadErrorMessage = e.message ?: "등록에 실패했습니다. 다시 시도해주세요."
            } finally {
                isUploading = false
            }
        }
    }
    // 화면을 나갈 때나 초기화할 때 호출
    override fun onCleared() {
        super.onCleared()
        processedImageUrl = null
    }



    fun resetStatus() {
        uploadStatus = null
        uploadErrorMessage = null
        lastUploadedReport = null
    }
}

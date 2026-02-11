package com.example.fillin.feature.report

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fillin.data.db.UploadedReportResult
import com.example.fillin.data.repository.ReportRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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

    var isUploading by mutableStateOf(false)
        private set
    /** null: 대기, true: 성공, false: 실패 */
    var uploadStatus by mutableStateOf<Boolean?>(null)
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

    fun uploadReport(
        category: String,
        title: String,
        location: String,
        imageUri: Uri,
        latitude: Double = 0.0,
        longitude: Double = 0.0
    ) {
        viewModelScope.launch {
            try {
                isUploading = true
                uploadStatus = null
                lastUploadedReport = null

                val result = repository.uploadReport(
                    category = category,
                    title = title,
                    location = location,
                    imageUri = imageUri,
                    latitude = latitude,
                    longitude = longitude
                )

                if (result != null) {
                    uploadStatus = true
                    lastUploadedReport = result
                } else {
                    uploadStatus = false
                }
            } catch (e: Exception) {
                e.printStackTrace()
                uploadStatus = false
            } finally {
                isUploading = false
            }
        }
    }

    fun resetStatus() {
        uploadStatus = null
        lastUploadedReport = null
    }
}

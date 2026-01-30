package com.example.fillin.feature.report

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fillin.data.db.FirestoreRepository
import com.example.fillin.data.db.UploadedReportResult
import kotlinx.coroutines.launch

class ReportViewModel(private val repository: FirestoreRepository) : ViewModel() {

    var isUploading by mutableStateOf(false)
        private set
    /** null: 대기, true: 성공, false: 실패 */
    var uploadStatus by mutableStateOf<Boolean?>(null)
        private set
    /** 업로드 성공 시 지도에 추가할 새 제보 데이터 (HomeScreen에서 소비 후 clear) */
    var lastUploadedReport by mutableStateOf<UploadedReportResult?>(null)
        private set

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

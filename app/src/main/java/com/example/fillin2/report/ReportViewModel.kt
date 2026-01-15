package com.example.fillin2.report

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fillin2.db.FirestoreRepository
import kotlinx.coroutines.launch

// [참고] FirestoreRepository를 주입받아 사용합니다.
class ReportViewModel(private val repository: FirestoreRepository) : ViewModel() {

    // 1. UI 상태 관리: 업로드 중인지 여부 (로딩 화면 표시용)
    var isUploading by mutableStateOf(false)
        private set
    // mutableStateOf + private set: 뷰모델 내부에서만 상태를 수정할 수 있고, UI(Compose)에서는 읽기만 가능하게 하여 데이터의 무결성을 지킵니다.
    // 2. UI 상태 관리: 업로드 결과 (성공/실패/대기)
    // null: 대기, true: 성공, false: 실패
    var uploadStatus by mutableStateOf<Boolean?>(null)
        private set

    /**
     * 실제로 등록 버튼을 눌렀을 때 호출되는 함수
     */
    fun uploadReport(category: String, title: String, location: String, imageUri: Uri) {
        // ViewModelScope를 사용하여 코루틴 실행 (비동기 작업)
        viewModelScope.launch {
            try {
                isUploading = true // 로딩 시작
                uploadStatus = null

                // Repository에 실제 데이터 전송 요청
                val result = repository.uploadReport(
                    category = category,
                    title = title,
                    location = location,
                    imageUri = imageUri
                )

                uploadStatus = result // 결과 반영 (true/false)
            } catch (e: Exception) {
                e.printStackTrace()
                uploadStatus = false // 예외 발생 시 실패 처리
            } finally {
                isUploading = false // 로딩 종료
            }// 어떤 상황에서도 isUploading = false가 실행되게 하여, 네트워크 오류가 나도 무한 로딩에 빠지지 않게 한다.
        }
    }

    // 결과 확인 후 상태를 초기화하는 함수
    fun resetStatus() {
        uploadStatus = null
    }
}
package com.example.fillin2.ai
// "AI 분석 중" 로딩 화면을 제어하기 위해 ViewModel에서 상태를 관리합니다.
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

// viewmodel/GeminiViewModel.kt
class GeminiViewModel(private val repository: GeminiRepository) : ViewModel() {
    var aiResult by mutableStateOf("") // AI가 분석한 제목 결과
        private set
    var isAnalyzing by mutableStateOf(false) // AI 분석 중 로딩 상태
        private set

    fun analyzeImage(context: Context, uri: Uri, apiKey: String) {
        viewModelScope.launch {
            isAnalyzing = true // 로딩 시작
            try {
                // GeminiRepository의 분석 함수 호출
                val result = repository.fetchAiAnalysis(context, uri, apiKey)
                aiResult = result // 결과 저장
            } catch (e: Exception) {
                if (e is retrofit2.HttpException) {
                    // 구글 서버가 보낸 에러 상세 내용 (예: "모델 이름이 틀림", "지역 제한" 등)을 출력합니다.
                    val errorBody = e.response()?.errorBody()?.string()
                    Log.e("FILLIN_DEBUG", "HTTP 에러 상세: $errorBody")
                }
                Log.e("FILLIN_DEBUG", "AI 분석 에러 발생: ${e.message}")
                aiResult = "분석 실패"
            } finally {
                isAnalyzing = false // 로딩 종료
            }
        }
    }

    // [추가] 분석 결과를 초기화하는 함수입니다.
    // 이 함수가 실행되어 aiResult가 ""(빈 문자열)이 되면,
    // ReportScreen의 if(aiResult.isNotEmpty()) 조건이 거짓이 되어 결과 화면이 닫힙니다.
    fun clearResult() {
        aiResult = ""
    }
}
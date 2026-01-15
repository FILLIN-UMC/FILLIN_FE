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
            Log.d("FILLIN_DEBUG", "========================================")
            Log.d("FILLIN_DEBUG", "ViewModel: analyzeImage 시작")
            Log.d("FILLIN_DEBUG", "이미지 URI: $uri")
            Log.d("FILLIN_DEBUG", "API 키 길이: ${apiKey.length}")
            Log.d("FILLIN_DEBUG", "========================================")

            try {
                // GeminiRepository의 분석 함수 호출
                Log.d("FILLIN_DEBUG", "Repository 호출 시작...")
                val result = repository.fetchAiAnalysis(context, uri, apiKey)
                Log.d("FILLIN_DEBUG", "Repository 호출 성공!")
                Log.d("FILLIN_DEBUG", "받은 결과: $result")
                aiResult = result // 결과 저장

            } catch (e: retrofit2.HttpException) {
                // HTTP 에러 (400, 404, 500 등)
                val errorBody = e.response()?.errorBody()?.string()
                Log.e("FILLIN_DEBUG", "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                Log.e("FILLIN_DEBUG", "❌ HTTP 에러 발생!")
                Log.e("FILLIN_DEBUG", "에러 코드: ${e.code()}")
                Log.e("FILLIN_DEBUG", "에러 메시지: ${e.message()}")
                Log.e("FILLIN_DEBUG", "HTTP 에러 상세: $errorBody")
                Log.e("FILLIN_DEBUG", "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                aiResult = "분석 실패"

            } catch (e: java.net.UnknownHostException) {
                // 인터넷 연결 없음
                Log.e("FILLIN_DEBUG", "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                Log.e("FILLIN_DEBUG", "❌ 인터넷 연결 없음!")
                Log.e("FILLIN_DEBUG", "WiFi 또는 모바일 데이터를 확인하세요")
                Log.e("FILLIN_DEBUG", "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                aiResult = "분석 실패"

            } catch (e: java.net.SocketTimeoutException) {
                // 타임아웃
                Log.e("FILLIN_DEBUG", "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                Log.e("FILLIN_DEBUG", "❌ 서버 응답 시간 초과!")
                Log.e("FILLIN_DEBUG", "네트워크가 느리거나 서버 문제일 수 있습니다")
                Log.e("FILLIN_DEBUG", "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                aiResult = "분석 실패"

            } catch (e: Exception) {
                // 기타 모든 에러
                Log.e("FILLIN_DEBUG", "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                Log.e("FILLIN_DEBUG", "❌ 예상치 못한 에러 발생!")
                Log.e("FILLIN_DEBUG", "에러 타입: ${e.javaClass.simpleName}")
                Log.e("FILLIN_DEBUG", "에러 메시지: ${e.message}")
                Log.e("FILLIN_DEBUG", "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                e.printStackTrace()
                aiResult = "분석 실패"

            } finally {
                isAnalyzing = false // 로딩 종료
                Log.d("FILLIN_DEBUG", "========================================")
                Log.d("FILLIN_DEBUG", "ViewModel: analyzeImage 종료")
                Log.d("FILLIN_DEBUG", "최종 결과: $aiResult")
                Log.d("FILLIN_DEBUG", "========================================")
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
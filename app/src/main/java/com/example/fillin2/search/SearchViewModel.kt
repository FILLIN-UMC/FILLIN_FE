package com.example.fillin2.search

import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fillin2.kakao.Place
import com.example.fillin2.kakao.RetrofitClient
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SearchViewModel : ViewModel() {
    // 1. 검색 결과 리스트를 담을 상태 변수 (Compose UI가 관찰함)
    private val _searchResult = mutableStateOf<List<Place>>(emptyList())
    val searchResult: State<List<Place>> = _searchResult

    // 연속 타이핑 시 이전 요청을 취소하기 위한 변수
    private var searchJob: Job? = null

    // 2. 검색 기능을 수행하는 함수
    fun searchPlaces(query: String) {
        // 검색어가 비어있으면 즉시 리스트 비우기
        if (query.isBlank()) {
            _searchResult.value = emptyList()
            return
        }

        // 새로운 글자가 입력되면 이전 대기 중인 검색 작업 취소 (데이터 절약)
        searchJob?.cancel()

        searchJob = viewModelScope.launch {
            // 0.3초 대기: 사용자가 타이핑을 멈췄을 때만 API 호출
            // 새로운 타이핑이 시작되면 아직 응답이 오지 않은 이전 요청은 가차 없이 버려서 앱 속도를 높여줘.
            delay(300L)

            try {
                // ★ 네가 가진 REST API 키 사용 (KakaoAK 뒤에 공백 필수!)
                val apiKey = "KakaoAK bace9c32155b5a56bcbb4a74fdd04e9a"

                // Retrofit을 이용해 서버에 요청
                val response = RetrofitClient.kakaoApi.searchPlace(apiKey, query)

                // 받아온 결과(documents)를 상태 변수에 저장
                _searchResult.value = response.documents

                Log.d("Search", "검색 성공: ${response.documents.size}건")

            } catch (e: Exception) {
                Log.e("Search", "검색 실패: ${e.message}")
                _searchResult.value = emptyList()
            }
        }
    }
}
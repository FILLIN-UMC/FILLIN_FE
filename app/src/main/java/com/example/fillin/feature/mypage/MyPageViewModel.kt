package com.example.fillin.feature.mypage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import com.example.fillin.data.AppPreferences
import com.example.fillin.data.SharedReportData
import com.example.fillin.domain.model.ReportType

class MyPageViewModel(
    private val appPreferences: AppPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow<MyPageUiState>(MyPageUiState.Loading)
    val uiState: StateFlow<MyPageUiState> = _uiState

    init {
        load()
        // 닉네임 변경 감지
        appPreferences.nicknameFlow
            .onEach { newNickname ->
                updateNickname(newNickname)
            }
            .launchIn(viewModelScope)
    }

    fun load() {
        // SharedReportData에서 제보 데이터 가져오기
        val userReports = SharedReportData.getUserReports()
        
        // 제보 통계 계산
        val totalReports = userReports.size
        val totalViews = userReports.sumOf { it.report.viewCount }
        val dangerCount = userReports.count { it.report.type == ReportType.DANGER }
        val inconvenienceCount = userReports.count { it.report.type == ReportType.INCONVENIENCE }
        val discoveryCount = userReports.count { it.report.type == ReportType.DISCOVERY }
        
        val summary = MyPageSummary(
            nickname = appPreferences.getNickname(),
            totalReports = totalReports,
            totalViews = totalViews,
            danger = dangerCount to 5, // 목표는 5로 설정
            inconvenience = inconvenienceCount to 5,
            discoveryCount = discoveryCount
        )

        // 제보 데이터를 MyReportCard로 변환
        val reports = userReports.map { reportWithLocation ->
            MyReportCard(
                id = reportWithLocation.report.id,
                title = reportWithLocation.report.title,
                meta = reportWithLocation.report.meta,
                imageResId = reportWithLocation.report.imageResId,
                viewCount = reportWithLocation.report.viewCount
            )
        }

        _uiState.value = MyPageUiState.Success(summary, reports)
    }

    private fun updateNickname(newNickname: String) {
        _uiState.update { currentState ->
            when (currentState) {
                is MyPageUiState.Success -> {
                    currentState.copy(
                        summary = currentState.summary.copy(nickname = newNickname)
                    )
                }
                else -> currentState
            }
        }
    }
}

package com.example.fillin.feature.mypage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import com.example.fillin.data.AppPreferences
import com.example.fillin.data.ReportStatusManager
import com.example.fillin.data.SharedReportData
import com.example.fillin.domain.model.ReportStatus
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
        // SharedReportData에서 제보 데이터 가져오기 + 상태 반영 (EXPIRING/EXPIRED)
        val userReports = SharedReportData.getUserReports()
        val updatedUserReports = userReports.map { rwl ->
            rwl.copy(report = ReportStatusManager.updateReportStatus(rwl.report))
        }

        // 제보 통계 계산 (업데이트된 리스트 기준)
        val totalReports = updatedUserReports.size
        val totalViews = updatedUserReports.sumOf { it.report.viewCount }
        val dangerCount = updatedUserReports.count { it.report.type == ReportType.DANGER }
        val inconvenienceCount = updatedUserReports.count { it.report.type == ReportType.INCONVENIENCE }
        val discoveryCount = updatedUserReports.count { it.report.type == ReportType.DISCOVERY }

        val summary = MyPageSummary(
            nickname = appPreferences.getNickname(),
            totalReports = totalReports,
            totalViews = totalViews,
            danger = dangerCount to 5,
            inconvenience = inconvenienceCount to 5,
            discoveryCount = discoveryCount
        )

        // 제보 데이터를 MyReportCard로 변환
        val reports = updatedUserReports.map { reportWithLocation ->
            MyReportCard(
                id = reportWithLocation.report.id,
                title = reportWithLocation.report.title,
                meta = reportWithLocation.report.meta,
                imageResId = reportWithLocation.report.imageResId,
                imageUrl = reportWithLocation.report.imageUrl,
                viewCount = reportWithLocation.report.viewCount
            )
        }

        // EXPIRING 상태 제보가 있을 때만 사라질 제보 알림 생성
        val expiringReports = updatedUserReports.filter { it.report.status == ReportStatus.EXPIRING }
        val expiringNotice = if (expiringReports.isEmpty()) null else {
            val threeDaysMillis = 3 * 24 * 60 * 60 * 1000L
            val oneDayMillis = 24 * 60 * 60 * 1000L
            val now = System.currentTimeMillis()
            val minExpiringAt = expiringReports.mapNotNull { it.report.expiringAtMillis }.minOrNull() ?: now
            val daysLeft = ((minExpiringAt + threeDaysMillis - now) / oneDayMillis).toInt().coerceAtLeast(0)
            val dangerExpiring = expiringReports.count { it.report.type == ReportType.DANGER }
            val inconvenienceExpiring = expiringReports.count { it.report.type == ReportType.INCONVENIENCE }
            val discoveryExpiring = expiringReports.count { it.report.type == ReportType.DISCOVERY }
            val parts = buildList {
                if (dangerExpiring > 0) add("위험 $dangerExpiring")
                if (inconvenienceExpiring > 0) add("불편 $inconvenienceExpiring")
                if (discoveryExpiring > 0) add("발견 $discoveryExpiring")
            }
            ExpiringReportNotice(daysLeft = daysLeft, summaryText = parts.joinToString(", "))
        }

        _uiState.value = MyPageUiState.Success(summary, reports, expiringNotice)
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

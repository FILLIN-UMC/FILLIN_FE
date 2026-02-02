package com.example.fillin.feature.mypage

import android.content.Context
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
        load(null)
        // 닉네임 변경 감지
        appPreferences.nicknameFlow
            .onEach { newNickname ->
                updateNickname(newNickname)
            }
            .launchIn(viewModelScope)
    }

    fun load(context: Context? = null) {
        // SharedReportData에서 제보 데이터 가져오기 + 상태 반영 (EXPIRING/EXPIRED)
        // context 제공 시 완전 삭제한 제보 제외하여 총 제보 수 반영
        val userReports = SharedReportData.getUserReports(context)
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

        // EXPIRING 제보를 daysLeft별 그룹화, 남은 기간 많은 순 (3일→2일→1일 순으로 알림 표시)
        val expiringReports = updatedUserReports.filter { it.report.status == ReportStatus.EXPIRING }
        val threeDaysMillis = 3 * 24 * 60 * 60 * 1000L
        val oneDayMillis = 24 * 60 * 60 * 1000L
        val now = System.currentTimeMillis()
        val expiringNoticeList = if (expiringReports.isEmpty()) emptyList() else {
            val groupedByDaysLeft = expiringReports.groupBy { rwl ->
                val expiringAt = rwl.report.expiringAtMillis ?: now
                ((expiringAt + threeDaysMillis - now) / oneDayMillis).toInt().coerceAtLeast(0)
            }
            groupedByDaysLeft.keys.sortedDescending().map { daysLeft ->
                // 같은 날 사라지는 제보: 등록일 오래된 순(먼저 등록된 것) → 왼쪽 이미지부터 표시
                val groupReports = (groupedByDaysLeft[daysLeft] ?: emptyList())
                    .sortedWith(compareBy({ it.report.createdAtMillis }, { it.report.id }))
                val parts = buildList {
                    val d = groupReports.count { it.report.type == ReportType.DANGER }
                    val i = groupReports.count { it.report.type == ReportType.INCONVENIENCE }
                    val s = groupReports.count { it.report.type == ReportType.DISCOVERY }
                    if (d > 0) add("위험 $d")
                    if (i > 0) add("불편 $i")
                    if (s > 0) add("발견 $s")
                }
                // 3개 이상: 등록일 오래된 순 3개만 / 2개: 2개 / 1개: 1개
                val reportImages = groupReports.take(3).map { rwl ->
                    ExpiringReportImage(
                        imageUrl = rwl.report.imageUrl,
                        imageResId = rwl.report.imageResId
                    )
                }
                ExpiringReportNotice(daysLeft = daysLeft, summaryText = parts.joinToString(", "), reportImages = reportImages)
            }
        }

        _uiState.value = MyPageUiState.Success(summary, reports, expiringNoticeList)
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

package com.example.fillin.feature.mypage

/** 사라질 제보 알림용 (EXPIRING 상태 제보가 있을 때만 표시) */
data class ExpiringReportNotice(
    val daysLeft: Int,
    val summaryText: String // e.g. "위험 1, 발견 2"
)

data class MyPageSummary(
    val nickname: String,
    val totalReports: Int,
    val totalViews: Int,
    val danger: Pair<Int, Int>,        // count to goal
    val inconvenience: Pair<Int, Int>, // count to goal
    val discoveryCount: Int            // 발견 제보 개수
)

data class MyReportCard(
    val id: Long,
    val title: String,
    val meta: String,
    val imageResId: Int? = null,  // 제보 이미지 리소스 ID (로컬 리소스)
    val imageUrl: String? = null,  // 제보 이미지 URL (Firestore/Storage)
    val viewCount: Int = 0  // 조회수
)

sealed interface MyPageUiState {
    data object Loading : MyPageUiState
    data class Success(
        val summary: MyPageSummary,
        val reports: List<MyReportCard>,
        /** EXPIRING 상태 제보가 있을 때만 non-null (사라질 제보 알림 표시) */
        val expiringNotice: ExpiringReportNotice? = null
    ) : MyPageUiState
    data class Error(val message: String) : MyPageUiState
}

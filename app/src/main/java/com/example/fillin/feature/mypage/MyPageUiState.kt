package com.example.fillin.feature.mypage

/** 사라질 제보 알림용 (EXPIRING 상태 제보가 있을 때만 표시) */
data class ExpiringReportNotice(
    val daysLeft: Int,
    val summaryText: String, // e.g. "위험 1, 발견 2"
    /** 같은 날 사라지는 제보 이미지 (등록일 오래된 순 = 왼쪽부터) */
    val reportImages: List<ExpiringReportImage> = emptyList()
)

/** 제보 이미지 정보 (imageUrl 우선, 없으면 imageResId) */
data class ExpiringReportImage(
    val imageUrl: String? = null,
    val imageResId: Int? = null
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
        /** EXPIRING 제보를 daysLeft별 그룹화, 남은 기간 많은 순 (사라질 제보 알림 순차 표시) */
        val expiringNoticeList: List<ExpiringReportNotice> = emptyList()
    ) : MyPageUiState
    data class Error(val message: String) : MyPageUiState
}

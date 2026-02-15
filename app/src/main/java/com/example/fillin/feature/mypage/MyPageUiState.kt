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
    /** achievement from API: ROOKIE, VETERAN, MASTER (null이면 totalReports로 계산) */
    val achievement: String? = null,
    val danger: Pair<Int, Int>,        // (currentCount, targetCount)
    val inconvenience: Pair<Int, Int>, // (currentCount, targetCount)
    val discovery: Pair<Int, Int>      // (currentCount, targetCount)
)

data class MyReportCard(
    val id: Long,
    /** 주소 (이미지 내부 표시) */
    val address: String,
    /** 거리 "가는길 255m" (이미지 내부 표시) */
    val distance: String,
    /** 제보 제목 (이미지 아래 표시) */
    val reportTitle: String,
    val imageResId: Int? = null,  // 제보 이미지 리소스 ID (로컬 리소스)
    val imageUrl: String? = null,  // 제보 이미지 URL (Firestore/Storage)
    val viewCount: Int = 0  // 조회수
)

sealed interface MyPageUiState {
    data object Loading : MyPageUiState
    data class Success(
        val summary: MyPageSummary,
        val reports: List<MyReportCard>,
        /** 저장한 제보 (좋아요한 제보) - GET /api/mypage/reports/like */
        val likedReports: List<MyReportCard> = emptyList(),
        /** EXPIRING 제보를 daysLeft별 그룹화, 남은 기간 많은 순 (사라질 제보 알림 순차 표시) */
        val expiringNoticeList: List<ExpiringReportNotice> = emptyList()
    ) : MyPageUiState
    data class Error(val message: String) : MyPageUiState
}

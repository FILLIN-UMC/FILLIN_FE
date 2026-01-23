package com.example.fillin.feature.mypage

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
    val imageResId: Int? = null  // 제보 이미지 리소스 ID
)

sealed interface MyPageUiState {
    data object Loading : MyPageUiState
    data class Success(
        val summary: MyPageSummary,
        val reports: List<MyReportCard>
    ) : MyPageUiState
    data class Error(val message: String) : MyPageUiState
}

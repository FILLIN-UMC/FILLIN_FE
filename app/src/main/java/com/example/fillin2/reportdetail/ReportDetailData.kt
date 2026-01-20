package com.example.fillin2.reportdetail

import java.util.concurrent.TimeUnit

/**
 * 1. 카테고리 정의 (Enum)
 * 한 파일에 같이 두면 참조하기 쉽습니다.
 */
enum class ReportCategory(val displayName: String) {
    DANGER("위험"),
    INCONVENIENCE("불편"),
    DISCOVERY("발견")
}

/**
 * 2. 상세 정보 데이터 모델 (Data Class)
 * UI에 필요한 모든 계산 로직을 포함합니다.
 */

data class ReportDetailData(
    /** [제보 시점에 결정되는 고정 데이터] **/
    val imageUrl: String,           // 제보 사진 URL
    val category: ReportCategory,  //  카테고리 (위험, 불편, 발견). 위에서 만든 Enum을 사용합니다.
    val title: String,          // 한 줄 설명
    val registrationDate: Long, // 제보 시점 (Timestamp)
    val address: String,        // 위치 정보 주소
    val reporterName: String,   // 제보자 이름
    val reporterLevel: String,  // 제보자 레벨
    /** [사용자 활동에 의해 변하는 유동 데이터] **/
    val positiveCount: Int,     // 긍정 피드백 수
    val negativeCount: Int,     // 부정 피드백 수
    val viewCount: Int,         // 조회수
    val isLiked: Boolean,       // 좋아요
    val isPositiveSelected: Boolean = false, // 긍정 버튼 선택 여부
    val isNegativeSelected: Boolean = false  // 부정 버튼 선택 여부
) {
    // [로직 A] 정보 유효성 상태 계산
    val validityStatus: String
        get() {
            val total = positiveCount + negativeCount
            val ratio = if (total > 0) (positiveCount.toFloat() / total * 100).toInt() else 0
            val days = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - registrationDate)

            return when {
                days >= 14 -> "오래된 제보일 수 있어요"            // 등록 2주 이상
                ratio in 70..100 -> "최근에도 확인됐어요"    // 긍정 70% 이상
                ratio in 40..60 -> "제보 의견이 나뉘어요"    // 긍정 40~60%
                else -> ""
            }
        }

    // [로직 B] 카테고리에 따른 가변 텍스트들 (else 없이 깔끔!)
    val feedbackQuestion: String
        get() = when (category) {
            ReportCategory.DANGER -> "지금도 조심해야 할 상황인가요?"
            ReportCategory.INCONVENIENCE -> "아직 불편한 상황인가요?"
            ReportCategory.DISCOVERY -> "지금도 참고할 만한 정보인가요?"
        }
    //  카테고리별 긍정 버튼 문구
    val positiveLabel: String
        get() = when (category) {
            ReportCategory.DANGER -> "아직 위험해요"
            ReportCategory.INCONVENIENCE -> "아직 불편해요"
            ReportCategory.DISCOVERY -> "지금도 있어요"
        }
    //  카테고리별 부정 버튼 문구
    val negativeLabel: String
        get() = when (category) {
            ReportCategory.DANGER -> "이제 괜찮아요"
            ReportCategory.INCONVENIENCE -> "해결됐어요"
            ReportCategory.DISCOVERY -> "이제 없어요"
        }

    //  몇일 전 등록한 제보인지 계산하는 로직
    val timeAgo: String
        get() {
            val diffMillis = System.currentTimeMillis() - registrationDate
            val diffMinutes = TimeUnit.MILLISECONDS.toMinutes(diffMillis)
            val diffHours = TimeUnit.MILLISECONDS.toHours(diffMillis)
            val diffDays = TimeUnit.MILLISECONDS.toDays(diffMillis)

            return when {
                diffMinutes < 1 -> "방금 전"
                diffMinutes < 60 -> "${diffMinutes}분 전"
                diffHours < 24 -> "${diffHours}시간 전"
                diffDays < 7 -> "${diffDays}일 전"
                else -> "${diffDays / 7}주 전" // 혹은 날짜 형식으로 표시
            }
        }
}


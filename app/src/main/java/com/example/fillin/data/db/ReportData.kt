package com.example.fillin.data.db

import com.google.firebase.Timestamp

// 사용자가 제보를 할 때마다 새로운 객체를 만들어야 하므로 data class를 사용
// val을 사용하여 한 번 생성된 제보 데이터가 전송 중에 변하지 않도록 보장하여 버그를 방지합니다.
data class ReportData(
    val category: String = "",
    val title: String = "",
    val location: String = "",
    val imageUrl: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val timestamp: Timestamp = Timestamp.now(),
    val userId: String = "guest_user",
    val positiveFeedbackCount: Int = 0,
    val negativeFeedbackCount: Int = 0,
    /** 부정 피드백이 발생한 시점 목록 (최근 7일 내 3건 이상이면 EXPIRING) */
    val negativeFeedbackTimestamps: List<Long>? = null,
    val viewCount: Int = 0,
    val likedByUserIds: List<String>? = null,
    val positive70SustainedSinceMillis: Long? = null,
    val positive40to60SustainedSinceMillis: Long? = null,
    /** 피드백 비율 조건(긍정≤30% 또는 부정≥70%) 만족 시점, 7일 유지 시 EXPIRING */
    val feedbackConditionMetAtMillis: Long? = null,
    /** EXPIRING 전환 시점, 3일 후 EXPIRED */
    val expiringAtMillis: Long? = null
)

package com.example.fillin.domain.model

import android.net.Uri

enum class ReportType { DANGER, INCONVENIENCE, DISCOVERY }

enum class ReportStatus { ACTIVE, EXPIRING, EXPIRED } // 등록된 제보 / 사라질 예정 / 사라진 제보

/**
 * 제보 등록자 정보
 */
data class ReporterInfo(
    val userId: Long,
    val nickname: String,
    val profileImageResId: Int? = null,
    val profileImageUrl: String? = null
)

data class Report(
    val id: Long,
    val documentId: String? = null,  // Firestore 문서 ID (피드백 업데이트용)
    val title: String,      // 행복길 2129-11
    val meta: String,       // 가는길 255m
    val type: ReportType,
    val viewCount: Int,
    val status: ReportStatus = ReportStatus.ACTIVE,
    val isSaved: Boolean = false,

    // 지금은 리소스 이미지로 개발하고, 나중엔 url로 확장 가능
    val imageResId: Int? = null,
    val imageUrl: String? = null,
    val imageUri: Uri? = null,  // 로컬 이미지 (API 등록 직후 배너 표시용)

    val createdAtMillis: Long = System.currentTimeMillis(),
    
    // 피드백 데이터 (카테고리별: 위험=아직위험해요/이제괜찮아요, 불편=아직불편해요/해결됐어요, 발견=지금도있어요/이제없어요)
    val positiveFeedbackCount: Int = 0,  // 긍정 피드백 수
    val negativeFeedbackCount: Int = 0,  // 부정 피드백 수
    /** 부정 피드백이 발생한 시점 목록 (최근 7일 내 3건 이상이면 EXPIRING) */
    val negativeFeedbackTimestamps: List<Long> = emptyList(),
    
    // 유효성 상태 3일 유지 추적: 긍정 70% 이상 / 40~60% 구간 진입 시점
    val positive70SustainedSinceMillis: Long? = null,
    val positive40to60SustainedSinceMillis: Long? = null,
    
    // 피드백 비율이 조건을 만족한 시점 (7일 이상 지속 추적용, EXPIRING 전환)
    val feedbackConditionMetAtMillis: Long? = null,
    
    // EXPIRING 상태로 변경된 시점 (3일 후 EXPIRED로 변경)
    val expiringAtMillis: Long? = null,
    
    // 현재 사용자가 등록한 제보인지 여부
    val isUserOwned: Boolean = false,
    
    // 제보 등록자 정보
    val reporterInfo: ReporterInfo? = null
)

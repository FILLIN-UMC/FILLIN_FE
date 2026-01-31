package com.example.fillin.data

import com.example.fillin.domain.model.Report
import com.example.fillin.domain.model.ReportStatus

/**
 * 제보 상태를 관리하는 유틸리티 객체
 * 피드백 데이터를 기반으로 제보 상태를 업데이트합니다.
 */
object ReportStatusManager {
    
    /**
     * 피드백 비율을 계산합니다.
     * @return Pair<긍정 비율(0.0~1.0), 부정 비율(0.0~1.0)>
     */
    fun calculateFeedbackRatio(report: Report): Pair<Double, Double> {
        val totalFeedback = report.positiveFeedbackCount + report.negativeFeedbackCount
        if (totalFeedback == 0) {
            return 0.0 to 0.0
        }
        val positiveRatio = report.positiveFeedbackCount.toDouble() / totalFeedback
        val negativeRatio = report.negativeFeedbackCount.toDouble() / totalFeedback
        return positiveRatio to negativeRatio
    }
    
    /**
     * 피드백 비율이 사라질 예정 조건을 만족하는지 확인합니다.
     * - 긍정 의견이 0%~30% 7일 이상 유지
     * - 부정 의견이 70%~100% 7일 이상 유지
     */
    fun shouldBeExpiring(report: Report, currentTimeMillis: Long): Boolean {
        val (positiveRatio, negativeRatio) = calculateFeedbackRatio(report)
        val totalFeedback = report.positiveFeedbackCount + report.negativeFeedbackCount
        
        // 피드백이 없으면 조건 불만족
        if (totalFeedback == 0) {
            return false
        }
        
        val sevenDaysInMillis = 7 * 24 * 60 * 60 * 1000L // 7일
        
        // 조건 1: 긍정 의견이 0%~30% 7일 이상 유지
        val positiveConditionMet = positiveRatio <= 0.3
        
        // 조건 2: 부정 의견이 70%~100% 7일 이상 유지
        val negativeConditionMet = negativeRatio >= 0.7
        
        // 조건을 만족한 시점이 기록되어 있는지 확인
        val conditionMetAt = report.feedbackConditionMetAtMillis
        
        if (conditionMetAt != null) {
            // 조건을 만족한 지 7일 이상 지났는지 확인
            val daysSinceConditionMet = currentTimeMillis - conditionMetAt
            if (daysSinceConditionMet >= sevenDaysInMillis) {
                // 조건 1 또는 조건 2를 만족하고 7일 이상 지속
                return positiveConditionMet || negativeConditionMet
            }
        } else {
            // 조건을 처음 만족한 경우, 현재 시점을 기록해야 함
            // (이 함수는 조건 만족 여부만 반환하고, 실제 기록은 updateReportStatus에서 수행)
            if (positiveConditionMet || negativeConditionMet) {
                return true // 조건을 만족했지만 아직 기록되지 않음
            }
        }
        
        return false
    }
    
    /**
     * 제보 상태를 업데이트합니다.
     * - 피드백 조건을 만족하면 EXPIRING 상태로 변경
     * - EXPIRING 상태로 변경된 지 3일이 지나면 EXPIRED 상태로 변경
     */
    fun updateReportStatus(report: Report, currentTimeMillis: Long = System.currentTimeMillis()): Report {
        val sevenDaysInMillis = 7 * 24 * 60 * 60 * 1000L // 7일
        val threeDaysInMillis = 3 * 24 * 60 * 60 * 1000L // 3일
        
        val (positiveRatio, negativeRatio) = calculateFeedbackRatio(report)
        val totalFeedback = report.positiveFeedbackCount + report.negativeFeedbackCount
        
        // 피드백이 없으면 상태 변경 없음
        if (totalFeedback == 0) {
            return report
        }
        
        // 조건 1: 긍정 의견이 0%~30%
        val positiveConditionMet = positiveRatio <= 0.3
        // 조건 2: 부정 의견이 70%~100%
        val negativeConditionMet = negativeRatio >= 0.7
        
        val conditionMet = positiveConditionMet || negativeConditionMet
        
        when (report.status) {
            ReportStatus.ACTIVE -> {
                // ACTIVE 상태에서 조건을 만족한 경우
                if (conditionMet) {
                    val conditionMetAt = report.feedbackConditionMetAtMillis
                    
                    if (conditionMetAt == null) {
                        // 조건을 처음 만족한 경우, 현재 시점 기록
                        return report.copy(
                            feedbackConditionMetAtMillis = currentTimeMillis
                        )
                    } else {
                        // 조건을 만족한 지 7일 이상 지난 경우, EXPIRING 상태로 변경
                        val daysSinceConditionMet = currentTimeMillis - conditionMetAt
                        if (daysSinceConditionMet >= sevenDaysInMillis) {
                            return report.copy(
                                status = ReportStatus.EXPIRING,
                                expiringAtMillis = currentTimeMillis
                            )
                        }
                    }
                } else {
                    // 조건을 만족하지 않으면, 조건 만족 시점 초기화
                    if (report.feedbackConditionMetAtMillis != null) {
                        return report.copy(
                            feedbackConditionMetAtMillis = null
                        )
                    }
                }
            }
            
            ReportStatus.EXPIRING -> {
                // EXPIRING 상태에서 3일이 지나면 EXPIRED로 변경
                val expiringAt = report.expiringAtMillis
                if (expiringAt != null) {
                    val daysSinceExpiring = currentTimeMillis - expiringAt
                    if (daysSinceExpiring >= threeDaysInMillis) {
                        return report.copy(
                            status = ReportStatus.EXPIRED
                        )
                    }
                }
                
                // 조건을 더 이상 만족하지 않으면 ACTIVE로 복귀
                if (!conditionMet) {
                    return report.copy(
                        status = ReportStatus.ACTIVE,
                        feedbackConditionMetAtMillis = null,
                        expiringAtMillis = null
                    )
                }
            }
            
            ReportStatus.EXPIRED -> {
                // EXPIRED 상태는 변경하지 않음
            }
        }
        
        return report
    }
    
    /**
     * 제보 리스트의 상태를 일괄 업데이트합니다.
     */
    fun updateReportsStatus(reports: List<Report>, currentTimeMillis: Long = System.currentTimeMillis()): List<Report> {
        return reports.map { updateReportStatus(it, currentTimeMillis) }
    }
    
    /**
     * 유효성 상태 표시를 위한 "지속 시간" 추적을 업데이트합니다.
     * - 긍정 70% 이상: 해당 구간 진입 시점 기록, 벗어나면 초기화
     * - 긍정 40~60%: 해당 구간 진입 시점 기록, 벗어나면 초기화
     */
    fun updateValiditySustainedTimestamps(report: Report, currentTimeMillis: Long = System.currentTimeMillis()): Report {
        val totalFeedback = report.positiveFeedbackCount + report.negativeFeedbackCount
        if (totalFeedback == 0) {
            return report.copy(
                positive70SustainedSinceMillis = null,
                positive40to60SustainedSinceMillis = null
            )
        }
        val positiveRatio = report.positiveFeedbackCount.toDouble() / totalFeedback
        return when {
            positiveRatio >= 0.7 -> report.copy(
                positive70SustainedSinceMillis = report.positive70SustainedSinceMillis ?: currentTimeMillis,
                positive40to60SustainedSinceMillis = null
            )
            positiveRatio >= 0.4 && positiveRatio <= 0.6 -> report.copy(
                positive70SustainedSinceMillis = null,
                positive40to60SustainedSinceMillis = report.positive40to60SustainedSinceMillis ?: currentTimeMillis
            )
            else -> report.copy(
                positive70SustainedSinceMillis = null,
                positive40to60SustainedSinceMillis = null
            )
        }
    }
}

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
    
    private const val SEVEN_DAYS_IN_MILLIS = 7 * 24 * 60 * 60 * 1000L
    private const val THREE_DAYS_IN_MILLIS = 3 * 24 * 60 * 60 * 1000L

    /**
     * 피드백 비율이 사라질 예정(EXPIRING) 조건을 만족하는지 확인합니다.
     * - 긍정 의견 0%~30% 7일 이상 유지
     * - 부정 의견 70%~100% 7일 이상 유지
     */
    fun shouldBeExpiring(report: Report, currentTimeMillis: Long): Boolean {
        val (positiveRatio, negativeRatio) = calculateFeedbackRatio(report)
        val totalFeedback = report.positiveFeedbackCount + report.negativeFeedbackCount
        if (totalFeedback == 0) return false

        val positiveConditionMet = positiveRatio <= 0.3
        val negativeConditionMet = negativeRatio >= 0.7
        val conditionMet = positiveConditionMet || negativeConditionMet

        val conditionMetAt = report.feedbackConditionMetAtMillis
        if (conditionMetAt != null && conditionMet) {
            val daysSinceConditionMet = currentTimeMillis - conditionMetAt
            return daysSinceConditionMet >= SEVEN_DAYS_IN_MILLIS
        }
        return false
    }

    /**
     * 제보 상태를 업데이트합니다.
     * - 긍정 0%~30% 또는 부정 70%~100%가 7일 이상 유지 → EXPIRING (사라질 예정)
     * - EXPIRING 상태가 된 지 3일 후 → EXPIRED (사라진 제보)
     * - 조건을 더 이상 만족하지 않으면 ACTIVE로 복귀
     */
    fun updateReportStatus(report: Report, currentTimeMillis: Long = System.currentTimeMillis()): Report {
        val (positiveRatio, negativeRatio) = calculateFeedbackRatio(report)
        val totalFeedback = report.positiveFeedbackCount + report.negativeFeedbackCount

        if (totalFeedback == 0) {
            return if (report.feedbackConditionMetAtMillis != null) {
                report.copy(feedbackConditionMetAtMillis = null)
            } else report
        }

        val positiveConditionMet = positiveRatio <= 0.3
        val negativeConditionMet = negativeRatio >= 0.7
        val conditionMet = positiveConditionMet || negativeConditionMet

        when (report.status) {
            ReportStatus.ACTIVE -> {
                if (conditionMet) {
                    val conditionMetAt = report.feedbackConditionMetAtMillis
                    return if (conditionMetAt == null) {
                        report.copy(feedbackConditionMetAtMillis = currentTimeMillis)
                    } else {
                        val daysSinceConditionMet = currentTimeMillis - conditionMetAt
                        if (daysSinceConditionMet >= SEVEN_DAYS_IN_MILLIS) {
                            report.copy(
                                status = ReportStatus.EXPIRING,
                                expiringAtMillis = currentTimeMillis
                            )
                        } else report
                    }
                } else {
                    return if (report.feedbackConditionMetAtMillis != null) {
                        report.copy(feedbackConditionMetAtMillis = null)
                    } else report
                }
            }

            ReportStatus.EXPIRING -> {
                val expiringAt = report.expiringAtMillis
                if (expiringAt != null) {
                    val daysSinceExpiring = currentTimeMillis - expiringAt
                    if (daysSinceExpiring >= THREE_DAYS_IN_MILLIS) {
                        return report.copy(status = ReportStatus.EXPIRED)
                    }
                }
                if (!conditionMet) {
                    return report.copy(
                        status = ReportStatus.ACTIVE,
                        feedbackConditionMetAtMillis = null,
                        expiringAtMillis = null
                    )
                }
            }

            ReportStatus.EXPIRED -> { /* 변경 없음 */ }
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

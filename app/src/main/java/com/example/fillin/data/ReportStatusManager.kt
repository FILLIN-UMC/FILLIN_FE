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
    
    /** 최근 7일 내 부정 피드백 건수 (EXPIRING 조건: 3건 이상) */
    private const val EXPIRING_NEGATIVE_COUNT_THRESHOLD = 3
    private const val SEVEN_DAYS_IN_MILLIS = 7 * 24 * 60 * 60 * 1000L
    private const val THREE_DAYS_IN_MILLIS = 3 * 24 * 60 * 60 * 1000L

    /**
     * 최근 7일 동안 부정 의견이 3건 이상인지 확인합니다.
     * (사라질 예정 EXPIRING 조건)
     */
    fun countNegativeInLast7Days(report: Report, currentTimeMillis: Long = System.currentTimeMillis()): Int {
        val cutoff = currentTimeMillis - SEVEN_DAYS_IN_MILLIS
        return report.negativeFeedbackTimestamps.count { it >= cutoff }
    }

    /**
     * 피드백이 사라질 예정(EXPIRING) 조건을 만족하는지 확인합니다.
     * - 최근 7일 동안 부정 의견이 3건 이상
     */
    fun shouldBeExpiring(report: Report, currentTimeMillis: Long = System.currentTimeMillis()): Boolean {
        return countNegativeInLast7Days(report, currentTimeMillis) >= EXPIRING_NEGATIVE_COUNT_THRESHOLD
    }

    /**
     * 제보 상태를 업데이트합니다.
     * - 최근 7일 동안 부정 의견 3건 이상 → EXPIRING (사라질 예정, 알림 표시)
     * - EXPIRING 상태가 된 지 3일 후 → EXPIRED (사라진 제보)
     * - EXPIRING인데 최근 7일 부정이 3건 미만이면 ACTIVE로 복귀
     */
    fun updateReportStatus(report: Report, currentTimeMillis: Long = System.currentTimeMillis()): Report {
        val conditionMet = shouldBeExpiring(report, currentTimeMillis)

        when (report.status) {
            ReportStatus.ACTIVE -> {
                if (conditionMet) {
                    return report.copy(
                        status = ReportStatus.EXPIRING,
                        expiringAtMillis = currentTimeMillis
                    )
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

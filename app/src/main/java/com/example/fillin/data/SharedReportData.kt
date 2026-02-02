package com.example.fillin.data

import android.content.Context
import android.content.SharedPreferences
import com.example.fillin.feature.home.ReportWithLocation
import com.example.fillin.domain.model.Report
import com.example.fillin.domain.model.ReporterInfo
import com.example.fillin.domain.model.ReportStatus
import com.example.fillin.data.ReportStatusManager

/**
 * 앱 전체에서 공유되는 제보 데이터를 관리하는 싱글톤 객체
 */
object SharedReportData {
    private var reports: List<ReportWithLocation> = emptyList()
    private const val PREFS_NAME = "fillin_report_feedback"
    
    private const val KEY_SAMPLE_DATA_MIGRATED = "sample_data_migrated"
    
    fun isSampleDataMigrated(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_SAMPLE_DATA_MIGRATED, false)
    }
    
    fun setSampleDataMigrated(context: Context, value: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_SAMPLE_DATA_MIGRATED, value)
            .apply()
    }
    
    /**
     * 제보 데이터를 설정합니다.
     */
    fun setReports(reports: List<ReportWithLocation>) {
        this.reports = reports
    }
    
    /**
     * 저장된 제보 데이터를 가져옵니다.
     */
    fun getReports(): List<ReportWithLocation> {
        return reports
    }
    
    /**
     * 사용자가 작성한 제보만 필터링하여 반환합니다.
     * isUserOwned == true인 제보만 반환합니다.
     * @param context 제공 시 완전 삭제한 제보(userPermanentlyDeletedIds) 제외
     */
    fun getUserReports(context: Context? = null): List<ReportWithLocation> {
        val userReports = reports.filter { it.report.isUserOwned }
        return if (context != null) {
            val permanentlyDeleted = loadUserPermanentlyDeletedIds(context)
            userReports.filter { it.report.id !in permanentlyDeleted }
        } else userReports
    }

    /** 메모리에서 제보 제거 (사라진 제보 탭에서 완전 삭제 시 호출) */
    fun removeReport(reportId: Long) {
        reports = reports.filter { it.report.id != reportId }
    }
    
    /**
     * 현재 로그인된 사용자 정보를 반환합니다.
     */
    fun getCurrentUser(): ReporterInfo {
        return SampleReportData.currentUser
    }
    
    /**
     * SharedPreferences에서 사용자 좋아요 상태를 로드합니다.
     */
    fun loadUserLikeStates(context: Context): Map<Long, Boolean> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val allKeys = prefs.all.keys
        val likeStates = mutableMapOf<Long, Boolean>()
        
        allKeys.forEach { key ->
            if (key.startsWith("user_like_")) {
                val reportId = key.removePrefix("user_like_").toLongOrNull()
                if (reportId != null) {
                    val isLiked = prefs.getBoolean(key, false)
                    likeStates[reportId] = isLiked
                }
            }
        }
        
        return likeStates
    }
    
    /**
     * 사용자 좋아요 상태를 SharedPreferences에 저장합니다.
     */
    fun saveUserLikeState(context: Context, reportId: Long, isLiked: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean("user_like_$reportId", isLiked).apply()
    }
    
    /**
     * SharedPreferences에서 사용자 피드백 선택 상태를 로드합니다.
     */
    fun loadUserFeedbackSelections(context: Context): Map<Long, String?> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val allKeys = prefs.all.keys
        val selections = mutableMapOf<Long, String?>()
        
        allKeys.forEach { key ->
            if (key.startsWith("user_feedback_")) {
                val reportId = key.removePrefix("user_feedback_").toLongOrNull()
                if (reportId != null) {
                    val selection = prefs.getString(key, null)
                    selections[reportId] = selection
                }
            }
        }
        
        return selections
    }
    
    /**
     * 사용자 피드백 선택 상태를 SharedPreferences에 저장합니다.
     */
    fun saveUserFeedbackSelection(context: Context, reportId: Long, selection: String?) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (selection == null) {
            prefs.edit().remove("user_feedback_$reportId").apply()
        } else {
            prefs.edit().putString("user_feedback_$reportId", selection).apply()
        }
    }
    
    /**
     * SharedPreferences에서 피드백 데이터를 로드하여 제보 데이터에 적용합니다.
     * 유효성 지속 시점(positive70, positive40to60)도 로드 후 업데이트 로직 적용합니다.
     */
    fun loadFeedbackFromPreferences(context: Context, reports: List<ReportWithLocation>): List<ReportWithLocation> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return reports.map { reportWithLocation ->
            val reportId = reportWithLocation.report.id
            val positiveCount = prefs.getInt("report_${reportId}_positive", reportWithLocation.report.positiveFeedbackCount)
            val negativeCount = prefs.getInt("report_${reportId}_negative", reportWithLocation.report.negativeFeedbackCount)
            val positive70Since = prefs.getLong("report_${reportId}_positive70_since", -1L).takeIf { it > 0 }
            val positive40to60Since = prefs.getLong("report_${reportId}_positive40to60_since", -1L).takeIf { it > 0 }
            
            val negativeTimestamps = prefs.getString("report_${reportId}_negative_timestamps", null)
                ?.split(",")
                ?.mapNotNull { it.toLongOrNull() }
                ?.filter { it > 0 }
                ?: reportWithLocation.report.negativeFeedbackTimestamps
            val feedbackConditionMet = prefs.getLong("report_${reportId}_feedback_condition_met", -1L).takeIf { it > 0 }
            val expiringAt = prefs.getLong("report_${reportId}_expiring_at", -1L).takeIf { it > 0 }

            var report = reportWithLocation.report.copy(
                positiveFeedbackCount = positiveCount,
                negativeFeedbackCount = negativeCount,
                negativeFeedbackTimestamps = negativeTimestamps,
                positive70SustainedSinceMillis = positive70Since,
                positive40to60SustainedSinceMillis = positive40to60Since,
                feedbackConditionMetAtMillis = feedbackConditionMet,
                expiringAtMillis = expiringAt
            )
            report = ReportStatusManager.updateValiditySustainedTimestamps(report)
            report = ReportStatusManager.updateReportStatus(report)
            // 나의 제보에서 사용자가 삭제한 제보는 EXPIRED로 강제
            if (reportId in loadUserDeletedFromRegisteredIds(context)) {
                report = report.copy(status = ReportStatus.EXPIRED)
            }
            // 로드 시 새로 설정된 지속 시점이 있으면 저장 (다음 로드 시 3일 누적 가능)
            if (report.positive70SustainedSinceMillis != positive70Since || report.positive40to60SustainedSinceMillis != positive40to60Since
                || report.feedbackConditionMetAtMillis != feedbackConditionMet || report.expiringAtMillis != expiringAt) {
                saveFeedbackToPreferences(
                    context, reportId, positiveCount, negativeCount,
                    report.positive70SustainedSinceMillis, report.positive40to60SustainedSinceMillis,
                    negativeTimestamps, report.feedbackConditionMetAtMillis, report.expiringAtMillis
                )
            }
            reportWithLocation.copy(report = report)
        }
    }
    
    /**
     * 특정 제보의 피드백 카운트, 부정 피드백 시점 목록, 유효성 지속 시점, EXPIRING 조건 시점을 SharedPreferences에 저장합니다.
     */
    fun saveFeedbackToPreferences(
        context: Context,
        reportId: Long,
        positiveCount: Int,
        negativeCount: Int,
        positive70SustainedSinceMillis: Long? = null,
        positive40to60SustainedSinceMillis: Long? = null,
        negativeFeedbackTimestamps: List<Long>? = null,
        feedbackConditionMetAtMillis: Long? = null,
        expiringAtMillis: Long? = null
    ) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
            .putInt("report_${reportId}_positive", positiveCount)
            .putInt("report_${reportId}_negative", negativeCount)
        positive70SustainedSinceMillis?.let { editor.putLong("report_${reportId}_positive70_since", it) }
        positive40to60SustainedSinceMillis?.let { editor.putLong("report_${reportId}_positive40to60_since", it) }
        if (feedbackConditionMetAtMillis != null) {
            editor.putLong("report_${reportId}_feedback_condition_met", feedbackConditionMetAtMillis)
        } else {
            editor.remove("report_${reportId}_feedback_condition_met")
        }
        if (expiringAtMillis != null) {
            editor.putLong("report_${reportId}_expiring_at", expiringAtMillis)
        } else {
            editor.remove("report_${reportId}_expiring_at")
        }
        if (negativeFeedbackTimestamps != null) {
            editor.putString("report_${reportId}_negative_timestamps", negativeFeedbackTimestamps.joinToString(","))
        }
        editor.apply()
    }
    
    /**
     * 특정 제보의 긍정 피드백 수를 증가시킵니다.
     */
    fun incrementPositiveFeedback(reportId: Long) {
        reports = reports.map { reportWithLocation ->
            if (reportWithLocation.report.id == reportId) {
                val updatedReport = reportWithLocation.report.copy(
                    positiveFeedbackCount = reportWithLocation.report.positiveFeedbackCount + 1
                )
                reportWithLocation.copy(report = updatedReport)
            } else {
                reportWithLocation
            }
        }
    }
    
    /**
     * 특정 제보의 부정 피드백 수를 증가시킵니다.
     */
    fun incrementNegativeFeedback(reportId: Long) {
        reports = reports.map { reportWithLocation ->
            if (reportWithLocation.report.id == reportId) {
                val updatedReport = reportWithLocation.report.copy(
                    negativeFeedbackCount = reportWithLocation.report.negativeFeedbackCount + 1
                )
                reportWithLocation.copy(report = updatedReport)
            } else {
                reportWithLocation
            }
        }
    }
    
    /**
     * 사용자의 총 제보 갯수를 반환합니다.
     * isUserOwned == true인 제보만 카운트합니다.
     */
    fun getTotalReportCount(): Int {
        return reports.count { it.report.isUserOwned }
    }
    
    /**
     * 제보 갯수에 따른 뱃지 이름을 반환합니다.
     * - 루키: 0~9개
     * - 베테랑: 10~29개
     * - 마스터: 30개 이상
     * isUserOwned == true인 제보만 카운트합니다.
     */
    fun getBadgeName(): String {
        val totalReports = reports.count { it.report.isUserOwned }
        return when {
            totalReports >= 30 -> "마스터"
            totalReports >= 10 -> "베테랑"
            else -> "루키"
        }
    }
    
    /**
     * 제보 타입별 통계를 반환합니다.
     * isUserOwned == true인 제보만 카운트합니다.
     */
    fun getReportStats(): ReportStats {
        val userReports = reports.filter { it.report.isUserOwned }
        val dangerCount = userReports.count { it.report.type == com.example.fillin.domain.model.ReportType.DANGER }
        val inconvenienceCount = userReports.count { it.report.type == com.example.fillin.domain.model.ReportType.INCONVENIENCE }
        val discoveryCount = userReports.count { it.report.type == com.example.fillin.domain.model.ReportType.DISCOVERY }
        return ReportStats(
            totalCount = userReports.size,
            dangerCount = dangerCount,
            inconvenienceCount = inconvenienceCount,
            discoveryCount = discoveryCount
        )
    }
    
    private const val KEY_EXPIRING_ALERT_DISMISSED_DAYS = "expiring_alert_dismissed_days_left"

    /** 사라질 제보 알림에서 X로 숨긴 daysLeft 목록 (순차 표시: 3일→2일→1일) */
    fun loadExpiringAlertDismissedDaysLeft(context: Context): Set<Int> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val str = prefs.getString(KEY_EXPIRING_ALERT_DISMISSED_DAYS, null) ?: return emptySet()
        return str.split(",").mapNotNull { it.toIntOrNull() }.toSet()
    }

    /** daysLeft 그룹을 X로 숨김 처리 (다음 알림 표시) */
    fun addExpiringAlertDismissedDaysLeft(context: Context, daysLeft: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val current = loadExpiringAlertDismissedDaysLeft(context) + daysLeft
        prefs.edit().putString(KEY_EXPIRING_ALERT_DISMISSED_DAYS, current.joinToString(",")).apply()
    }

    /** 사라질 제보 알림 상태 초기화 (EXPIRING 제보 없을 때) */
    fun clearExpiringAlertState(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .remove(KEY_EXPIRING_ALERT_DISMISSED_DAYS)
            .apply()
    }

    private const val KEY_USER_DELETED_FROM_REGISTERED = "user_deleted_from_registered_ids"
    private const val KEY_USER_PERMANENTLY_DELETED = "user_permanently_deleted_ids"

    /** 등록된 제보 탭에서 사용자가 삭제(사라진 제보로 이동)한 제보 ID 목록 */
    fun loadUserDeletedFromRegisteredIds(context: Context): Set<Long> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val str = prefs.getString(KEY_USER_DELETED_FROM_REGISTERED, null) ?: return emptySet()
        return str.split(",").mapNotNull { it.toLongOrNull() }.toSet()
    }

    /** 등록된 제보에서 삭제 시 사라진 제보로 이동 (저장) */
    fun addUserDeletedFromRegisteredId(context: Context, reportId: Long) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val current = loadUserDeletedFromRegisteredIds(context) + reportId
        prefs.edit().putString(KEY_USER_DELETED_FROM_REGISTERED, current.joinToString(",")).apply()
        setReportStatusToExpired(reportId)
    }

    /** 메모리 내 제보 상태를 EXPIRED로 변경 (삭제 시 즉시 반영) */
    fun setReportStatusToExpired(reportId: Long) {
        reports = reports.map { rwl ->
            if (rwl.report.id == reportId) {
                rwl.copy(report = rwl.report.copy(status = ReportStatus.EXPIRED))
            } else rwl
        }
    }

    /** 사용자가 완전 삭제한 제보 ID 목록 */
    fun loadUserPermanentlyDeletedIds(context: Context): Set<Long> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val str = prefs.getString(KEY_USER_PERMANENTLY_DELETED, null) ?: return emptySet()
        return str.split(",").mapNotNull { it.toLongOrNull() }.toSet()
    }

    /** 사라진 제보에서 완전 삭제 시 저장 */
    fun addUserPermanentlyDeletedId(context: Context, reportId: Long) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val current = loadUserPermanentlyDeletedIds(context) + reportId
        prefs.edit().putString(KEY_USER_PERMANENTLY_DELETED, current.joinToString(",")).apply()
    }

    /**
     * 알림 확인 상태를 저장하는 SharedPreferences 이름
     */
    private const val NOTIFICATION_PREFS_NAME = "fillin_notifications"
    
    /**
     * SharedPreferences에서 알림 확인 상태를 로드합니다.
     */
    fun loadNotificationReadStates(context: Context): Set<String> {
        val prefs = context.getSharedPreferences(NOTIFICATION_PREFS_NAME, Context.MODE_PRIVATE)
        val allKeys = prefs.all.keys
        return allKeys.filter { key ->
            prefs.getBoolean(key, false)
        }.toSet()
    }
    
    /**
     * 알림 확인 상태를 SharedPreferences에 저장합니다.
     */
    fun saveNotificationReadState(context: Context, notificationId: String, isRead: Boolean) {
        val prefs = context.getSharedPreferences(NOTIFICATION_PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(notificationId, isRead).apply()
    }
}

/**
 * 제보 타입별 통계 데이터 클래스
 */
data class ReportStats(
    val totalCount: Int,
    val dangerCount: Int,
    val inconvenienceCount: Int,
    val discoveryCount: Int
)

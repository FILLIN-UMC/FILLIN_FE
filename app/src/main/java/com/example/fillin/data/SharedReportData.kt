package com.example.fillin.data

import android.content.Context
import android.content.SharedPreferences
import com.example.fillin.feature.home.ReportWithLocation
import com.example.fillin.domain.model.Report
import com.example.fillin.domain.model.ReporterInfo

/**
 * 앱 전체에서 공유되는 제보 데이터를 관리하는 싱글톤 객체
 */
object SharedReportData {
    private var reports: List<ReportWithLocation> = emptyList()
    private const val PREFS_NAME = "fillin_report_feedback"
    
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
     */
    fun getUserReports(): List<ReportWithLocation> {
        return reports.filter { it.report.isUserOwned }
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
     */
    fun loadFeedbackFromPreferences(context: Context, reports: List<ReportWithLocation>): List<ReportWithLocation> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return reports.map { reportWithLocation ->
            val reportId = reportWithLocation.report.id
            val positiveCount = prefs.getInt("report_${reportId}_positive", reportWithLocation.report.positiveFeedbackCount)
            val negativeCount = prefs.getInt("report_${reportId}_negative", reportWithLocation.report.negativeFeedbackCount)
            
            reportWithLocation.copy(
                report = reportWithLocation.report.copy(
                    positiveFeedbackCount = positiveCount,
                    negativeFeedbackCount = negativeCount
                )
            )
        }
    }
    
    /**
     * 특정 제보의 피드백 카운트를 SharedPreferences에 저장합니다.
     */
    fun saveFeedbackToPreferences(context: Context, reportId: Long, positiveCount: Int, negativeCount: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putInt("report_${reportId}_positive", positiveCount)
            .putInt("report_${reportId}_negative", negativeCount)
            .apply()
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

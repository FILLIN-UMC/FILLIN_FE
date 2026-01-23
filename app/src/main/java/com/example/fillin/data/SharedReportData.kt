package com.example.fillin.data

import com.example.fillin.feature.home.ReportWithLocation

/**
 * 앱 전체에서 공유되는 제보 데이터를 관리하는 싱글톤 객체
 */
object SharedReportData {
    private var reports: List<ReportWithLocation> = emptyList()
    
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
     * 현재는 모든 제보를 사용자의 제보로 간주합니다.
     */
    fun getUserReports(): List<ReportWithLocation> {
        return reports
    }
}

package com.example.fillin.domain.repository

import com.example.fillin.domain.model.Report
import kotlinx.coroutines.flow.Flow

interface ReportRepository {
    fun observeAllReports(): Flow<List<Report>>

    fun observeSavedReports(): Flow<List<Report>>
    fun observeMyActiveReports(): Flow<List<Report>>
    fun observeMyExpiredReports(): Flow<List<Report>>

    suspend fun moveToExpired(reportId: Long)       // 삭제 -> 사라진 제보로 이동
    suspend fun toggleSaved(reportId: Long)         // 저장/해제 (원하면)
}

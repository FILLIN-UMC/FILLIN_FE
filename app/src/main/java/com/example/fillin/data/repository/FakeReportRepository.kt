package com.example.fillin.data.repository

import com.example.fillin.R
import com.example.fillin.domain.model.Report
import com.example.fillin.domain.model.ReportStatus
import com.example.fillin.domain.model.ReportType
import com.example.fillin.domain.repository.ReportRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class FakeReportRepository : ReportRepository {

    private val reports = MutableStateFlow(
        listOf(
            Report(
                id = 1,
                title = "행복길 2129-11",
                meta = "가는길 255m",
                type = ReportType.DANGER,
                viewCount = 5,
                status = ReportStatus.ACTIVE,
                isSaved = true,
                imageResId = R.drawable.ic_report_img
            ),
            Report(
                id = 2,
                title = "행복길 2129-11",
                meta = "가는길 255m",
                type = ReportType.INCONVENIENCE,
                viewCount = 12,
                status = ReportStatus.ACTIVE,
                isSaved = true,
                imageResId = R.drawable.ic_report_img
            ),
            Report(
                id = 3,
                title = "행복길 2129-11",
                meta = "가는길 255m",
                type = ReportType.DISCOVERY,
                viewCount = 2,
                status = ReportStatus.EXPIRED,
                isSaved = false,
                imageResId = R.drawable.ic_report_img
            )
        )
    )

    override fun observeAllReports(): Flow<List<Report>> = reports

    override fun observeSavedReports(): Flow<List<Report>> =
        reports.map { it.filter { r -> r.isSaved } }

    override fun observeMyActiveReports(): Flow<List<Report>> =
        reports.map { it.filter { r -> r.status == ReportStatus.ACTIVE } }

    override fun observeMyExpiredReports(): Flow<List<Report>> =
        reports.map { it.filter { r -> r.status == ReportStatus.EXPIRED } }

    override suspend fun moveToExpired(reportId: Long) {
        reports.update { list ->
            list.map { r ->
                if (r.id == reportId) r.copy(status = ReportStatus.EXPIRED) else r
            }
        }
    }

    override suspend fun toggleSaved(reportId: Long) {
        reports.update { list ->
            list.map { r ->
                if (r.id == reportId) r.copy(isSaved = !r.isSaved) else r
            }
        }
    }
}

package com.example.fillin.data.repository

import com.example.fillin.domain.model.HotReportItem
import com.example.fillin.domain.model.PlaceItem
import com.example.fillin.domain.model.VoteType

data class HotReportResult(
    val reports: List<HotReportItem>,
    val places: List<PlaceItem>
)

interface HotReportRepository {
    suspend fun getHotReportsNear(lat: Double, lon: Double): HotReportResult
    suspend fun vote(reportId: String, type: VoteType)
}

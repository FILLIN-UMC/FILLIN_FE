package com.example.fillin.data.repository

import com.example.fillin.domain.model.HotReportItem
import com.example.fillin.domain.model.PlaceItem
import com.example.fillin.domain.model.VoteType

interface HotReportRepository {
    suspend fun getHotReportsNear(lat: Double, lon: Double): HotReportResult

    // 🌟 [수정] reportId 타입을 String -> Long으로 변경해주세요!
    suspend fun vote(reportId: Long, type: VoteType)
}

data class HotReportResult(
    val reports: List<HotReportItem>,
    val places: List<PlaceItem>
)
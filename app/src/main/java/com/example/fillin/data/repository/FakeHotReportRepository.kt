package com.example.fillin.data.repository

import com.example.fillin.domain.model.HotReportItem
import com.example.fillin.domain.model.PlaceItem
import com.example.fillin.domain.model.VoteType
import kotlinx.coroutines.delay

class FakeHotReportRepository(
    private val radiusMeters: Int = 3000,
    private val maxItems: Int = 20
) : HotReportRepository {

    private val fakeReports = mutableListOf<HotReportItem>()

    init {
        repeat(maxItems) { i ->
            fakeReports.add(
                HotReportItem(
                    id = "hot-$i",
                    title = "인기 제보 ${i + 1}",
                    address = "서울특별시 중구 세종대로 110",
                    tag = listOf("위험", "불편", "발견")[i % 3],
                    imageUrl = null,
                    likeCount = (10..50).random(),
                    distanceMeters = (100..radiusMeters).random(),
                    daysAgo = (0..7).random(),
                    stillDangerCount = (0..5).random(),
                    nowSafeCount = (0..5).random()
                )
            )
        }
    }

    override suspend fun getHotReportsNear(lat: Double, lon: Double): HotReportResult {
        delay(300)
        val center = doubleArrayOf(lat, lon)
        val withDistance = fakeReports.map { report ->
            val dist = (100..radiusMeters).random()
            report.copy(distanceMeters = dist)
        }.sortedBy { it.distanceMeters }.take(maxItems)
        val places = withDistance.map { r ->
            PlaceItem(
                id = r.id,
                name = r.title,
                address = r.address,
                category = r.tag
            )
        }
        return HotReportResult(reports = withDistance, places = places)
    }

    override suspend fun vote(reportId: String, type: VoteType) {
        delay(100)
    }
}

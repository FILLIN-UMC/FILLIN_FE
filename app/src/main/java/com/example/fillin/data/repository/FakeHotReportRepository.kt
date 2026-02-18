package com.example.fillin.data.repository

import com.example.fillin.domain.model.HotReportItem
import com.example.fillin.domain.model.PlaceItem
import com.example.fillin.domain.model.VoteType
import kotlinx.coroutines.delay
import kotlin.random.Random

class FakeHotReportRepository(
    private val radiusMeters: Int = 3000,
    private val maxItems: Int = 20
) : HotReportRepository {

    private val fakeReports = mutableListOf<HotReportItem>()

    init {
        // 서울 시청 근처 기준 좌표
        val baseLat = 37.5665
        val baseLon = 126.9780

        repeat(maxItems) { i ->
            // 랜덤 좌표 생성 (약 1~2km 반경 내)
            val randomLat = baseLat + (Random.nextDouble() - 0.5) * 0.02
            val randomLon = baseLon + (Random.nextDouble() - 0.5) * 0.02

            fakeReports.add(
                HotReportItem(
                    id = i.toLong(), // 🌟 [수정] String -> Long
                    title = "인기 제보 ${i + 1}",
                    address = "서울특별시 중구 세종대로 ${100 + i}",

                    // 🌟 [수정] tag -> category (API 값인 DANGER / CAUTION 등 사용)
                    category = if (i % 2 == 0) "DANGER" else "CAUTION",

                    // 🌟 [추가] 좌표 및 조회수
                    latitude = randomLat,
                    longitude = randomLon,
                    viewCount = (100..5000).random(),

                    imageUrl = null, // null이거나 더미 이미지 URL
                    distanceMeters = (100..radiusMeters).random(),
                    daysAgo = (0..7).random(),
                    stillDangerCount = (0..5).random(),
                    nowSafeCount = (0..5).random()
                )
            )
        }
    }

    override suspend fun getHotReportsNear(lat: Double, lon: Double): HotReportResult {
        delay(300) // 네트워킹 흉내

        // 거리순 정렬 시뮬레이션
        val withDistance = fakeReports.map { report ->
            val dist = (100..radiusMeters).random()
            report.copy(distanceMeters = dist)
        }.sortedBy { it.distanceMeters }.take(maxItems)

        // 지도 표시용 PlaceItem 변환
        val places = withDistance.map { r ->
            PlaceItem(
                id = r.id.toString(), // PlaceItem ID는 보통 String이므로 변환
                name = r.title,
                address = r.address,
                category = if (r.category == "DANGER") "위험" else "발견", // 한글 변환
                x = r.longitude.toString(), // 🌟 좌표 연결
                y = r.latitude.toString()   // 🌟 좌표 연결
            )
        }
        return HotReportResult(reports = withDistance, places = places)
    }

    // 🌟 [수정] String -> Long
    override suspend fun vote(reportId: Long, type: VoteType) {
        delay(100)
        // 실제 로직은 없고 딜레이만 줌
    }
}
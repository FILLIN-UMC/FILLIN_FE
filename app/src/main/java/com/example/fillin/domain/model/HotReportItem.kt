package com.example.fillin.domain.model

data class HotReportItem(
    val id: Long,              // API가 숫자로 주므로 Long (String으로 변환해서 써도 됨)
    val category: String,      // "DANGER" 등
    val title: String,
    val latitude: Double,      // 🌟 핵심: 위도 추가
    val longitude: Double,     // 🌟 핵심: 경도 추가
    val viewCount: Int,        // 🌟 조회수 추가
    val address: String,       // 주소

    // 아래는 앱 내부에서 쓰는 필드라면 유지, API에 없으면 기본값 처리
    val imageUrl: String? = null,
    val distanceMeters: Int = 0,
    val daysAgo: Int = 0,
    val stillDangerCount: Int = 0,
    val nowSafeCount: Int = 0
)

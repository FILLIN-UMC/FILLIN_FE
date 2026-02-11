package com.example.fillin.domain.model

data class HotReportItem(
    val id: String,
    val title: String,
    val address: String,
    val tag: String,           // "위험" | "불편" | "발견"
    val imageUrl: String? = null,
    val likeCount: Int = 0,
    val distanceMeters: Int = 0,
    val daysAgo: Int = 0,
    val stillDangerCount: Int = 0,
    val nowSafeCount: Int = 0
)

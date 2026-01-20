package com.example.fillin.domain.model

enum class ReportType { DANGER, INCONVENIENCE, DISCOVERY }

enum class ReportStatus { ACTIVE, EXPIRED } // 등록된 제보 / 사라진 제보

data class Report(
    val id: Long,
    val title: String,      // 행복길 2129-11
    val meta: String,       // 가는길 255m
    val type: ReportType,
    val viewCount: Int,
    val status: ReportStatus = ReportStatus.ACTIVE,
    val isSaved: Boolean = false,

    // 지금은 리소스 이미지로 개발하고, 나중엔 url로 확장 가능
    val imageResId: Int? = null,
    val imageUrl: String? = null,

    val createdAtMillis: Long = System.currentTimeMillis()
)

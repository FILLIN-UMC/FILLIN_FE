package com.example.fillin.data.model.report

// AI 분석 API(POST /api/reports/analyze)의 결과인 추천 제목과 카테고리를 받아오기 위해 필요.

import com.google.gson.annotations.SerializedName

/** 제보 이미지 AI 분석 응답 */
data class ReportAnalyzeResponse(
    @SerializedName("status") val status: String? = null,
    @SerializedName("data") val data: AnalyzeData? = null
)

data class AnalyzeData(
    @SerializedName("title") val title: String? = null,
    @SerializedName("category") val category: String? = null // DANGER, INCONVENIENCE 등
)
package com.example.fillin.data.model.report

import com.google.gson.annotations.SerializedName

/**
 * 제보 등록 API 요청 DTO
 * Backend ReportCreateRequestDto와 매핑
 */
data class ReportCreateRequest(
    @SerializedName("title") val title: String,
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
    @SerializedName("category") val category: ReportCategory,
    @SerializedName("address") val address: String = ""
)

enum class ReportCategory {
    @SerializedName("DANGER") DANGER,       // 위험
    @SerializedName("INCONVENIENCE") INCONVENIENCE,  // 불편
    @SerializedName("DISCOVERY") DISCOVERY   // 발견
}

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
    @SerializedName("address") val address: String = "",
    // ✨ [추가] 모자이크 처리가 완료된 이미지의 URL을 담는 필드입니다.
    // 백엔드에서 이 URL을 확인하여 제보 이미지로 확정하게 됩니다.
    @SerializedName("reportImageUrl") val reportImageUrl: String? = null
)

enum class ReportCategory {
    @SerializedName("DANGER") DANGER,       // 위험
    @SerializedName("INCONVENIENCE") INCONVENIENCE,  // 불편
    @SerializedName("DISCOVERY") DISCOVERY   // 발견
}

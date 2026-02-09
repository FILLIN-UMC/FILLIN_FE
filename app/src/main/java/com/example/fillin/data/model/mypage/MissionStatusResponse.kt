package com.example.fillin.data.model.mypage

import com.google.gson.annotations.SerializedName

/**
 * 미션 진행도 조회 API 응답
 * GET /api/mypage/missions
 */
data class MissionStatusResponse(
    @SerializedName("status") val status: String? = null,
    @SerializedName("code") val code: String? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("data") val data: List<MissionStatusDto>? = null
)

data class MissionStatusDto(
    @SerializedName("category") val category: String? = null, // DANGER, INCONVENIENCE, DISCOVERY
    @SerializedName("currentCount") val currentCount: Int = 0,
    @SerializedName("targetCount") val targetCount: Int = 0
)

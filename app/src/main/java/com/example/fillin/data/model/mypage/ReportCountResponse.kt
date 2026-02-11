package com.example.fillin.data.model.mypage

import com.google.gson.annotations.SerializedName

data class ReportCountResponse(
    @SerializedName("status") val status: String? = null,
    @SerializedName("code") val code: String? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("data") val data: ReportCountData? = null
)

data class ReportCountData(
    @SerializedName("memberId") val memberId: Long? = null,
    @SerializedName("totalReportCount") val totalReportCount: Int = 0,
    @SerializedName("totalViewCount") val totalViewCount: Int = 0
)

package com.example.fillin.data.model.mypage

import com.google.gson.annotations.SerializedName

data class MyReportListResponse(
    @SerializedName("status") val status: String? = null,
    @SerializedName("code") val code: String? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("data") val data: List<MyReportItem>? = null
)

data class MyReportItem(
    @SerializedName("memberId") val memberId: Long? = null,
    @SerializedName("reportId") val reportId: Long? = null,
    @SerializedName("reportCategory") val reportCategory: String? = null,
    @SerializedName("reportImageUrl") val reportImageUrl: String? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("latitude") val latitude: Double? = null,
    @SerializedName("longitude") val longitude: Double? = null,
    @SerializedName("address") val address: String? = null,
    @SerializedName("viewCount") val viewCount: Int = 0
)

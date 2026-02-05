package com.example.fillin.data.model.report

import com.google.gson.annotations.SerializedName

data class PopularReportListResponse(
    @SerializedName("status") val status: String? = null,
    @SerializedName("code") val code: String? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("data") val data: PopularReportListData? = null
)

data class PopularReportListData(
    @SerializedName("popularReports") val popularReports: List<PopularReportItem>? = null
)

data class PopularReportItem(
    @SerializedName("id") val id: Long? = null,
    @SerializedName("category") val category: String? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("latitude") val latitude: Double? = null,
    @SerializedName("longitude") val longitude: Double? = null,
    @SerializedName("viewCount") val viewCount: Int = 0,
    @SerializedName("address") val address: String? = null
)

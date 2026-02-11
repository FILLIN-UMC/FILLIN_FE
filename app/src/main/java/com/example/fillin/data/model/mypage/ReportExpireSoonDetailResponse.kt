package com.example.fillin.data.model.mypage

import com.google.gson.annotations.SerializedName

data class ReportExpireSoonDetailResponse(
    @SerializedName("status") val status: String? = null,
    @SerializedName("code") val code: String? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("data") val data: List<ReportExpireSoonDetailItem>? = null
)

data class ReportExpireSoonDetailItem(
    @SerializedName("memberId") val memberId: Long? = null,
    @SerializedName("achievement") val achievement: String? = null,
    @SerializedName("profileImageUrl") val profileImageUrl: String? = null,
    @SerializedName("reportId") val reportId: Long? = null,
    @SerializedName("reportCategory") val reportCategory: String? = null,
    @SerializedName("validType") val validType: String? = null,
    @SerializedName("reportImageUrl") val reportImageUrl: String? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("latitude") val latitude: Double? = null,
    @SerializedName("longitude") val longitude: Double? = null,
    @SerializedName("address") val address: String? = null,
    @SerializedName("expireTime") val expireTime: String? = null,
    @SerializedName("viewCount") val viewCount: Int = 0,
    @SerializedName("createAt") val createAt: String? = null,
    @SerializedName("doneCount") val doneCount: Int = 0,
    @SerializedName("nowCount") val nowCount: Int = 0
)

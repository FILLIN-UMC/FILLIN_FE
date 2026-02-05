package com.example.fillin.data.model.mypage

import com.google.gson.annotations.SerializedName

data class ReportExpireSoonResponse(
    @SerializedName("status") val status: String? = null,
    @SerializedName("code") val code: String? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("data") val data: ReportExpireSoonData? = null
)

data class ReportExpireSoonData(
    @SerializedName("memberId") val memberId: Long? = null,
    @SerializedName("listDtos") val listDtos: List<ReportExpireSoonItem>? = null,
    @SerializedName("dangerCount") val dangerCount: Int = 0,
    @SerializedName("inconvenienceCount") val inconvenienceCount: Int = 0,
    @SerializedName("discoveryCount") val discoveryCount: Int = 0
)

data class ReportExpireSoonItem(
    @SerializedName("reportId") val reportId: Long? = null,
    @SerializedName("reportCategory") val reportCategory: String? = null,
    @SerializedName("reportImageUrl") val reportImageUrl: String? = null,
    @SerializedName("expireTime") val expireTime: String? = null
)

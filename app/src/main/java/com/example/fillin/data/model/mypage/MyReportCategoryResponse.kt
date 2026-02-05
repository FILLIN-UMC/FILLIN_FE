package com.example.fillin.data.model.mypage

import com.google.gson.annotations.SerializedName

data class MyReportCategoryResponse(
    @SerializedName("status") val status: String? = null,
    @SerializedName("code") val code: String? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("data") val data: MyReportCategoryData? = null
)

data class MyReportCategoryData(
    @SerializedName("memberId") val memberId: Long? = null,
    @SerializedName("dangerCount") val dangerCount: Int = 0,
    @SerializedName("inconvenienceCount") val inconvenienceCount: Int = 0,
    @SerializedName("discoveryCount") val discoveryCount: Int = 0
)

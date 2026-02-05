package com.example.fillin.data.model.report

import com.google.gson.annotations.SerializedName

/** 제보 API 공통 응답 (feedback, like 등) */
data class ReportApiResponse(
    @SerializedName("status") val status: String? = null,
    @SerializedName("code") val code: String? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("data") val data: Any? = null
)

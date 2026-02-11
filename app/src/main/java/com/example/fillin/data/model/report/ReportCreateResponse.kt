package com.example.fillin.data.model.report

import com.google.gson.annotations.SerializedName

/**
 * 제보 등록 API 응답
 * Backend Response<Long>와 매핑 (data = reportId)
 */
data class ReportCreateResponse(
    @SerializedName("status") val status: String? = null,
    @SerializedName("code") val code: String? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("data") val data: Long? = null  // reportId
)

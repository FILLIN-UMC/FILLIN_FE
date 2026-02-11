package com.example.fillin.data.model.alarm

import com.google.gson.annotations.SerializedName

/**
 * 개별 알림 응답 (GET /api/alarm/list)
 */
data class AlarmResponse(
    @SerializedName("alarmId") val alarmId: Long,
    @SerializedName("alarmType") val alarmType: String, // REPORT, LIKE, LEVEL_UP, EXPIRATION, NOTICE
    @SerializedName("message") val message: String,
    @SerializedName("read") val read: Boolean,
    @SerializedName("referId") val referId: Long? = null,
    @SerializedName("createdAt") val createdAt: String
)

/**
 * 알림 목록 API 응답 래퍼
 */
data class AlarmListResponse(
    @SerializedName("status") val status: String? = null,
    @SerializedName("code") val code: String? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("data") val data: List<AlarmResponse>? = null
)

package com.example.fillin.data.model.mypage

import com.google.gson.annotations.SerializedName

data class NotificationResponse(
    @SerializedName("status") val status: String? = null,
    @SerializedName("code") val code: String? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("data") val data: NotificationData? = null
)

data class NotificationData(
    @SerializedName("memberId") val memberId: Long? = null,
    @SerializedName("reportAlarm") val reportAlarm: Boolean? = null,
    @SerializedName("feedbackAlarm") val feedbackAlarm: Boolean? = null,
    @SerializedName("serviceAlarm") val serviceAlarm: Boolean? = null
)

data class NotificationRequest(
    @SerializedName("reportAlarm") val reportAlarm: Boolean? = null,
    @SerializedName("feedbackAlarm") val feedbackAlarm: Boolean? = null,
    @SerializedName("serviceAlarm") val serviceAlarm: Boolean? = null
)

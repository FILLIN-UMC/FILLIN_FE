package com.example.fillin.data.model.mypage

import com.google.gson.annotations.SerializedName

data class RankResponse(
    @SerializedName("status") val status: String? = null,
    @SerializedName("code") val code: String? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("data") val data: RankData? = null
)

data class RankData(
    @SerializedName("memberId") val memberId: Long? = null,
    @SerializedName("achievement") val achievement: String? = null,
    @SerializedName("boangwan") val boangwan: String? = null,
    @SerializedName("haegyeolsa") val haegyeolsa: String? = null,
    @SerializedName("tamheomga") val tamheomga: String? = null
)

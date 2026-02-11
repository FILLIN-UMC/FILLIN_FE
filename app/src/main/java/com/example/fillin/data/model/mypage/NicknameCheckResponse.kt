package com.example.fillin.data.model.mypage

import com.google.gson.annotations.SerializedName

data class NicknameCheckResponse(
    @SerializedName("status") val status: String? = null,
    @SerializedName("code") val code: String? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("data") val data: String? = null
)

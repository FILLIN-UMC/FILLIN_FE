package com.example.fillin.data.model.mypage

import com.google.gson.annotations.SerializedName

data class ProfileResponse(
    @SerializedName("status") val status: String? = null,
    @SerializedName("code") val code: String? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("data") val data: ProfileData? = null
)

data class ProfileData(
    @SerializedName("memberId") val memberId: Long? = null,
    @SerializedName("nickname") val nickname: String? = null,
    @SerializedName("profileImageUrl") val profileImageUrl: String? = null,
    @SerializedName("ranks") val ranks: List<String>? = null
)

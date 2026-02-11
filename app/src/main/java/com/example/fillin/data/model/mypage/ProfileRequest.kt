package com.example.fillin.data.model.mypage

import com.google.gson.annotations.SerializedName

data class ProfileRequest(
    @SerializedName("nickname") val nickname: String? = null,
    @SerializedName("profileImageUrl") val profileImageUrl: String? = null
)

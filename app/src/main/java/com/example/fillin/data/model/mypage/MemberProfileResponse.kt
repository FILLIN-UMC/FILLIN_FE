package com.example.fillin.data.model.mypage

import com.google.gson.annotations.SerializedName

/**
 * 회원 프로필 조회 API 응답 (writerId로 다른 사용자 닉네임 조회)
 * GET /api/members/{memberId}
 */
data class MemberProfileResponse(
    @SerializedName("status") val status: String? = null,
    @SerializedName("code") val code: String? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("data") val data: MemberProfileData? = null
)

data class MemberProfileData(
    @SerializedName("nickname") val nickname: String? = null,
    @SerializedName("profileImageUrl") val profileImageUrl: String? = null
)

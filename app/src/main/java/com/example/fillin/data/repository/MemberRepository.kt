package com.example.fillin.data.repository

import android.content.Context
import com.example.fillin.data.api.FcmTokenRequest
import com.example.fillin.data.api.MemberApiService
import com.example.fillin.data.api.RetrofitClient

/**
 * 회원 API Repository (FCM 토큰 등록 등)
 */
class MemberRepository(private val context: Context) {

    private val api: MemberApiService = RetrofitClient.getMemberApi(context)

    suspend fun registerFcmToken(fcmToken: String) = runCatching {
        api.registerFcmToken(FcmTokenRequest(fcmToken = fcmToken))
    }
}

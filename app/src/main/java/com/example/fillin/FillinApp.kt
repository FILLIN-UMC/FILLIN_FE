package com.example.fillin

import android.app.Application
import android.util.Log
import com.kakao.sdk.common.KakaoSdk
import com.kakao.sdk.common.util.Utility

class FillinApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // Kakao SDK 초기화 (키: local.properties → BuildConfig)
        val nativeKey = BuildConfig.KAKAO_NATIVE_APP_KEY
        Log.d("KAKAO_NATIVE_KEY", nativeKey)
        Log.d("KAKAO_KEY_HASH", Utility.getKeyHash(this))
        KakaoSdk.init(this, nativeKey)

        // Naver Map SDK는 AndroidManifest.xml의 메타데이터로 자동 초기화됨
    }
}

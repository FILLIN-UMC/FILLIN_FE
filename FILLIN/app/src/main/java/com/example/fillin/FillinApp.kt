package com.example.fillin

import android.app.Application
import android.util.Log
import com.kakao.sdk.common.KakaoSdk
import com.kakao.sdk.common.util.Utility

class FillinApp : Application() {
    override fun onCreate() {
        super.onCreate()

        val nativeKey = getString(R.string.kakao_native_app_key)

        Log.d("KAKAO_NATIVE_KEY", nativeKey)
        Log.d("KAKAO_KEY_HASH", Utility.getKeyHash(this))   // ✅ 추가

        KakaoSdk.init(this, nativeKey)
    }
}

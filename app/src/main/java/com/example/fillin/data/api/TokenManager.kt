package com.example.fillin.data.api

import android.content.Context
import android.content.SharedPreferences

/**
 * 인증 토큰 관리
 * API 명세서의 인증 방식에 맞게 수정 필요
 */
object TokenManager {
    private const val PREFS_NAME = "fillin_api_tokens"
    private const val KEY_ACCESS_TOKEN = "access_token"
    private const val KEY_REFRESH_TOKEN = "refresh_token"
    private const val KEY_LOGIN_TYPE = "login_type" // "KAKAO", "NAVER", "EMAIL" 등

    private fun getPrefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun saveAccessToken(context: Context, token: String) {
        getPrefs(context).edit().putString(KEY_ACCESS_TOKEN, token).apply()
    }

    fun getAccessToken(context: Context): String? =
        getPrefs(context).getString(KEY_ACCESS_TOKEN, null)

    fun saveRefreshToken(context: Context, token: String) {
        getPrefs(context).edit().putString(KEY_REFRESH_TOKEN, token).apply()
    }

    fun getRefreshToken(context: Context): String? =
        getPrefs(context).getString(KEY_REFRESH_TOKEN, null)

    fun saveLoginType(context: Context, loginType: String) {
        getPrefs(context).edit().putString(KEY_LOGIN_TYPE, loginType).apply()
    }

    fun getLoginType(context: Context): String? =
        getPrefs(context).getString(KEY_LOGIN_TYPE, null)

    fun clearTokens(context: Context) {
        getPrefs(context).edit()
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_REFRESH_TOKEN)
            .remove(KEY_LOGIN_TYPE)
            .apply()
    }

    fun isLoggedIn(context: Context): Boolean = getAccessToken(context) != null
}

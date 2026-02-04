package com.example.fillin.data.api

import android.content.Context
import android.content.SharedPreferences

/**
 * 인증 토큰 관리
 * - accessToken: API 인증용 (Bearer)
 * - refreshToken: 토큰 재발급, 로그아웃
 * - tempToken: 온보딩 미완료 시 onboarding API 호출용
 */
object TokenManager {
    private const val PREFS_NAME = "fillin_api_tokens"
    private const val KEY_ACCESS_TOKEN = "access_token"
    private const val KEY_REFRESH_TOKEN = "refresh_token"
    private const val KEY_TEMP_TOKEN = "temp_token"
    private const val KEY_LOGIN_TYPE = "login_type"

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

    fun saveTempToken(context: Context, token: String) {
        getPrefs(context).edit().putString(KEY_TEMP_TOKEN, token).apply()
    }

    fun getTempToken(context: Context): String? =
        getPrefs(context).getString(KEY_TEMP_TOKEN, null)

    fun clearTempToken(context: Context) {
        getPrefs(context).edit().remove(KEY_TEMP_TOKEN).apply()
    }

    fun saveLoginType(context: Context, loginType: String) {
        getPrefs(context).edit().putString(KEY_LOGIN_TYPE, loginType).apply()
    }

    fun getLoginType(context: Context): String? =
        getPrefs(context).getString(KEY_LOGIN_TYPE, null)

    fun clearTokens(context: Context) {
        getPrefs(context).edit()
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_REFRESH_TOKEN)
            .remove(KEY_TEMP_TOKEN)
            .remove(KEY_LOGIN_TYPE)
            .apply()
    }

    /** Bearer 토큰: accessToken 우선, 없으면 tempToken (온보딩용) */
    fun getBearerToken(context: Context): String? =
        getAccessToken(context) ?: getTempToken(context)

    fun isLoggedIn(context: Context): Boolean =
        getAccessToken(context) != null || getTempToken(context) != null
}

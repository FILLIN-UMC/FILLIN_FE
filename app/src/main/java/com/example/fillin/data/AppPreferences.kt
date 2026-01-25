package com.example.fillin.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "fillin_prefs",
        Context.MODE_PRIVATE
    )

    // 로그인 및 온보딩 상태 (AuthViewModel/LoginViewModel에서 사용)
    private val _isLoggedInFlow = MutableStateFlow(isLoggedIn())
    val isLoggedInFlow: StateFlow<Boolean> = _isLoggedInFlow.asStateFlow()

    private val _isTermsAcceptedFlow = MutableStateFlow(isTermsAccepted())
    val isTermsAcceptedFlow: StateFlow<Boolean> = _isTermsAcceptedFlow.asStateFlow()

    private val _isPermissionGrantedFlow = MutableStateFlow(isPermissionGranted())
    val isPermissionGrantedFlow: StateFlow<Boolean> = _isPermissionGrantedFlow.asStateFlow()

    private val _nicknameFlow = MutableStateFlow(getNickname())
    val nicknameFlow: StateFlow<String> = _nicknameFlow.asStateFlow()

    private val _profileImageUriFlow = MutableStateFlow(getProfileImageUri())
    val profileImageUriFlow: StateFlow<String?> = _profileImageUriFlow.asStateFlow()

    init {
        // SharedPreferences 변경 감지
        prefs.registerOnSharedPreferenceChangeListener { _, key ->
            when (key) {
                KEY_NICKNAME -> _nicknameFlow.value = getNickname()
                KEY_LOGGED_IN -> _isLoggedInFlow.value = isLoggedIn()
                KEY_TERMS_ACCEPTED -> _isTermsAcceptedFlow.value = isTermsAccepted()
                KEY_PERMISSION_GRANTED -> _isPermissionGrantedFlow.value = isPermissionGranted()
                KEY_PROFILE_IMAGE_URI -> _profileImageUriFlow.value = getProfileImageUri()
            }
        }
    }

    // 로그인 상태
    fun isLoggedIn(): Boolean = prefs.getBoolean(KEY_LOGGED_IN, false)
    fun setLoggedIn(loggedIn: Boolean) {
        prefs.edit().putBoolean(KEY_LOGGED_IN, loggedIn).apply()
        _isLoggedInFlow.value = loggedIn
    }

    // 약관 동의 상태
    fun isTermsAccepted(): Boolean = prefs.getBoolean(KEY_TERMS_ACCEPTED, false)
    fun setTermsAccepted(accepted: Boolean) {
        prefs.edit().putBoolean(KEY_TERMS_ACCEPTED, accepted).apply()
        _isTermsAcceptedFlow.value = accepted
    }

    // 필수 권한 허용 상태
    fun isPermissionGranted(): Boolean = prefs.getBoolean(KEY_PERMISSION_GRANTED, false)
    fun setPermissionGranted(granted: Boolean) {
        prefs.edit().putBoolean(KEY_PERMISSION_GRANTED, granted).apply()
        _isPermissionGrantedFlow.value = granted
    }

    fun getNickname(): String {
        return prefs.getString(KEY_NICKNAME, "방태림") ?: "방태림"
    }

    fun setNickname(nickname: String) {
        prefs.edit().putString(KEY_NICKNAME, nickname).commit()
        // commit()은 동기이므로 즉시 반영됨
        _nicknameFlow.value = nickname
    }

    // 프로필 이미지 URI
    fun getProfileImageUri(): String? {
        return prefs.getString(KEY_PROFILE_IMAGE_URI, null)
    }

    fun setProfileImageUri(uri: String?) {
        if (uri == null) {
            prefs.edit().remove(KEY_PROFILE_IMAGE_URI).commit()
        } else {
            prefs.edit().putString(KEY_PROFILE_IMAGE_URI, uri).commit()
        }
        _profileImageUriFlow.value = uri
    }

    suspend fun clearAll() {
        prefs.edit().clear().apply()
        _isLoggedInFlow.value = false
        _isTermsAcceptedFlow.value = false
        _isPermissionGrantedFlow.value = false
        _nicknameFlow.value = "방태림"
        _profileImageUriFlow.value = null
    }

    suspend fun setLocationHistoryConsent(value: Boolean) {
        prefs.edit().putBoolean(KEY_LOCATION_HISTORY_CONSENT, value).apply()
    }

    suspend fun setMarketingConsent(value: Boolean) {
        prefs.edit().putBoolean(KEY_MARKETING_CONSENT, value).apply()
    }

    companion object {
        private const val KEY_NICKNAME = "nickname"
        private const val KEY_LOGGED_IN = "logged_in"
        private const val KEY_TERMS_ACCEPTED = "terms_accepted"
        private const val KEY_PERMISSION_GRANTED = "permission_granted"
        private const val KEY_LOCATION_HISTORY_CONSENT = "location_history_consent"
        private const val KEY_MARKETING_CONSENT = "marketing_consent"
        private const val KEY_PROFILE_IMAGE_URI = "profile_image_uri"
    }
}

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

    private val _nicknameFlow = MutableStateFlow(getNickname())
    val nicknameFlow: StateFlow<String> = _nicknameFlow.asStateFlow()

    init {
        // SharedPreferences 변경 감지
        prefs.registerOnSharedPreferenceChangeListener { sharedPrefs, key ->
            if (key == KEY_NICKNAME) {
                _nicknameFlow.value = getNickname()
            }
        }
    }

    fun getNickname(): String {
        return prefs.getString(KEY_NICKNAME, "방태림") ?: "방태림"
    }

    fun setNickname(nickname: String) {
        prefs.edit().putString(KEY_NICKNAME, nickname).commit()
        // commit()은 동기이므로 즉시 반영됨
        _nicknameFlow.value = nickname
    }

    companion object {
        private const val KEY_NICKNAME = "nickname"
    }
}

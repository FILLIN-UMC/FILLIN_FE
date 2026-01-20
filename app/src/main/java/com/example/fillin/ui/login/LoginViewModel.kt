package com.example.fillin.ui.login

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fillin.data.AppPreferences
import com.example.fillin.data.auth.GoogleAuthManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

class LoginViewModel(
    private val appPreferences: AppPreferences
) : ViewModel() {

    private val _navEvent = MutableSharedFlow<LoginNavEvent>()
    val navEvent: SharedFlow<LoginNavEvent> = _navEvent

    fun loginWithGoogle(activity: Activity, webClientId: String) {
        viewModelScope.launch {
            val googleAuthManager = GoogleAuthManager(
                activity = activity,
                webClientId = webClientId
            )

            val result = googleAuthManager.signIn()
            if (result.isSuccess) {
                val googleCred = result.getOrNull()!!

                // 필요시 사용
                val idToken = googleCred.idToken
                val displayName = googleCred.displayName

                // ✅ 여기 함수명은 네 AppPreferences에 있는 실제 함수명으로 맞춰야 함
                // (setLoggedIn이 없다면 컴파일 에러남)
                appPreferences.setLoggedIn(true)

                _navEvent.emit(LoginNavEvent.ToTerms)
            } else {
                // 실패 처리
            }
        }
    }
}

sealed class LoginNavEvent {
    object ToTerms : LoginNavEvent()
}

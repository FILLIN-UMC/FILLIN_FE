package com.example.fillin.ui.login

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fillin.data.AppPreferences
import com.kakao.sdk.user.UserApiClient
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

// ✅ Google(Credential Manager) imports
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

sealed class AuthNavEvent {
    data object GoTerms : AuthNavEvent()
    data object GoPermissions : AuthNavEvent()
    data object GoAfterLoginSplash : AuthNavEvent()
    data object Logout : AuthNavEvent()
    data class ShowError(val message: String) : AuthNavEvent()
}

data class AuthUiState(
    val isLoading: Boolean = false
)

class AuthViewModel(
    private val appPreferences: AppPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _navEvents = Channel<AuthNavEvent>(Channel.BUFFERED)
    val navEvents: Flow<AuthNavEvent> = _navEvents.receiveAsFlow()

    /** 카카오 로그인 버튼 클릭 */
    fun loginWithKakao(context: Context, activity: Activity) {
        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)

            // 1) 카카오 로그인 (Talk 우선, 실패 시 Account)
            val loginResult = kakaoLogin(context, activity)
            if (!loginResult.ok) {
                _uiState.value = AuthUiState(isLoading = false)
                _navEvents.send(
                    AuthNavEvent.ShowError("카카오 로그인 실패: ${loginResult.errorMsg ?: "알 수 없는 오류"}")
                )
                return@launch
            }

            // 2) 사용자 정보 조회까지 성공하면 "진짜 로그인 성공"으로 간주
            val meResult = kakaoMe()
            if (!meResult.ok) {
                _uiState.value = AuthUiState(isLoading = false)
                _navEvents.send(
                    AuthNavEvent.ShowError("카카오 사용자 정보 조회 실패: ${meResult.errorMsg ?: "알 수 없는 오류"}")
                )
                return@launch
            }

            // ✅ 로그인 완료 저장
            appPreferences.setLoggedIn(true)

            // 3) 로그인 이후 플로우 분기 (약관 -> 권한 -> 스플래시)
            routeAfterAuth()

            _uiState.value = AuthUiState(isLoading = false)
        }
    }

    /** 약관/권한 상태에 따라 다음 화면 분기 */
    private suspend fun routeAfterAuth() {
        val isTermsAccepted = appPreferences.isTermsAcceptedFlow.first()
        val isPermissionGranted = appPreferences.isPermissionGrantedFlow.first()

        when {
            !isTermsAccepted -> _navEvents.send(AuthNavEvent.GoTerms)
            !isPermissionGranted -> _navEvents.send(AuthNavEvent.GoPermissions)
            else -> _navEvents.send(AuthNavEvent.GoAfterLoginSplash)
        }
    }

    /** KakaoTalk 가능하면 talk, 아니면 account. talk 실패 시 account fallback */
    private suspend fun kakaoLogin(context: Context, activity: Activity): KakaoStepResult {
        return if (UserApiClient.instance.isKakaoTalkLoginAvailable(context)) {
            val talk = loginWithKakaoTalk(activity)
            if (talk.ok) talk else loginWithKakaoAccount(activity)
        } else {
            loginWithKakaoAccount(activity)
        }
    }

    private suspend fun loginWithKakaoTalk(activity: Activity): KakaoStepResult =
        suspendCancellableCoroutine { cont ->
            UserApiClient.instance.loginWithKakaoTalk(activity) { token, error ->
                if (error != null) {
                    Log.e("KAKAO_LOGIN", "loginWithKakaoTalk failed", error)
                    cont.resume(KakaoStepResult(false, error.message))
                } else if (token == null) {
                    Log.e("KAKAO_LOGIN", "loginWithKakaoTalk token is null")
                    cont.resume(KakaoStepResult(false, "token is null"))
                } else {
                    Log.d("KAKAO_LOGIN", "loginWithKakaoTalk success token=${token.accessToken}")
                    cont.resume(KakaoStepResult(true, null))
                }
            }
        }

    private suspend fun loginWithKakaoAccount(activity: Activity): KakaoStepResult =
        suspendCancellableCoroutine { cont ->
            UserApiClient.instance.loginWithKakaoAccount(activity) { token, error ->
                if (error != null) {
                    Log.e("KAKAO_LOGIN", "loginWithKakaoAccount failed", error)
                    cont.resume(KakaoStepResult(false, error.message))
                } else if (token == null) {
                    Log.e("KAKAO_LOGIN", "loginWithKakaoAccount token is null")
                    cont.resume(KakaoStepResult(false, "token is null"))
                } else {
                    Log.d("KAKAO_LOGIN", "loginWithKakaoAccount success token=${token.accessToken}")
                    cont.resume(KakaoStepResult(true, null))
                }
            }
        }

    private suspend fun kakaoMe(): KakaoStepResult =
        suspendCancellableCoroutine { cont ->
            UserApiClient.instance.me { user, error ->
                if (error != null) {
                    Log.e("KAKAO_LOGIN", "me() failed", error)
                    cont.resume(KakaoStepResult(false, error.message))
                } else if (user == null) {
                    Log.e("KAKAO_LOGIN", "me() user is null")
                    cont.resume(KakaoStepResult(false, "user is null"))
                } else {
                    Log.d("KAKAO_LOGIN", "me() success id=${user.id}")
                    cont.resume(KakaoStepResult(true, null))
                }
            }
        }

    // ✅ 경로: app/src/main/java/com/example/fillin/ui/login/AuthViewModel.kt
    // ✅ 구글 로그인 버튼 클릭 (Credential Manager + authorized → 전체 계정 fallback)
    fun loginWithGoogle(activity: Activity, webClientId: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)

            try {
                val idToken = getGoogleIdTokenWithFallback(
                    activity = activity,
                    webClientId = webClientId
                )

                Log.d("GOOGLE_LOGIN", "Google idToken length=${idToken.length}")

                // ✅ 로그인 완료 저장
                appPreferences.setLoggedIn(true)

                // ✅ 카카오와 동일: 약관/권한 상태에 따라 다음 화면 분기
                routeAfterAuth()

            } catch (t: Throwable) {
                Log.e("GOOGLE_LOGIN", "Google login failed", t)
                val msg = t.message ?: "구글 로그인에 실패했어요."
                _navEvents.send(AuthNavEvent.ShowError(msg))
            }

            _uiState.value = AuthUiState(isLoading = false)
        }
    }

    /** Google ID Token 가져오기 (authorized 먼저 → 없으면 전체 계정 fallback) */
    private suspend fun getGoogleIdTokenWithFallback(
        activity: Activity,
        webClientId: String
    ): String {
        val credentialManager = CredentialManager.create(activity)

        suspend fun request(filterByAuthorized: Boolean): String {
            val googleIdOption = GetGoogleIdOption.Builder()
                .setServerClientId(webClientId) // ✅ "웹 클라이언트 ID"
                .setFilterByAuthorizedAccounts(filterByAuthorized)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(
                context = activity,
                request = request
            )

            val googleCred = GoogleIdTokenCredential.createFrom(result.credential.data)
            return googleCred.idToken
        }

        return try {
            // 1) 기존 승인된 계정만 먼저
            request(filterByAuthorized = true)
        } catch (e: NoCredentialException) {
            // 2) 없으면 전체 계정 대상으로 다시
            request(filterByAuthorized = false)
        }
    }

    /** 로그아웃 처리 */
    fun logout() {
        viewModelScope.launch {
            try {
                // 카카오 로그아웃 시도
                UserApiClient.instance.logout { error ->
                    if (error != null) {
                        Log.e("LOGOUT", "Kakao logout failed", error)
                    } else {
                        Log.d("LOGOUT", "Kakao logout success")
                    }
                }
                
                // 로그인 상태 초기화
                appPreferences.setLoggedIn(false)
                
                // 모든 사용자 데이터 초기화
                appPreferences.clearAll()
                
                // 로그인 화면으로 이동
                _navEvents.send(AuthNavEvent.Logout)
            } catch (e: Exception) {
                Log.e("LOGOUT", "Logout error", e)
                // 에러가 발생해도 로그인 상태는 초기화
                appPreferences.setLoggedIn(false)
                appPreferences.clearAll()
                _navEvents.send(AuthNavEvent.Logout)
            }
        }
    }

    private data class KakaoStepResult(
        val ok: Boolean,
        val errorMsg: String?
    )
}

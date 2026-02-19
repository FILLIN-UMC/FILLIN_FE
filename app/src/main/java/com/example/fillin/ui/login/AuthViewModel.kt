package com.example.fillin.ui.login

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fillin.data.AppPreferences
import com.example.fillin.data.model.auth.ApiErrorParser
import com.example.fillin.data.repository.AuthRepository
import com.kakao.sdk.user.UserApiClient
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import retrofit2.HttpException

sealed class AuthNavEvent {
    data object GoTerms : AuthNavEvent()
    data object GoPermissions : AuthNavEvent()
    data object GoAfterLoginSplash : AuthNavEvent()
    data object GoOnboarding : AuthNavEvent()
    data object Logout : AuthNavEvent()
    data class ShowError(val message: String) : AuthNavEvent()
}

data class AuthUiState(
    val isLoading: Boolean = false
)

class AuthViewModel(
    private val appPreferences: AppPreferences,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _navEvents = Channel<AuthNavEvent>(Channel.BUFFERED)
    val navEvents: Flow<AuthNavEvent> = _navEvents.receiveAsFlow()

    /** 카카오 로그인 버튼 클릭 */
    fun loginWithKakao(context: Context, activity: Activity) {
        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)

            val kakaoToken = getKakaoAccessToken(context, activity)
            Log.d("KAKAO_LOGIN", "2. 카카오 토큰 결과: $kakaoToken") // 토큰 확인 로그
            if (kakaoToken == null) {
                _uiState.value = AuthUiState(isLoading = false)
                _navEvents.send(
                    AuthNavEvent.ShowError("카카오 로그인 실패: 토큰을 받지 못했어요.")
                )
                return@launch
            }

            val result = authRepository.kakaoLogin(kakaoToken)
            result.fold(
                onSuccess = { response ->
                    val needOnboarding = response.data?.needOnboarding == true
                    if (needOnboarding) {
                        _navEvents.send(AuthNavEvent.GoOnboarding)
                    } else {
                        appPreferences.setLoggedIn(true)
                        appPreferences.setTermsAccepted(true)
                        appPreferences.setPermissionGranted(true)

                        routeAfterAuth()
                    }
                },
                onFailure = { e ->
                    Log.e("KAKAO_LOGIN", "API failed: ${e.javaClass.simpleName}", e)
                    if (e is HttpException) Log.e("KAKAO_LOGIN", "HTTP ${e.code()}")
                    val msg = when (e) {
                        is HttpException -> when (e.code()) {
                            403 -> ApiErrorParser.getMessage(e, "카카오 로그인 접근이 거부되었어요.")
                            404 -> "카카오 로그인 API를 찾을 수 없어요."
                            else -> ApiErrorParser.getMessage(e, "카카오 로그인에 실패했어요.")
                        }
                        else -> e.message ?: "카카오 로그인에 실패했어요."
                    }
                    _navEvents.send(AuthNavEvent.ShowError(msg))
                }
            )

            _uiState.value = AuthUiState(isLoading = false)
        }
    }

    /** 구글 로그인 버튼 클릭 */
    fun loginWithGoogle(activity: Activity, webClientId: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)

            try {
                val idToken = getGoogleIdTokenWithFallback(
                    activity = activity,
                    webClientId = webClientId
                )
                Log.d("GOOGLE_LOGIN", "Google idToken length=${idToken.length}")
                // 토큰 검증: Logcat에서 복사 후 터미널에서 curl "https://oauth2.googleapis.com/tokeninfo?id_token=토큰" 실행
                if (com.example.fillin.BuildConfig.DEBUG) {
                    Log.d("GOOGLE_LOGIN", "idToken for verification: $idToken")
                }

                if (idToken.isBlank()) {
                    Log.e("GOOGLE_LOGIN", "idToken is empty - cannot proceed with API call")
                    _navEvents.send(AuthNavEvent.ShowError("Google 인증 토큰을 받지 못했어요.\n다시 시도해주세요."))
                    return@launch
                }

                val result = authRepository.googleLogin(idToken)
                result.fold(
                    onSuccess = { response ->
                        val needOnboarding = response.data?.needOnboarding == true
                        if (needOnboarding) {
                            _navEvents.send(AuthNavEvent.GoOnboarding)
                        } else {
                            appPreferences.setLoggedIn(true)
                            appPreferences.setTermsAccepted(true)
                            appPreferences.setPermissionGranted(true)

                            routeAfterAuth()
                        }
                    },
                    onFailure = { e ->
                        Log.e("GOOGLE_LOGIN", "API failed: ${e.javaClass.simpleName}", e)
                        if (e is HttpException) Log.e("GOOGLE_LOGIN", "HTTP ${e.code()}")
                        val msg = when (e) {
                            is HttpException -> when (e.code()) {
                                404 -> "구글 로그인 API를 찾을 수 없어요.\n서버 엔드포인트가 배포되었는지 확인해주세요."
                                403 -> ApiErrorParser.getMessage(e, "구글 로그인 접근이 거부되었어요.")
                                else -> ApiErrorParser.getMessage(e, "구글 로그인에 실패했어요.")
                            }
                            else -> e.message?.takeIf { it.isNotBlank() } ?: "구글 로그인에 실패했어요."
                        }
                        _navEvents.send(AuthNavEvent.ShowError(msg))
                    }
                )
            } catch (t: Throwable) {
                Log.e("GOOGLE_LOGIN", "Google login failed: ${t.javaClass.simpleName}, msg=${t.message}", t)
                val msg = when {
                    t.message?.contains("No credentials available", ignoreCase = true) == true ->
                        "이 기기에 Google 계정이 없거나 로그인을 취소했어요.\n설정에서 Google 계정을 추가한 뒤 다시 시도해주세요."
                    t.message?.contains("Developer console is not set up correctly", ignoreCase = true) == true ->
                        "Google 로그인 설정이 올바르지 않아요.\nGoogle Cloud Console에서 OAuth 클라이언트 ID와 SHA-1 지문을 확인해주세요."
                    t.message?.contains("cancel", ignoreCase = true) == true ->
                        "Google 로그인이 취소되었어요."
                    t.message?.contains("access_denied", ignoreCase = true) == true ->
                        "Google 로그인 접근이 거부되었어요.\n테스트 사용자에 추가되었는지 확인해주세요."
                    t.message?.contains("DeveloperError", ignoreCase = true) == true ||
                    t.message?.contains("DEVELOPER_ERROR", ignoreCase = true) == true ->
                        "앱 설정 오류가 있어요.\n앱이 테스트 사용자에 추가되었는지 확인해주세요."
                    t.message?.contains("invalid_grant", ignoreCase = true) == true ->
                        "Google 계정 인증에 실패했어요.\n테스트 사용자에 이 계정이 추가되었는지 확인해주세요."
                    else -> t.message?.takeIf { it.isNotBlank() } ?: "구글 로그인에 실패했어요."
                }
                _navEvents.send(AuthNavEvent.ShowError(msg))
            }

            _uiState.value = AuthUiState(isLoading = false)
        }
    }

    /** 로그아웃 처리 */
    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            UserApiClient.instance.logout { error ->
                if (error != null) Log.e("LOGOUT", "Kakao logout failed", error)
            }
            appPreferences.setLoggedIn(false)
            appPreferences.clearAll()
            _navEvents.send(AuthNavEvent.Logout)
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

    private suspend fun getKakaoAccessToken(context: Context, activity: Activity): String? {
        val loginResult = if (UserApiClient.instance.isKakaoTalkLoginAvailable(context)) {
            val talk = loginWithKakaoTalk(activity)
            if (talk.accessToken != null) talk else loginWithKakaoAccount(activity)
        } else {
            loginWithKakaoAccount(activity)
        }
        return loginResult.accessToken
    }

    private suspend fun loginWithKakaoTalk(activity: Activity): KakaoTokenResult =
        suspendCancellableCoroutine { cont ->
            UserApiClient.instance.loginWithKakaoTalk(activity) { token, error ->
                if (error != null) {
                    Log.e("KAKAO_LOGIN", "loginWithKakaoTalk failed", error)
                    cont.resume(KakaoTokenResult(null))
                } else if (token == null) {
                    cont.resume(KakaoTokenResult(null))
                } else {
                    cont.resume(KakaoTokenResult(token.accessToken))
                }
            }
        }

    private suspend fun loginWithKakaoAccount(activity: Activity): KakaoTokenResult =
        suspendCancellableCoroutine { cont ->
            UserApiClient.instance.loginWithKakaoAccount(activity) { token, error ->
                if (error != null) {
                    Log.e("KAKAO_LOGIN", "loginWithKakaoAccount failed", error)
                    cont.resume(KakaoTokenResult(null))
                } else if (token == null) {
                    cont.resume(KakaoTokenResult(null))
                } else {
                    cont.resume(KakaoTokenResult(token.accessToken))
                }
            }
        }

    private suspend fun getGoogleIdTokenWithFallback(
        activity: Activity,
        webClientId: String
    ): String {
        val credentialManager = CredentialManager.create(activity)

        suspend fun request(filterByAuthorized: Boolean): String {
            val googleIdOption = GetGoogleIdOption.Builder()
                .setServerClientId(webClientId)
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
            request(filterByAuthorized = true)
        } catch (e: NoCredentialException) {
            request(filterByAuthorized = false)
        }
    }

    private data class KakaoTokenResult(val accessToken: String?)
}

package com.example.fillin.ui.onboarding

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fillin.data.AppPreferences
import com.example.fillin.data.model.auth.ApiErrorParser
import com.example.fillin.data.model.auth.OnboardingRequest
import com.example.fillin.data.repository.AuthRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import com.example.fillin.utils.PermissionUtils

sealed class OnboardingNavEvent {
    data object GoTerms : OnboardingNavEvent()
    data object GoPermissions : OnboardingNavEvent()
    data object GoAfterLoginSplash : OnboardingNavEvent()
    data class ShowError(val message: String) : OnboardingNavEvent()
}

data class OnboardingUiState(
    val isLoading: Boolean = false,
    val nicknameError: String? = null,
    val emailError: String? = null
)

class OnboardingViewModel(
    private val context: Context,
    private val appPreferences: AppPreferences,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    private val _navEvents = Channel<OnboardingNavEvent>(Channel.BUFFERED)
    val navEvents: Flow<OnboardingNavEvent> = _navEvents.receiveAsFlow()

    fun clearNicknameError() {
        _uiState.update { it.copy(nicknameError = null) }
    }

    fun clearEmailError() {
        _uiState.update { it.copy(emailError = null) }
    }

    fun completeOnboarding(
        nickname: String,
        email: String,
        agreeService: Boolean,
        agreeLocationHistory: Boolean,
        agreeMarketing: Boolean
    ) {
        if (nickname.isBlank() || email.isBlank()) {
            _navEvents.trySend(OnboardingNavEvent.ShowError("닉네임과 이메일을 입력해주세요."))
            return
        }
        if (!agreeService) {
            _navEvents.trySend(OnboardingNavEvent.ShowError("[필수] 서비스 이용약관에 동의해주세요."))
            return
        }

        viewModelScope.launch {
            _uiState.value = OnboardingUiState(isLoading = true)

            val agreedIds = mutableListOf<Int>()
            if (agreeService) agreedIds.add(1)
            if (agreeLocationHistory) agreedIds.add(2)
            if (agreeMarketing) agreedIds.add(3)

            val request = OnboardingRequest(
                nickname = nickname.trim(),
                email = email.trim(),
                agreedAgreementIds = agreedIds
            )

            val result = authRepository.completeOnboarding(request)
            result.fold(
                onSuccess = {
                    appPreferences.setTermsAccepted(true)
                    appPreferences.setLocationHistoryConsent(agreeLocationHistory)
                    appPreferences.setMarketingConsent(agreeMarketing)
                    appPreferences.setNickname(nickname.trim())
                    appPreferences.setLoggedIn(true)
                    routeAfterAuth()
                },
                onFailure = { e ->
                    val msg = when (e) {
                        is HttpException -> ApiErrorParser.getMessage(e, "온보딩 완료에 실패했어요.")
                        else -> e.message ?: "온보딩 완료에 실패했어요."
                    }

                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            nicknameError = if (msg.contains("닉네임")) msg else null,
                            emailError = if (msg.contains("이메일")) msg else null
                        )
                    }

                    if (!msg.contains("닉네임") && !msg.contains("이메일")) {
                        _navEvents.send(OnboardingNavEvent.ShowError(msg))
                    }
                }
            )

            _uiState.value = OnboardingUiState(isLoading = false)
        }
    }

    private suspend fun routeAfterAuth() {
        val hasActualPermission = PermissionUtils.hasLocationPermissions(context)

        when {
            !hasActualPermission -> _navEvents.send(OnboardingNavEvent.GoPermissions)
            else -> _navEvents.send(OnboardingNavEvent.GoAfterLoginSplash)
        }
    }
}

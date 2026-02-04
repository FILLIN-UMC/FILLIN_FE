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
import kotlinx.coroutines.launch
import retrofit2.HttpException

sealed class OnboardingNavEvent {
    data object GoTerms : OnboardingNavEvent()
    data object GoPermissions : OnboardingNavEvent()
    data object GoAfterLoginSplash : OnboardingNavEvent()
    data class ShowError(val message: String) : OnboardingNavEvent()
}

data class OnboardingUiState(
    val isLoading: Boolean = false
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
                    _navEvents.send(OnboardingNavEvent.ShowError(msg))
                }
            )

            _uiState.value = OnboardingUiState(isLoading = false)
        }
    }

    private suspend fun routeAfterAuth() {
        val isPermissionGranted = appPreferences.isPermissionGrantedFlow.first()

        when {
            !isPermissionGranted -> _navEvents.send(OnboardingNavEvent.GoPermissions)
            else -> _navEvents.send(OnboardingNavEvent.GoAfterLoginSplash)
        }
    }
}

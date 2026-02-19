package com.example.fillin.ui.onboarding

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.fillin.R
import com.example.fillin.data.AppPreferences
import com.example.fillin.navigation.Screen
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(
    navController: NavController,
    appPreferences: AppPreferences
) {
    val context = LocalContext.current
    val viewModel: OnboardingViewModel = viewModel(
        factory = OnboardingViewModelFactory(context, appPreferences)
    )
    val uiState by viewModel.uiState.collectAsState()

    var step by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        viewModel.navEvents.collectLatest { event ->
            when (event) {
                OnboardingNavEvent.GoPermissions -> {
                    navController.navigate(Screen.Permission.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
                OnboardingNavEvent.GoAfterLoginSplash -> {
                    navController.navigate(Screen.AfterLoginSplash.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
                is OnboardingNavEvent.ShowError -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }
                else -> {}
            }
        }
    }

    OnboardingContent(
        currentStep = step,
        uiState = uiState,
        onStepChange = { step = it },
        onNicknameChange = { viewModel.clearNicknameError() },
        onEmailChange = { viewModel.clearEmailError() },
        isLoading = uiState.isLoading,
        onComplete = { nickname, email, service, location, marketing ->
            viewModel.completeOnboarding(nickname, email, service, location, marketing)
        }
    )
}

@Composable
fun OnboardingContent(
    currentStep: Int,
    uiState: OnboardingUiState,
    onStepChange: (Int) -> Unit,
    onNicknameChange: () -> Unit,
    onEmailChange: () -> Unit,
    isLoading: Boolean,
    onComplete: (String, String, Boolean, Boolean, Boolean) -> Unit
) {
    var nickname by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var agreeService by remember { mutableStateOf(false) }
    var agreeLocationHistory by remember { mutableStateOf(false) }
    var agreeMarketing by remember { mutableStateOf(false) }

    val primaryBlue = Color(0xFF4A90E2)
    val lightGray = Color(0xFFBDBDBD)
    val chevronGray = Color(0xFF9E9E9E)

    val isNothingChecked = !agreeService && !agreeLocationHistory && !agreeMarketing

    val scope = rememberCoroutineScope()
    var isAnimating by remember { mutableStateOf(false) }

    BackHandler(enabled = currentStep > 0) {
        onStepChange(0)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .navigationBarsPadding()
            .padding(horizontal = 24.dp)
    ) {
        Spacer(Modifier.height(100.dp))

        Image(
            painter = painterResource(R.drawable.img_logo_round),
            contentDescription = "FILLIN Logo",
            modifier = Modifier.align(Alignment.CenterHorizontally),
            contentScale = ContentScale.Fit
        )

        Spacer(Modifier.height(20.dp))

        AnimatedContent(
            targetState = currentStep,
            modifier = Modifier.weight(1f),
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "StepContentAnim"
        ) { step ->
            Column(modifier = Modifier.fillMaxSize()) {
                if (step == 0) {
                    Text(
                        text = "지도 서비스를 이용하기 위해 \n동의가 필요해요.",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontSize = 22.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.weight(1f))

                    // ✅ 에러가 났던 OnboardingTermsRow 호출 부분
                    OnboardingTermsRow(
                        checked = agreeService,
                        text = "[필수] 필인 지도 서비스 이용약관",
                        onToggle = { agreeService = !agreeService },
                        primaryBlue = primaryBlue,
                        lightGray = lightGray,
                        chevronGray = chevronGray
                    )
                    Spacer(Modifier.height(10.dp))
                    OnboardingTermsRow(
                        checked = agreeLocationHistory,
                        text = "[선택] 개인정보(이동이력) 수집 및 이용",
                        onToggle = { agreeLocationHistory = !agreeLocationHistory },
                        primaryBlue = primaryBlue,
                        lightGray = lightGray,
                        chevronGray = chevronGray
                    )
                    Spacer(Modifier.height(10.dp))
                    OnboardingTermsRow(
                        checked = agreeMarketing,
                        text = "[선택] 마케팅 정보 수신 동의",
                        onToggle = { agreeMarketing = !agreeMarketing },
                        primaryBlue = primaryBlue,
                        lightGray = lightGray,
                        chevronGray = chevronGray
                    )
                } else {
                    Text(
                        text = "서비스를 이용하기 위해 \n정보를 입력해주세요.",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontSize = 22.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.weight(1f))

                    OutlinedTextField(
                        value = nickname,
                        onValueChange = {
                            nickname = it
                            onNicknameChange()
                        },
                        label = { Text("닉네임") },
                        isError = uiState.nicknameError != null,
                        supportingText = {
                            if (uiState.nicknameError != null) {
                                Text(text = uiState.nicknameError!!, color = Color.Red)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryBlue,
                            unfocusedBorderColor = lightGray,
                            errorBorderColor = Color.Red
                        )
                    )

                    Spacer(Modifier.height(16.dp))

                    OutlinedTextField(
                        value = email,
                        onValueChange = {
                            email = it
                            onEmailChange()
                        },
                        label = { Text("이메일") },
                        isError = uiState.emailError != null,
                        supportingText = {
                            if (uiState.emailError != null) {
                                Text(text = uiState.emailError!!, color = Color.Red)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryBlue,
                            unfocusedBorderColor = lightGray,
                            errorBorderColor = Color.Red
                        )
                    )
                }
            }
        }

        Spacer(Modifier.height(72.dp))

        Button(
            onClick = {
                if (currentStep == 0) {
                    if (isNothingChecked && !isAnimating) {
                        // ✅ '전체 동의하기' 스르륵 로직
                        scope.launch {
                            isAnimating = true

                            agreeService = true
                            delay(500L) // 0.2초 간격

                            agreeLocationHistory = true
                            delay(500L)

                            agreeMarketing = true
                            delay(300L) // 모든 체크가 끝난 걸 보여주기 위해 조금 더 대기

                            onStepChange(1)
                            isAnimating = false
                        }
                    } else if (agreeService && !isAnimating) {
                        onStepChange(1)
                    }
                } else {
                    onComplete(nickname, email, agreeService, agreeLocationHistory, agreeMarketing)
                }
            },
            // ✅ 0단계: 아무것도 안 눌렀을 때(전체동의용) 또는 필수(agreeService)가 눌렸을 때 활성화
            enabled = !isLoading && !isAnimating && (if (currentStep == 0) isNothingChecked || agreeService else nickname.isNotBlank()),
            modifier = Modifier
                .fillMaxWidth()
                .height(53.dp),
            shape = RoundedCornerShape(32.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = primaryBlue,
                disabledContainerColor = primaryBlue.copy(alpha = 0.45f)
            )
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
            } else {
                // ✅ 문구 분기 로직
                val buttonText = when {
                    currentStep == 0 && isNothingChecked -> "전체 동의하기"
                    currentStep == 0 -> "다음"
                    else -> "완료"
                }

                Text(
                    text = buttonText,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp,
                    ),
                    color = Color.White
                )
            }
        }

        Spacer(Modifier.height(18.dp))
    }
}

// ✅ 이 부분이 파일 하단에 꼭 있어야 에러가 나지 않습니다!
@Composable
private fun OnboardingTermsRow(
    checked: Boolean,
    text: String,
    onToggle: () -> Unit,
    primaryBlue: Color,
    lightGray: Color,
    chevronGray: Color
) {
    val interactionSource = remember { MutableInteractionSource() }

    val animatedBgColor by animateColorAsState(
        targetValue = if (checked) primaryBlue else Color.White,
        animationSpec = tween(durationMillis = 500),
        label = "bgColor"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 54.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null
                ) { onToggle() },
            shape = CircleShape,
            color = animatedBgColor,
            border = androidx.compose.foundation.BorderStroke(2.dp, if (checked) primaryBlue else lightGray)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = "check",
                    tint = if (checked) Color.White else lightGray,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Medium,
                color = colorResource(R.color.grey5),
                fontSize = 16.sp
            ),
            modifier = Modifier.weight(1f)
        )

        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "Open",
            tint = chevronGray,
            modifier = Modifier.size(26.dp).clickable { /* 약관 보기 */ }
        )
    }
}

@Preview(showBackground = true, name = "1단계: 약관 동의", device = "spec:width=411dp,height=891dp")
@Composable
fun OnboardingStep0Preview() {
    OnboardingContent(
        currentStep = 0,
        uiState = OnboardingUiState(),
        onStepChange = {},
        onNicknameChange = {},
        onEmailChange = {},
        isLoading = false,
        onComplete = { _, _, _, _, _ -> }
    )
}

@Preview(showBackground = true, name = "2단계: 정보 입력", device = "spec:width=411dp,height=891dp")
@Composable
fun OnboardingStep1Preview() {
    OnboardingContent(
        currentStep = 1,
        uiState = OnboardingUiState(nicknameError = "중복된 닉네임입니다."),
        onStepChange = {},
        onNicknameChange = {},
        onEmailChange = {},
        isLoading = false,
        onComplete = { _, _, _, _, _ -> }
    )
}
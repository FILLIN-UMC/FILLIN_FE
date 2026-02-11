package com.example.fillin.ui.onboarding

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.fillin.R
import com.example.fillin.data.AppPreferences
import com.example.fillin.navigation.Screen
import kotlinx.coroutines.flow.collectLatest

@Composable
fun OnboardingScreen(
    navController: NavController,
    appPreferences: AppPreferences
) {
    val context = LocalContext.current
    val viewModel: OnboardingViewModel = viewModel(
        factory = OnboardingViewModelFactory(context, appPreferences)
    )

    LaunchedEffect(Unit) {
        viewModel.navEvents.collectLatest { event ->
            when (event) {
                OnboardingNavEvent.GoTerms -> { /* 온보딩에서는 약관 이미 완료 */ }
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
            }
        }
    }

    var nickname by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var agreeService by remember { mutableStateOf(false) }
    var agreeLocationHistory by remember { mutableStateOf(false) }
    var agreeMarketing by remember { mutableStateOf(false) }

    val primaryBlue = Color(0xFF4A90E2)
    val lightGray = Color(0xFFBDBDBD)
    val chevronGray = Color(0xFF9E9E9E)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        Spacer(Modifier.height(48.dp))

        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF7DB7F0)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_fillin_logo),
                    contentDescription = "FILLIN Logo",
                    tint = Color.Unspecified,
                    modifier = Modifier.size(56.dp)
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        Text(
            text = "서비스를 이용하기 위해\n정보를 입력해주세요.",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold
            ),
            modifier = Modifier.fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = nickname,
            onValueChange = { nickname = it },
            label = { Text("닉네임") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = primaryBlue,
                unfocusedBorderColor = lightGray,
                focusedLabelColor = primaryBlue
            )
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("이메일") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = primaryBlue,
                unfocusedBorderColor = lightGray,
                focusedLabelColor = primaryBlue
            )
        )

        Spacer(Modifier.height(24.dp))

        OnboardingTermsRow(
            checked = agreeService,
            text = "[필수] 필인 지도 서비스 이용약관",
            onToggle = { agreeService = !agreeService },
            onOpen = { },
            primaryBlue = primaryBlue,
            lightGray = lightGray,
            chevronGray = chevronGray
        )

        Spacer(Modifier.height(10.dp))

        OnboardingTermsRow(
            checked = agreeLocationHistory,
            text = "[선택] 개인정보(이동이력) 수집 및 이용",
            onToggle = { agreeLocationHistory = !agreeLocationHistory },
            onOpen = { },
            primaryBlue = primaryBlue,
            lightGray = lightGray,
            chevronGray = chevronGray
        )

        Spacer(Modifier.height(10.dp))

        OnboardingTermsRow(
            checked = agreeMarketing,
            text = "[선택] 마케팅 정보 수신 동의",
            onToggle = { agreeMarketing = !agreeMarketing },
            onOpen = { },
            primaryBlue = primaryBlue,
            lightGray = lightGray,
            chevronGray = chevronGray
        )

        Spacer(Modifier.weight(1f))

        val isLoading = viewModel.uiState.collectAsState().value.isLoading

        Button(
            onClick = {
                if (!isLoading) {
                    viewModel.completeOnboarding(
                        nickname = nickname,
                        email = email,
                        agreeService = agreeService,
                        agreeLocationHistory = agreeLocationHistory,
                        agreeMarketing = agreeMarketing
                    )
                }
            },
            enabled = !isLoading && agreeService,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            shape = RoundedCornerShape(32.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = primaryBlue,
                disabledContainerColor = primaryBlue.copy(alpha = 0.45f)
            )
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = "완료",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
            }
        }

        Spacer(Modifier.height(18.dp))
    }
}

@Composable
private fun OnboardingTermsRow(
    checked: Boolean,
    text: String,
    onToggle: () -> Unit,
    onOpen: () -> Unit,
    primaryBlue: Color,
    lightGray: Color,
    chevronGray: Color
) {
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
                .clickable { onToggle() },
            shape = CircleShape,
            color = if (checked) primaryBlue else Color.Transparent,
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
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF444444)
            ),
            modifier = Modifier.weight(1f)
        )

        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "Open",
            tint = chevronGray,
            modifier = Modifier
                .size(26.dp)
                .clickable { onOpen() }
        )
    }
}

package com.example.fillin.ui.terms

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.fillin.R
import com.example.fillin.data.AppPreferences
import com.example.fillin.navigation.Screen
import kotlinx.coroutines.launch

@Composable
fun TermsScreen(
    navController: NavController,
    appPreferences: AppPreferences
) {
    val scope = rememberCoroutineScope()

    // [필수] 필인 지도 서비스 이용약관
    var agreeService by remember { mutableStateOf(false) }

    // [선택] 개인정보(이동이력) 수집 및 이용
    var agreeLocationHistory by remember { mutableStateOf(false) }

    // [선택] 마케팅 정보 수신 동의
    var agreeMarketing by remember { mutableStateOf(false) }

    val allUnchecked = !agreeService && !agreeLocationHistory && !agreeMarketing
    val requiredOk = agreeService

    // 사진처럼: 아무것도 체크 안 했을 때는 "전체 동의하기", 그 외에는 "다음"
    val buttonText = if (allUnchecked) "전체 동의하기" else "다음"

    // 색상(사진 느낌의 블루)
    val primaryBlue = Color(0xFF4A90E2)
    val lightGray = Color(0xFFBDBDBD)
    val chevronGray = Color(0xFF9E9E9E)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        Spacer(Modifier.height(92.dp))

        // 상단 원형 로고
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF7DB7F0)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_fillin_logo),
                    contentDescription = "FILLIN Logo",
                    tint = Color.Unspecified,
                    modifier = Modifier.size(70.dp)
                )
            }
        }

        Spacer(Modifier.height(28.dp))

        // 중앙 타이틀 (사진처럼 2줄)
        Text(
            text = "지도 서비스를 이용하기 위해\n동의가 필요해요.",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold
            ),
            modifier = Modifier.fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(Modifier.weight(1f))

        // 약관 리스트
        TermsRow(
            checked = agreeService,
            text = "[필수] 필인 지도 서비스 이용약관",
            onToggle = { agreeService = !agreeService },
            onOpen = { /* TODO */ },
            primaryBlue = primaryBlue,
            lightGray = lightGray,
            chevronGray = chevronGray
        )

        Spacer(Modifier.height(10.dp))

        TermsRow(
            checked = agreeLocationHistory,
            text = "[선택] 개인정보(이동이력) 수집 및 이용",
            onToggle = { agreeLocationHistory = !agreeLocationHistory },
            onOpen = { /* TODO */ },
            primaryBlue = primaryBlue,
            lightGray = lightGray,
            chevronGray = chevronGray
        )

        Spacer(Modifier.height(10.dp))

        TermsRow(
            checked = agreeMarketing,
            text = "[선택] 마케팅 정보 수신 동의",
            onToggle = { agreeMarketing = !agreeMarketing },
            onOpen = { /* TODO */ },
            primaryBlue = primaryBlue,
            lightGray = lightGray,
            chevronGray = chevronGray
        )

        Spacer(Modifier.height(26.dp))

        Button(
            onClick = {
                if (allUnchecked) {
                    // "전체 동의하기" 누르면 전부 체크
                    agreeService = true
                    agreeLocationHistory = true
                    agreeMarketing = true
                    return@Button
                }

                // "다음": 필수만 체크되어 있으면 진행
                if (!requiredOk) return@Button

                scope.launch {
                    appPreferences.setTermsAccepted(true)
                    appPreferences.setLocationHistoryConsent(agreeLocationHistory)
                    appPreferences.setMarketingConsent(agreeMarketing)

                    // ✅ (핵심) 약관 동의 후 곧바로 "앱 접근권한" 화면으로 이동
                    navController.navigate(Screen.Permission.route) {
                        popUpTo(Screen.Terms.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            },
            enabled = allUnchecked || requiredOk,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            shape = RoundedCornerShape(32.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = primaryBlue,
                disabledContainerColor = primaryBlue.copy(alpha = 0.45f)
            )
        ) {
            Text(
                text = buttonText,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
        }

        Spacer(Modifier.height(18.dp))
    }
}

@Composable
private fun TermsRow(
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
        RoundCheck(
            checked = checked,
            onClick = onToggle,
            primaryBlue = primaryBlue,
            lightGray = lightGray
        )

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

@Composable
private fun RoundCheck(
    checked: Boolean,
    onClick: () -> Unit,
    primaryBlue: Color,
    lightGray: Color
) {
    val bg = if (checked) primaryBlue else Color.Transparent
    val border = if (checked) primaryBlue else lightGray
    val checkTint = if (checked) Color.White else lightGray

    Surface(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .clickable { onClick() },
        shape = CircleShape,
        color = bg,
        border = androidx.compose.foundation.BorderStroke(2.dp, border)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = "check",
                tint = checkTint,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

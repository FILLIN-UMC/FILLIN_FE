package com.example.fillin.ui.login

import com.example.fillin.config.GoogleConfig
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape // ✅ 완전한 원형을 위해 추가
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale // ✅ 이미지 비율 유지를 위해 추가
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.fillin.R
import com.example.fillin.data.AppPreferences
import com.example.fillin.navigation.Screen
import com.example.fillin.ui.components.FillinBlueGradientBackground
import kotlinx.coroutines.flow.collectLatest

@Composable
fun LoginScreen(
    navController: NavController,
    appPreferences: AppPreferences
) {
    val context = LocalContext.current
    val activity = context.findActivity()

    val authViewModel: AuthViewModel = viewModel(
        factory = AuthViewModelFactory(appPreferences)
    )

    LaunchedEffect(Unit) {
        authViewModel.navEvents.collectLatest { event ->
            when (event) {
                AuthNavEvent.GoTerms -> {
                    navController.navigate(Screen.Terms.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
                AuthNavEvent.GoPermissions -> {
                    navController.navigate(Screen.Permission.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
                AuthNavEvent.GoAfterLoginSplash -> {
                    navController.navigate(Screen.AfterLoginSplash.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
                AuthNavEvent.Logout -> {
                    // 로그인 화면에 이미 있으므로 아무 동작도 하지 않음
                }
                is AuthNavEvent.ShowError -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    FillinBlueGradientBackground {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(1f))

            // ✅ 로고: 색상을 쨍한 하얀색으로 입히고 비율을 맞춤
            Image(
                painter = painterResource(R.drawable.ic_fillin_logo),
                contentDescription = "FILLIN Logo",
                modifier = Modifier.size(180.dp),
                colorFilter = ColorFilter.tint(Color.White),
                contentScale = ContentScale.Fit // 이미지 왜곡 방지
            )

            Spacer(modifier = Modifier.weight(1f))

            Row(
                horizontalArrangement = Arrangement.spacedBy(28.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconCircleButton(
                    iconRes = R.drawable.ic_kakao,
                    onClick = {
                        if (activity == null) {
                            Toast.makeText(context, "Activity를 찾을 수 없어요.", Toast.LENGTH_SHORT).show()
                            return@IconCircleButton
                        }
                        authViewModel.loginWithKakao(context, activity)
                    }
                )

                IconCircleButton(
                    iconRes = R.drawable.ic_google,
                    onClick = {
                        if (activity == null) {
                            Toast.makeText(context, "Activity를 찾을 수 없어요.", Toast.LENGTH_SHORT).show()
                            return@IconCircleButton
                        }
                        authViewModel.loginWithGoogle(
                            activity = activity,
                            webClientId = GoogleConfig.WEB_CLIENT_ID
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "로그인 안하고 구경할래요",
                color = Color.White.copy(alpha = 0.8f), // 기획안처럼 약간 투명한 하얀색
                style = MaterialTheme.typography.bodyMedium.copy(
                    textDecoration = TextDecoration.Underline
                ),
                modifier = Modifier.clickable {
                    // 둘러보기 로직
                }
            )

            Spacer(modifier = Modifier.height(64.dp))
        }
    }
}

private fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

@Composable
private fun IconCircleButton(
    iconRes: Int,
    onClick: () -> Unit
) {
    Surface(
        // ✅ shape를 CircleShape로 변경하여 완전한 원형 버튼으로 수정
        shape = CircleShape, 
        color = Color.White, // 배경색 강제 지정
        tonalElevation = 0.dp,
        shadowElevation = 2.dp, // 기획안은 그림자가 아주 연함
        modifier = Modifier.size(64.dp), // 기획안 비율에 맞춰 소폭 조정
        onClick = onClick
    ) {
        Box(contentAlignment = Alignment.Center) {
            Image(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier.size(30.dp) // 아이콘 크기 최적화
            )
        }
    }
}

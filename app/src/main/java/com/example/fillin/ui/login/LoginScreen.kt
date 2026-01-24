package com.example.fillin.ui.login

import com.example.fillin.config.GoogleConfig
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
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

    // ✅ ViewModel 네비게이션 이벤트 구독
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

            // ✅ 로고 이미지: colorFilter를 사용하여 하얀색으로 변경
            Image(
                painter = painterResource(R.drawable.ic_fillin_logo),
                contentDescription = "FILLIN Logo",
                modifier = Modifier.size(180.dp),
                colorFilter = ColorFilter.tint(Color.White)
            )

            Spacer(modifier = Modifier.weight(1f))

            Row(
                horizontalArrangement = Arrangement.spacedBy(28.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // ✅ 카카오 버튼
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

                // ✅ 구글 버튼
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
                color = Color.White, // 배경이 블루 그래디언트라면 텍스트도 하얀색이 잘 보일 거예요
                style = MaterialTheme.typography.bodyMedium.copy(
                    textDecoration = TextDecoration.Underline
                ),
                modifier = Modifier.clickable {
                    // 필요 시 둘러보기 라우트로 이동
                }
            )

            Spacer(modifier = Modifier.height(64.dp))
        }
    }
}

/** Compose context에서 Activity 찾기 */
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
        shape = MaterialTheme.shapes.extraLarge,
        tonalElevation = 2.dp,
        shadowElevation = 4.dp,
        modifier = Modifier.size(72.dp),
        onClick = onClick
    ) {
        Box(contentAlignment = Alignment.Center) {
            Image(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier.size(34.dp)
            )
        }
    }
}

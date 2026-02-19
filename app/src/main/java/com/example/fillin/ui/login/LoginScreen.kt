package com.example.fillin.ui.login

import com.example.fillin.config.GoogleConfig
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview // 추가
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
        factory = AuthViewModelFactory(context, appPreferences)
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
                AuthNavEvent.GoOnboarding -> {
                    navController.navigate(Screen.Onboarding.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
                AuthNavEvent.Logout -> {}
                is AuthNavEvent.ShowError -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // 실제 UI 컴포저블에 로직 전달
    LoginScreenContent(
        onKakaoLogin = {
            if (activity != null) authViewModel.loginWithKakao(context, activity)
            else Toast.makeText(context, "Activity를 찾을 수 없어요.", Toast.LENGTH_SHORT).show()
        },
        onGoogleLogin = {
            if (activity != null) {
                authViewModel.loginWithGoogle(
                    activity = activity,
                    webClientId = GoogleConfig.WEB_CLIENT_ID
                )
            } else {
                Toast.makeText(context, "Activity를 찾을 수 없어요.", Toast.LENGTH_SHORT).show()
            }
        }
    )
}

@Composable
fun LoginScreenContent(
    onKakaoLogin: () -> Unit,
    onGoogleLogin: () -> Unit
) {
    FillinBlueGradientBackground {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(360.dp))

            Image(
                painter = painterResource(R.drawable.img_logo),
                contentDescription = "FILLIN Logo"
            )

            Spacer(modifier = Modifier.weight(1f))

            Row(
                horizontalArrangement = Arrangement.spacedBy(0.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(R.drawable.btn_kakao_login),
                    contentDescription = "Kakao Login",
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .clickable { onKakaoLogin() },
                    contentScale = ContentScale.Fit
                )

                Image(
                    painter = painterResource(R.drawable.btn_google_login),
                    contentDescription = "Google Login",
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .clickable { onGoogleLogin() },
                    contentScale = ContentScale.Fit
                )
            }

            Spacer(modifier = Modifier.height(144.dp))
        }
    }
}
    
@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    LoginScreenContent(
        onKakaoLogin = {},
        onGoogleLogin = {}
    )
}

private fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}
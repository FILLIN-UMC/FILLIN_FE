package com.example.fillin.ui.splash

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.fillin.R
import com.example.fillin.data.AppPreferences
import com.example.fillin.navigation.Screen
import com.example.fillin.ui.components.FillinBlueGradientBackground
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import com.example.fillin.utils.PermissionUtils

@Composable
fun AfterLoginSplashScreen(
    navController: NavController,
    appPreferences: AppPreferences
) {
    val context = LocalContext.current

    // 화면 진입 자체 확인용 로그
    Log.d("LOGIN_FLOW", "Entered AfterLoginSplashScreen")

    LaunchedEffect(Unit) {
        delay(800)

        val isLoggedIn = appPreferences.isLoggedInFlow.first()
        val isTermsAccepted = appPreferences.isTermsAcceptedFlow.first()
        val hasActualPermission = PermissionUtils.hasLocationPermissions(context)

        Log.d(
            "LOGIN_FLOW",
            "values -> loggedIn=$isLoggedIn, terms=$isTermsAccepted, actualPermission=$hasActualPermission"
        )

        val nextRoute = when {
            !isLoggedIn -> Screen.Login.route
            !isTermsAccepted -> Screen.Onboarding.route
            !hasActualPermission -> Screen.Permission.route
            else -> Screen.Main.route
        }

        Log.d("LOGIN_FLOW", "AfterLoginSplash nextRoute=$nextRoute")

        navController.navigate(nextRoute) {
            popUpTo(Screen.AfterLoginSplash.route) { inclusive = true }
            launchSingleTop = true
        }
    }

    // UI 부분만 호출
    AfterLoginSplashContent()
}

@Composable
fun AfterLoginSplashContent() {
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

            Spacer(modifier = Modifier.height(474.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AfterLoginSplashScreenPreview() {
    AfterLoginSplashContent()
}
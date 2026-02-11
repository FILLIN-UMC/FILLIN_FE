package com.example.fillin.ui.splash

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.fillin.R
import com.example.fillin.data.AppPreferences
import com.example.fillin.navigation.Screen
import com.example.fillin.ui.components.FillinBlueGradientBackground
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

@Composable
fun AfterLoginSplashScreen(
    navController: NavController,
    appPreferences: AppPreferences
) {
    // 화면 진입 자체 확인용 로그
    Log.d("LOGIN_FLOW", "Entered AfterLoginSplashScreen")

    LaunchedEffect(Unit) {
        delay(800)

        val isLoggedIn = appPreferences.isLoggedInFlow.first()
        val isTermsAccepted = appPreferences.isTermsAcceptedFlow.first()
        val isPermissionGranted = appPreferences.isPermissionGrantedFlow.first()

        // 분기 값 확인용 로그 (제일 중요)
        Log.d(
            "LOGIN_FLOW",
            "values -> loggedIn=$isLoggedIn, terms=$isTermsAccepted, permission=$isPermissionGranted"
        )

        val nextRoute = when {
            !isLoggedIn -> Screen.Login.route
            !isTermsAccepted -> Screen.Terms.route
            !isPermissionGranted -> Screen.Permission.route
            else -> Screen.Main.route
        }

        Log.d("LOGIN_FLOW", "AfterLoginSplash nextRoute=$nextRoute")

        navController.navigate(nextRoute) {
            popUpTo(Screen.AfterLoginSplash.route) { inclusive = true }
            launchSingleTop = true
        }
    }

    FillinBlueGradientBackground {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Image(
                painter = painterResource(R.drawable.ic_fillin_logo),
                contentDescription = "FILLIN Logo",
                modifier = Modifier.size(180.dp)
            )
        }
    }
}

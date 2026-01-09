package com.example.fillin.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.fillin.data.AppPreferences
import com.example.fillin.ui.login.LoginScreen
import com.example.fillin.ui.permission.PermissionScreen
import com.example.fillin.ui.splash.AfterLoginSplashScreen
import com.example.fillin.ui.terms.TermsScreen
import com.example.fillin.ui.main.MainScreen

@Composable
fun NavGraph(startDestination: String) {
    val navController = rememberNavController()
    val context = LocalContext.current

    // ✅ recomposition 때마다 AppPreferences 새로 만들지 않게 고정
    val appPreferences = remember { AppPreferences(context) }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Login.route) {
            LoginScreen(navController = navController, appPreferences = appPreferences)
        }

        composable(Screen.AfterLoginSplash.route) {
            AfterLoginSplashScreen(navController = navController, appPreferences = appPreferences)
        }

        composable(Screen.Terms.route) {
            TermsScreen(navController = navController, appPreferences = appPreferences)
        }

        composable(Screen.Permission.route) {
            PermissionScreen(navController = navController, appPreferences = appPreferences)
        }

        composable(Screen.Main.route) {
            MainScreen(navController = navController, appPreferences = appPreferences)
        }
    }
}

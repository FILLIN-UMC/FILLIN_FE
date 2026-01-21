package com.example.fillin.navigation

sealed class Screen(val route: String) {
    data object Login : Screen("login")
    data object Terms : Screen("terms")
    data object Permission : Screen("permission")
    data object AfterLoginSplash : Screen("after_login_splash")
}


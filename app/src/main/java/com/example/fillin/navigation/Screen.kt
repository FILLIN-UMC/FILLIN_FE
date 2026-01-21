package com.example.fillin.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object AfterLoginSplash : Screen("after_login_splash")
    object Terms : Screen("terms")
    object Permission : Screen("permission")
    object Main : Screen("main")
}


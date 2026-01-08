package com.example.fillin.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

import androidx.compose.foundation.layout.padding
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.fillin.ui.main.MainTab
import com.example.fillin.feature.home.HomeScreen
import com.example.fillin.feature.mypage.MyPageScreen
import com.example.fillin.feature.mypage.ProfileEditScreen
import com.example.fillin.feature.mypage.SettingsScreen
import com.example.fillin.feature.mypage.ROUTE_PROFILE_EDIT
import com.example.fillin.feature.mypage.ROUTE_SETTINGS
import com.example.fillin.feature.mypage.ROUTE_NOTIFICATIONS
import com.example.fillin.feature.mypage.NotificationsScreen
import com.example.fillin.feature.report.ReportScreen


@Composable
fun MainNavGraph(
    navController: NavHostController,
    innerPadding: androidx.compose.foundation.layout.PaddingValues,
    onHideBottomBar: () -> Unit = {},
    onShowBottomBar: () -> Unit = {}
) {
    NavHost(
        navController = navController,
        startDestination = MainTab.Home.route,
    ) {
        composable(MainTab.Home.route) { HomeScreen() }
        composable(MainTab.Report.route) { ReportScreen() }
        composable(MainTab.My.route) {
            MyPageScreen(
                navController = navController,
                onHideBottomBar = onHideBottomBar,
                onShowBottomBar = onShowBottomBar
            )
        }
        composable(ROUTE_PROFILE_EDIT) { ProfileEditScreen(navController = navController) }
        composable(ROUTE_SETTINGS) { SettingsScreen(navController = navController) }
        composable(ROUTE_NOTIFICATIONS) {
            NotificationsScreen(navController = navController)
        }
    }
}


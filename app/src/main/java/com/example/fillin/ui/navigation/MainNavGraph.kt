package com.example.fillin.ui.navigation

import android.util.Log
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
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
import com.example.fillin.feature.mypage.ROUTE_MY_REPORTS
import com.example.fillin.feature.mypage.ROUTE_EXPIRING_REPORT_DETAIL
import com.example.fillin.feature.expiringreport.ExpiringReportDetailScreen
import com.example.fillin.feature.myreports.MyReportsScreen
import com.example.fillin.feature.notifications.NotificationsScreen
import com.example.fillin.data.AppPreferences
import com.example.fillin.feature.search.SearchScreen
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut


@Composable
fun MainNavGraph(
    navController: NavHostController,
    innerPadding: androidx.compose.foundation.layout.PaddingValues,
    appPreferences: AppPreferences,
    onHideBottomBar: () -> Unit = {},
    onShowBottomBar: () -> Unit = {}
) {
    NavHost(
        navController = navController,
        startDestination = MainTab.Home.route,
    ) {
        composable(MainTab.Home.route) { 
            HomeScreen(
                navController = navController,
                onHideBottomBar = onHideBottomBar,
                onShowBottomBar = onShowBottomBar
            ) 
        }
        composable(MainTab.My.route) {
            MyPageScreen(
                navController = navController,
                appPreferences = appPreferences,
                onHideBottomBar = onHideBottomBar,
                onShowBottomBar = onShowBottomBar
            )
        }
        composable(ROUTE_PROFILE_EDIT) { 
            ProfileEditScreen(
                navController = navController,
                appPreferences = appPreferences
            ) 
        }
        composable(ROUTE_SETTINGS) { SettingsScreen(navController = navController) }
        composable(ROUTE_NOTIFICATIONS) {
            NotificationsScreen(navController = navController)
        }
        composable(ROUTE_MY_REPORTS) {
            MyReportsScreen(navController = navController)
        }
        composable(ROUTE_EXPIRING_REPORT_DETAIL) {
            ExpiringReportDetailScreen(navController = navController)
        }
        composable("expiring_report_detail") {
            ExpiringReportDetailScreen(navController = navController)
        }

        composable(
            route = "search",
            enterTransition = { fadeIn(animationSpec = tween(700)) },
            popExitTransition = { fadeOut(animationSpec = tween(700)) }
        ) {
            Log.d("SearchTest", "3. MainNavGraph 라우트 도착! SearchScreen 띄움!")
            SearchScreen(
                onBack = {
                    navController.popBackStack()
                },
                onSelectPlace = { place ->
                    navController.popBackStack()
                },
                onClickHotReport = { reportId ->
                    navController.previousBackStackEntry?.savedStateHandle?.set("selected_report_id", reportId)
                    navController.popBackStack()
                }
            )
        }
    }
}

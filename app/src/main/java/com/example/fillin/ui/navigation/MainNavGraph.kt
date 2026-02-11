package com.example.fillin.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.fillin.ui.main.MainTab
import com.example.fillin.feature.search.SearchScreen
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
        composable("search") {
            SearchScreen(
                onBack = { navController.popBackStack() },
                onSelectPlace = { place ->
                    // 검색 결과에서 선택한 장소를 Home 화면에 전달
                    val homeEntry = navController.getBackStackEntry(MainTab.Home.route)
                    val lat = place.y?.toDoubleOrNull()
                    val lng = place.x?.toDoubleOrNull()
                    homeEntry.savedStateHandle["search_place_name"] = place.name
                    homeEntry.savedStateHandle["search_place_address"] = place.address
                    if (lat != null && lng != null) {
                        homeEntry.savedStateHandle["search_place_lat"] = lat
                        homeEntry.savedStateHandle["search_place_lng"] = lng
                        // 단순 변경 감지를 위한 ID (같은 장소를 다시 눌러도 동작하도록 시간값 사용)
                        homeEntry.savedStateHandle["search_place_id"] = System.currentTimeMillis().toString()
                    }
                    // Home으로 복귀
                    navController.popBackStack()
                }
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
    }
}

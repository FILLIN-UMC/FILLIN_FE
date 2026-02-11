package com.example.fillin.ui.main

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.fillin.feature.mypage.ROUTE_MY_REPORTS
import com.example.fillin.feature.mypage.ROUTE_NOTIFICATIONS
import com.example.fillin.feature.mypage.ROUTE_SETTINGS
import com.example.fillin.feature.mypage.ROUTE_PROFILE_EDIT
import com.example.fillin.ui.components.BottomNavBar
import com.example.fillin.ui.components.TabSpec
import com.example.fillin.ui.navigation.MainNavGraph
import com.example.fillin.data.AppPreferences
import androidx.compose.ui.platform.LocalContext

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val appPreferences = remember { AppPreferences(context) }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    // Routes that should NOT show the bottom navigation bar.
    val hideBottomBarRoutes = setOf(
        ROUTE_MY_REPORTS,
        ROUTE_NOTIFICATIONS,
        ROUTE_SETTINGS,
        ROUTE_PROFILE_EDIT
    )
    val hideBottomBar = currentRoute in hideBottomBarRoutes

    val isMyPage = currentRoute?.startsWith(MainTab.My.route) == true
    val showBottomBar = !hideBottomBar && (
        currentRoute == MainTab.Home.route ||
                currentRoute == MainTab.Report.route ||
                isMyPage
    )

    var isMyPageBottomBarVisible by remember { mutableStateOf(true) }

    Scaffold(
        bottomBar = {
            // Use Scaffold bottomBar for non-MyPage screens only.
            // (On MyPage we draw the bar as an overlay so dragging it down reveals the content behind it.)
            if (showBottomBar && !isMyPage) {
                BottomNavBar(
                    selectedRoute = currentRoute,
                    home = TabSpec(MainTab.Home.route, MainTab.Home.label, MainTab.Home.icon),
                    report = TabSpec(MainTab.Report.route, MainTab.Report.label, MainTab.Report.icon),
                    my = TabSpec(MainTab.My.route, MainTab.My.label, MainTab.My.icon),
                    onTabClick = { route ->
                        Log.d("BottomNav", "onTabClick(nonMy) route=$route current=$currentRoute")
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onReportClick = {
                        Log.d("BottomNav", "onReportClick(nonMy) current=$currentRoute")
                        navController.navigate(MainTab.Report.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onSearchClick = {
                        navController.navigate("search")
                    },
                    enableDragToHide = false
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFFFFFFF))
        ) {
            MainNavGraph(
                navController = navController,
                innerPadding = innerPadding,
                appPreferences = appPreferences,
                onHideBottomBar = { isMyPageBottomBarVisible = false },
                onShowBottomBar = { isMyPageBottomBarVisible = true }
            )

            // Overlay BottomNavBar on MyPage so dragging it down reveals the content behind it.
            if (showBottomBar && isMyPage) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                ) {
                    AnimatedVisibility(
                        visible = isMyPageBottomBarVisible,
                        enter = slideInVertically(
                            initialOffsetY = { fullHeight -> fullHeight },
                            animationSpec = tween(durationMillis = 350)
                        ),
                        exit = slideOutVertically(
                            targetOffsetY = { fullHeight -> fullHeight },
                            animationSpec = tween(durationMillis = 350)
                        )
                    ) {
                        BottomNavBar(
                            selectedRoute = currentRoute,
                            home = TabSpec(MainTab.Home.route, MainTab.Home.label, MainTab.Home.icon),
                            report = TabSpec(MainTab.Report.route, MainTab.Report.label, MainTab.Report.icon),
                            my = TabSpec(MainTab.My.route, MainTab.My.label, MainTab.My.icon),
                            onTabClick = { route ->
                                Log.d("BottomNav", "onTabClick(MyOverlay) route=$route current=$currentRoute")
                                navController.navigate(route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            onReportClick = {
                                Log.d("BottomNav", "onReportClick(MyOverlay) current=$currentRoute")
                                navController.navigate(MainTab.Report.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            onSearchClick = {
                                navController.navigate("search")
                            },
                            enableDragToHide = false
                        )
                    }
                }
            }
        }
    }
}
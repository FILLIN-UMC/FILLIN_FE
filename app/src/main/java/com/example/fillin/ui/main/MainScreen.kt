package com.example.fillin.ui.main

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.fillin.ui.components.BottomNavBar
import com.example.fillin.ui.components.TabSpec
import com.example.fillin.ui.navigation.MainNavGraph

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val isMyPage = currentRoute?.startsWith(MainTab.My.route) == true
    val showBottomBar =
        currentRoute == MainTab.Home.route ||
                currentRoute == MainTab.Report.route ||
                isMyPage

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
                    enableDragToHide = false
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFFFFFFF))
                .padding(innerPadding)
        ) {
            MainNavGraph(
                navController = navController,
                innerPadding = innerPadding
            )

            // Overlay BottomNavBar on MyPage so dragging it down reveals the content behind it.
            if (showBottomBar && isMyPage) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
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
                        enableDragToHide = true
                    )
                }
            }
        }
    }
}
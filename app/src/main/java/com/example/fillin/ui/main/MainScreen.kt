package com.example.fillin.ui.main

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.fillin.feature.mypage.ROUTE_MY_REPORTS
import com.example.fillin.feature.mypage.ROUTE_NOTIFICATIONS
import com.example.fillin.feature.mypage.ROUTE_SETTINGS
import com.example.fillin.feature.mypage.ROUTE_PROFILE_EDIT
import com.example.fillin.feature.report.ReportOptionMenu
import com.example.fillin.ui.components.BottomNavBar
import com.example.fillin.ui.components.TabSpec
import com.example.fillin.ui.navigation.MainNavGraph
import com.example.fillin.ui.main.MainTab
import com.example.fillin.data.AppPreferences
import com.example.fillin.data.api.TokenManager
import com.example.fillin.data.repository.MemberRepository
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await

@Composable
fun MainScreen(
    navController: NavController,
    appPreferences: AppPreferences
) {
    val innerNavController = rememberNavController()
    val context = LocalContext.current
    val accessToken = TokenManager.getAccessToken(context)
    LaunchedEffect(accessToken) {
        // FCM 토큰 등록은 온보딩용 tempToken이 아니라 accessToken이 있을 때만 수행
        if (accessToken != null) {
            runCatching {
                val token = FirebaseMessaging.getInstance().token.await()
                if (token.isNotBlank()) {
                    MemberRepository(context).registerFcmToken(token)
                        .onSuccess { Log.d("FCM", "FCM token registered") }
                        .onFailure { e -> Log.e("FCM", "Failed to register FCM token", e) }
                }
            }.onFailure { e -> Log.e("FCM", "FCM token error", e) }
        }
    }
    val backStackEntry by innerNavController.currentBackStackEntryAsState()
    val currentRoute: String? = backStackEntry?.destination?.route

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
                isMyPage
    )

    var isMyPageBottomBarVisible by remember { mutableStateOf(true) }
    var isHomeBottomBarVisible by remember { mutableStateOf(true) }
    var showReportMenu by remember { mutableStateOf(false) }
    // 제보 플로우 타입을 저장 (navigate 후에 savedStateHandle에 설정하기 위함)
    var pendingReportFlow by remember { mutableStateOf<String?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            bottomBar = {
            // Use Scaffold bottomBar for non-MyPage screens only.
            // (On MyPage we draw the bar as an overlay so dragging it down reveals the content behind it.)
            if (showBottomBar && !isMyPage) {
                // Home 화면에서는 isHomeBottomBarVisible 상태를 확인
                val shouldShowBottomBar = when (currentRoute) {
                    MainTab.Home.route -> isHomeBottomBarVisible
                    else -> true
                }
                
                if (shouldShowBottomBar) {
                    BottomNavBar(
                    selectedRoute = currentRoute,
                    home = TabSpec(MainTab.Home.route, MainTab.Home.label, MainTab.Home.icon),
                    report = TabSpec(MainTab.Report.route, MainTab.Report.label, MainTab.Report.icon),
                    my = TabSpec(MainTab.My.route, MainTab.My.label, MainTab.My.icon),
                    onTabClick = { route ->
                        Log.d("BottomNav", "onTabClick(nonMy) route=$route current=$currentRoute")
                        // Report 탭을 클릭하면 제보 메뉴 표시, 아니면 해당 화면으로 이동
                        if (route == MainTab.Report.route) {
                            showReportMenu = true
                        } else {
                            innerNavController.navigate(route) {
                                popUpTo(innerNavController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                    onReportClick = {
                        Log.d("BottomNav", "onReportClick(nonMy) current=$currentRoute")
                        showReportMenu = true
                    },
                        onSearchClick = {
                            innerNavController.navigate("search")
                        },
                    enableDragToHide = false
                )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFFFFFFF))
        ) {
            MainNavGraph(
                navController = innerNavController,
                innerPadding = innerPadding,
                appPreferences = appPreferences,
                onHideBottomBar = { 
                    when {
                        isMyPage -> isMyPageBottomBarVisible = false
                        currentRoute == MainTab.Home.route -> isHomeBottomBarVisible = false
                    }
                },
                onShowBottomBar = { 
                    when {
                        isMyPage -> isMyPageBottomBarVisible = true
                        currentRoute == MainTab.Home.route -> isHomeBottomBarVisible = true
                    }
                }
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
                            // Report 탭을 클릭하면 제보 메뉴 표시, 아니면 해당 화면으로 이동
                            if (route == MainTab.Report.route) {
                                showReportMenu = true
                            } else {
                                innerNavController.navigate(route) {
                                    popUpTo(innerNavController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        onReportClick = {
                            Log.d("BottomNav", "onReportClick(MyOverlay) current=$currentRoute")
                                showReportMenu = true
                            },
                        onSearchClick = {
                            innerNavController.navigate("search")
                        },
                            enableDragToHide = false
                        )
        }
                }
            }

        }
    }

    // navigate 후에 savedStateHandle에 값을 설정
    LaunchedEffect(currentRoute, pendingReportFlow) {
        if (currentRoute == MainTab.Home.route && pendingReportFlow != null) {
            val flowValue = pendingReportFlow // 로컬 변수로 저장하여 null 체크 문제 해결
            // navigate가 완료된 후 savedStateHandle에 값 설정
            delay(50) // navigate 완료를 위한 짧은 지연
            try {
                innerNavController.getBackStackEntry(MainTab.Home.route)
                    .savedStateHandle
                    .set("report_flow", flowValue)
                pendingReportFlow = null // 설정 완료 후 초기화
            } catch (e: Exception) {
                // BackStackEntry를 찾을 수 없는 경우 재시도
                delay(100)
                try {
                    innerNavController.getBackStackEntry(MainTab.Home.route)
                        .savedStateHandle
                        .set("report_flow", flowValue)
                    pendingReportFlow = null
                } catch (e2: Exception) {
                    Log.e("MainScreen", "Failed to set report_flow", e2)
                }
            }
        }
    }

        // 제보 버튼 클릭 시 "지난 상황 제보" / "실시간 제보" 팝업 (bottomBar 위에 표시되도록 Scaffold 바깥에 배치)
        if (showReportMenu) {
            Box(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.2f))
                        .clickable { showReportMenu = false }
                )
                ReportOptionMenu(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 120.dp),
                    onPastReportClick = {
                        showReportMenu = false
                        // [수정] '현재' 화면이 아니라 'Home' 화면의 저장소에 직접 값을 세팅합니다.
                        innerNavController.getBackStackEntry(MainTab.Home.route)
                            .savedStateHandle
                            .set("report_flow", "past")

                        innerNavController.navigate(MainTab.Home.route) {
                            popUpTo(innerNavController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onRealtimeReportClick = {
                        showReportMenu = false
                        // [수정] 동일하게 'Home' 화면의 저장소를 대상으로 합니다.
                        innerNavController.getBackStackEntry(MainTab.Home.route)
                            .savedStateHandle
                            .set("report_flow", "realtime")

                        innerNavController.navigate(MainTab.Home.route) {
                            popUpTo(innerNavController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    }
}

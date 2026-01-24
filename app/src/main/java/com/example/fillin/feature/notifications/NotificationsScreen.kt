package com.example.fillin.feature.notifications

import androidx.compose.runtime.Composable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import com.example.fillin.R
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController
import com.example.fillin.ui.theme.FILLINTheme
import com.example.fillin.ui.main.MainTab
import com.example.fillin.feature.mypage.ROUTE_NOTIFICATIONS
import com.example.fillin.feature.mypage.ROUTE_EXPIRING_REPORT_DETAIL
import com.example.fillin.data.SharedReportData
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.navigation.NavGraph.Companion.findStartDestination

private data class NotificationUiModel(
    val id: String,
    val content: NotificationContent,
    val createdAt: Instant,
    val isRead: Boolean,
    val avatarRes: Int,
    val thumbnailRes: Int?
)

@Composable
fun NotificationsScreen(navController: NavController) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val now = remember { Instant.now() }
    
    // 알림 확인 상태 로드
    var readNotificationIds by remember(context) { 
        mutableStateOf(SharedReportData.loadNotificationReadStates(context))
    }

    // 사용자 제보 통계 가져오기
    val reportStats = remember { SharedReportData.getReportStats() }
    val badgeName = remember { SharedReportData.getBadgeName() }
    
    // 5가지 종류(피드백/저장/사라질제보/뱃지 획득/시스템) 예시 알림
    val sampleNotifications = remember(now, readNotificationIds, reportStats, badgeName) {
        listOf(
            NotificationUiModel(
                id = "sample_feedback",
                content = NotificationContent.Feedback(
                    actorName = "조치원고라니",
                    choice = FeedbackChoice.STILL_DANGEROUS,
                    reportId = 1L // 예시 제보 ID
                ),
                createdAt = now.minus(5, ChronoUnit.MINUTES),
                isRead = readNotificationIds.contains("sample_feedback"),
                avatarRes = R.drawable.ic_user_img,
                thumbnailRes = R.drawable.ic_report_img
            ),
            NotificationUiModel(
                id = "sample_saved",
                content = NotificationContent.Saved(
                    actorName = "조치원고라니",
                    reportId = 2L // 예시 제보 ID
                ),
                createdAt = now.minus(2, ChronoUnit.HOURS),
                isRead = readNotificationIds.contains("sample_saved"),
                avatarRes = R.drawable.ic_user_img,
                thumbnailRes = R.drawable.ic_report_img
            ),
            NotificationUiModel(
                id = "sample_expiring",
                content = NotificationContent.ExpiringReport(
                    daysLeft = 3,
                    summaryText = "위험1, 발견2"
                ),
                createdAt = now.minus(3, ChronoUnit.DAYS),
                isRead = readNotificationIds.contains("sample_expiring"),
                avatarRes = R.drawable.ic_user_img,
                thumbnailRes = R.drawable.ic_report_img
            ),
            NotificationUiModel(
                id = "sample_badge",
                content = NotificationContent.BadgeAcquired(
                    badgeName = badgeName,
                    totalCompletedReports = reportStats.totalCount
                ),
                createdAt = now.minus(1, ChronoUnit.DAYS),
                isRead = readNotificationIds.contains("sample_badge"),
                avatarRes = R.drawable.ic_user_img,
                thumbnailRes = null
            ),
            NotificationUiModel(
                id = "sample_system",
                content = NotificationContent.SystemNotice(
                    title = "[공지] 서비스 업데이트 안내"
                ),
                createdAt = now.minus(20, ChronoUnit.DAYS),
                isRead = readNotificationIds.contains("sample_system"),
                avatarRes = R.drawable.ic_user_img,
                thumbnailRes = null
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
//            .padding(horizontal = 16.dp)
    ) {
        // Top bar (design match)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 0.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .clickable { navController.popBackStack() }
                    .align(Alignment.CenterStart),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.foundation.Image(
                    painter = painterResource(id = R.drawable.ic_back_btn),
                    contentDescription = "뒤로가기",
                    modifier = Modifier.size(48.dp)
                )
            }

            Text(
                text = "알림",
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF252526),
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // 리스트가 비어있으면 기존 empty state 유지
        if (sampleNotifications.isEmpty()) {
            Spacer(Modifier.height(140.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Outlined.Notifications,
                    contentDescription = null,
                    tint = Color(0xFFAAADB3),
                    modifier = Modifier.size(34.dp)
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "아직 받은 알림이 없어요",
                    color = Color(0xFFAAADB3),
                    fontWeight = FontWeight.SemiBold
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(sampleNotifications, key = { it.id }) { n ->
                    val timeText = remember(n.createdAt, now) {
                        val diff = Duration.between(n.createdAt, now)
                        when {
                            diff.toMinutes() < 60 -> "${diff.toMinutes()}분 전"
                            diff.toHours() < 24 -> "${diff.toHours()}시간 전"
                            diff.toDays() < 6 -> "${diff.toDays()}일 전"
                            else -> {
                                val date = LocalDateTime.ofInstant(n.createdAt, ZoneId.systemDefault())
                                "${date.monthValue}월 ${date.dayOfMonth}일"
                            }
                        }
                    }

                    NotificationItem(
                        content = n.content,
                        timeText = timeText,
                        isRead = n.isRead,
                        avatarResId = n.avatarRes,
                        thumbnailResId = n.thumbnailRes,
                        onClick = {
                            // 알림 확인 상태 저장
                            if (!n.isRead) {
                                readNotificationIds = readNotificationIds + n.id
                                SharedReportData.saveNotificationReadState(context, n.id, true)
                            }
                            
                            // 피드백 또는 좋아요 알림인 경우 해당 제보로 이동
                            when (val content = n.content) {
                                is NotificationContent.Feedback -> {
                                    // Home 화면으로 이동하면서 제보 ID 전달
                                    coroutineScope.launch {
                                        // 먼저 알림 화면을 백스택에서 제거
                                        navController.popBackStack()
                                        
                                        // popBackStack 완료 대기
                                        delay(100)
                                        
                                        // Home으로 이동 (마이페이지도 제거)
                                        navController.navigate(MainTab.Home.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                        
                                        // navigate 완료 대기
                                        delay(300)
                                        
                                        // savedStateHandle에 제보 ID 설정
                                        try {
                                            val entry = navController.getBackStackEntry(MainTab.Home.route)
                                            entry.savedStateHandle.set("selected_report_id", content.reportId)
                                        } catch (e: Exception) {
                                            android.util.Log.e("NotificationsScreen", "Failed to set reportId", e)
                                            // 재시도
                                            delay(300)
                                            try {
                                                val entry = navController.getBackStackEntry(MainTab.Home.route)
                                                entry.savedStateHandle.set("selected_report_id", content.reportId)
                                            } catch (e2: Exception) {
                                                android.util.Log.e("NotificationsScreen", "Failed to set reportId on retry", e2)
                                            }
                                        }
                                    }
                                }
                                is NotificationContent.Saved -> {
                                    // Home 화면으로 이동하면서 제보 ID 전달
                                    coroutineScope.launch {
                                        // 먼저 알림 화면을 백스택에서 제거
                                        navController.popBackStack()
                                        
                                        // popBackStack 완료 대기
                                        delay(100)
                                        
                                        // Home으로 이동 (마이페이지도 제거)
                                        navController.navigate(MainTab.Home.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                        
                                        // navigate 완료 대기
                                        delay(300)
                                        
                                        // savedStateHandle에 제보 ID 설정
                                        try {
                                            val entry = navController.getBackStackEntry(MainTab.Home.route)
                                            entry.savedStateHandle.set("selected_report_id", content.reportId)
                                        } catch (e: Exception) {
                                            android.util.Log.e("NotificationsScreen", "Failed to set reportId", e)
                                            // 재시도
                                            delay(300)
                                            try {
                                                val entry = navController.getBackStackEntry(MainTab.Home.route)
                                                entry.savedStateHandle.set("selected_report_id", content.reportId)
                                            } catch (e2: Exception) {
                                                android.util.Log.e("NotificationsScreen", "Failed to set reportId on retry", e2)
                                            }
                                        }
                                    }
                                }
                                is NotificationContent.ExpiringReport -> {
                                    // 사라질 제보 화면으로 이동
                                    navController.navigate(ROUTE_EXPIRING_REPORT_DETAIL) {
                                        // 알림 화면을 백스택에서 제거
                                        popUpTo(ROUTE_NOTIFICATIONS) {
                                            inclusive = true
                                        }
                                    }
                                }
                                is NotificationContent.BadgeAcquired -> {
                                    // 마이페이지로 이동하면서 뱃지 정보 전달
                                    android.util.Log.d("NotificationsScreen", "BadgeAcquired clicked: ${content.badgeName}")
                                    coroutineScope.launch {
                                        android.util.Log.d("NotificationsScreen", "Navigating to My page")
                                        // 마이페이지로 이동 (알림 화면도 백스택에서 제거)
                                        navController.navigate(MainTab.My.route) {
                                            popUpTo(ROUTE_NOTIFICATIONS) {
                                                inclusive = true
                                            }
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                        
                                        // navigate 완료 대기
                                        delay(500)
                                        
                                        android.util.Log.d("NotificationsScreen", "Trying to get backStackEntry for ${MainTab.My.route}")
                                        // savedStateHandle에 뱃지 정보 설정
                                        try {
                                            val entry = navController.getBackStackEntry(MainTab.My.route)
                                            android.util.Log.d("NotificationsScreen", "Got backStackEntry, setting badge info: ${content.badgeName}, reports: ${content.totalCompletedReports}")
                                            entry.savedStateHandle.set("badge_name", content.badgeName)
                                            entry.savedStateHandle.set("total_completed_reports", content.totalCompletedReports)
                                            // 제보 타입별 통계 (실제 데이터 사용)
                                            entry.savedStateHandle.set("danger_count", reportStats.dangerCount)
                                            entry.savedStateHandle.set("inconvenience_count", reportStats.inconvenienceCount)
                                            entry.savedStateHandle.set("discovery_count", reportStats.discoveryCount)
                                            android.util.Log.d("NotificationsScreen", "Badge info set successfully")
                                        } catch (e: Exception) {
                                            android.util.Log.e("NotificationsScreen", "Failed to set badge info", e)
                                            // 재시도
                                            delay(500)
                                            try {
                                                val entry = navController.getBackStackEntry(MainTab.My.route)
                                                android.util.Log.d("NotificationsScreen", "Retrying to set badge info: ${content.badgeName}")
                                                entry.savedStateHandle.set("badge_name", content.badgeName)
                                                entry.savedStateHandle.set("total_completed_reports", content.totalCompletedReports)
                                                entry.savedStateHandle.set("danger_count", reportStats.dangerCount)
                                                entry.savedStateHandle.set("inconvenience_count", reportStats.inconvenienceCount)
                                                entry.savedStateHandle.set("discovery_count", reportStats.discoveryCount)
                                                android.util.Log.d("NotificationsScreen", "Badge info set successfully on retry")
                                            } catch (e2: Exception) {
                                                android.util.Log.e("NotificationsScreen", "Failed to set badge info on retry", e2)
                                            }
                                        }
                                    }
                                }
                                else -> {
                                    // 다른 알림 타입은 기본 동작 (읽음 처리만)
                                }
                            }
                        },
//                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun NotificationsScreenPreview() {
    FILLINTheme {
        val navController = rememberNavController()
        NotificationsScreen(navController = navController)
    }
}
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateListOf
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
import com.example.fillin.data.api.TokenManager
import com.example.fillin.data.repository.AlarmRepository
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.navigation.NavGraph.Companion.findStartDestination

private data class NotificationUiModel(
    val id: Long,
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
    val alarmRepository = remember(context) { AlarmRepository(context) }
    val isLoggedIn = TokenManager.getBearerToken(context) != null

    var isLoading by remember { mutableStateOf(true) }
    val notifications = remember { mutableStateListOf<NotificationUiModel>() }

    val reportStats = remember { SharedReportData.getReportStats() }
    val badgeName = remember { SharedReportData.getBadgeName() }

    LaunchedEffect(isLoggedIn) {
        if (!isLoggedIn) {
            notifications.clear()
            isLoading = false
            return@LaunchedEffect
        }
        isLoading = true
        alarmRepository.getAlarmList().onSuccess { response ->
            val list = response.data?.map { alarm ->
                val createdAt = runCatching {
                    Instant.parse(alarm.createdAt)
                }.getOrElse { now }
                NotificationUiModel(
                    id = alarm.alarmId,
                    content = NotificationContent.ApiMessage(
                        message = alarm.message,
                        referId = alarm.referId,
                        alarmType = alarm.alarmType
                    ),
                    createdAt = createdAt,
                    isRead = alarm.read,
                    avatarRes = R.drawable.ic_user_img,
                    thumbnailRes = null
                )
            } ?: emptyList()
            notifications.clear()
            notifications.addAll(list)
        }.onFailure {
            notifications.clear()
        }
        isLoading = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
    ) {
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

        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF4595E5))
                }
            }
            notifications.isEmpty() -> {
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
                        text = if (isLoggedIn) "아직 받은 알림이 없어요" else "로그인 후 알림을 확인할 수 있어요",
                        color = Color(0xFFAAADB3),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(notifications, key = { it.id }) { n ->
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
                                if (!n.isRead) {
                                    coroutineScope.launch {
                                        alarmRepository.markAlarmAsRead(n.id).onSuccess {
                                            val idx = notifications.indexOfFirst { it.id == n.id }
                                            if (idx >= 0) {
                                                val updated = notifications[idx].copy(isRead = true)
                                                notifications[idx] = updated
                                            }
                                        }
                                    }
                                }

                                when (val content = n.content) {
                                    is NotificationContent.ApiMessage -> {
                                        val reportId = content.referId
                                        when (content.alarmType) {
                                            "REPORT", "LIKE" -> {
                                                if (reportId != null) {
                                                    coroutineScope.launch {
                                                        navController.popBackStack()
                                                        delay(100)
                                                        navController.navigate(MainTab.Home.route) {
                                                            popUpTo(navController.graph.findStartDestination().id) {
                                                                saveState = true
                                                            }
                                                            launchSingleTop = true
                                                            restoreState = true
                                                        }
                                                        delay(300)
                                                        try {
                                                            val entry = navController.getBackStackEntry(MainTab.Home.route)
                                                            entry.savedStateHandle.set("selected_report_id", reportId)
                                                        } catch (_: Exception) {}
                                                    }
                                                }
                                            }
                                            "EXPIRATION" -> {
                                                navController.navigate(ROUTE_EXPIRING_REPORT_DETAIL) {
                                                    popUpTo(ROUTE_NOTIFICATIONS) { inclusive = true }
                                                }
                                            }
                                            "LEVEL_UP" -> {
                                                coroutineScope.launch {
                                                    navController.navigate(MainTab.My.route) {
                                                        popUpTo(ROUTE_NOTIFICATIONS) { inclusive = true }
                                                        popUpTo(navController.graph.findStartDestination().id) {
                                                            saveState = true
                                                        }
                                                        launchSingleTop = true
                                                        restoreState = true
                                                    }
                                                    delay(500)
                                                    try {
                                                        val entry = navController.getBackStackEntry(MainTab.My.route)
                                                        entry.savedStateHandle.set("badge_name", badgeName)
                                                        entry.savedStateHandle.set("total_completed_reports", reportStats.totalCount)
                                                        entry.savedStateHandle.set("danger_count", reportStats.dangerCount)
                                                        entry.savedStateHandle.set("inconvenience_count", reportStats.inconvenienceCount)
                                                        entry.savedStateHandle.set("discovery_count", reportStats.discoveryCount)
                                                    } catch (_: Exception) {}
                                                }
                                            }
                                            else -> { /* NOTICE 등 - 읽음 처리만 */ }
                                        }
                                    }
                                    else -> { /* 기존 타입 호환 */ }
                                }
                            }
                        )
                    }
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

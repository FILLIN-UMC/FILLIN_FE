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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.fillin.R
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController
import com.example.fillin.ui.theme.FILLINTheme
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

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
    val now = remember { Instant.now() }

    // 5가지 종류(피드백/저장/사라질제보/뱃지 획득/시스템) 예시 알림
    val sampleNotifications = remember(now) {
        listOf(
            NotificationUiModel(
                id = "sample_feedback",
                content = NotificationContent.Feedback(
                    actorName = "조치원고라니",
                    choice = FeedbackChoice.STILL_DANGEROUS
                ),
                createdAt = now.minus(5, ChronoUnit.MINUTES),
                isRead = false,
                avatarRes = R.drawable.ic_user_img,
                thumbnailRes = R.drawable.ic_report_img
            ),
            NotificationUiModel(
                id = "sample_saved",
                content = NotificationContent.Saved(
                    actorName = "조치원고라니"
                ),
                createdAt = now.minus(2, ChronoUnit.HOURS),
                isRead = true,
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
                isRead = false,
                avatarRes = R.drawable.ic_user_img,
                thumbnailRes = R.drawable.ic_report_img
            ),
            NotificationUiModel(
                id = "sample_badge",
                content = NotificationContent.BadgeAcquired(
                    badgeName = "루키",
                    totalCompletedReports = 9
                ),
                createdAt = now.minus(1, ChronoUnit.DAYS),
                isRead = false,
                avatarRes = R.drawable.ic_user_img,
                thumbnailRes = null
            ),
            NotificationUiModel(
                id = "sample_system",
                content = NotificationContent.SystemNotice(
                    title = "[공지] 서비스 업데이트 안내"
                ),
                createdAt = now.minus(20, ChronoUnit.DAYS),
                isRead = true,
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
                            // TODO: 클릭 시 읽음 처리/이동 등
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
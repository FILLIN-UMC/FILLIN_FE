package com.example.fillin.feature.notifications

import androidx.compose.ui.tooling.preview.Preview
import com.example.fillin.R
import com.example.fillin.ui.theme.FILLINTheme

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.text.Regex

/**
 * One row/card in the notifications list.
 * This file should NOT contain NotificationsScreen or demo data models.
 */

enum class FeedbackChoice {
    STILL_DANGEROUS,
    NOW_SAFE
}

sealed class NotificationContent {
    data class Feedback(
        val actorName: String,
        val choice: FeedbackChoice
    ) : NotificationContent()

    /**
     * 2. 내가 아닌 제3자가 나의 제보에 좋아요(저장)한 경우
     */
    data class Saved(
        val actorName: String
    ) : NotificationContent()

    /**
     * 3. 사라질 제보 알림
     */
    data class ExpiringReport(
        val daysLeft: Int,
        val summaryText: String? = null // e.g. "위험1, 발견2"
    ) : NotificationContent()

    /**
     * 4. 뱃지 획득 알림 (썸네일 없음)
     */
    data class BadgeAcquired(
        val badgeName: String,
        val totalCompletedReports: Int
    ) : NotificationContent()

    /**
     * 5. 시스템 알림 (썸네일 없음)
     */
    data class SystemNotice(
        val title: String
    ) : NotificationContent()
}

@Composable
fun NotificationItem(
    content: NotificationContent,
    timeText: String,
    isRead: Boolean,
    avatarResId: Int,
    thumbnailResId: Int?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = if (!isRead) Color(0xFFE7EBF2) else Color.Transparent

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 104.dp)
            .clickable(onClick = onClick),
        color = bg
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left avatar
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE5E7EB)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = avatarResId),
                    contentDescription = "프로필",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(Modifier.width(16.dp))

            // Middle text
            Column(
                modifier = Modifier.weight(1f)
            ) {
                val messageText = remember(content) {
                    buildAnnotatedString {
                        when (content) {
                            is NotificationContent.Feedback -> {
                                append("${content.actorName}님이 회원님의 제보에\n")
                                val keyword = when (content.choice) {
                                    FeedbackChoice.STILL_DANGEROUS -> "아직 위험해요"
                                    FeedbackChoice.NOW_SAFE -> "이제 괜찮아요"
                                }
                                append("‘")
                                withStyle(SpanStyle(fontWeight = FontWeight.ExtraBold)) {
                                    append(keyword)
                                }
                                append("’ 피드백을 남겼어요!")
                            }

                            is NotificationContent.Saved -> {
                                append("${content.actorName}님이 회원님의 제보를\n")
                                append("저장했어요.")
                            }

                            is NotificationContent.ExpiringReport -> {
                                append("내 제보가 ${content.daysLeft}일 뒤 사라져요")
                                content.summaryText?.let {
                                    append("\n")
                                    // Bold keyword(위험/불편/발견) and its count if present
                                    val src = it
                                    val regex = Regex("""(위험|불편|발견)(\d+)?""")
                                    var last = 0
                                    for (m in regex.findAll(src)) {
                                        val range = m.range
                                        if (range.first > last) {
                                            append(src.substring(last, range.first))
                                        }
                                        withStyle(SpanStyle(fontWeight = FontWeight.ExtraBold)) {
                                            append(m.groupValues[1])
                                        }
                                        val count = m.groupValues.getOrNull(2)
                                        if (!count.isNullOrEmpty()) {
                                            withStyle(SpanStyle(fontWeight = FontWeight.ExtraBold)) {
                                                append(count)
                                            }
                                        }
                                        withStyle(SpanStyle(fontWeight = FontWeight.ExtraBold)) {
                                            // no-op placeholder if count missing
                                        }
                                        last = range.last + 1
                                    }
                                    if (last < src.length) {
                                        append(src.substring(last))
                                    }
                                }
                            }

                            is NotificationContent.BadgeAcquired -> {
                                withStyle(SpanStyle(fontWeight = FontWeight.ExtraBold)) {
                                    append("${content.badgeName} 뱃지를 획득했어요!")
                                }
                                append("\n")
                                append("총 ${content.totalCompletedReports}개의 제보를 완료했어요")
                            }

                            is NotificationContent.SystemNotice -> {
                                append(content.title)
                            }
                        }
                    }
                }
                Text(
                    text = messageText,
                    color = Color(0xFF252526),
                    fontSize = 16.sp,
                    lineHeight = 22.sp,
                    maxLines = Int.MAX_VALUE,
                    overflow = TextOverflow.Clip
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = timeText,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFAAADB3),
                    fontSize = 14.sp,
                    lineHeight = 14.sp
                )
            }

            Spacer(Modifier.width(16.dp))

            // Right thumbnail
            if (thumbnailResId != null) {
            Image(
                painter = painterResource(id = thumbnailResId),
                contentDescription = "알림 썸네일",
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
            } else {
                Spacer(
                    modifier = Modifier
                        .size(56.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "NotificationItem Preview")
@Composable
private fun NotificationItemPreview() {
    FILLINTheme {
        NotificationItem(
            content = NotificationContent.Feedback(
                actorName = "조치원고라니",
                choice = FeedbackChoice.STILL_DANGEROUS
            ),
            timeText = "5분 전",
            isRead = false,
            avatarResId = R.drawable.ic_profile_img,
            thumbnailResId = R.drawable.ic_report_img,
            onClick = {}
        )
    }
}
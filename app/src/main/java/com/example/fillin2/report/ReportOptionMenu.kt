package com.example.fillin2.report

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.History // 지난 상황용 아이콘 예시
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun ReportOptionMenu(
    modifier: Modifier = Modifier,
    onPastReportClick: () -> Unit,
    onRealtimeReportClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .width(200.dp)
            .shadow(12.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        color = Color.White
    ) {
        Column {
            // 지난 상황 제보
            ReportMenuItem(
                icon = Icons.Outlined.History,
                label = "지난 상황 제보",
                onClick = onPastReportClick
            )

            HorizontalDivider(color = Color(0xFFF5F5F5))

            // 실시간 제보
            ReportMenuItem(
                icon = Icons.Outlined.Campaign,
                label = "실시간 제보",
                onClick = onRealtimeReportClick
            )
        }
    }
}

@Composable
fun ReportMenuItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF4090E0), // 이미지의 포인트 블루 색상
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF333333)
        )
    }
}
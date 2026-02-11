package com.example.fillin.feature.report.locationselect

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun CenterPin() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // '제보' 문구가 적힌 파란색 박스
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF4090E0),
            shadowElevation = 4.dp
        ) {
            Text(
                text = "제보",
                color = Color.White,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }
        // 아래로 뻗은 선 (핀의 꼭짓점 역할)
        Box(
            modifier = Modifier
                .width(2.dp)
                .height(12.dp)
                .background(Color(0xFF4090E0))
        )
        // 바닥의 작은 점
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(Color(0xFF4090E0))
        )
    }
}

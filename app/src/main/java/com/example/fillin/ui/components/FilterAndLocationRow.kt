package com.example.fillin.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun FilterAndLocationRow(
    modifier: Modifier = Modifier,
    onLocationClick: () -> Unit
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 필터 칩 (현재는 비활성화 상태로 표시)
        FilterChip(
            selected = false,
            onClick = { /* TODO: 필터 기능 구현 */ },
            label = { Text("전체") },
            modifier = Modifier.height(32.dp)
        )

        // 내 위치 버튼
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(Color.White, CircleShape)
                .clickable(onClick = onLocationClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = "내 위치",
                tint = Color(0xFF4090E0),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

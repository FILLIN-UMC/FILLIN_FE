package com.example.fillin2.search


import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fillin2.kakao.Place

@Composable
fun RouteSelectionScreen(
    startPlace: Place?, // 출발지로 선택된 장소 (없으면 null)
    endPlace: Place?,   // 도착지로 선택된 장소 (없으면 null)
    onBackClick: () -> Unit,
    onSearchFieldClick: (Boolean) -> Unit // true면 출발지 검색, false면 도착지 검색으로 이동
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
    ) {
        // [상단 입력 영역]
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(Icons.Default.ArrowBackIosNew, contentDescription = "뒤로가기", modifier = Modifier.size(20.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                // 1. 출발지 입력 칸
                RouteInputBox(
                    label = startPlace?.place_name ?: "출발지를 입력해주세요",
                    isSelected = startPlace != null,
                    circleColor = Color(0xFFDCEBFF), // 연한 파랑
                    onClick = { onSearchFieldClick(true) }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 2. 도착지 입력 칸
                RouteInputBox(
                    label = endPlace?.place_name ?: "도착지를 입력해주세요",
                    isSelected = endPlace != null,
                    circleColor = Color(0xFF4090E0), // 진한 파랑
                    onClick = { onSearchFieldClick(false) }
                )
            }
        }

        Divider(thickness = 1.dp, color = Color(0xFFF2F4F7))

        // [하단 추천/최근 장소 리스트]
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            // 여기에 검색 결과나 최근 기록 아이템 배치
            // items(...) { ... }
        }
    }
}

@Composable
fun RouteInputBox(
    label: String,
    isSelected: Boolean,
    circleColor: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(26.dp),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE7EBF2))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 왼쪽 동그라미 아이콘
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(circleColor, RoundedCornerShape(999.dp))
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = label,
                color = if (isSelected) Color.Black else Color(0xFFAAADB3),
                fontSize = 16.sp
            )
        }
    }
}
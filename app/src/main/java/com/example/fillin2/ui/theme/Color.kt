package com.example.fillin2.ui.theme

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

// 이미지 참고: 중앙의 메인 색상 (main)
val PointMain = Color(0xFF4595E5)

// 이미지 참고: 바깥쪽의 서브 색상 (sub)
val PointSub = Color(0xFFD7E8F9)

// 중앙(main)에서 바깥(sub)으로 퍼지는 원형 그라데이션 브러시
val PointGradientBrush = Brush.radialGradient(
    colorStops = arrayOf(
        0.0f to PointSub,      // 위쪽 밝은 영역
        0.35f to PointMain,   // 중앙 메인 블루
        0.75f to PointMain,   // 메인 색 유지
        1.0f to PointSub      // 가장자리 살짝 밝게
    ),
    // 🔴 핵심: 중심을 위쪽으로
    center = Offset(0.5f, 0.25f),
    // 🔴 핵심: 버튼 크기에 맞는 반지름
    radius = 220f
)
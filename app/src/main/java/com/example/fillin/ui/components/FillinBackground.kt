package com.example.fillin.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

@Composable
fun FillinBlueGradientBackground(content: @Composable () -> Unit) {
    val bg = Brush.radialGradient(
        colors = listOf(
            Color(0xFFBFE7FF), // 가장자리 밝은 하늘색
            Color(0xFF4FA6E6)  // 중앙쪽 진한 파랑
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
    ) {
        content()
    }
}

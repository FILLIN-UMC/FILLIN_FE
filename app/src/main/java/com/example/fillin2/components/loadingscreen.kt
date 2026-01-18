package com.example.fillin2.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fillin2.R
// 로딩 화면
@Composable
fun AiLoadingOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            shape = RoundedCornerShape(24.dp),
            shadowElevation = 12.dp
        ) {
            Column(
                modifier = Modifier
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF6BA4F8),
                                Color(0xFF3178D6)
                            )
                        )
                    )
                    // 전체 높이를 충분히 줘서 "상단 / 중앙 / 하단" 구조 만들기
                    .padding(horizontal = 24.dp)
                    .height(420.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                /* ---------- 상단 : 로고 ---------- */
                Spacer(modifier = Modifier.height(32.dp))

                Image(
                    painter = painterResource(id = R.drawable.fillin_logo),
                    contentDescription = "FILLIN Logo",
                    modifier = Modifier
                        .fillMaxWidth(0.55f)
                        .height(42.dp),
                    contentScale = ContentScale.Fit
                )

                /* ---------- 중앙 : 텍스트 ---------- */
                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = "분석이 다 됐어요!\n열심히 작성하고 있어요.",
                    color = Color.White,
                    fontSize = 20.sp,              // 🔥 텍스트 크기 키움
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    lineHeight = 28.sp             // 줄 간격도 같이 키워서 시원하게
                )

                Spacer(modifier = Modifier.weight(1f))

                /* ---------- 하단 : 프로그레스 ---------- */
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp),
                    color = Color.White,
                    trackColor = Color.White.copy(alpha = 0.3f),
                    strokeCap = StrokeCap.Round
                )

                Spacer(modifier = Modifier.height(28.dp))
            }
        }
    }
}
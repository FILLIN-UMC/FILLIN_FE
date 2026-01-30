package com.example.fillin.ui.components


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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fillin.R
import kotlin.math.sqrt

// 로딩 화면 (isUploading == true 이면 "제보 등록 중" 문구 표시)
@Preview
@Composable
fun AiLoadingOverlay(isUploading: Boolean = false) {
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
            shadowElevation = 12.dp,
            // Surface 자체의 배경색을 투명하게 해서 그라데이션이 잘 보이게 합니다.
            color = Color.Transparent
        ) {
            // ✅ 수정한 부분: fillMaxSize() 대신 Column의 크기에 맞춤
            FillinBlueGradientBackgroundd(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(420.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
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
                        text = if (isUploading) "제보를 등록하고 있어요!" else "AI가 제보 사진을\n분석하고 있어요!",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        lineHeight = 28.sp
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
}

@Composable
fun FillinBlueGradientBackgroundd(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    // 1. 화면 크기 정보를 가져옵니다.
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current

    // 2. 화면의 대각선 길이를 계산하여 충분히 큰 반지름을 만듭니다.
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
    // 대각선 길이의 약 80% 정도로 설정 (값을 조절하여 범위를 변경할 수 있습니다)
    val radius = sqrt((screenWidthPx * screenWidthPx + screenHeightPx * screenHeightPx).toDouble()).toFloat() * 0.65f

    val bg = Brush.radialGradient(
        colors = listOf(
            Color(0xFF4FA6E6),
            Color(0xFFA6DEFF) // 중앙 (밝은 하늘색)
             // 외곽 (진한 파랑)
        ),
        // 3. 계산한 반지름을 적용합니다.
        radius = radius,
        // 중심점은 기본값인 Center를 유지합니다.
        // center = Offset.Unspecified
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bg)
    ) {
        content()
    }
}
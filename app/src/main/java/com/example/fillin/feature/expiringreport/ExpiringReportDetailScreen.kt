
package com.example.fillin.feature.expiringreport

import android.app.Activity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.fillin.R
import com.example.fillin.ui.theme.FILLINTheme
import androidx.compose.ui.platform.LocalInspectionMode

@Preview(showBackground = true, name = "ExpiringReportDetail")
@Composable
private fun ExpiringReportDetailPreview() {
    FILLINTheme {
        ExpiringReportDetailScreen(navController = rememberNavController())
    }
}

@Composable
fun ExpiringReportDetailScreen(navController: NavController) {
    val backgroundColor = Color(0xFFF7FBFF)

    val view = LocalView.current
    val isPreview = LocalInspectionMode.current
    if (!isPreview) {
        DisposableEffect(view, backgroundColor) {
            val window = (view.context as Activity).window

            // 백업
            val prevStatusBarColor = window.statusBarColor
            val prevNavBarColor = window.navigationBarColor

            // 이 화면에서만 상태바 영역까지 배경이 보이도록 edge-to-edge 적용
            androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = backgroundColor.toArgb()

            val controller = WindowInsetsControllerCompat(window, view)
            controller.isAppearanceLightStatusBars = true
            controller.isAppearanceLightNavigationBars = true

            onDispose {
                // 다른 화면에 영향 없도록 원복
                androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, true)
                window.statusBarColor = prevStatusBarColor
                window.navigationBarColor = prevNavBarColor
            }
        }
    }


    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
        // Top bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(56.dp)
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
                Image(
                    painter = painterResource(id = R.drawable.ic_back_btn),
                    contentDescription = "뒤로가기",
                    modifier = Modifier.size(48.dp)
                )
            }

            Text(
                text = "사라질 제보",
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF111827),
                fontSize = 20.sp,
                lineHeight = 20.sp,
                modifier = Modifier.align(Alignment.Center)
            )
        }
        // Example data (later replace with backend-connected data)
        val expiringReports = remember {
            listOf(
                ExpiringReportUi(
                    warningText = "오래된 제보일 수 있어요",
                    imageRes = R.drawable.ic_report_img,
                    views = 5,
                    typeLabel = "위험",
                    typeColor = Color(0xFFFF6060),
                    userName = "조치원 고라니",
                    userBadge = "루키",
                    title = "맨홀 뚜껑 역류",
                    createdLabel = "5일 전",
                    address = "행복길 1239-11",
                    distance = "가는 길 20m",
                    okCount = 6,
                    dangerCount = 2,
                    isLiked = true
                ),
                ExpiringReportUi(
                    warningText = "오래된 제보일 수 있어요",
                    imageRes = R.drawable.ic_report_img,
                    views = 12,
                    typeLabel = "불편",
                    typeColor = Color(0xFF4595E5),
                    userName = "조치원 고라니",
                    userBadge = "베테랑",
                    title = "인도 블록 파손",
                    createdLabel = "7일 전",
                    address = "행복길 122-11",
                    distance = "가는 길 255m",
                    okCount = 3,
                    dangerCount = 4,
                    isLiked = false
                )
            )
        }

        Spacer(Modifier.height(10.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 0.dp, end = 0.dp, top = 0.dp, bottom = 24.dp)
        ) {
            // Headline
            item {
                Text(
                    text = buildAnnotatedString {
                        append("총 ")
                        withStyle(
                            SpanStyle(
                                color = Color(0xFF4595E5),
                                fontWeight = FontWeight.ExtraBold
                            )
                        ) { append("20명") }
                        append("에게 도움이 되었어요!")
                    },
                    color = Color(0xFF252526),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 24.sp,
                    lineHeight = 24.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(top = 4.dp)
                )

                Spacer(Modifier.height(12.dp))
            }

            // Cards
            items(expiringReports) { report ->
                ExpiringReportCard(
                    report = report,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )
                Spacer(Modifier.height(32.dp))
            }
        }
        }
    }
}

data class ExpiringReportUi(
    val warningText: String,
    val imageRes: Int,
    val views: Int,
    val typeLabel: String,
    val typeColor: Color,
    val userName: String,
    val userBadge: String,
    val title: String,
    val createdLabel: String,
    val address: String,
    val distance: String,
    val okCount: Int,
    val dangerCount: Int,
    val isLiked: Boolean
)

@Composable
private fun ExpiringReportCard(
    report: ExpiringReportUi,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        shadowElevation = 10.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp)
        ) {
            // 카드 내부 상단에 포함된 "오래된 제보일 수 있어요" 배너
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = Color(0xFFF7FBFF),
                    border = BorderStroke(1.dp, Color(0xFFE7EBF2)),
                    shadowElevation = 0.dp
                ) {
                    Text(
                        text = report.warningText,
                        color = Color(0xFF555659),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        lineHeight = 14.sp,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(16.dp))
            ) {
                Image(
                    painter = painterResource(id = report.imageRes),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // top dark overlay
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .align(Alignment.TopCenter)
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Black.copy(alpha = 0.45f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                // bottom dark overlay
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.55f)
                                )
                            )
                        )
                )

                // top-left views
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_view),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = report.views.toString(),
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp,
                        lineHeight = 12.sp
                    )
                }

                // top-right badge
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp),
                    shape = RoundedCornerShape(999.dp),
                    color = report.typeColor
                ) {
                    Text(
                        text = report.typeLabel,
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp,
                        lineHeight = 12.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }

                // bottom-left mini profile + badge
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE5E7EB)),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_user_img),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Spacer(Modifier.width(6.dp))

                    Text(
                        text = report.userName,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 12.sp
                    )

                    Spacer(Modifier.width(6.dp))

                    TagChip(text = report.userBadge)
                }
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text = report.title,
                color = Color(0xFF252526),
                fontWeight = FontWeight.ExtraBold,
                fontSize = 22.sp,
                lineHeight = 22.sp,
                modifier = Modifier.padding(horizontal = 20.dp)
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = report.createdLabel,
                color = Color(0xFFAAADB3),
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                lineHeight = 12.sp,
                modifier = Modifier.padding(horizontal = 20.dp)
            )

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 위치 아이콘
                    Image(
                        painter = painterResource(id = R.drawable.ic_location),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))

                    // 주소
                    Text(
                        text = report.address,
                        color = Color(0xFF555659),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    // 거리(주소 옆에 붙는 텍스트)
                    Text(
                        text = report.distance,
                        color = Color(0xFF86878C),
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        lineHeight = 14.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF7FBFF)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_like),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(
                            if (report.isLiked) Color(0xFF4595E5) else Color(0xFFAAADB3)
                        )
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            HorizontalDivider(
                color = Color(0xFFE7EBF2),
                thickness = 1.dp,
                modifier = Modifier.padding(horizontal = 14.dp)
            )
            Spacer(Modifier.height(20.dp))

            Text(
                text = "지금도 조심해야 할 상황인가요?",
                color = Color(0xFF252526),
                fontWeight = FontWeight.ExtraBold,
                fontSize = 14.sp,
                lineHeight = 14.sp,
                modifier = Modifier
                    .padding(horizontal = 14.dp)
                    .align(Alignment.CenterHorizontally)
            )

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(all = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(51.dp),
                    shape = RoundedCornerShape(32.dp),
                    color = Color.White,
                    border = BorderStroke(2.dp, Color(0xFF4595E5))
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "이제 괜찮아요 ${report.okCount}",
                            color = Color(0xFF4595E5),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            lineHeight = 16.sp
                        )
                    }
                }

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(51.dp),
                    shape = RoundedCornerShape(32.dp),
                    color = Color.White,
                    border = BorderStroke(2.dp, Color(0xFFFF6060))
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "아직 위험해요 ${report.dangerCount}",
                            color = Color(0xFFFF6060),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TagChip(
    text: String,
    modifier: Modifier = Modifier
) {
    val badgeColor = Color(0xFF4595E5)

    Box(
        modifier = modifier
            .height(24.dp) // ✅ 높이만 고정
            .clip(RoundedCornerShape(6.dp))
            .background(Color.White)
            .border(
                BorderStroke(1.dp, badgeColor),
                shape = RoundedCornerShape(6.dp)
            )
            .padding(horizontal = 8.dp), // ✅ 텍스트 길이에 따라 폭 결정
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            lineHeight = 12.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            color = badgeColor,
            textAlign = TextAlign.Center
        )
    }
}


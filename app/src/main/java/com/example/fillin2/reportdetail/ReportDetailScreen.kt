package com.example.fillin2.reportdetail

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex

@Composable
fun ReportDetailScreen(
    data: ReportDetailData,
    onLikeClick: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    // 카테고리별 테마 색상 설정
    val themeColor = when(data.category) {
        ReportCategory.DANGER -> Color(0xFFE57373)
        ReportCategory.INCONVENIENCE -> Color(0xFFFFB74D)
        ReportCategory.DISCOVERY -> Color(0xFF4DB6AC)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f)) // 배경 딤 처리
            .clickable { onDismiss() }, // 외부 클릭 시 닫기
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .clickable(enabled = false) { }, // 부모(Box)의 클릭 이벤트 차단, 카드 내부 클릭해도 닫히지 않게 함
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. 정보 유효성 상태 배지 (카드 상단에 걸쳐짐)
            Surface(
                // offset(y=12.dp) : 카드 위로 살짝 이동, zIndex(1f) : 카드보다 앞에 보이게
                modifier = Modifier.offset(y = 12.dp).zIndex(1f),
                shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
                color = Color.White,
                shadowElevation = 2.dp
            ) {
                Text(
                    text = data.validityStatus,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.DarkGray
                )
            }

            // [메인 카드]
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(32.dp),
                color = Color.White,
                shadowElevation = 8.dp
            ) {
                Column {
                    // 2. 이미지 영역
                    Box(modifier = Modifier.fillMaxWidth().height(300.dp).padding(12.dp)) {
                        // 실제 구현 시 Coil로 data.imageUrl 로드
                        Surface(modifier = Modifier.fillMaxSize(), shape = RoundedCornerShape(24.dp), color = Color.LightGray) {
                            // 임시 샘플 이미지 (drawable에 이미지가 있어야 함)
                            // Image(painter = painterResource(id = R.drawable.sample), contentScale = ContentScale.Crop)
                        }

                        // 조회수 (상단 좌측)
                        Row(
                            modifier = Modifier.align(Alignment.TopStart).padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Visibility, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("${data.viewCount}", color = Color.White, fontSize = 12.sp)
                        }

                        // 카테고리 배지 (상단 우측)
                        Surface(
                            modifier = Modifier.align(Alignment.TopEnd).padding(12.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = themeColor
                        ) {
                            Text(data.category.displayName, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }

                        // 제보자 정보 (하단 좌측)
                        Row(
                            modifier = Modifier.align(Alignment.BottomStart).padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(modifier = Modifier.size(24.dp), shape = CircleShape, color = Color.Gray) {} // 프로필 자리
                            Spacer(Modifier.width(8.dp))
                            Text(data.reporterName, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold) // 제보자 이름
                            Spacer(Modifier.width(6.dp))
                            Surface(color = Color(0xFF4090E0), shape = RoundedCornerShape(4.dp)) {
                                Text(data.reporterLevel, modifier = Modifier.padding(horizontal = 6.dp), color = Color.White, fontSize = 10.sp)
                            }
                        }
                    }

                    // 3. 텍스트 정보 영역
                    Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text(data.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                                Text(data.timeAgo, style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                            }
                            // 12. 좋아요(저장) 버튼
                            IconButton(
                                onClick = { onLikeClick(!data.isLiked) },
                                modifier = Modifier.size(20.dp).background(Color(0xFFF2F2F7), CircleShape)
                            ) {
                                Icon(
                                    imageVector = if (data.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = null,
                                    tint = if (data.isLiked) Color.Red else Color.LightGray,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        // 5. 위치 정보
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Gray)
                            Spacer(Modifier.width(4.dp))
                            Text(data.address, style = MaterialTheme.typography.bodyMedium, color = Color.DarkGray)
                        }

                        Spacer(Modifier.height(20.dp))
                        HorizontalDivider(color = Color(0xFFF5F5F5))
                        Spacer(Modifier.height(20.dp))

                        // 7. 피드백 텍스트
                        Text(data.feedbackQuestion, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, fontSize = 15.sp)

                        Spacer(Modifier.height(16.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            // 부정 버튼 (좌측)
                            FeedbackButton(
                                text = "${data.negativeLabel} ${data.negativeCount}",
                                color = Color(0xFF4090E0),
                                modifier = Modifier.weight(1f),
                                onClick = { /* 부정 피드백 API 호출 */ }
                            )
                            // 긍정 버튼 (우측)
                            FeedbackButton(
                                text = "${data.positiveLabel} ${data.positiveCount}",
                                color = themeColor,
                                modifier = Modifier.weight(1f),
                                onClick = { /* 긍정 피드백 API 호출 */ }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // 13. 닫기 버튼
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(56.dp).background(Color.White, CircleShape).shadow(4.dp, CircleShape)
            ) {
                Icon(Icons.Default.Close, contentDescription = null, tint = Color.Gray)
            }
        }
    }
}

@Composable
fun FeedbackButton(text: String, color: Color, modifier: Modifier, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        border = BorderStroke(1.dp, color),
        shape = RoundedCornerShape(24.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = color)
    ) {
        Text(text, fontWeight = FontWeight.Bold)
    }
}


@Preview(showBackground = true)
@Composable
fun ReportDetailPreview() {
    // Preview용 가상 데이터 생성
    val previewData = ReportDetailData(
        imageUrl = "",
        category = ReportCategory.DANGER,
        title = "맨홀 뚜껑 역류",
        registrationDate = System.currentTimeMillis() - (1000L * 60 * 60 * 24 * 5), // 5일 전
        address = "행복길 1239-11 가는 길 20m",
        positiveCount = 2,
        negativeCount = 6,
        viewCount = 5,
        reporterName = "초치원 고리나",
        reporterLevel = "루키",
        isLiked = false
    )

    ReportDetailScreen(
        data = previewData,
        onLikeClick = {},
        onDismiss = {}
    )
}
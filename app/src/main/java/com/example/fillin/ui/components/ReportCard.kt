package com.example.fillin.ui.components

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fillin.R

data class ReportCardUi(
    val reportId: Long, // 제보 ID 추가
    val validityStatus: ValidityStatus, // 유효성 상태
    val imageRes: Int,
    val imageUrl: String? = null, // URL 이미지 (등록 제보 등)
    val imageUri: Uri? = null, // 로컬 이미지 (API 등록 직후 배너 표시용)
    val views: Int,
    val typeLabel: String,
    val typeColor: Color,
    val userName: String,
    val userBadge: String,
    val profileImageUrl: String? = null, // 작성자 프로필 이미지 URL
    val title: String,
    val createdLabel: String,
    val address: String,
    val distance: String,
    val okCount: Int,
    val dangerCount: Int,
    val isLiked: Boolean
)

enum class ValidityStatus(val text: String, val textColor: Color) {
    VALID("최근에도 확인됐어요", Color(0xFF29C488)), // 현재 유효
    INTERMEDIATE("제보 의견이 나뉘어요", Color(0xFFF5C72F)), // 중간 상태
    INVALID("오래된 제보일 수 있어요", Color(0xFF555659)) // 안 유효
}

@Composable
fun ReportCard(
    report: ReportCardUi,
    modifier: Modifier = Modifier,
    selectedFeedback: String? = null, // "positive" | "negative" | null
    isLiked: Boolean = report.isLiked, // 좋아요 상태 (기본값은 report.isLiked)
    feedbackButtonsEnabled: Boolean = true, // false면 피드백 버튼 비활성(사라질 제보 화면 등)
    onPositiveFeedback: () -> Unit = {},
    onNegativeFeedback: () -> Unit = {},
    onLikeToggle: () -> Unit = {}
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
            // 카드 내부 상단에 포함된 유효성 상태 배너
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
                        text = report.validityStatus.text,
                        color = report.validityStatus.textColor,
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
                when {
                    report.imageUri != null -> {
                        coil.compose.AsyncImage(
                            model = report.imageUri,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    !report.imageUrl.isNullOrBlank() -> {
                        coil.compose.AsyncImage(
                            model = report.imageUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    else -> {
                        Image(
                            painter = painterResource(id = report.imageRes),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

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
                        if (!report.profileImageUrl.isNullOrBlank()) {
                            coil.compose.AsyncImage(
                                model = report.profileImageUrl,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Image(
                                painter = painterResource(id = R.drawable.ic_user_img),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
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
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF7FBFF))
                        .clickable { onLikeToggle() },
                    contentAlignment = Alignment.Center
                ) {
                    if (isLiked) {
                        // 그라데이션 적용된 아이콘 (방사형)
                        Image(
                            painter = painterResource(id = R.drawable.ic_like),
                            contentDescription = null,
                            modifier = Modifier
                                .size(20.dp)
                                .drawWithContent {
                                    drawIntoCanvas { canvas ->
                                        canvas.nativeCanvas.saveLayer(
                                            null,
                                            null
                                        )
                                        drawContent()
                                        val centerX = size.width / 2f
                                        val centerY = size.height / 2f
                                        val radius = kotlin.math.max(size.width, size.height) / 2f
                                        val paint = android.graphics.Paint().apply {
                                            shader = android.graphics.RadialGradient(
                                                centerX, centerY, radius,
                                                intArrayOf(
                                                    Color(0xFF4595E5).toArgb(), // 더 진한 파란색 (중앙)
                                                    Color(0xFF87CEEB).toArgb()  // 밝은 파스텔 블루 (바깥쪽)
                                                ),
                                                null,
                                                android.graphics.Shader.TileMode.CLAMP
                                            )
                                            blendMode = android.graphics.BlendMode.SRC_IN
                                        }
                                        canvas.nativeCanvas.drawRect(
                                            0f, 0f, size.width, size.height,
                                            paint
                                        )
                                        canvas.nativeCanvas.restore()
                                    }
                                }
                        )
                    } else {
                        // 일반 아이콘
                        Image(
                            painter = painterResource(id = R.drawable.ic_like),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(
                                Color(0xFFAAADB3)
                            )
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            HorizontalDivider(
                color = Color(0xFFE7EBF2),
                thickness = 1.dp,
                modifier = Modifier.padding(horizontal = 14.dp)
            )
            Spacer(Modifier.height(20.dp))

            // 제보 타입에 따른 질문 및 피드백 버튼 텍스트 결정
            val questionText = when (report.typeLabel) {
                "위험" -> "지금도 조심해야할 상황인가요?"
                "불편" -> "아직 불편한 상황인가요?"
                "발견" -> "지금도 참고할 만한 정보인가요?"
                else -> "지금도 조심해야 할 상황인가요?"
            }
            
            val (positiveButtonText, negativeButtonText, negativeButtonColor) = when (report.typeLabel) {
                "위험" -> Triple("아직 위험해요", "이제 괜찮아요", Color(0xFFFF6060))
                "불편" -> Triple("아직 불편해요", "해결됐어요", Color(0xFFF5C72F))
                "발견" -> Triple("지금도 있어요", "이제 없어요", Color(0xFF29C488))
                else -> Triple("이제 괜찮아요", "아직 위험해요", Color(0xFFFF6060))
            }

            Text(
                text = questionText,
                color = Color(0xFF252526),
                fontWeight = FontWeight.ExtraBold,
                fontSize = 14.sp,
                lineHeight = 14.sp,
                modifier = Modifier
                    .padding(horizontal = 14.dp)
                    .align(Alignment.CenterHorizontally)
            )

            // 피드백 버튼: feedbackButtonsEnabled == false 이면 비활성 + 흰 글씨 + 각 색상 채움
            val positiveFillColor = Color(0xFF5D9BE9)   // 이미지와 동일한 파란색
            val negativeFillColor = Color(0xFFF0665E)   // 이미지와 동일한 코랄 레드
            val feedbackTextColor = if (feedbackButtonsEnabled) null else Color(0xFFFFFFFF) // 비활성 시 FFFFFF

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(all = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val isPositiveSelected = selectedFeedback == "positive"
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(51.dp)
                        .then(
                            if (feedbackButtonsEnabled) Modifier.clickable { onPositiveFeedback() }
                            else Modifier
                        ),
                    shape = RoundedCornerShape(32.dp),
                    color = when {
                        !feedbackButtonsEnabled -> positiveFillColor
                        isPositiveSelected -> Color(0xFF4595E5)
                        else -> Color.White
                    },
                    border = if (feedbackButtonsEnabled) BorderStroke(2.dp, Color(0xFF4595E5)) else null
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$positiveButtonText ${report.okCount}",
                            color = feedbackTextColor ?: if (isPositiveSelected) Color.White else Color(0xFF4595E5),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            lineHeight = 16.sp
                        )
                    }
                }

                val isNegativeSelected = selectedFeedback == "negative"
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(51.dp)
                        .then(
                            if (feedbackButtonsEnabled) Modifier.clickable { onNegativeFeedback() }
                            else Modifier
                        ),
                    shape = RoundedCornerShape(32.dp),
                    color = when {
                        !feedbackButtonsEnabled -> negativeFillColor
                        isNegativeSelected -> negativeButtonColor
                        else -> Color.White
                    },
                    border = if (feedbackButtonsEnabled) BorderStroke(2.dp, negativeButtonColor) else null
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$negativeButtonText ${report.dangerCount}",
                            color = feedbackTextColor ?: if (isNegativeSelected) Color.White else negativeButtonColor,
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

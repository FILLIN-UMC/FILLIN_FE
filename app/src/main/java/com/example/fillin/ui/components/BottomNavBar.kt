package com.example.fillin.ui.components

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.fillin.ui.theme.FILLINTheme
import kotlin.math.roundToInt

@Composable
fun BottomNavBar(
    selectedRoute: String?,
    home: TabSpec,
    report: TabSpec,
    my: TabSpec,
    onTabClick: (String) -> Unit,
    onReportClick: () -> Unit,
    enableDragToHide: Boolean = false
) {
    var barHeightPx by remember { mutableFloatStateOf(0f) }
    var offsetYPx by remember { mutableFloatStateOf(0f) }

    val density = LocalDensity.current

    // ⭐ 바깥에서 scale을 구해 height를 제어해야 bottomBar 예약공간이 줄어듦
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val scale = maxWidth / 380.dp

        val expandedHeight = 162.dp * scale     // 원래 바 높이
        val handleHeight = 18.dp * scale        // 숨김 상태에서도 남겨둘 잡는 영역(검색 텍스트가 보이지 않게 더 얇게)
        val handleVisiblePx = with(density) { handleHeight.toPx() }

        val maxOffsetPx = (barHeightPx - handleVisiblePx).coerceAtLeast(0f)
        fun clamp(v: Float) = v.coerceIn(0f, maxOffsetPx)

        Surface(
            modifier = Modifier
                .onSizeChanged { barHeightPx = it.height.toFloat() }
                .offset { IntOffset(0, offsetYPx.roundToInt()) }
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 40.dp)
                .height(expandedHeight)
                .shadow(18.dp, RoundedCornerShape(28.dp), clip = false),
            shape = RoundedCornerShape(28.dp),
            color = Color.White
        ) {
            Box(modifier = Modifier.fillMaxSize()) {

                val pad16 = 16.dp * scale
                val pad14 = 14.dp * scale
                val h48 = 48.dp * scale
                val vPad19 = 16.dp * scale
                val searchTextLineHeight = (h48 - (vPad19 * 2))
                val gap20 = 20.dp * scale
                val bottomGap20 = 20.dp * scale
                val iconTextGap8 = 8.dp * scale

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(1f)
                        .padding(start = pad16, end = pad16, top = pad16, bottom = bottomGap20)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(h48)
                            .clip(RoundedCornerShape(999.dp))
                            .border(
                                BorderStroke(1.dp, Color(0xFFE7EBF2)),
                                RoundedCornerShape(999.dp)
                            )
                            .background(Color(0xFFF7FBFF))
                            .padding(horizontal = pad14),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = null,
                            tint = Color(0xFFAAADB3)
                        )
                        Spacer(Modifier.width(iconTextGap8))
                        Text(
                            text = "어디로 갈까요?",
                            color = Color(0xFFAAADB3),
                            fontSize = searchTextLineHeight.value.sp,
                            lineHeight = searchTextLineHeight.value.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(gap20))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp * scale),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Left half: center HOME between the big container's left edge and the report button
                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                BottomNavItem(
                                    icon = home.icon,
                                    label = home.label,
                                    selected = selectedRoute == home.route,
                                    onClick = {
                                        Log.d("BottomNavTap", "BottomNavItem tap route=${home.route}")
                                        onTabClick(home.route)
                                    },
                                    iconTextGap = iconTextGap8,
                                    scale = scale
                                )
                            }

                            ReportCenterButton(
                                onClick = {
                                    Log.d("BottomNavTap", "ReportCenterButton tap")
                                    onReportClick()
                                },
                                scale = scale
                            )

                            // Right half: center MY between the report button and the big container's right edge
                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                BottomNavItem(
                                    icon = my.icon,
                                    label = my.label,
                                    selected = selectedRoute == my.route,
                                    onClick = {
                                        Log.d("BottomNavTap", "BottomNavItem tap route=${my.route}")
                                        onTabClick(my.route)
                                    },
                                    iconTextGap = iconTextGap8,
                                    scale = scale
                                )
                            }
                        }
                    }
                }

                if (enableDragToHide) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .offset(y = (-8.dp * scale))
                            .padding(top = (5.dp * scale))
                            .fillMaxWidth()
                            .height(handleHeight)
                            .zIndex(2f)
                            .pointerInput(enableDragToHide, maxOffsetPx) {
                                detectVerticalDragGestures(
                                    onVerticalDrag = { change, dragAmount ->
                                        change.consume()
                                        offsetYPx = clamp(offsetYPx + dragAmount)
                                    },
                                    onDragEnd = {
                                        val shouldHide = offsetYPx > maxOffsetPx * 0.5f
                                        offsetYPx = if (shouldHide) maxOffsetPx else 0f
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        // Grabber (swipe handle)
                        Box(
                            modifier = Modifier
                                .size(width = 44.dp * scale, height = 5.dp * scale)
                                .clip(RoundedCornerShape(999.dp))
                                .background(Color(0xFFCBD5E1))
                        )
                    }
                }
            }
        }
    }
}

data class TabSpec(
    val route: String,
    val label: String,
    val icon: ImageVector
)

@Composable
private fun BottomNavItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    iconTextGap: Dp = 8.dp,
    scale: Float = 1f
) {
    val tint = if (selected) Color(0xFF252526) else Color(0xFFAAADB3)
    Column(
        modifier = Modifier
            .padding(horizontal = 8.dp)
            .clickable {
                Log.d("BottomNavTap", "BottomNavItem tap label=$label selected=$selected")
                onClick()
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(30.dp * scale)
        )
        Spacer(Modifier.height(iconTextGap))
        Text(
            text = label,
            color = tint,
            fontSize = 12.sp * scale,
            lineHeight = 14.sp * scale
        )
    }
}

@Composable
private fun ReportCenterButton(onClick: () -> Unit, scale: Float = 1f) {
    val shape = RoundedCornerShape(999.dp)
    Box(
        modifier = Modifier
            .size(width = 98.dp * scale, height = 56.dp * scale)
            .shadow(14.dp, shape, clip = false)
            .clip(shape)
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF8EC5FF), Color(0xFF4090E0))
                )
            )
            .clickable {
                Log.d("BottomNavTap", "ReportCenterButton tap")
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.Campaign,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(30.dp * scale)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun BottomNavBarPreview() {
    val home = TabSpec(route = "home", label = "홈", icon = Icons.Outlined.Home)
    val report = TabSpec(route = "report", label = "제보", icon = Icons.Outlined.Campaign)
    val my = TabSpec(route = "my", label = "마이", icon = Icons.Outlined.PersonOutline)

    FILLINTheme {
        BottomNavBar(
            selectedRoute = home.route,
            home = home,
            report = report,
            my = my,
            onTabClick = {},
            onReportClick = {},
            enableDragToHide = true
        )
    }
}



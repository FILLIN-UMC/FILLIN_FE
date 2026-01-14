package com.example.fillin.feature.myreports

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController
import com.example.fillin.ui.theme.FILLINTheme
import com.example.fillin.R

private enum class MyReportsTab { REGISTERED, EXPIRED }

data class MyReportUi(
    val id: Int,
    val badgeText: String, // "발견" / "불편" 등
    val badgeBg: Color,
    val viewCount: Int, // 좌측 상단 조회수
    val titleTop: String,  // "행복길 122-11"
    val titleBottom: String, // "가는길 255m"
    val placeName: String // 카드 아래 "붕어빵 가게"
)

@Composable
fun MyReportsScreen(navController: NavController) {
    var tab by remember { mutableStateOf(MyReportsTab.REGISTERED) }
    var editMode by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<MyReportUi?>(null) }

    // 예시 데이터(이미지처럼 2개만)
    val registered = remember {
        mutableStateListOf(
            MyReportUi(
                id = 1,
                badgeText = "발견",
                badgeBg = Color(0xFF29C488),
                viewCount = 5,
                titleTop = "행복길 122-11",
                titleBottom = "가는길 255m",
                placeName = "붕어빵 가게"
            ),
            MyReportUi(
                id = 2,
                badgeText = "불편",
                badgeBg = Color(0xFFF5C72F),
                viewCount = 5,
                titleTop = "행복길 122-11",
                titleBottom = "가는길 255m",
                placeName = "붕어빵 가게"
            )
        )
    }
    val expired = remember { mutableStateListOf<MyReportUi>() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
    ) {
        // Top bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 16.dp),
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_back_btn),
                contentDescription = "뒤로가기",
                modifier = Modifier
                    .size(48.dp)
                    .align(Alignment.CenterStart)
                    .clickable { navController.popBackStack() }
            )

            Text(
                text = "나의 제보",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 20.sp,
                color = Color(0xFF111827),
                modifier = Modifier.align(Alignment.Center)
            )

            Text(
                text = if (editMode) "완료" else "편집",
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = if (editMode) Color(0xFFFF6A6A) else Color(0xFF252526),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .clickable { editMode = !editMode }
            )
        }

        Spacer(Modifier.height(10.dp))

        // Tabs (등록된 제보 / 사라진 제보)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                TabText(
                    text = "등록된 제보",
                    selected = tab == MyReportsTab.REGISTERED,
                    onClick = { tab = MyReportsTab.REGISTERED }
                )
            }
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                TabText(
                    text = "사라진 제보",
                    selected = tab == MyReportsTab.EXPIRED,
                    onClick = { tab = MyReportsTab.EXPIRED }
                )
            }
        }

        // underline
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(Color(0xFFE7EBF2))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.5f)
                    .align(if (tab == MyReportsTab.REGISTERED) Alignment.CenterStart else Alignment.CenterEnd)
                    .background(Color(0xFF4595E5))
            )
        }

        Spacer(Modifier.height(16.dp))

        val list = if (tab == MyReportsTab.REGISTERED) registered else expired

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            items(list, key = { it.id }) { item ->
                MyReportGridItem(
                    item = item,
                    editMode = editMode,
                    onDeleteClick = { pendingDelete = item }
                )
            }
        }

        // Delete confirm dialog
        if (pendingDelete != null) {
            AlertDialog(
                onDismissRequest = { pendingDelete = null },
                title = {
                    Text(
                        text = "등록한 제보를 삭제할까요?",
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF111827)
                    )
                },
                text = {
                    Text(
                        text = "지도 및 마이페이지에서\n모두 삭제되며 복구할 수 없습니다.",
                        color = Color(0xFF6B7280),
                        fontSize = 14.sp,
                        lineHeight = 18.sp
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val target = pendingDelete
                            if (target != null) {
                                if (tab == MyReportsTab.REGISTERED) {
                                    // Remove from registered
                                    val removed = registered.removeAll { it.id == target.id }
                                    // If actually removed, add to expired so it appears under "사라진 제보"
                                    if (removed) {
                                        // add to the top (most recent)
                                        expired.add(0, target)
                                    }
                                } else {
                                    // If already in expired tab, remove permanently
                                    expired.removeAll { it.id == target.id }
                                }
                            }

                            pendingDelete = null

                            // If the current tab list becomes empty, exit edit mode
                            val currentListEmpty = if (tab == MyReportsTab.REGISTERED) registered.isEmpty() else expired.isEmpty()
                            if (currentListEmpty) editMode = false
                        }
                    ) {
                        Text(
                            text = "삭제",
                            color = Color(0xFFFF3B30),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { pendingDelete = null }) {
                        Text(
                            text = "취소",
                            color = Color(0xFF007AFF),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                },
                containerColor = Color.White,
                shape = RoundedCornerShape(12.dp)
            )
        }
    }
}

@Composable
private fun TabText(text: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text = text,
        fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
        color = if (selected) Color(0xFF4595E5) else Color(0xFFAAADB3),
        fontSize = 18.sp,
        modifier = Modifier.clickable { onClick() }
    )
}

@Composable
private fun MyReportGridItem(
    item: MyReportUi,
    editMode: Boolean,
    onDeleteClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // 카드
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFF111827),
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
        ) {
            Box(Modifier.fillMaxSize()) {
                Image(
                    painter = painterResource(id = R.drawable.ic_report_img),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                if (editMode) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFFFF6A6A).copy(alpha = 0.5f))
                    )
                }

                // top dark bar (gradient)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .align(Alignment.TopCenter)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.45f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                // view count (top-left)
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 10.dp, top = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_view),
                        contentDescription = "조회수",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = item.viewCount.toString(),
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        lineHeight = 12.sp
                    )
                }

                // badge
                Box(
                    modifier = Modifier
                        .padding(10.dp)
                        .align(Alignment.TopEnd)
                        .size(width = 45.dp, height = 28.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(item.badgeBg),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = item.badgeText,
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp
                    )
                }

                // bottom dark bar (gradient)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.55f)
                                )
                            )
                        )
                )

                // bottom text
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(10.dp)
                ) {
                    Text(
                        text = item.titleTop,
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        lineHeight = 14.sp
                    )
//                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = item.titleBottom,
                        color = Color(0xFFE5E7EB),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        lineHeight = 12.sp
                    )
                }

                if (editMode) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(44.dp)
                            .clickable { onDeleteClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_delete),
                            contentDescription = "삭제",
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Text(
            text = item.placeName,
            color = Color(0xFF252526),
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 6.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MyReportsScreenPreview() {
    FILLINTheme {
        MyReportsScreen(navController = rememberNavController())
    }
}

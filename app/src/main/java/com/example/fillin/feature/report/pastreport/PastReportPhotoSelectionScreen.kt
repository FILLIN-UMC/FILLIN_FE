package com.example.fillin.feature.report.pastreport

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fillin.R // R 클래스 임포트 확인

@Composable
fun PastReportPhotoSelectionScreen(
    onClose: () -> Unit,
    onPhotoSelected: (Uri) -> Unit
) {
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                onPhotoSelected(uri)
            }
        }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // 1. 상단 헤더 영역 (PastReportLocationScreen과 디자인 통일)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White,
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 8.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(
                        painter = painterResource(id = R.drawable.btn_close),
                        contentDescription = "닫기",
                        tint = Color.Unspecified
                    )
                }
                Text(
                    text = "지난 상황 제보",
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold, // SemiBold 적용
                        fontSize = 20.sp,                // 20sp 적용
                        letterSpacing = (-0.5).sp        // 자간 조정
                    )
                )
                // 아이콘 크기(24dp) + 패딩 등을 고려하여 균형을 맞추기 위한 Spacer
                Spacer(Modifier.size(48.dp))
            }
        }

        Spacer(modifier = Modifier.height(90.dp))

        // 2. 메인 콘텐츠 영역
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("제보할 사진을 추가해주세요!", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("사진만 등록하면 AI가 분석해 게시글을 작성해요", fontSize = 14.sp, color = Color.Gray)

            Spacer(modifier = Modifier.height(48.dp))

            Surface(
                modifier = Modifier
                    .size(220.dp)
                    .clickable {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFFF2F2F7)
            ) {
                Column(
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.AddPhotoAlternate,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = Color.Gray
                    )
                    Text("사진 추가+", color = Color.Gray)
                }
            }
            Spacer(modifier = Modifier.height(400.dp))
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PastReportPhotoSelectionScreenPreview() {
    MaterialTheme {
        PastReportPhotoSelectionScreen(
            onClose = { },
            onPhotoSelected = { }
        )
    }
}
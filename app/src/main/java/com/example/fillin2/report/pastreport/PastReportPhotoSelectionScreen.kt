package com.example.fillin2.report.pastreport

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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PastReportPhotoSelectionScreen(
    onClose: () -> Unit,
    onPhotoSelected: (Uri) -> Unit // 사진 선택 완료 시 호출될 콜백
) {
    // 1. 갤러리 실행을 위한 Launcher 선언
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            // 사용자가 사진을 선택하면 uri가 돌아옵니다.
            if (uri != null) {
                onPhotoSelected(uri)
            }
        }
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("지난 상황 제보", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "닫기")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color.White),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("제보할 사진을 추가해주세요!", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("사진만 등록하면 AI가 분석해 게시글을 작성해요", fontSize = 14.sp, color = Color.Gray)

            Spacer(modifier = Modifier.height(48.dp))

            // 2. 사진 추가 버튼 클릭 시 Launcher 실행
            Surface(
                modifier = Modifier
                    .size(220.dp)
                    .clickable {
                        // 이미지 파일만 필터링하여 갤러리 오픈
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
                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, Modifier.size(48.dp), tint = Color.Gray)
                    Text("사진 추가+", color = Color.Gray)
                }
            }
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}
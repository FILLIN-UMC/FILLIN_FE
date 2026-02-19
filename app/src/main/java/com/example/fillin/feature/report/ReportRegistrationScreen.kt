package com.example.fillin.feature.report

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import coil.compose.AsyncImage
import com.example.fillin.R

// 1. Stateful Container: ViewModel과 연결
@Composable
fun ReportRegistrationScreen(
    topBarTitle: String,
    viewModel: ReportViewModel,
    imageUri: Uri?,
    initialTitle: String,
    initialLocation: String,
    onLocationFieldClick: () -> Unit,
    onDismiss: () -> Unit,
    onRegister: (String, String, String, Uri) -> Unit
) {
    ReportRegistrationContent(
        topBarTitle = topBarTitle,
        processedImageUrl = viewModel.processedImageUrl,
        isProcessingImage = viewModel.isProcessingImage,
        imageUri = imageUri,
        initialTitle = initialTitle,
        initialLocation = initialLocation,
        onLocationFieldClick = onLocationFieldClick,
        onDismiss = onDismiss,
        onRegister = onRegister
    )
}

// 2. Stateless UI: 프리뷰 및 재사용 가능
@Composable
fun ReportRegistrationContent(
    topBarTitle: String,
    processedImageUrl: String?,
    isProcessingImage: Boolean,
    imageUri: Uri?,
    initialTitle: String,
    initialLocation: String,
    onLocationFieldClick: () -> Unit,
    onDismiss: () -> Unit,
    onRegister: (String, String, String, Uri) -> Unit
) {
    var selectedCategory by remember { mutableStateOf("위험") }
    var title by remember { mutableStateOf(initialTitle) }
    var location by remember { mutableStateOf(initialLocation) }

    LaunchedEffect(initialLocation) { location = initialLocation }

    val pureCharCount = title.count { !it.isWhitespace() }
    val isFormValid = title.isNotBlank() && location.isNotBlank()

    // 공통 디자인 사양
    val textFieldShape = RoundedCornerShape(38.dp)
    val textFieldColors = OutlinedTextFieldDefaults.colors(
        unfocusedContainerColor = colorResource(R.color.grey1),
        focusedContainerColor = colorResource(R.color.grey1),
        disabledContainerColor = colorResource(R.color.grey1),
        unfocusedBorderColor = colorResource(R.color.grey2),
        focusedBorderColor = colorResource(R.color.grey2),
        disabledBorderColor = colorResource(R.color.grey2),
        cursorColor = Color.Black
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // --- [상단 바] PastReportLocationScreen 디자인과 완벽 통일 ---
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
                IconButton(onClick = onDismiss) {
                    Icon(
                        painter = painterResource(id = R.drawable.btn_close),
                        contentDescription = "닫기",
                        tint = Color.Unspecified
                    )
                }
                Text(
                    text = topBarTitle,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 20.sp,
                        letterSpacing = (-0.5).sp
                    )
                )
                Spacer(Modifier.size(48.dp))
            }
        }

        // --- [메인 콘텐츠 영역] ---
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
        ) {
            Spacer(Modifier.height(12.dp))

            // 제보 사진 미리보기
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                AsyncImage(
                    model = processedImageUrl ?: imageUri,
                    contentDescription = null,
                    modifier = Modifier
                        .size(260.dp)
                        .clip(RoundedCornerShape(24.dp)),
                    contentScale = ContentScale.Crop
                )
                if (isProcessingImage) {
                    CircularProgressIndicator(modifier = Modifier.size(44.dp), color = Color.White)
                }
            }

            Spacer(Modifier.height(16.dp))

            // 1. 카테고리 선택 영역
            Text("카테고리", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
            Row(modifier = Modifier.padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CategorySelectChip("위험", Icons.Default.Warning, Color(0xFFE57373), selectedCategory == "위험") { selectedCategory = "위험" }
                CategorySelectChip("불편", Icons.Default.RemoveCircleOutline, Color(0xFFFFB74D), selectedCategory == "불편") { selectedCategory = "불편" }
                CategorySelectChip("발견", Icons.Default.Visibility, Color(0xFF4DB6AC), selectedCategory == "발견") { selectedCategory = "발견" }
            }

            Spacer(Modifier.height(24.dp))

            // 2. 제목 입력창 (곡률 38dp, Grey1/2, X 버튼 8dp 정렬)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start, verticalAlignment = Alignment.CenterVertically) {
                Text("제목", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.width(6.dp))
                Text("$pureCharCount/15자", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
            }
            OutlinedTextField(
                value = title,
                onValueChange = { if (it.length <= 15) title = it },
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).height(52.dp),
                placeholder = { Text("제목을 작성해 주세요.", color = Color.Gray) },
                trailingIcon = {
                    if (title.isNotEmpty()) {
                        IconButton(
                            onClick = { title = "" },
                            modifier = Modifier.offset(x = 4.dp) // 시각적으로 8dp 여백 확보
                        ) {
                            Icon(Icons.Default.Cancel, contentDescription = null, tint = Color.LightGray)
                        }
                    }
                },
                shape = textFieldShape,
                colors = textFieldColors,
                singleLine = true
            )

            Spacer(Modifier.height(16.dp))

            // 3. 장소 박스 (커스텀 Surface, 곡률 38dp, Grey1/2, X 버튼 8dp 정렬)
            Text("장소", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clickable { onLocationFieldClick() },
                shape = textFieldShape,
                color = colorResource(R.color.grey1),
                border = BorderStroke(1.dp, colorResource(R.color.grey2))
            ) {
                Row(
                    modifier = Modifier.padding(start = 16.dp, end = 0.dp), // Row 끝 패딩 0
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = Color(0xFF4090E0),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(4.dp)) // 아이콘-텍스트 간격 밀착
                    Text(
                        text = location.ifEmpty { "장소를 선택해 주세요." },
                        color = if (location.isEmpty()) Color.Gray else Color.Black,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp, fontWeight = FontWeight.Medium),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (location.isNotEmpty()) {
                        IconButton(
                            onClick = { location = "" },
                            modifier = Modifier.offset(x = 4.dp) // 시각적으로 8dp 여백 확보
                        ) {
                            Icon(Icons.Default.Cancel, null, tint = Color.LightGray)
                        }
                    } else {
                        Spacer(Modifier.width(16.dp)) // 버튼 없을 때의 여백 확보
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            // 하단 안내 문구 및 등록 버튼
            Text(
                text = "ⓘ AI가 분석한 내용은 사실과 다를 수 있어요",
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelMedium,
                color = Color.Gray
            )
            Button(
                onClick = { imageUri?.let { uri -> onRegister(selectedCategory, title, location, uri) } },
                modifier = Modifier.fillMaxWidth().height(53.dp).padding(bottom = 12.dp),
                enabled = isFormValid,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4090E0),
                    disabledContainerColor = Color(0xFFBDBDBD)
                ),
                shape = RoundedCornerShape(28.dp)
            ) {
                Text("등록하기", fontWeight = FontWeight.SemiBold, color = Color.White, fontSize = 18.sp)
            }
            Spacer(Modifier.height(35.dp))
        }
    }
}

@Composable
fun CategorySelectChip(text: String, icon: ImageVector, color: Color, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) color else Color.White,
        border = null,
        shadowElevation = if (isSelected) 0.dp else 2.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = if (isSelected) Color.White else color
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = text,
                color = if (isSelected) Color.White else Color.Black,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun ReportRegistrationScreenPreview() {
    MaterialTheme {
        ReportRegistrationContent(
            topBarTitle = "지난 상황 제보",
            processedImageUrl = null,
            isProcessingImage = false,
            imageUri = null,
            initialTitle = "도로 위 포트홀 발견",
            initialLocation = "서울시 용산구 행복대로 392",
            onLocationFieldClick = {},
            onDismiss = {},
            onRegister = { _, _, _, _ -> }
        )
    }
}